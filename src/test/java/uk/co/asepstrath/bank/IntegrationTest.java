package uk.co.asepstrath.bank;

import io.jooby.test.JoobyTest;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JoobyTest(App.class)
class IntegrationTest {

    @Test
    void mainPageShouldReturnOk(int serverPort) {
        int status = Unirest.get("http://localhost:" + serverPort + "/").asString().getStatus();
        assertEquals(200, status);
    }

    @Test
    void investorsApiShouldReturnData(int serverPort) {
        // Wait up to 5 seconds for the background sync to finish
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = Unirest.get("http://localhost:" + serverPort + "/api/investors").asString();

            assertEquals(200, response.getStatus());
            // Now check if the body actually contains more than []
            assertTrue(response.getBody().length() > 2);
        });
    }

    @Test
    void pricesApiShouldReturnData(int serverPort) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = Unirest.get("http://localhost:" + serverPort + "/api/prices/SMLX").asString();

            assertEquals(200, response.getStatus(), "Failed with body: " + response.getBody());
            assertTrue(response.getBody().length() > 2);
        });
    }

    @Test
    void transactionsApiShouldReturnData(int serverPort) {
        String testID = "00000000-0000-0000-aa61-6abe17610c9a";

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = Unirest.get("http://localhost:" + serverPort + "/api/investors/" + testID + "/transactions").asString();
            assertEquals(200, response.getStatus());
            assertTrue(response.getBody().length() > 2);
        });
    }

    @Test
    void portfolioApiShouldReturnData(int serverPort) {
        String testID = "00000000-0000-0000-aa61-6abe17610c9a";

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = Unirest.get("http://localhost:" + serverPort + "/api/investors/" + testID + "/portfolio").asString();

            assertEquals(200, response.getStatus());
            assertTrue(response.getBody().length() > 2);
        });
    }

}