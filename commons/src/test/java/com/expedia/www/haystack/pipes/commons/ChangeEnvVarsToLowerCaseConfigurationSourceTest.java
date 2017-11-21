package com.expedia.www.haystack.pipes.commons;

import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.system.EnvironmentVariablesConfigurationSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.expedia.www.haystack.pipes.commons.ChangeEnvVarsToLowerCaseConfigurationSource.lowerCaseKeysThatStartWithPrefix;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ChangeEnvVarsToLowerCaseConfigurationSourceTest {
    private static final Environment ENVIRONMENT = new ImmutableEnvironment("");

    @Mock
    private EnvironmentVariablesConfigurationSource mockEnvironmentVariablesConfigurationSource;

    private int initCallCount = 1;
    private EnvironmentInfo ppes;
    private ChangeEnvVarsToLowerCaseConfigurationSource changeEnvVarsToLowerCaseConfigurationSource;

    @Before
    public void setUp() {
        ppes = getEnvironmentInfo();
        //noinspection ConstantConditions : pps will never be null
        changeEnvVarsToLowerCaseConfigurationSource =
                new ChangeEnvVarsToLowerCaseConfigurationSource(ppes.prefix, mockEnvironmentVariablesConfigurationSource);
    }

    @After
    public void tearDown() {
        verify(mockEnvironmentVariablesConfigurationSource, times(initCallCount)).init();

        Mockito.verifyNoMoreInteractions(mockEnvironmentVariablesConfigurationSource);
    }

    @Test
    public void testGetConfiguration() {
        final Properties copyOfCf4jProperties = new Properties();
        copyOfCf4jProperties.putAll(ppes.properties);
        Mockito.when(mockEnvironmentVariablesConfigurationSource.getConfiguration(ENVIRONMENT)).thenReturn(copyOfCf4jProperties);

        final Properties configuration = changeEnvVarsToLowerCaseConfigurationSource.getConfiguration(ENVIRONMENT);

        verify(mockEnvironmentVariablesConfigurationSource).getConfiguration(ENVIRONMENT);
        verify(mockEnvironmentVariablesConfigurationSource).init();
        assertLowerCaseKeyIsPresentInDestination(configuration);
        assertUpperCaseKeyIsStillPresentInDestination(ppes, configuration);
        assertSourceAndDestinationValuesAreEqual(ppes, configuration);
    }

    private void assertLowerCaseKeyIsPresentInDestination(Properties destination) {
        final String lowerCaseKey = ppes.entireString.toLowerCase().replace('_', '.');
        final String format = "Destination should contain %s; its keys are %s";
        final String failureMessage = String.format(format, lowerCaseKey, destination.keySet());
        assertTrue(failureMessage, destination.containsKey(lowerCaseKey));
    }

    private void assertUpperCaseKeyIsStillPresentInDestination(EnvironmentInfo ppes, Properties destination) {
        assertNotNull(destination.getProperty(ppes.entireString));
    }

    private void assertSourceAndDestinationValuesAreEqual(EnvironmentInfo ppes, Properties destination) {
        final String expected = ppes.properties.getProperty(ppes.entireString);
        final String lowerCaseKey = ppes.entireString.toLowerCase();
        final String actual = destination.getProperty(lowerCaseKey);
        assertEquals(expected, actual);
    }

    /**
     * EnvironmentInfo encapsulates data about the data that happens to be set in the environment variables of the host
     * where these unit tests are being run. Since environment variable keys are often upper case, the
     * ChangeEnvVarsToLowerCaseConfigurationSourceTest.testGetConfiguration() looks for any environment variable that
     * contains an upper case character and uses that environment variable to test the upper to lower case conversion
     * behavior of ChangeEnvVarsToLowerCaseConfigurationSource.getConfiguration(). If such a key is not found, the test
     * will fail, but this is highly unlikely. Such a failure could be addressed with reflection similar to the rather
     * ugly code in ProtobufToJsonTransformerTest.putKafkaPortIntoEnvironmentVariables(). (Addressing the failure would
     * require reflection to add an environment variable with an upper case key to the environment.)
     */
    private static class EnvironmentInfo {
        // The replacing of _ by . in properties is performed by cfg4j's EnvironmentVariablesConfigurationSource class
        private final Properties properties; // all environment variables and their values, with _ replaced by . in keys

        private final String prefix;         // prefix of environment variable that contains upper case in key
        private final String entireString;   // entire string of environment variable that contains upper case in key

        private EnvironmentInfo(Properties properties, String prefix, String entireString) {
            this.properties = properties;
            this.prefix = prefix;
            this.entireString = entireString;
        }
    }

    private Properties getPropertiesFromCfg4jEnvironmentVariablesConfigurationSource() {
        final EnvironmentVariablesConfigurationSource sourceToRetrieveProperties =
                new EnvironmentVariablesConfigurationSource();
        sourceToRetrieveProperties.init();
        return sourceToRetrieveProperties.getConfiguration(ENVIRONMENT);
    }

    private EnvironmentInfo getEnvironmentInfo() {
        final Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.putAll(System.getenv());
        for (final String key : environmentVariables.keySet()) {
            final Pattern pattern = Pattern.compile("([A-Z]+).*");
            final Matcher matcher = pattern.matcher(key);
            if(matcher.find()) {
                return new EnvironmentInfo(
                        getPropertiesFromCfg4jEnvironmentVariablesConfigurationSource(), matcher.group(), key);
            }
        }
        Assert.fail("An environment variable containing upper case letters could not be found");
        return null;
    }

    @Test
    public void testInit() {
        changeEnvVarsToLowerCaseConfigurationSource.init();

        initCallCount = 2;
    }

    @Test
    public void testToString() {
        final String actual = changeEnvVarsToLowerCaseConfigurationSource.toString();

        assertEquals(ChangeEnvVarsToLowerCaseConfigurationSource.class.getSimpleName() + "{}", actual);
    }

    @Test
    public void testReload() {
        changeEnvVarsToLowerCaseConfigurationSource.reload();

        verify(mockEnvironmentVariablesConfigurationSource).reload();
    }

    @Test
    public void testLowerCaseKeysThatStartWithPrefix() {
        final Properties properties = new Properties();
        final String prefix = "FOO";
        final String value1 = "1";
        final String matchingKey = prefix + value1;
        properties.put(matchingKey, value1);
        final String value2 = "2";
        final String nonMatchingKey = "foo";
        properties.put(nonMatchingKey, value2);

        final Properties actual = lowerCaseKeysThatStartWithPrefix(properties, prefix);

        assertEquals(3, actual.size());
        assertEquals(value1, actual.getProperty(matchingKey.toLowerCase()));
        assertEquals(value2, actual.getProperty(nonMatchingKey));
    }
}
