# MSA Backend Patterns

Detailed conventions for writing a service by hand that matches the template.
When in doubt, open the corresponding file under
`../assets-msa-base-template/service-template/` and mirror it.

## Table of contents
1. Package & directory layout
2. Response wrapper (`ApiResponse`)
3. Entities & auditing (`BaseTimeEntity`)
4. DTOs & validation
5. Controller / service / repository
6. Kafka: producer, consumer, config, serializers
7. Cross-service REST (LoadBalanced WebClient)
8. Security (dev vs prod)
9. application.yml (main vs test)
10. Tests (H2, no broker)
11. Scheduler
12. Python service equivalents

---

## 1. Package & directory layout

`com.example.msa.<service>` with sub-packages:
`config/ · controller/ · service/ · repository/ · entity/ · dto/ · kafka/ · client/`.
Rename `com.example.msa.template` when copying; `new-service.sh` does this for you.

## 2. Response wrapper

Every controller returns `ResponseEntity<ApiResponse<T>>`. `ApiResponse` has
`success`, `message`, `data` and static `success(data)` / `error(message)`.
Keep one copy per service (each has its own `dto/ApiResponse.java`).

## 3. Entities & auditing

Extend `BaseTimeEntity` (uses `@CreatedDate` / `@LastModifiedDate`) and enable it
with a `@Configuration @EnableJpaAuditing` class (`config/JpaConfig`). Entities
use Lombok `@Getter @NoArgsConstructor @AllArgsConstructor @Builder` and expose
intent-revealing mutators (`update(...)`, `cancel()`) rather than public setters.

## 4. DTOs & validation

Group request/response records under one `<Domain>Dto` class as static nested
`CreateRequest` / `UpdateRequest` / `Response`, with a `Response.from(entity)`
factory. Validate requests with Jakarta annotations (`@NotBlank`, `@Email`,
`@DecimalMin`, `@Size`) and `@Valid` in the controller — `GlobalExceptionHandler`
turns violations into a 400 `ApiResponse`.

## 5. Controller / service / repository

- Controller: thin, `@RestController @RequestMapping("/api/<plural>")`, delegates
  to the service, wraps results in `ApiResponse`. Add
  `/internal/exists/{id}` (and `/internal/{id}`) for other services to call.
- Service: `@Service @RequiredArgsConstructor @Transactional`; read paths
  `@Transactional(readOnly = true)`; throw `IllegalArgumentException` when not
  found.
- Repository: `extends JpaRepository<Entity, Long>` with derived queries as
  needed (`existsByX`, `findByX`).

## 6. Kafka

**Producer** (`kafka/<X>EventProducer`): inject `KafkaTemplate<String, Object>`,
read the topic from `@Value("${kafka.topic.outbound}")`, `send(topic, key, event)`
using a stable partition key (the domain id). Log success/failure via
`whenComplete`.

**Consumer** (`kafka/InboundEventConsumer`): `@KafkaListener(topics =
"${kafka.topic.inbound}", groupId = "${spring.application.name:<name>}")` taking a
`Map<String,Object>` (matches the JSON default type). Keep a default in the
groupId placeholder so the test profile resolves it.

**Topic auto-creation** (`config/KafkaConfig`): a `NewTopic` bean per outbound
topic via `TopicBuilder.name(topic).partitions(3).replicas(1).build()`.

**Serializers** (application.yml): producer uses `StringSerializer` +
`JsonSerializer` with `spring.json.add.type.headers: false`; consumer uses
`ErrorHandlingDeserializer` delegating to `StringDeserializer` /
`JsonDeserializer` with `spring.json.value.default.type: java.util.HashMap` and
`spring.json.use.type.headers: false`. Copy this block verbatim — mismatched
serializer settings are the most common cause of silent consume failures.

Event payloads are plain Lombok POJOs in `kafka/` (e.g. `SampleCreatedEvent`).

## 7. Cross-service REST

`config/WebClientConfig` exposes a `@LoadBalanced WebClient.Builder`. A client
(`client/ExternalServiceClient` or `service/<X>Client`) calls
`http://<service-name>/api/...` — the host is a Eureka service id, not a
host:port. Set the target service id in `application.yml`
(`client.target-service`) so it's configurable. Wrap failures and rethrow a
domain exception; never leak `WebClientResponseException` to the controller.

## 8. Security

`config/SecurityConfig`: stateless, CSRF off, CORS open, `anyRequest().permitAll()`
for development so the service runs without the gateway/auth infra. The
production variant (JWT resource server + role/scope rules) is included as a
commented block — enable it and set `spring.security.oauth2.resourceserver.jwt.*`
(issuer-uri / jwk-set-uri) when securing, and bring up the compose `infra`
profile so auth-server/gateway exist.

## 9. application.yml

Main config parameterizes everything via env with local defaults:
`SPRING_DATASOURCE_URL`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`,
`SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SERVER_PORT`. JPA `ddl-auto: update`. Expose
`kafka.topic.inbound/outbound`, `client.target-service`, `scheduler.fixed-delay`.
Test config (`src/test/resources/application.yml`) switches to H2
(`MODE=MariaDB`), `ddl-auto: create-drop`, disables Eureka
(`eureka.client.enabled=false`), and sets `spring.kafka.listener.auto-startup:
false` + `admin.fail-fast: false` so no broker is needed. Include
`spring.application.name` in the test config so `@KafkaListener` groupId resolves.

## 10. Tests

`@SpringBootTest` with `@MockBean` on the event producer so `create` paths don't
block trying to reach a broker. Test the service layer directly (create/find/
update/delete, event published) — see `ServiceTemplateApplicationTests`. Unit-test
pure logic (e.g. an `Evaluator`) without Spring. Keep a plain `contextLoads()`.

## 11. Scheduler

`@Component` with an `@Scheduled(fixedDelayString = "${scheduler.fixed-delay}")`
method; enable with `@EnableScheduling` on the application class. Set a long delay
in the test profile so it doesn't fire during tests.

## 12. Python service equivalents

FastAPI template mirrors the Java one:
- REST router under `/api/resources` returning plain dicts.
- Kafka worker: background thread consumer (`app/kafka/consumer.py`) +
  lazy producer (`app/kafka/producer.py`), consume→process→republish.
- `app/client/external_client.py`: httpx call to another service, unwrapping the
  `{data:...}` envelope.
- `app/config/security.py`: JWT verification against the auth-server JWK.
- `app/config/settings.py`: pydantic-settings, env-overridable, `.env.example`
  documents the keys.
- Eureka registration + `/health` in `main.py` lifespan.
