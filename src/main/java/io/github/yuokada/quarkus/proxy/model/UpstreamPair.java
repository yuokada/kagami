package io.github.yuokada.quarkus.proxy.model;

public record UpstreamPair(UpstreamResponse master, UpstreamResponse shadow) {}
