/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.www.haystack.pipes.secretDetector;

import com.expedia.open.tracing.Log;
import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.pipes.secretDetector.actions.EmailerDetectedAction;
import com.google.common.annotations.VisibleForTesting;
import com.netflix.servo.monitor.Counter;
import io.dataapps.chlorine.finder.FinderEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.expedia.www.haystack.pipes.secretDetector.Constants.APPLICATION;

/**
 * Finds that tag keys and field keys in a Span that contain secrets.
 */
public class Detector implements ValueMapper<Span, Iterable<String>> {
    @VisibleForTesting
    static final Set<String> FINDERS_TO_LOG = Collections.singleton("Credit_Card");
    @VisibleForTesting
    static final String ERRORS_METRIC_GROUP = "errors";
    @VisibleForTesting
    static final Map<FinderNameAndServiceName, Counter> COUNTERS = Collections.synchronizedMap(new HashMap<>());
    @VisibleForTesting
    static final String COUNTER_NAME = "SECRETS";
    private static final Map<String, Map<String, FinderNameAndServiceName>> CACHED_FINDER_NAME_AND_SECRET_NAME_OBJECTS =
            new ConcurrentHashMap<>();
    private final FinderEngine finderEngine;
    private final Logger logger;
    private final Factory factory;
    private final S3ConfigFetcher s3ConfigFetcher;

    public Detector(Logger detectorLogger,
                    FinderEngine finderEngine,
                    Factory detectorFactory,
                    S3ConfigFetcher s3ConfigFetcher) {
        this.logger = detectorLogger;
        this.finderEngine = finderEngine;
        this.factory = detectorFactory;
        this.s3ConfigFetcher = s3ConfigFetcher;
    }

    @VisibleForTesting
    Map<String, List<String>> findSecrets(Span span) {
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = new HashMap<>();
        findSecretsInTags(mapOfTypeToKeysOfSecrets, span);
        findSecretsInLogFields(mapOfTypeToKeysOfSecrets, span);
        return mapOfTypeToKeysOfSecrets;
    }

    private void findSecretsInTags(Map<String, List<String>> mapOfTypeToKeysOfSecrets, Span span) {
        findSecrets(mapOfTypeToKeysOfSecrets, span.getTagsList());
    }

    private void findSecretsInLogFields(Map<String, List<String>> mapOfTypeToKeysOfSecrets, Span span) {
        for (final Log log : span.getLogsList()) {
            findSecrets(mapOfTypeToKeysOfSecrets, log.getFieldsList());
        }
    }

    private void findSecrets(Map<String, List<String>> mapOfTypeToKeysOfSecrets, List<Tag> tags) {
        for (final Tag tag : tags) {
            if (StringUtils.isNotEmpty(tag.getVStr())) {
                putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, tag, finderEngine.findWithType(tag.getVStr()));
            } else if (tag.getVBytes().size() > 0) {
                final String input = new String(tag.getVBytes().toByteArray());
                putKeysOfSecretsIntoMap(mapOfTypeToKeysOfSecrets, tag, finderEngine.findWithType(input));
            }
        }
    }

    private void putKeysOfSecretsIntoMap(Map<String, List<String>> mapOfTypeToKeysOfSecrets,
                                         Tag tag,
                                         Map<String, List<String>> mapOfTypeToKeysOfSecretsJustFound) {
        for (final String finderName : mapOfTypeToKeysOfSecretsJustFound.keySet()) {
            mapOfTypeToKeysOfSecrets.computeIfAbsent(finderName, (l -> new ArrayList<>())).add(tag.getKey());
        }
    }

    @Override
    public Iterable<String> apply(Span span) {
        final Map<String, List<String>> mapOfTypeToKeysOfSecrets = findSecrets(span);
        final String serviceName = span.getServiceName();
        final String operationName = span.getOperationName();
        if (mapOfTypeToKeysOfSecrets.isEmpty()) {
            return Collections.emptyList();
        }
        final String emailText = EmailerDetectedAction.getEmailText(span, mapOfTypeToKeysOfSecrets);
        for (Map.Entry<String, List<String>> finderNameToKeysOfSecrets : mapOfTypeToKeysOfSecrets.entrySet()) {
            final String finderName = finderNameToKeysOfSecrets.getKey();
            final List<String> keysOfSecrets = finderNameToKeysOfSecrets.getValue();
            for (int index = 0; index < keysOfSecrets.size(); index++) {
                final String tagName = keysOfSecrets.get(index);
                if (s3ConfigFetcher.isTagInWhiteList(finderName, serviceName, operationName, tagName)) {
                    keysOfSecrets.remove(index);
                }
            }
            if (!keysOfSecrets.isEmpty() && FINDERS_TO_LOG.contains(finderName)) {
                logger.info(emailText);
                incrementCounter(serviceName, finderName);
            }
        }

        return Collections.singleton(emailText);
    }

    private void incrementCounter(String serviceName, String finderName) {
        final FinderNameAndServiceName finderNameAndServiceName = CACHED_FINDER_NAME_AND_SECRET_NAME_OBJECTS
                .computeIfAbsent(finderName, (v -> new HashMap<>()))
                .computeIfAbsent(serviceName, (v -> new FinderNameAndServiceName(finderName, serviceName)));
        COUNTERS.computeIfAbsent(finderNameAndServiceName, (c -> factory.createCounter(finderNameAndServiceName)))
                .increment();
    }

    public static class Factory {
        private final MetricObjects metricObjects;

        public Factory(MetricObjects metricObjects) {
            this.metricObjects = metricObjects;
        }

        Counter createCounter(FinderNameAndServiceName finderAndServiceName) {
            return metricObjects.createAndRegisterResettingCounter(ERRORS_METRIC_GROUP, APPLICATION,
                    finderAndServiceName.getFinderName(), finderAndServiceName.getServiceName(), COUNTER_NAME);
        }
    }
}