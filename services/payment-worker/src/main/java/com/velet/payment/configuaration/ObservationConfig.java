package com.velet.payment.configuaration;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

@Configuration
public class ObservationConfig {
    @Bean
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }

    /**
     * Micrometer context (trace) only work on 1 thread
     * this config enable to delegate the context to another thread
     * allow to process async task with the same origin trace context
     * @return
     */
    @Bean
    public TaskDecorator contextPropagatingTaskDecorator() {
        ContextSnapshotFactory factory = ContextSnapshotFactory.builder().build();
        return runnable -> {
            ContextSnapshot snapshot = factory.captureAll();
            return () -> {
                try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                    runnable.run();
                }
            };
        };
    }
}
