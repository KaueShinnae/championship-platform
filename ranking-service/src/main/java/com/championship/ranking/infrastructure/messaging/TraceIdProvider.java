package com.championship.ranking.infrastructure.messaging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {

    private final ObjectProvider<Tracer> tracer;

    public TraceIdProvider(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    public String currentTraceId() {
        Tracer available = tracer.getIfAvailable();
        if (available == null) {
            return null;
        }
        Span span = available.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}
