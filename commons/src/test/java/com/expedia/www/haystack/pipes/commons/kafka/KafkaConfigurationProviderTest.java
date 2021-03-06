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
package com.expedia.www.haystack.pipes.commons.kafka;

import org.junit.Before;
import org.junit.Test;

import static com.expedia.www.haystack.pipes.commons.ConfigurationTest.THREAD_COUNT_CONFIGURATION_IN_TEST_BASE_DOT_YAML;
import static org.junit.Assert.assertEquals;

public class KafkaConfigurationProviderTest {
    private KafkaConfigurationProvider kafkaConfigurationProvider;

    @Before
    public void setUp() {
        kafkaConfigurationProvider = new KafkaConfigurationProvider();
    }

    @Test
    public void testBrokers() {
        assertEquals("localhost", kafkaConfigurationProvider.brokers());
    }

    @Test
    public void testPort() {
        assertEquals(65534, kafkaConfigurationProvider.port());
    }

    @Test
    public void testFromTopic() {
        assertEquals("haystack.kafka.fromtopic", kafkaConfigurationProvider.fromtopic());
    }

    @Test
    public void testToTopic() {
        assertEquals("haystack.kafka.totopic", kafkaConfigurationProvider.totopic());
    }

    @Test
    public void testThreadCount() {
        assertEquals(THREAD_COUNT_CONFIGURATION_IN_TEST_BASE_DOT_YAML, kafkaConfigurationProvider.threadcount());
    }

    @Test
    public void testSessionTimeout() {
        assertEquals(15000, kafkaConfigurationProvider.sessiontimeout());
    }
}
