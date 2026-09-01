# MSA Base Template

새 백엔드 프로젝트를 빠르게 시작하기 위한 **재사용 가능한 MSA 뼈대**입니다.
교수님이 주신 `msa-lecture` 인프라(Eureka + API Gateway + Auth Server + Kafka + MariaDB + Java/Python 서비스 + Vue)를 유지하되, **도메인 로직을 전부 걷어내고** "복사해서 이름만 바꿔 시작하는" 템플릿 형태로 정리했습니다.

> 원본 ShariahGuard(샤리아 종목 심사) 소스는 저장소 루트에 그대로 보존돼 있고, 이 템플릿은 `msa-base-template/` 아래 독립적으로 존재합니다.

---

## 구성

```
msa-base-template/
├── new-service.sh              # service-template 을 복제해 새 Java 서비스 생성
├── docker-compose.yml          # 인프라 + 예시 서비스 + 프런트엔드 (+ 선택 Gateway/Auth)
├── eureka-server/              # 서비스 디스커버리 (그대로 사용)
├── service-template/           # ★ Java(Spring Boot) 서비스 템플릿 — 모든 패턴 포함
├── python-service-template/    # ★ Python(FastAPI) 서비스 템플릿 — 모든 패턴 포함
├── vue-frontend/               # 최소 Vue 3 + Vite 프런트엔드
└── init-db/                    # 선택적 초기 DDL
```

서비스 이름을 미리 박아두지 않았습니다. **필요한 수만큼 템플릿을 복제**해서 프로젝트를 조립합니다.

---

## service-template 이 담고 있는 패턴 (한 서비스 안에 전부)

| 계층/기능 | 파일 | 설명 |
|---|---|---|
| REST CRUD | `controller/SampleController` | `/api/resources` CRUD + 내부 호출용 엔드포인트 |
| 도메인 | `entity/Sample`, `dto/SampleDto` | `BaseTimeEntity`(생성/수정시각 자동) 상속 |
| 영속성 | `repository/SampleRepository` | Spring Data JPA |
| **Kafka 발행** | `kafka/SampleEventProducer` | 생성 시 이벤트 발행 |
| **Kafka 구독** | `kafka/InboundEventConsumer` | `@KafkaListener` (기본값은 자기 토픽을 구독해 단독 실행만으로 왕복 확인 가능) |
| **서비스 간 REST** | `client/ExternalServiceClient` | LoadBalanced WebClient 로 `http://서비스이름/...` 호출 |
| **스케줄러** | `service/SampleScheduler` | `@Scheduled` 배치 예시 |
| 보안 | `config/SecurityConfig` | 개발=permitAll, 운영=JWT 검증 예시(주석) |
| 공통 | `dto/ApiResponse`, `config/GlobalExceptionHandler` | 응답 래퍼 + 예외 처리 |
| 문서 | Swagger(springdoc) | `/swagger-ui.html` |
| 테스트 | `ServiceTemplateApplicationTests` | H2 기반, MariaDB/Kafka 없이 실행 |

`python-service-template` 도 동일하게 REST + Kafka(consume/produce) + httpx 서비스 호출 + JWT 검증 + Eureka 등록을 한 서비스에 담고 있습니다.

---

## 새 서비스 만들기

### Java 서비스

```bash
./new-service.sh <service-name> <base-package> [port]

# 예시
./new-service.sh order-service com.myapp.order 8090
```

스크립트가 자동으로 처리하는 것:
- `service-template/` → `order-service/` 복사
- 패키지 `com.example.msa.template` → `com.myapp.order`
- 메인 클래스 `ServiceTemplateApplication` → `OrderServiceApplication`
- `settings.gradle` / `spring.application.name` / 기본 포트 치환

그다음 직접 할 일:
1. `entity/Sample.java` 등 도메인을 실제 도메인으로 교체
2. 컨트롤러 경로 `/api/resources` 를 실제 경로로 변경
3. `kafka.topic.inbound/outbound` 를 실제 토픽으로 설정
4. `docker-compose.yml` 에 서비스 블록 추가(아래 `sample-service` 블록 복사)

### Python 서비스

`python-service-template/` 를 원하는 이름으로 복사하고 `app/config/settings.py` 의 `app_name`·포트·토픽을 바꿉니다.

---

## 실행

### 핵심 스택 (Gateway/Auth 없이)

```bash
cd msa-base-template
docker compose up -d --build
```

- Eureka: http://localhost:8761
- 예시 Java 서비스(service-template): http://localhost:8081/swagger-ui.html
- 예시 Python 서비스: http://localhost:8085/health
- 프런트엔드: http://localhost:3000

각 서비스 `SecurityConfig` 기본값이 `permitAll` 이라 Gateway/Auth 없이 직접 호출됩니다.

### Gateway + Auth Server 까지 (사전 빌드 이미지 필요)

`auth-server`, `api-gateway` 는 소스가 아니라 사전 빌드 이미지입니다. 원본의 `infra-images.tar` 를 로드한 뒤 `infra` 프로필로 실행합니다.

```bash
docker load -i /path/to/infra-images.tar
docker compose --profile infra up -d --build
```

### 개별 로컬 실행 (도커 없이)

```bash
docker compose up -d mariadb kafka eureka-server   # 인프라만
cd service-template && ./gradlew bootRun            # Java
cd python-service-template && pip install -r requirements.txt && uvicorn main:app --reload --port 8085
```

---

## API 예시

```bash
# 리소스 생성 → Kafka 이벤트 발행 → (자기 구독) 소비 로그 확인
curl -X POST http://localhost:8081/api/resources \
  -H 'Content-Type: application/json' \
  -d '{"name":"first","description":"hello"}'

curl http://localhost:8081/api/resources
```

---

## 검증

Java 테스트는 H2·`listener.auto-startup=false` 로 MariaDB/Kafka 없이 실행됩니다.

```bash
(cd eureka-server && ./gradlew test)
(cd service-template && ./gradlew test)
python3 -m compileall -q python-service-template
docker compose config --quiet
(cd vue-frontend && npm install && npm run build)
```

---

## 왜 이렇게 만들었나

이전 버전은 원본 서비스 7개(course/enrollment/payment…)를 그대로 두고 내용만 제네릭하게 바꿨는데, 서비스 이름과 이벤트 흐름이 원본을 그대로 따라가 "이름만 바꾼 같은 프로젝트"처럼 보였습니다. 그래서 도메인 이름을 없애고 **모든 패턴을 담은 단일 템플릿 + 복제 스크립트** 형태로 재구성했습니다. 새 프로젝트는 `new-service.sh` 로 필요한 서비스를 찍어내면서 시작하면 됩니다.
