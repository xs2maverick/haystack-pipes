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

import com.expedia.www.haystack.commons.secretDetector.span.SpanDetector;
import com.expedia.www.haystack.commons.secretDetector.span.SpanNameAndCountRecorder;
import com.expedia.www.haystack.commons.secretDetector.span.SpanS3ConfigFetcher;
import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.pipes.commons.CountersAndTimer;
import com.expedia.www.haystack.pipes.commons.health.HealthController;
import com.expedia.www.haystack.pipes.commons.health.HealthStatusListener;
import com.expedia.www.haystack.pipes.commons.kafka.KafkaStreamStarter;
import com.expedia.www.haystack.pipes.secretDetector.actions.EmailerDetectedAction;
import com.expedia.www.haystack.pipes.secretDetector.config.ActionsConfigurationProvider;
import com.expedia.www.haystack.pipes.secretDetector.mains.ProtobufToDetectorAction;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Timer;
import io.dataapps.chlorine.finder.FinderEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static com.expedia.www.haystack.pipes.commons.CommonConstants.SUBSYSTEM;
import static com.expedia.www.haystack.pipes.commons.health.HealthController.HealthStatus.HEALTHY;
import static com.expedia.www.haystack.pipes.secretDetector.Constants.APPLICATION;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringConfigTest {
    @Mock
    private MetricObjects mockMetricObjects;
    @Mock
    private Counter mockCounter;
    @Mock
    private Timer mockTimer;
    @Mock
    private HealthController mockHealthController;
    @Mock
    private HealthStatusListener mockHealthStatusListener;
    @Mock
    private CountersAndTimer mockCountersAndTimer;
    @Mock
    private SpanDetector mockSpanDetector;
    @Mock
    private Logger mockLogger;
    @Mock
    private FinderEngine mockFinderEngine;
    @Mock
    private ActionsConfigurationProvider mockActionsConfigurationProvider;
    @Mock
    private SpanDetector.Factory mockSpanDetectorFactory;
    @Mock
    private SpanS3ConfigFetcher mockSpanS3ConfigFetcher;

    private SpringConfig springConfig;

    @Before
    public void setUp() {
        springConfig = new SpringConfig(mockMetricObjects);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockMetricObjects, mockCounter, mockTimer, mockHealthController,
                mockHealthStatusListener, mockCountersAndTimer, mockSpanDetector, mockLogger, mockFinderEngine,
                mockActionsConfigurationProvider, mockSpanDetectorFactory);
    }

    @Test
    public void testDetectorIsActiveControllerLogger() {
        final Logger logger = springConfig.detectorIsActiveControllerLogger();

        assertEquals(DetectorIsActiveController.class.getName(), logger.getName());
    }

    @Test
    public void testDetectorActionLogger() {
        final Logger logger = springConfig.detectorActionLogger();

        assertEquals(DetectorAction.class.getName(), logger.getName());
    }

    @Test
    public void testEmailerDetectedActionLogger() {
        final Logger logger = springConfig.emailerDetectedActionLogger();

        assertEquals(EmailerDetectedAction.class.getName(), logger.getName());
    }

    @Test
    public void testDetectorLogger() {
        final Logger logger = springConfig.detectorLogger();

        assertEquals(SpanDetector.class.getName(), logger.getName());
    }

    @Test
    public void testSpanS3ConfigFetcherLogger() {
        final Logger logger = springConfig.spanS3ConfigFetcherLogger();

        assertEquals(SpanS3ConfigFetcher.class.getName(), logger.getName());
    }

    @Test
    public void testSpanNameAndCountRecorderLogger() {
        final Logger logger = springConfig.spanNameAndCountRecorderLogger();

        assertEquals(SpanNameAndCountRecorder.class.getName(), logger.getName());
    }

    @Test
    public void testKafkaStreamStarter() {
        final KafkaStreamStarter kafkaStreamStarter = springConfig.kafkaStreamStarter(mockHealthController);

        assertSame(ProtobufToDetectorAction.class, kafkaStreamStarter.containingClass);
        assertSame(APPLICATION, kafkaStreamStarter.clientId);
    }

    @Test
    public void testHealthController() {
        final HealthController healthController = springConfig.healthController(mockHealthStatusListener);

        healthController.setHealthy();
        verify(mockHealthStatusListener).onChange(HEALTHY);
    }

    @Test
    public void testHealthStatusListener() {
        assertNotNull(springConfig.healthStatusListener());
    }

    @Test
    public void testDetectorActionRequestCounter() {
        when(mockMetricObjects.createAndRegisterResettingCounter(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockCounter);

        assertNotNull(springConfig.detectorActionRequestCounter());

        verify(mockMetricObjects).createAndRegisterResettingCounter(SUBSYSTEM, APPLICATION,
                DetectorAction.class.getSimpleName(), "DETECTOR_SPAN");
    }

    @Test
    public void testDetectorDetectTimer() {
        when(mockMetricObjects.createAndRegisterBasicTimer(
                anyString(), anyString(), anyString(), anyString(), any(TimeUnit.class)))
                .thenReturn(mockTimer);

        assertNotNull(springConfig.detectorDetectTimer());

        verify(mockMetricObjects).createAndRegisterBasicTimer(SUBSYSTEM, APPLICATION,
                DetectorAction.class.getSimpleName(), "DETECTOR_DETECT", MICROSECONDS);
    }

    @Test
    public void testCountersAndTimer() {
        assertNotNull(springConfig.countersAndTimer(mockCounter, mockTimer));
    }

    @Test
    public void testDetectorAction() {
        assertNotNull(springConfig.detectorAction(
                mockCountersAndTimer, mockSpanDetector, mockLogger, mockActionsConfigurationProvider));
    }

    @Test
    public void testDetector() {
        assertNotNull(springConfig.spanDetector(mockLogger, mockFinderEngine, mockSpanDetectorFactory, mockSpanS3ConfigFetcher));
    }

    @Test
    public void testSerdeFactory() {
        assertNotNull(springConfig.serdeFactory());
    }

    @Test
    public void testDetectorIsActiveControllerFactory() {
        assertNotNull(springConfig.detectorIsActiveControllerFactory());
    }

    @Test
    public void testFinderEngine() {
        assertNotNull(springConfig.finderEngine());
    }

    @Test
    public void testEmailerDetectedActionFactory() {
        assertNotNull(springConfig.emailerDetectedActionFactory());
    }
}
