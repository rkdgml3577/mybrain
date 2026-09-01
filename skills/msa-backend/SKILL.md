---
name: msa-backend
description: >-
  Scaffold and extend a Spring Boot + FastAPI microservices (MSA) backend using
  this user's standard base template — Eureka service discovery, Kafka events,
  MariaDB/JPA, OAuth2/JWT security, an API Gateway, and a copy-a-template model
  for adding services. Use this WHENEVER the user starts a new backend or MSA
  project, sets up microservices, scaffolds a Spring Boot or FastAPI service,
  wires up Eureka/Kafka/Gateway, or asks to "add a service" / "새 서비스" /
  "백엔드 구조 잡아줘" / "MSA 뼈대" — even if they don't name the template. Prefer
  this skill's conventions over improvising a fresh project layout so every
  service in the project stays consistent.
---

# MSA Backend Base

A reusable microservices backend skeleton and the conventions that go with it.
Use it to (1) start a new MSA project, (2) add a service to an existing one, and
(3) keep every service consistent (package layout, response wrapper, event flow,
security, tests).

The bundled, already-validated template lives next to this file at
`assets-msa-base-template/`. Treat it as the source of truth — copy from it
rather than hand-writing boilerplate, because the wiring (Kafka serializers,
JWT resource server, H2 test profiles, LoadBalanced WebClient) is fiddly and
easy to get subtly wrong.

## What the stack is

| Component | Tech | Port | Role |
|---|---|---:|---|
| eureka-server | Spring Boot | 8761 | Service discovery |
| `service-template` | Spring Boot | 8081 | **Java service template** — copy to make each Java service |
| `python-service-template` | FastAPI | 8085 | **Python service template** — copy to make each Python service |
| vue-frontend | Vue 3 + Vite | 3000 | Minimal UI calling the gateway |
| api-gateway *(optional)* | prebuilt image | 8080 | JWT verify + routing |
| auth-server *(optional)* | prebuilt image | 9000 | OAuth2 / JWT issue |
| MariaDB / Kafka | infra | 3306 / 9092 | Persistence / event bus |

Java package convention: `com.example.msa.<service>` (group `com.example.msa`).
Gateway and Auth Server are prebuilt images kept behind a docker-compose
`infra` profile — the core stack runs without them because each service's
`SecurityConfig` defaults to `permitAll` for development.

## Deciding what to do

- **New backend project** → follow "Start a new project".
- **Add one Java service** → run `new-service.sh` (see "Add a Java service").
- **Add one Python service** → copy `python-service-template/`.
- **Just a question about the patterns** → answer from `references/patterns.md`.

Confirm the target directory with the user before copying a whole stack into it.

## Start a new project

1. Copy the template into the project (or a subfolder). From this skill's dir:
   ```bash
   cp -r assets-msa-base-template/. <project-dir>/
   ```
   This brings eureka-server, both templates, docker-compose, init-db,
   vue-frontend, `new-service.sh`, and the README.
2. Ask the user what services they need and what each one's domain is. Map each
   to a copy of a template — don't keep the literal `service-template` name in
   production; generate real services from it.
3. For each Java service, run `new-service.sh` (below). For each Python service,
   copy `python-service-template/`.
4. Replace the placeholder domain (`Sample` entity, `/api/resources` path,
   `sample.created` topic) with the real domain in each service.
5. Register every service in `docker-compose.yml` by copying the `sample-service`
   block and adjusting context, container_name, `SERVER_PORT`,
   `SPRING_APPLICATION_NAME`, and ports.
6. Validate before declaring done (see "Validate").

## Add a Java service

Use the bundled script — it does the tedious, error-prone renaming (package
move, main/test class rename incl. filenames, settings.gradle, app name, port):

```bash
./new-service.sh <service-name> <base-package> [port]
# e.g.
./new-service.sh order-service com.myapp.order 8090
```

Then: replace the `Sample` domain with the real one, rename the controller path,
set `kafka.topic.inbound/outbound`, and add a compose block. The generated
service compiles and passes its tests as-is, so run its tests first to confirm a
clean starting point, then evolve it.

## Add a Python service

Copy `python-service-template/` to a new directory, then edit
`app/config/settings.py` (`app_name`, port, topics, `target_service_url`) and
replace the `Sample` REST/worker logic. It already includes REST, a Kafka
consumer+producer worker, an httpx service client, JWT verification, and Eureka
registration.

## The event-flow model

Services talk two ways; keep both consistent when adding a service:

- **Async (Kafka)**: a service publishes a domain event on create
  (`SampleEventProducer`) and others subscribe (`InboundEventConsumer`,
  `@KafkaListener`). The template's inbound topic defaults to its own outbound
  topic so a single running service demonstrates the full publish→consume loop.
  Name topics `<noun>.<pastTenseVerb>` (e.g. `order.created`).
- **Sync (REST)**: `ExternalServiceClient` calls another service by name via a
  `@LoadBalanced` WebClient (`http://<service-name>/...`), resolved through
  Eureka. Add a method per remote call; never hard-code host:port.

## Conventions to keep every service consistent

These are why the services feel like one system. Details and rationale are in
`references/patterns.md` — read it before writing service code by hand.

- Every response is wrapped in `ApiResponse<T>` (`success`/`message`/`data`).
- Entities extend `BaseTimeEntity` for automatic `createdAt`/`updatedAt`.
- `GlobalExceptionHandler` maps exceptions to `ApiResponse` — throw
  `IllegalArgumentException` for 400s, let it handle the rest.
- Layering: `controller → service → repository`, DTOs in `dto/`, events in
  `kafka/`, cross-service calls in `client/` or `service/*Client`.
- `SecurityConfig` is `permitAll` for dev; the production JWT block is present as
  a comment — enable it plus the compose `infra` profile when securing.
- Tests use an H2 profile with `kafka.listener.auto-startup=false`; mock the
  Kafka producer bean so `create` paths don't block on a broker.

## Run

```bash
docker compose up -d --build                 # core stack (no gateway/auth)
docker compose --profile infra up -d --build # + gateway/auth (needs infra-images.tar)
```

Local single service: `docker compose up -d mariadb kafka eureka-server`, then
`./gradlew bootRun` (Java) or `uvicorn main:app --reload` (Python).

## Validate

Before saying it works, run what a contributor would:

```bash
(cd eureka-server && ./gradlew test)
(cd <java-service> && ./gradlew test)     # H2 profile, no MariaDB/Kafka needed
python3 -m compileall -q <python-service>
docker compose config --quiet
(cd vue-frontend && npm install && npm run build)
```

Report actual results. If a service was generated but its domain not yet
replaced, say so rather than implying the domain is done.

## Reference files

- `references/patterns.md` — per-layer conventions, exact wiring for Kafka
  serializers / JWT / WebClient / test profiles, and copy-paste snippets. Read
  this whenever writing or reviewing service code by hand.
