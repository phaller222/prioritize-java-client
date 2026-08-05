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

Create an `ApiClient`, point it at your Prioritize instance, add credentials, then call one of the
per-resource API classes (one per tag: `UsersApi`, `ProjectsApi`, `TasksApi`, `ResourcesApi`,
`DocumentsApi`, `SkillsApi`, `ProcessDefinitionsApi`, …).

### Basic auth (default / dev profile)

```java
import de.hallerweb.enterprise.prioritize.client.invoker.ApiClient;
import de.hallerweb.enterprise.prioritize.client.api.UsersApi;
import de.hallerweb.enterprise.prioritize.client.model.UserDTO;

ApiClient client = new ApiClient();
client.setBasePath("http://localhost:8080"); // default; the spec paths already include /api/v1
client.setUsername("admin");
client.setPassword("p@ssword");

UsersApi users = new UsersApi(client);
UserDTO me = users.userGetByUsername("admin");
System.out.println(me.getId() + " " + me.getEmail());
```

### Bearer token (Keycloak profile)

```java
ApiClient client = new ApiClient();
client.setBasePath("https://prioritize.example.com");
client.setBearerToken("<your-JWT-access-token>");
// or a supplier that refreshes the token:
// client.setBearerToken(() -> tokenProvider.currentAccessToken());

ProjectsApi projects = new ProjectsApi(client);
projects.projectGetMyProjects().forEach(p -> System.out.println(p.getName()));
```

Both auth schemes are declared in the spec; the app has exactly one active at a time depending on the
server profile (Basic on the default profile, Bearer/JWT on the `keycloak` profile).

## Building

```bash
mvn clean install
```

This regenerates the client from [`openapi/openapi.json`](openapi/openapi.json) into
`target/generated-sources/openapi` and compiles it. There is nothing to hand-edit.

## Updating to a new API release

The client is spec-first. To move it to a new Prioritize release:

1. Take the released `docs/openapi.json` from the Prioritize repo for that version.
2. Copy it over [`openapi/openapi.json`](openapi/openapi.json) and bump this project's `<version>` to match.
3. Rebuild.

`./generate.sh <path-to-released-openapi.json>` automates steps 1–2 and regenerates.

### Note on the OpenAPI version field

Prioritize (Spring Boot 4 / springdoc) emits **OpenAPI 3.1.0**. OpenAPI Generator's schema resolver
currently chokes on the `3.1.0` document header even when the schemas themselves use no 3.1-only
constructs (this spec doesn't). The committed `openapi/openapi.json` therefore carries `"openapi":
"3.0.1"` — a **loss-less** adjustment for this contract — while the schemas are byte-for-byte the
released ones. `generate.sh` applies this pin automatically.

> Recommended upstream fix (removes the need for the pin): set
> `springdoc.api-docs.version: openapi_3_0` in the Prioritize app so future released specs are already
> 3.0.x.

## What's inside

- `de.hallerweb.enterprise.prioritize.client.api` — one `*Api` class per resource/tag.
- `de.hallerweb.enterprise.prioritize.client.model` — the request/response DTOs.
- `de.hallerweb.enterprise.prioritize.client.invoker` — `ApiClient` and support classes.
