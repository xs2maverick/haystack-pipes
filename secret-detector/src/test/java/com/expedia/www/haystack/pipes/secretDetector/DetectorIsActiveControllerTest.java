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

import com.expedia.www.haystack.pipes.commons.kafka.Main;
import com.expedia.www.haystack.pipes.secretDetector.DetectorIsActiveController.Factory;
import com.expedia.www.haystack.pipes.secretDetector.config.ActionsConfigurationProvider;
import com.expedia.www.haystack.pipes.secretDetector.mains.ProtobufToDetectorAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;

import static com.expedia.www.haystack.pipes.commons.test.TestConstantsAndCommonCode.RANDOM;
import static com.expedia.www.haystack.pipes.secretDetector.DetectorIsActiveController.STARTUP_MSG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DetectorIsActiveControllerTest {
    private static final String MAIN_CLASS = RANDOM.nextLong() + "MAIN_CLASS";
    @Mock
    private Factory mockFactory;
    @Mock
    private Logger mockLogger;
    @Mock
    private SpringApplication mockSpringApplication;
    @Mock
    private ProtobufToDetectorAction mockProtobufToDetectorAction;
    @Mock
    private ActionsConfigurationProvider mockActionsConfigurationProvider;
    @Mock
    private AnnotationConfigApplicationContext mockAnnotationConfigApplicationContext;
    @Mock
    private Main mockMainBean;

    private Factory factory;

    @Before
    public void setUp() {
        DetectorIsActiveController.INSTANCE.set(null);
        storeKafkaProducerIsActiveControllerWithMocksInStaticInstance();
        factory = new Factory();
    }

    private void storeKafkaProducerIsActiveControllerWithMocksInStaticInstance() {
        new DetectorIsActiveController(mockFactory, mockLogger, mockActionsConfigurationProvider);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockFactory, mockLogger, mockSpringApplication, mockProtobufToDetectorAction,
                mockActionsConfigurationProvider, mockAnnotationConfigApplicationContext, mockMainBean);
        clearKafkaProducerIsActiveControllerInStaticInstance();
    }

    private void clearKafkaProducerIsActiveControllerInStaticInstance() {
        DetectorIsActiveController.INSTANCE.set(null);
    }

    @Test
    public void testMain() {
        final String beanName = "detectorProducer";
        when(mockActionsConfigurationProvider.mainbean()).thenReturn(beanName);
        when(mockFactory.createSpringApplication()).thenReturn(mockSpringApplication);
        when(mockFactory.createBean(any(AnnotationConfigApplicationContext.class), anyString()))
                .thenReturn(mockProtobufToDetectorAction);

        final String[] args = new String[0];
        DetectorIsActiveController.main(args);

        verify(mockLogger).info(STARTUP_MSG);
        verify(mockFactory).createSpringApplication();
        verify(mockActionsConfigurationProvider).mainbean();
        verify(mockFactory).createBean(any(AnnotationConfigApplicationContext.class), eq(beanName));
        verify(mockProtobufToDetectorAction).main();
        verify(mockSpringApplication).run(args);
    }

    @Test
    public void testFactoryCreateSpringApplication() {
        final SpringApplication springApplication = factory.createSpringApplication();

        final Set<Object> sources = springApplication.getSources();
        assertEquals(1, sources.size());
        final Object[] objects = sources.toArray();
        assertSame(DetectorIsActiveController.class, objects[0]);
    }

    @Test
    public void testFactoryCreateBean() {
        when(mockAnnotationConfigApplicationContext.getBean(anyString())).thenReturn(mockMainBean);

        assertSame(mockMainBean, factory.createBean(mockAnnotationConfigApplicationContext, MAIN_CLASS));

        verify(mockAnnotationConfigApplicationContext).getBean(MAIN_CLASS);
    }
}
