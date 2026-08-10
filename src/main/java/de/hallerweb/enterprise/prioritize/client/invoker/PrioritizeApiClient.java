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

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * An {@link ApiClient} that can actually speak {@code PATCH} — use this one.
 *
 * <p>The generated {@code ApiClient} builds a plain {@code new RestTemplate()}, which always transports
 * over {@code SimpleClientHttpRequestFactory} and therefore {@code java.net.HttpURLConnection}. That class
 * has never supported {@code PATCH} (JDK-7016595), so every call to a partial-update endpoint dies before
 * it reaches the server:
 *
 * <pre>
 * org.springframework.web.client.ResourceAccessException: I/O error on PATCH request for
 *   http://localhost:8080/api/v1/resources/40: Invalid HTTP method: PATCH
 * Caused by: java.net.ProtocolException: Invalid HTTP method: PATCH
 * </pre>
 *
 * <p>This subclass swaps the transport for {@link JdkClientHttpRequestFactory} (Spring's adapter for the
 * JDK 11+ {@code java.net.http.HttpClient}), which has no such method restriction. It needs no additional
 * dependency — {@code spring-web} already ships it — and it keeps the buffering wrapper as well as the
 * URI encoding configuration of the generated client, so it behaves identically in every other respect.
 *
 * <p>The affected endpoints are all the {@code *PartialUpdate*} operations: resources, users,
 * telemetry rules and task schedules. Everything else works with the plain {@code ApiClient} too — but
 * there is no reason not to use this one throughout:
 *
 * <pre>{@code
 * ApiClient client = new PrioritizeApiClient();
 * client.setBasePath("http://localhost:8080");
 * client.setUsername("admin");
 * client.setPassword("p@ssword");
 *
 * ResourceRequest patch = new ResourceRequest().mqttOnline(true);
 * new ResourcesApi(client).resourcePartialUpdateResource(40L, patch);
 * }</pre>
 *
 * <p>This class is hand-written on purpose. It is the one thing in this project that is not generated,
 * because the generator offers no knob for the request factory — but it only overrides the factory and
 * touches no generated source, so a regeneration for the next API release leaves it untouched.
 *
 * @see ApiClient
 */
public class PrioritizeApiClient extends ApiClient {

    /**
     * Builds the {@link RestTemplate} of the generated client and then replaces its transport with one
     * that supports {@code PATCH}. Called from the {@link ApiClient} constructor.
     *
     * @return the configured RestTemplate
     */
    @Override
    protected RestTemplate buildRestTemplate() {
        RestTemplate restTemplate = super.buildRestTemplate();
        // Buffering is kept for the same reason the generated client wants it: a response body that can
        // be read more than once (error handling, debugging, logging interceptors).
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(new JdkClientHttpRequestFactory()));
        return restTemplate;
    }
}
