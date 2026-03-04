package io.github.yuokada.quarkus.proxy;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ProxyUpstreamTestResource.class)
class ProxyResourceTest {

    @BeforeEach
    void resetCounts() {
        ProxyUpstreamTestResource.resetCounts();
    }

    @Test
    void proxyReturnsMasterResponse() {
        RestAssured.given()
                .when()
                .get("/proxy-check")
                .then()
                .statusCode(200)
                .body(equalTo("{\"message\":\"master\"}"));

        assertEquals(1, ProxyUpstreamTestResource.masterCount());
        assertEquals(1, ProxyUpstreamTestResource.shadowCount());
    }

    @Test
    void shadowIsSkippedWhenHeaderPresent() {
        RestAssured.given()
                .header("X-Shadow", "true")
                .when()
                .get("/shadow-skip")
                .then()
                .statusCode(200);

        assertEquals(1, ProxyUpstreamTestResource.masterCount());
        assertEquals(0, ProxyUpstreamTestResource.shadowCount());
    }

    @Test
    void gzipResponsesAreDecodedForComparison() {
        String output = captureStdout(() ->
                RestAssured.given()
                        .when()
                        .get("/gzip")
                        .then()
                        .statusCode(200));

        assertEquals(1, ProxyUpstreamTestResource.masterCount());
        assertEquals(1, ProxyUpstreamTestResource.shadowCount());
        assertTrue(output.contains("\"path\":\"/gzip\""));
        assertTrue(output.contains("\"result\":\"DIFF\""));
    }

    @Test
    void largeBodiesAreReportedAsTooLarge() {
        String output = captureStdout(() ->
                RestAssured.given()
                        .when()
                        .get("/large")
                        .then()
                        .statusCode(200));

        assertEquals(1, ProxyUpstreamTestResource.masterCount());
        assertEquals(1, ProxyUpstreamTestResource.shadowCount());
        assertTrue(output.contains("\"path\":\"/large\""));
        assertTrue(output.contains("\"result\":\"TOO_LARGE\""));
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
