package io.github.yuokada.quarkus.proxy;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(TimeoutTestProfile.class)
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

    @Test
    void emptyBodyRedirectIsForwarded() {
        RestAssured.given()
                .redirects().follow(false)
                .when()
                .get("/redirect")
                .then()
                .statusCode(303)
                .header("Location", notNullValue());

        assertEquals(1, ProxyUpstreamTestResource.masterCount());
        assertEquals(1, ProxyUpstreamTestResource.shadowCount());
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
