/*
 * Copyright 2026 Peter Michael Haller and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.hallerweb.enterprise.prioritize.client.invoker;

import com.sun.net.httpserver.HttpServer;
import de.hallerweb.enterprise.prioritize.client.api.ResourcesApi;
import de.hallerweb.enterprise.prioritize.client.model.ResourceDTO;
import de.hallerweb.enterprise.prioritize.client.model.ResourceRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that a PATCH call actually leaves the JVM. With the plain generated {@code ApiClient} this
 * fails with {@code java.net.ProtocolException: Invalid HTTP method: PATCH} before any request is sent,
 * because {@code HttpURLConnection} rejects the method (JDK-7016595).
 *
 * <p>Runs against a throwaway JDK {@link HttpServer} on a loopback port — no Prioritize instance needed.
 */
class PrioritizeApiClientTest {

    private HttpServer server;
    private final AtomicReference<String> observedMethod = new AtomicReference<>();
    private final AtomicReference<String> observedBody = new AtomicReference<>();

    @BeforeEach
    void startStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/resources/40", exchange -> {
            observedMethod.set(exchange.getRequestMethod());
            observedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] response = "{\"id\":40,\"name\":\"Bautrockner\",\"mqttOnline\":true}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void stopStubServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("PATCH reaches the server and the response is deserialized")
    void patchIsSentAndResponseIsRead() {
        ApiClient client = new PrioritizeApiClient();
        client.setBasePath("http://127.0.0.1:" + server.getAddress().getPort());

        ResourceDTO updated = new ResourcesApi(client)
                .resourcePartialUpdateResource(40L, new ResourceRequest().mqttOnline(true));

        assertEquals("PATCH", observedMethod.get());
        assertTrue(observedBody.get().contains("\"mqttOnline\":true"), observedBody.get());
        assertEquals(Long.valueOf(40L), updated.getId());
        assertEquals(Boolean.TRUE, updated.getMqttOnline());
    }
}
