/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.geode.cache.wan.GatewayReceiver;
import org.apache.geode.cache.wan.GatewayTransportFilter;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean;
import org.springframework.mock.env.MockPropertySource;

/**
 * Tests for {@link EnableGatewayReceiver}.
 *
 * @author Udo Kohlmeyer
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.apache.geode.cache.server.CacheServer
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean
 * @see GatewayReceiverConfigurer
 * @see GatewayReceiverConfiguration
 * @since 2.2.0
 */
public class GatewayReceiverPropertiesTests {

    private ConfigurableApplicationContext applicationContext;

    @After
    public void tearDown() {
        Optional.ofNullable(this.applicationContext).ifPresent(ConfigurableApplicationContext::close);
    }

    private ConfigurableApplicationContext newApplicationContext(PropertySource<?> testPropertySource,
            Class<?>... annotatedClasses) {

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();

        propertySources.addFirst(testPropertySource);

        applicationContext.registerShutdownHook();
        applicationContext.register(annotatedClasses);
        applicationContext.refresh();

        return applicationContext;
    }

    @Test
    public void gatewayReceiverPropertiesConfiguration() {

        MockPropertySource testPropertySource = new MockPropertySource()
                .withProperty("spring.data.gemfire.gateway.receiver.bind-address", "123.123.123.123")
                .withProperty("spring.data.gemfire.gateway.receiver.hostname-for-senders", "testHostName")
                .withProperty("spring.data.gemfire.gateway.receiver.start-port", 16000)
                .withProperty("spring.data.gemfire.gateway.receiver.end-port", 17000)
                .withProperty("spring.data.gemfire.gateway.receiver.maximum-time-between-pings", 30000)
                .withProperty("spring.data.gemfire.gateway.receiver.socket-buffer-size", 32768)
                .withProperty("spring.data.gemfire.gateway.receiver.manual-start", true)
                .withProperty("spring.data.gemfire.gateway.receiver.transport-filters", "transportBean2,transportBean1");

        this.applicationContext = newApplicationContext(testPropertySource,
            GatewayReceiverPropertiesTests.TestConfigurationWithProperties.class);

        assertThat(this.applicationContext).isNotNull();
        assertThat(this.applicationContext.containsBean("GatewayReceiver")).isTrue();
        GatewayReceiver gatewayReceiver = this.applicationContext.getBean("GatewayReceiver",GatewayReceiver.class);

        assertThat(gatewayReceiver.getStartPort()).isEqualTo(16000);
        assertThat(gatewayReceiver.getEndPort()).isEqualTo(17000);
        assertThat(gatewayReceiver.getBindAddress()).isEqualTo("123.123.123.123");
        assertThat(gatewayReceiver.getHostnameForSenders()).isEqualTo("testHostName");
        assertThat(gatewayReceiver.getMaximumTimeBetweenPings()).isEqualTo(30000);
        assertThat(gatewayReceiver.getSocketBufferSize()).isEqualTo(32768);
        assertThat(gatewayReceiver.isManualStart()).isEqualTo(true);
        assertThat(gatewayReceiver.getGatewayTransportFilters().size()).isEqualTo(2);
        assertThat(((TestGatewayTransportFilter)gatewayReceiver.getGatewayTransportFilters().get(0)).name).isEqualTo("transportBean2");
        assertThat(((TestGatewayTransportFilter)gatewayReceiver.getGatewayTransportFilters().get(1)).name).isEqualTo("transportBean1");

    }

    @CacheServerApplication
    @EnableGatewayReceiver
    static class TestConfigurationWithProperties{

        @Bean("transportBean1")
        GatewayTransportFilter createGatewayTransportBean1() {
            return new GatewayReceiverPropertiesTests.TestGatewayTransportFilter("transportBean1");
        }

        @Bean("transportBean2")
        GatewayTransportFilter createGatewayTransportBean2() {
            return new GatewayReceiverPropertiesTests.TestGatewayTransportFilter("transportBean2");
        }

        @Bean("gatewayConfigurer")
        GatewayReceiverConfigurer gatewayReceiverConfigurer() {
            return new GatewayReceiverPropertiesTests.TestGatewayReceiverConfigurer();
        }
    }

    private static class TestGatewayTransportFilter implements GatewayTransportFilter {

        private String name;

        public TestGatewayTransportFilter(String name) {
            this.name = name;
        }

        @Override
        public InputStream getInputStream(InputStream inputStream) {
            return null;
        }

        @Override
        public OutputStream getOutputStream(OutputStream outputStream) {
            return null;
        }
    }

    private static class TestGatewayReceiverConfigurer implements GatewayReceiverConfigurer, Iterable<String> {

        private final List<String> beanNames = new ArrayList<>();

        @Override
        public Iterator<String> iterator() {
            return Collections.unmodifiableList(this.beanNames).iterator();
        }

        @Override
        public void configure(String beanName, GatewayReceiverFactoryBean bean) {
            bean.getTransportFilters().stream().forEach(o -> beanNames.add(((GatewayReceiverPropertiesTests.TestGatewayTransportFilter) o).name));
        }
    }
}
