package com.expedia.www.haystack.pipes.commons;

import org.cfg4j.provider.ConfigurationProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConfigurationTest {
    private final static String HAYSTACK_KAFKA_BROKERS = "haystack.kafka.brokers";
    private final static String HAYSTACK_KAFKA_FROM_TOPIC = "haystack.kafka.fromtopic";
    private final static String HAYSTACK_KAFKA_PORT = "haystack.kafka.port";
    private final static String HAYSTACK_KAFKA_TO_TOPIC = "haystack.kafka.totopic";
    private final static String HAYSTACK_PIPE_STREAMS_REPLICATION_FACTOR = "haystack.pipe.streams.replicationfactor";
    private final static String HAYSTACK_GRAPHITE_PREFIX = "haystack.graphite.prefix";
    private final static String HAYSTACK_GRAPHITE_ADDRESS = "haystack.graphite.address";
    private final static String HAYSTACK_GRAPHITE_PORT = "haystack.graphite.port";
    private final static String HAYSTACK_GRAPHITE_POLL_INTERVAL_SECONDS = "haystack.graphite.pollintervalseconds";
    private final static String HAYSTACK_GRAPHITE_QUEUE_SIZE = "haystack.graphite.queuesize";
    private final static Object[][] TEST_DATA = {
            { HAYSTACK_KAFKA_BROKERS, "localhost" },
            { HAYSTACK_KAFKA_FROM_TOPIC, HAYSTACK_KAFKA_FROM_TOPIC },
            { HAYSTACK_KAFKA_PORT, 65534 },
            { HAYSTACK_KAFKA_TO_TOPIC, HAYSTACK_KAFKA_TO_TOPIC },
            { HAYSTACK_PIPE_STREAMS_REPLICATION_FACTOR, 2147483645 },
            { HAYSTACK_GRAPHITE_PREFIX, HAYSTACK_GRAPHITE_PREFIX },
            { HAYSTACK_GRAPHITE_ADDRESS, HAYSTACK_GRAPHITE_ADDRESS },
            { HAYSTACK_GRAPHITE_PORT, 65535 },
            { HAYSTACK_GRAPHITE_POLL_INTERVAL_SECONDS, 2147483646 },
            { HAYSTACK_GRAPHITE_QUEUE_SIZE, 2147483647 },
    };
    private static final char UNDERSCORE = '_';
    private static final char PERIOD = '.';
    private Configuration configuration;

    @Before
    public void setUp() {
        configuration = new Configuration();
    }

    @Test
    public void testCreateMergeConfigurationProvider() {
        final ConfigurationProvider configurationProvider = configuration.createMergeConfigurationProvider();

        final Properties properties = configurationProvider.allConfigurationAsProperties();
        verifyExpectedFileConfigurationsFoundThenRemove(properties);
        verifyEnvironmentVariablesAreInConfigurationPropertiesThenRemove(properties);
        verifyConfigurationsAreNowEmpty(properties);
    }

    private void verifyExpectedFileConfigurationsFoundThenRemove(Properties properties) {
        for(Object[] objects : TEST_DATA) {
            assertEquals(objects[1], properties.remove(objects[0]));
        }
    }

    private void verifyEnvironmentVariablesAreInConfigurationPropertiesThenRemove(Properties properties) {
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            final String keyWithPeriodsInsteadOfUnderscores = entry.getKey().replace(UNDERSCORE, PERIOD);
            assertEquals(entry.getValue(), properties.remove(keyWithPeriodsInsteadOfUnderscores));
        }
    }

    private void verifyConfigurationsAreNowEmpty(Properties properties) {
        assertEquals(0, properties.size());
    }
}
