# Prioritize Java Client

Official Java client library for the [Prioritize](https://github.com/phaller222/Prioritize) REST API
(`/api/v1`). It is **generated** from Prioritize's frozen OpenAPI specification with
[OpenAPI Generator](https://openapi-generator.tech/) (`java` generator, `resttemplate` library) — the
sources are never hand-written, so the client always matches the contract of the release it targets.

- **Client version tracks the API version:** a `1.x` client targets the `1.x` REST API.
- **License:** Apache-2.0 (same as Prioritize).

## Requirements

- Java 17+
- The generated client uses Spring's `RestTemplate` and Jackson (pulled in transitively).

## Coordinates

Published on Maven Central:

```xml
<dependency>
    <groupId>de.hallerweb</groupId>
    <artifactId>prioritize-java-client</artifactId>
    <version>1.3.0</version>
</dependency>
```

The client version tracks the Prioritize API release it was generated from, so `1.3.0` speaks the
`1.3.0` contract.

## Usage

Create a `PrioritizeApiClient`, point it at your Prioritize instance, add credentials, then call one of
the per-resource API classes (one per tag: `UsersApi`, `ProjectsApi`, `TasksApi`, `ResourcesApi`,
`DocumentsApi`, `SkillsApi`, `ProcessDefinitionsApi`, …).

### Basic auth (default / dev profile)

```java
import de.hallerweb.enterprise.prioritize.client.invoker.ApiClient;
import de.hallerweb.enterprise.prioritize.client.invoker.PrioritizeApiClient;
import de.hallerweb.enterprise.prioritize.client.api.UsersApi;
import de.hallerweb.enterprise.prioritize.client.model.UserDTO;

ApiClient client = new PrioritizeApiClient();
client.setBasePath("http://localhost:8080"); // default; the spec paths already include /api/v1
client.setUsername("admin");
client.setPassword("p@ssword");

UsersApi users = new UsersApi(client);
UserDTO me = users.userGetByUsername("admin");
System.out.println(me.getId() + " " + me.getEmail());
```

### Bearer token (Keycloak profile)

```java
ApiClient client = new PrioritizeApiClient();
client.setBasePath("https://prioritize.example.com");
client.setBearerToken("<your-JWT-access-token>");
// or a supplier that refreshes the token:
// client.setBearerToken(() -> tokenProvider.currentAccessToken());

ProjectsApi projects = new ProjectsApi(client);
projects.projectGetMyProjects().forEach(p -> System.out.println(p.getName()));
```

Both auth schemes are declared in the spec; the app has exactly one active at a time depending on the
server profile (Basic on the default profile, Bearer/JWT on the `keycloak` profile).

### Why `PrioritizeApiClient` and not `ApiClient`?

Because otherwise every `PATCH` call fails before it leaves the JVM:

```
org.springframework.web.client.ResourceAccessException: I/O error on PATCH request for
  http://localhost:8080/api/v1/resources/40: Invalid HTTP method: PATCH
Caused by: java.net.ProtocolException: Invalid HTTP method: PATCH
```

The generated `ApiClient` builds a plain `new RestTemplate()`, which always transports over
`SimpleClientHttpRequestFactory` → `java.net.HttpURLConnection`, and that class has never supported
`PATCH` ([JDK-7016595](https://bugs.openjdk.org/browse/JDK-7016595)). `PrioritizeApiClient` is a
three-line subclass that swaps the transport for `JdkClientHttpRequestFactory` (Spring's adapter for
`java.net.http.HttpClient`, already part of `spring-web` — no extra dependency) and is otherwise
identical. It affects the `*PartialUpdate*` operations (resources, users, telemetry rules, task
schedules); everything else works either way, so just use `PrioritizeApiClient` everywhere.

If you build the `RestTemplate` yourself and pass it to `new ApiClient(restTemplate)`, note that this
constructor skips the generated setup entirely — you then have to configure a `DefaultUriBuilderFactory`
with `EncodingMode.VALUES_ONLY` yourself, or query parameters end up double-encoded.

## Building

```bash
mvn clean install
```

This regenerates the client from [`openapi/openapi.json`](openapi/openapi.json) into
`target/generated-sources/openapi` and compiles it. Apart from the single hand-written
`PrioritizeApiClient` (see above) there is nothing to hand-edit.

## Updating to a new API release

The client is spec-first. To move it to a new Prioritize release:

1. Take the released `docs/openapi.json` from the Prioritize repo for that version.
2. Copy it over [`openapi/openapi.json`](openapi/openapi.json) and bump this project's `<version>` to match.
3. Rebuild.

`./generate.sh <path-to-released-openapi.json>` automates steps 1–2 and regenerates.

### Note on the OpenAPI version field

OpenAPI Generator's schema resolver chokes on a `3.1.0` document header even when the schemas
themselves use no 3.1-only constructs (this contract doesn't). Prioritize therefore emits **3.0.1**
since the `1.2.0` release (`springdoc.api-docs.version: openapi_3_0`), so a released `docs/openapi.json`
is directly consumable. `generate.sh` still pins the header to `3.0.1` when it imports a spec — now a
no-op safety net for older or hand-exported documents.

## What's inside

- `de.hallerweb.enterprise.prioritize.client.api` — one `*Api` class per resource/tag.
- `de.hallerweb.enterprise.prioritize.client.model` — the request/response DTOs.
- `de.hallerweb.enterprise.prioritize.client.invoker` — `ApiClient` and support classes, plus
  `PrioritizeApiClient`: the one hand-written class in this project (`src/main/java`), a `PATCH`-capable
  `ApiClient` subclass. Everything else lives in `target/generated-sources`.
