package io.github.yuokada.quarkus.proxy;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(TimeoutTestProfile.class)
@QuarkusTestResource(ProxyUpstreamTestResource.class)
class ProxyTimeoutTest {

    @BeforeEach
    void resetCounts() {
        ProxyUpstreamTestResource.resetCounts();
    }

    @Test
    void timeoutsAreReported() {
        String output = captureStdout(() ->
                RestAssured.given()
                        .when()
                        .get("/slow")
                        .then()
                        .statusCode(504));

        assertEquals(1, ProxyUpstreamTestResource.masterCount());
        assertEquals(1, ProxyUpstreamTestResource.shadowCount());
        assertTrue(output.contains("\"path\":\"/slow\""));
        assertTrue(output.contains("\"result\":\"TIMEOUT\""));
    }

    private String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }
}
