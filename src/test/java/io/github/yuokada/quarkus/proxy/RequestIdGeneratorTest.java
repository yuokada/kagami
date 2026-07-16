package io.github.yuokada.quarkus.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdGeneratorTest {
    private static final String ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    @Test
    void ulidContainsTheCurrentTimestampInItsFirstTenCharacters() {
        RequestIdGenerator generator = new RequestIdGenerator();
        long before = System.currentTimeMillis();
        String ulid = generator.generateUlid();
        long after = System.currentTimeMillis();

        assertTrue(ulid.matches("[0-7][0-9a-hjkmnp-tv-z]{25}"));
        long timestamp = decodeTimestamp(ulid);
        assertTrue(timestamp >= before && timestamp <= after);
    }

    private long decodeTimestamp(String ulid) {
        long value = 0;
        for (int i = 0; i < 10; i++) {
            value = (value << 5) | ENCODING.indexOf(Character.toUpperCase(ulid.charAt(i)));
        }
        return value;
    }
}
