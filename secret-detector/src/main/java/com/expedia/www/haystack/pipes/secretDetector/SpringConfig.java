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

import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.pipes.commons.CountersAndTimer;
import com.expedia.www.haystack.pipes.commons.health.HealthController;
import com.expedia.www.haystack.pipes.commons.health.HealthStatusListener;
import com.expedia.www.haystack.pipes.commons.health.UpdateHealthStatusFile;
import com.expedia.www.haystack.pipes.commons.kafka.KafkaConfigurationProvider;
import com.expedia.www.haystack.pipes.commons.kafka.KafkaStreamStarter;
import com.expedia.www.haystack.pipes.commons.serialization.SpanSerdeFactory;
import com.expedia.www.haystack.pipes.secretDetector.config.ActionsConfigurationProvider;
import com.expedia.www.haystack.pipes.secretDetector.actions.EmailerDetectedAction;
import com.expedia.www.haystack.pipes.secretDetector.actions.EmailerDetectedActionFactory;
import com.expedia.www.haystack.pipes.secretDetector.actions.SenderImpl;
import com.expedia.www.haystack.pipes.secretDetector.config.SecretsEmailConfigurationProvider;
import com.expedia.www.haystack.pipes.secretDetector.mains.ProtobufSpanToEmailInKafkaTransformer;
import com.expedia.www.haystack.pipes.secretDetector.mains.ProtobufToDetectorAction;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Timer;
import io.dataapps.chlorine.finder.FinderEngine;
import org.cfg4j.provider.ConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static com.expedia.www.haystack.pipes.commons.CommonConstants.SUBSYSTEM;
import static com.expedia.www.haystack.pipes.secretDetector.Constants.APPLICATION;

@Configuration
@ComponentScan(basePackageClasses = SpringConfig.class)
public class SpringConfig {

    private final MetricObjects metricObjects;

    /**
     * @param metricObjects provided by a static inner class that is loaded first
     * @see MetricObjectsSpringConfig
     */
    @Autowired
    SpringConfig(MetricObjects metricObjects) {
        this.metricObjects = metricObjects;
    }

    @Bean
    @Autowired
    ProtobufToDetectorAction detectorProducer(KafkaStreamStarter kafkaStreamStarter,
                                              SpanSerdeFactory spanSerdeFactory,
                                              DetectorAction detectorAction,
                                              KafkaConfigurationProvider kafkaConfigurationProvider) {
        return new ProtobufToDetectorAction(kafkaStreamStarter, spanSerdeFactory, detectorAction, kafkaConfigurationProvider);
    }

    @Bean
    @Autowired
    ProtobufSpanToEmailInKafkaTransformer protobufSpanToEmailInKafkaTransformer(KafkaStreamStarter kafkaStreamStarter,
                                                                                SpanSerdeFactory spanSerdeFactory,
                                                                                Detector detector) {
        return new ProtobufSpanToEmailInKafkaTransformer(kafkaStreamStarter, spanSerdeFactory, detector);
    }

    @Bean
    @Autowired
    DetectorIsActiveController detectorIsActiveController(DetectorIsActiveController.Factory detectorIsActiveControllerFactory,
                                                          Logger detectorIsActiveControllerLogger,
                                                          ActionsConfigurationProvider actionsConfigurationProvider) {
        return new DetectorIsActiveController(
                detectorIsActiveControllerFactory, detectorIsActiveControllerLogger, actionsConfigurationProvider);
    }

    @Bean
    Logger detectorIsActiveControllerLogger() {
        return LoggerFactory.getLogger(DetectorIsActiveController.class);
    }

    @Bean
    Logger detectorActionLogger() {
        return LoggerFactory.getLogger(DetectorAction.class);
    }

    @Bean
    Logger emailerDetectedActionLogger() {
        return LoggerFactory.getLogger(EmailerDetectedAction.class);
    }

    @Bean
    Logger detectorLogger() {
        return LoggerFactory.getLogger(Detector.class);
    }

    @Bean
    @Autowired
    KafkaStreamStarter kafkaStreamStarter(final HealthController healthController) {
        return new KafkaStreamStarter(ProtobufToDetectorAction.class, APPLICATION, healthController);
    }

    @Bean
    HealthStatusListener healthStatusListener() {
        return new UpdateHealthStatusFile("/app/isHealthy"); // TODO should come from config
    }

    @Bean
    @Autowired
    HealthController healthController(HealthStatusListener healthStatusListener) {
        final HealthController healthController = new HealthController();
        healthController.addListener(healthStatusListener);
        return healthController;
    }

    @Bean
    Counter detectorActionRequestCounter() {
        return metricObjects.createAndRegisterResettingCounter(SUBSYSTEM, APPLICATION,
                DetectorAction.class.getSimpleName(), "DETECTOR_SPAN");
    }

    @Bean
    Timer detectorDetectTimer() {
        return metricObjects.createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                DetectorAction.class.getSimpleName(), "DETECTOR_DETECT", TimeUnit.MICROSECONDS);
    }

    @Bean
    SpanSerdeFactory spanSerdeFactory() {
        return new SpanSerdeFactory();
    }

    @Bean
    DetectorIsActiveController.Factory detectorIsActiveControllerFactory() {
        return new DetectorIsActiveController.Factory();
    }

    @Bean
    @Autowired
    CountersAndTimer countersAndTimer(Counter detectorActionRequestCounter,
                                      Timer detectorDetectTimer) {
        return new CountersAndTimer(detectorDetectTimer, detectorActionRequestCounter);
    }

    @Bean
    FinderEngine finderEngine() {
        return new FinderEngine();
    }

    @Bean
    Detector.Factory detectorFactory(MetricObjects metricObjects) {
        return new Detector.Factory(metricObjects);
    }

    @Bean
    @Autowired
    Detector detector(Logger detectorLogger,
                      FinderEngine finderEngine,
                      Detector.Factory detectorFactory) {
        return new Detector(detectorLogger, finderEngine, detectorFactory);
    }

    @Bean
    @Autowired
    DetectorAction detectorAction(CountersAndTimer detectorDetectTimer,
                                  Detector detector,
                                  Logger detectorActionLogger,
                                  ActionsConfigurationProvider actionsConfigurationProvider) {
        return new DetectorAction(detectorDetectTimer, detector, detectorActionLogger, actionsConfigurationProvider);
    }

    @Bean
    EmailerDetectedAction.MimeMessageFactory emailerDetectedActionFactory() {
        return new EmailerDetectedAction.MimeMessageFactory();
    }

    @Bean
    EmailerDetectedAction.Sender sender() {
        return new SenderImpl();
    }

    @Bean
    EmailerDetectedAction.MimeMessageFactory mimeMessageFactory() {
        return new EmailerDetectedAction.MimeMessageFactory();
    }

    @Bean
    @Autowired
    EmailerDetectedActionFactory emailerDetectedActionFactory(EmailerDetectedAction.MimeMessageFactory mimeMessageFactory,
                                                              Logger emailerDetectedActionLogger,
                                                              EmailerDetectedAction.Sender sender,
                                                              SecretsEmailConfigurationProvider secretsEmailConfigurationProvider) {
        return new EmailerDetectedActionFactory(mimeMessageFactory, emailerDetectedActionLogger,
                sender, secretsEmailConfigurationProvider);
    }

    @Bean
    @Autowired
    EmailerDetectedAction emailerDetectedAction(EmailerDetectedAction.MimeMessageFactory mimeMessageFactory,
                                                Logger emailerDetectedActionLogger,
                                                EmailerDetectedAction.Sender sender,
                                                SecretsEmailConfigurationProvider secretsEmailConfigurationProvider) {
        return new EmailerDetectedAction(mimeMessageFactory,
                emailerDetectedActionLogger,
                sender,
                secretsEmailConfigurationProvider);
    }

    @Bean
    PhoneNumberUtil phoneNumberUtil() {
        return PhoneNumberUtil.getInstance();
    }

    /*
     * Spring loads this static inner class before loading the SpringConfig outer class so that its bean is available to
     * the outer class constructor.
     *
     * @see SpringConfig
     */
    @Configuration
    static class MetricObjectsSpringConfig {
        @Bean
        public MetricObjects metricObjects() {
            return new MetricObjects();
        }
    }

    /*
     * Spring loads this static inner class before loading the SpringConfig outer class so that its beans are available
     * to the outer class constructor.
     *
     * @see SpringConfig
     */
    @Configuration
    static class ActionsConfigurationProviderSpringConfig {
        @Bean
        Logger actionsConfigurationProviderLogger() {
            return LoggerFactory.getLogger(ActionsConfigurationProvider.class);
        }

        @Bean
        ConfigurationProvider configurationProvider() {
            final com.expedia.www.haystack.pipes.commons.Configuration configuration
                    = new com.expedia.www.haystack.pipes.commons.Configuration();
            return configuration.createMergeConfigurationProvider();
        }

        @Bean
        @Autowired
        ActionsConfigurationProvider actionsConfigurationProvider(Logger actionsConfigurationProviderLogger,
                                                                  ConfigurationProvider configurationProvider) {
            return new ActionsConfigurationProvider(actionsConfigurationProviderLogger, configurationProvider);
        }

        @Bean
        SecretsEmailConfigurationProvider secretsEmailConfigurationProvider() {
            return new SecretsEmailConfigurationProvider();
        }

        @Bean
        KafkaConfigurationProvider kafkaConfigurationProvider() {
            return new KafkaConfigurationProvider();
        }

    }
}
