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
package com.expedia.www.haystack.pipes.firehoseWriter;

import com.expedia.www.haystack.metrics.MetricObjects;
import com.expedia.www.haystack.pipes.commons.kafka.KafkaStreamStarter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.expedia.www.haystack.pipes.commons.CommonConstants.SUBSYSTEM;
import static com.expedia.www.haystack.pipes.firehoseWriter.Constants.APPLICATION;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class SpringConfigTest {
    @Mock
    private MetricObjects mockMetricObjects;

    private SpringConfig springConfig;

    @Before
    public void setUp() {
        springConfig = new SpringConfig(mockMetricObjects);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockMetricObjects);
    }

    /**
     * Test that the request counter is created with the appropriate arguments
     */
    @Test
    public void testRequestCounter() {
        springConfig.requestCounter();

        verify(mockMetricObjects).createAndRegisterResettingCounter(SUBSYSTEM, APPLICATION,
                FirehoseAction.class.getName(), "REQUEST");
    }

    /**
     * Test that the KafkaStreamStarter is created with the appropriate arguments
     */
    @Test
    public void testKafkaStreamStarter() {
        final KafkaStreamStarter kafkaStreamStarter = springConfig.kafkaStreamStarter();

        assertSame(ProtobufToFirehoseProducer.class, kafkaStreamStarter.containingClass);
        assertSame(APPLICATION, kafkaStreamStarter.clientId);
    }

    // All of the other beans in SpringConfig are default constructor calls, or use arguments provided by other Spring
    // beans, so no other tests are necessary, because Spring is started by the unit test
    // FirehoseIsActiveControllerTest.testMainCreatesApplicationContext() which verifies the Spring wiring is correct.
}
