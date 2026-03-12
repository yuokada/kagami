package io.github.yuokada.quarkus.proxy.logging;

import org.jboss.logging.MDC;

public final class LoggingContext {

    private LoggingContext() {}

    public static void startRequest(String requestId, String method, String path) {
        MDC.put(LoggingKeys.REQUEST_ID, requestId);
        MDC.put(LoggingKeys.METHOD, method);
        MDC.put(LoggingKeys.PATH, path);
    }

    public static void clear() {
        MDC.clear();
    }
}
