package com.championship.ranking.infrastructure.messaging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Trace id corrente (OpenTelemetry via Micrometer Tracing) para gravar junto
 * dos registros de auditoria (processed_events / outbox_event), ligando cada
 * evento persistido ao trace distribuído correspondente.
 */
@Component
public class TraceIdProvider {

    private final ObjectProvider<Tracer> tracer;

    public TraceIdProvider(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    /** Trace id atual, ou null quando não há span ativo (ex.: testes locais). */
    public String currentTraceId() {
        Tracer available = tracer.getIfAvailable();
        if (available == null) {
            return null;
        }
        Span span = available.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}
