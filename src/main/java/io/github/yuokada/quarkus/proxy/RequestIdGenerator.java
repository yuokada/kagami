package io.github.yuokada.quarkus.proxy;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class RequestIdGenerator {
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
