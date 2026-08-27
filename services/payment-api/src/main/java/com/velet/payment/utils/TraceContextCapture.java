package com.velet.payment.utils;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TraceContextCapture {
    private final Tracer tracer;
    private final Propagator propagator;

    public String captureTraceParent() {
        Map<String, String> carrier = new HashMap<>();

        // inject() method will write context to carrier follow W3C format
        propagator.inject(tracer.currentTraceContext().context(), carrier, Map::put);
        return carrier.get("traceparent");
    }
}