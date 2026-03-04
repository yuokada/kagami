package io.github.yuokada.quarkus.proxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class RequestIdGenerator {
    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int TIME_BYTES = 6;

    @Inject
    ProxyConfig proxyConfig;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        String type = proxyConfig.requestId().type();
        if (type != null && type.equalsIgnoreCase("ULID")) {
            return generateUlid();
        }
        return UUID.randomUUID().toString();
    }

    private String generateUlid() {
        byte[] data = new byte[16];
        long time = Instant.now().toEpochMilli();
        for (int i = TIME_BYTES - 1; i >= 0; i--) {
            data[i] = (byte) (time & 0xFF);
            time >>= 8;
        }
        byte[] random = new byte[10];
        secureRandom.nextBytes(random);
        System.arraycopy(random, 0, data, TIME_BYTES, random.length);
        return encodeBase32(data);
    }

    private String encodeBase32(byte[] data) {
        char[] chars = new char[26];
        int index = 0;
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int code = (buffer >> (bitsLeft - 5)) & 0x1F;
                chars[index++] = ENCODING[code];
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int code = (buffer << (5 - bitsLeft)) & 0x1F;
            chars[index] = ENCODING[code];
        }
        return new String(chars).toLowerCase(Locale.ROOT);
    }
}
