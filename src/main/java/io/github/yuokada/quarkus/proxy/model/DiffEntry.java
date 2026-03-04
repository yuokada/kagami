package io.github.yuokada.quarkus.proxy.model;

public record DiffEntry(String path, Object master, Object shadow) {}
