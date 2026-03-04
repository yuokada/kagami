package io.github.yuokada.quarkus.proxy.model;

import java.util.List;

public record ComparisonResult(Result result, List<DiffEntry> diffEntries, String errorMessage) {
    public enum Result {
        SAME,
        DIFF,
        TIMEOUT,
        TOO_LARGE,
        ERROR
    }
}
