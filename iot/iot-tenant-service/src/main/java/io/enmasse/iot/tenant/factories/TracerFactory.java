/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.factories;

import io.jaegertracing.thrift.sampling_manager.PerOperationSamplingStrategies;
import io.jaegertracing.thrift.sampling_manager.ProbabilisticSamplingStrategy;
import io.jaegertracing.thrift.sampling_manager.RateLimitingSamplingStrategy;
import io.jaegertracing.thrift.sampling_manager.SamplingStrategyResponse;
import io.jaegertracing.thrift.sampling_manager.SamplingStrategyType;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.noop.NoopTracerFactory;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.enterprise.context.Dependent;
import javax.inject.Singleton;
import java.util.Optional;

@RegisterForReflection(targets = {
        SamplingStrategyResponse.class,
        SamplingStrategyType.class,
        ProbabilisticSamplingStrategy.class,
        RateLimitingSamplingStrategy.class,
        PerOperationSamplingStrategies.class,
})
class TracerFixup {
}

@Dependent
public class TracerFactory {

    /**
     * Exposes an OpenTracing {@code Tracer} as a bean.
     * <p>
     * The Tracer will be resolved by means of a Java service lookup.
     * If no tracer can be resolved this way, the {@code NoopTracer} is
     * returned.
     *
     * @return The tracer.
     */
    @Singleton
    @DefaultBean
    public Tracer getTracer() {
        return Optional
                .ofNullable(TracerResolver.resolveTracer())
                .orElse(NoopTracerFactory.create());
    }

    @Singleton
    @IfBuildProfile("dev")
    Tracer noopTracer() {
        return NoopTracerFactory.create();
    }

}
