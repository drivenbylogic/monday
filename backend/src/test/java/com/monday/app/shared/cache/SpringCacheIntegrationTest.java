package com.monday.app.shared.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SpringCacheIntegrationTest {

    static class SampleService {
        private final AtomicInteger executionCount = new AtomicInteger(0);

        @Cacheable(value = CacheNames.HOME_DASHBOARD, key = "#param")
        public String computeExpensiveData(String param) {
            executionCount.incrementAndGet();
            return "computed:" + param;
        }

        public int getExecutionCount() {
            return executionCount.get();
        }
    }

    @Configuration
    @EnableCaching
    static class TestConfig extends CacheConfig {
        @Bean
        public SampleService sampleService() {
            return new SampleService();
        }
    }

    @Test
    @DisplayName("Second identical call returns cached result without re-executing service method")
    void cacheable_shouldReturnCachedResultOnSecondCall() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
        SampleService service = context.getBean(SampleService.class);

        // First call - executes service
        String res1 = service.computeExpensiveData("test-key");
        assertThat(res1).isEqualTo("computed:test-key");
        assertThat(service.getExecutionCount()).isEqualTo(1);

        // Second call with same parameter - must hit cache and NOT increment executionCount
        String res2 = service.computeExpensiveData("test-key");
        assertThat(res2).isEqualTo("computed:test-key");
        assertThat(service.getExecutionCount()).isEqualTo(1);

        // Third call with different parameter - executes service once
        String res3 = service.computeExpensiveData("other-key");
        assertThat(res3).isEqualTo("computed:other-key");
        assertThat(service.getExecutionCount()).isEqualTo(2);

        context.close();
    }
}

