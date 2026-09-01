---
name: spring-ai-chatbot
description: Spring AI 로 멀티 세션 챗봇을 만들 때 쓴다. SSE 스트리밍 대화, ChatMemory 로 세션별 맥락 유지, Redis 대화 이력, 사용자별 인증과 사용량 제한, 응답시간 계측, Docker Compose 배포까지의 순서와 각 단계 검증법을 담고 있다. "챗봇 만들어줘", "Spring AI 스트리밍", "ChatMemory", "대화 이력을 Redis 에", "conversationId", "SSE 로 흘려보내기" 같은 요청에 쓴다. 단발성 요약·분류 API 에는 쓰지 않는다 — 대화가 이어져야 할 때만 쓴다.
---

# Spring AI 멀티 세션 챗봇

대화가 **이어지는** 서비스를 만든다. 단발 요청과의 차이는 `ChatMemory` 하나가 아니라,
그로 인해 따라붙는 세션 분리 · 사용자 격리 · 이력 조회 · 저장소 선택 전부다.

## 원칙

1. **AI 호출은 서비스 계층에 둔다.** 컨트롤러에 두면 재시도와 계측을 붙일 자리가 없다.
2. **프롬프트와 옵션은 `ChatClient` 빈에 모은다.** 호출하는 쪽에 프롬프트 문자열이 남으면 안 된다.
3. **공급자와 모델 옵션은 코드가 아니라 설정으로 바꾼다.** 자바 코드는 `openai` 인지 `ollama` 인지 몰라야 한다.
4. **한 단계 끝낼 때마다 실제로 돌려서 확인한다.** 다음 단계로 넘어가기 전에 관문을 통과시킨다.

## 진행 순서

각 STEP 끝의 **관문**을 통과해야 다음으로 간다. 통과 못 하면 다음 단계는 디버깅이 두 배가 된다.

### STEP 0 — 버전 정렬

- Spring Boot 와 Spring AI 버전을 먼저 못박는다. Initializr 기본값이 최신이라
  참조하려는 예제·기존 모듈과 어긋나기 쉽다.
- Boot 4 는 스타터 이름이 다르다 (`starter-web` → `starter-webmvc`).
  버전을 내리거나 올릴 때 스타터 이름도 함께 바꾼다.
- 필요한 것 : `web`, `webflux`(SSE 의 `Flux`), `validation`, `actuator`(`MeterRegistry` 빈),
  `spring-ai-starter-model-openai`, `spring-ai-starter-model-ollama`
- 한글 프롬프트·주석이 깨지지 않게 `JavaCompile` · `Test` · `bootRun` 인코딩을 UTF-8 로 고정한다.

**관문** : `./gradlew compileJava` 통과.

### STEP 1 — 설정을 빈 하나에 모은다

- `AiConfig` 에 `ChatMemory` 빈과 `ChatClient` 빈을 둔다.
- 시스템 프롬프트는 `classpath:/prompts/system.st` 로 빼고 `defaultSystem(resource, UTF_8)` 으로 넣는다.
  자바 텍스트 블록에 박지 않는다.
- `defaultAdvisors(MessageChatMemoryAdvisor, TokenUsageAdvisor)` — 메모리가 바깥, 토큰이 안쪽.
- 공급자 전환은 `@Qualifier("openAiChatModel")` / `@Qualifier("ollamaChatModel")` 두 개를 받아
  설정값으로 고른다.

**관문** : 키 없이 애플리케이션이 뜨고 `/actuator/health` 가 `UP`.

> **함정** : `api-key: ${OPENAI_API_KEY:}` 는 안 된다. Spring AI 는 `OpenAiApi` **빈을 만드는
> 순간** 키가 비었는지 검사하고 거절해서, 모델을 부르지도 않았는데 기동이 실패한다.
> 비어 있지 않은 플레이스홀더를 기본값으로 준다. 그러면 기동은 되고 실제 호출 때 401 이 난다.

### STEP 2 — 서비스 계층

- `ChatService.streamChat(userId, sessionId, message)` 가 `Flux<String>` 을 돌려준다.
- `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))` 이 한 줄이 대화를 가른다.
- 계측은 `Flux.defer` 안에서 시작한다. `Flux` 는 구독될 때 흐르므로 시계도 그때 시작해야 한다.
- `doOnComplete` · `doOnError` · **`doOnCancel`** 셋을 다 건다.
- `QuotaService.checkAndDecrease(userId)` 는 한도 초과 시 예외를 던진다.

> **함정** : `doOnCancel` 이 없으면 사용자가 떠난 뒤에도 모델 호출이 계속 흐르고
> 비용이 계속 나간다. 취소도 계측 대상이다.

### STEP 3 — SSE 엔드포인트

- `produces = TEXT_EVENT_STREAM_VALUE`, 반환은 `Flux<ServerSentEvent<String>>`.
- 조각은 `message` 이벤트, 끝나면 `done`, 도중 실패는 `error` 이벤트.
- **사용량 확인은 스트림을 만들기 전에** 한다. 예외는 `@RestControllerAdvice` 가 429 로 받는다.

> **함정** : SSE 는 응답 헤더가 먼저 나가므로 스트림 시작 후의 실패는 HTTP 상태를 바꿀 수 없다.
> 쿼터를 `onErrorResume` 안에서 잡으면 사용자는 "모델이 죽은 것" 과 "내 한도가 찬 것" 을
> 구분할 수 없다. 앞에서 막아야 429 가 나간다.

**관문** — 여기서 처음으로 대화가 된다. 키가 없어도 아래 셋은 확인된다.

```bash
curl -s $AUTH localhost:8080/api/chat/ping              # 200
curl -N $AUTH -X POST .../stream -d '{"message":""}'    # 400 (쿼터 소비 안 됨)
curl -N $AUTH -X POST .../stream -d '{"message":"안녕"}' # event:message ... event:done
# 한도까지 반복 → 마지막에 429
```

### STEP 4 — 테스트 화면

- `EventSource` 는 GET 만 된다. POST SSE 는 `fetch` + `ReadableStream` 으로 직접 파싱한다.
- `decode(value, { stream: true })` — 한글은 3바이트라 조각 경계에서 잘린다.
  이 옵션이 없으면 잘린 글자가 `U+FFFD` 로 깨진다.
- Enter 핸들러에 `if (event.isComposing || event.keyCode === 229) return;` 를 넣는다.

> **함정** : 위 조합 검사가 없으면 한글에서 마지막 글자가 한 번 더 나간다. `keydown` 이
> 조합 확정보다 먼저 오기 때문이다. 영어로 테스트하면 안 보인다.

**관문** — 이 실습의 핵심이다.

```
같은 sessionId 로 두 번  → 기억해야 통과
다른 sessionId 로        → 몰라야 통과
```

**토큰 로그가 더 확실한 물증이다.** 같은 세션에서 입력 토큰이 늘어나면 이력이 실린 것이고,
새 세션에서 처음 값으로 돌아가면 격리된 것이다. 모델의 말버릇에 휘둘리지 않는다.

### STEP 5 — Redis 로 이력을 옮긴다

- Spring AI **1.1.x 에는 Redis 저장소가 없다** (2.0.0 부터). jdbc · cassandra · neo4j 뿐이다.
  1.1.x 면 `ChatMemoryRepository` 네 메서드를 직접 구현한다.
- 바꾸는 것은 `AiConfig` 의 `chatMemoryRepository(...)` **한 줄**이다.
  `ChatService` 와 컨트롤러는 손대지 않는다 — 안 그러면 추상화가 새는 것이다.

구현 주의

- `KEYS` 대신 `SCAN` — `KEYS` 는 Redis 를 통째로 멈춰 세운다
- `Message` 를 Jackson 에 그대로 맡기지 않는다. 구현체가 여러 개라 되읽을 때 타입을 알 수 없다.
  `{type, text}` 로 적고 읽을 때 다시 조립한다
- **TTL 을 반드시 건다.** 안 걸면 버려진 세션이 영원히 남는다
- Redis 컨테이너는 `--appendonly yes` + 이름 붙은 볼륨

**관문** : 앱 컨테이너를 **지웠다가** 새로 띄운 뒤에도 이전 대화를 기억한다.
재시작이 아니라 삭제 후 재생성이어야 의미가 있다.

```bash
docker compose rm -sf app && docker compose up -d app
docker compose exec redis redis-cli LRANGE 'chat:memory:<key>' 0 -1
```

### STEP 6 — 이력 조회

- `SessionService` 는 **`ChatMemoryRepository` 인터페이스만** 안다. Redis 를 몰라야 한다.
- `Message` 를 응답에 그대로 싣지 않는다. `MessageRecord(role, text)` 로 갈아 끼운다.
  안 그러면 라이브러리 내부 구조가 API 스펙이 된다.
- `GET /sessions`, `GET /sessions/{id}`, `DELETE /sessions/{id}`.
  없는 세션은 404 가 아니라 빈 목록으로 돌려주는 편이 다루기 쉽다.

### STEP 7 — 인증, 그리고 데이터 격리

**이 둘은 별개의 일이다.** 인증만 붙이면 401 은 잘 나오지만, 두 사용자가 같은 세션 이름을
쓰는 순간 같은 저장소 키를 공유한다 — 로그인은 되는데 남의 대화가 보이는 상태다.
인증이 있어서 더 위험하다.

- `SecurityConfig` : `InMemoryUserDetailsManager`, HTTP Basic, `/api/**` 인증.
  화면(`/`)과 `/actuator/prometheus` 는 열어 둔다.
- 컨트롤러를 `@AuthenticationPrincipal UserDetails user` 로 바꾼다.
- **저장소 키에 사용자를 넣는다** : `conversationId = userId + ":" + sessionId`.
  키 규칙은 도메인 한 곳에 모은다.
- 목록은 내 접두사로 거른다. 조회는 **사용자가 보낸 `sessionId` 를 그대로 키로 쓰지 않는다** —
  그대로 쓰면 남의 세션 이름을 넣어 훔쳐볼 수 있다.
- `fetch` 는 401 을 받아도 브라우저 인증창을 띄우지 않는다. `Authorization: Basic` 헤더를 직접 만든다.

**관문** : 두 사용자가 **같은 세션 이름**으로 각각 대화한 뒤

```
저장소 키가 갈라졌는가              chat:memory:alice:공용 · chat:memory:bob:공용
서로의 목록에 안 보이는가
한쪽이 지워도 다른 쪽이 남는가
사용량과 메트릭이 따로 세지는가
```

### STEP 8 — Compose 로 묶는다

- `app` · `redis` · `prometheus` · `grafana`.
- 멀티스테이지 Dockerfile : 빌드는 JDK, 실행은 JRE, root 로 돌리지 않는다.
- 키는 이미지에 굽지 않는다. `docker history` 로 확인한다.

> **함정 1** : `OPENAI_API_KEY: ${OPENAI_API_KEY:-}` 는 키가 없을 때 **빈 문자열을 주입한다.**
> 스프링의 `${VAR:기본값}` 은 변수가 없을 때만 기본값을 쓰므로 빈 값이 기본값을 이기고,
> STEP 1 에서 고친 기동 실패가 되살아난다. 값 없는 리스트 형태(`- OPENAI_API_KEY`)로 적으면
> 호스트에 있을 때만 전달된다.
>
> **함정 2** : 컨테이너 안의 `localhost` 는 컨테이너 자신이다. 호스트의 Ollama 는
> `host.docker.internal` 로 부르고, 리눅스는 `extra_hosts: ["host.docker.internal:host-gateway"]`
> 가 있어야 그 이름이 생긴다. 환경변수로 덮으므로 자바 코드는 안 고친다.
>
> **함정 3** : 메트릭 이름이 바뀐다. `chatbot.response.duration` →
> `chatbot_response_duration_seconds_count`. 점이 밑줄로, 단위가 접미사로.

**관문** : Prometheus 의 `/api/v1/targets` 에서 앱이 `up`, 쿼리에 우리 메트릭이 잡힌다.

## 모델 고르기 — 체감 속도

**추론(thinking) 모델을 로컬 실습에 기본으로 쓰지 않는다.** 답하기 전에 속으로 길게 생각하고,
그 생각은 `content` 가 아니라 `thinking` 으로 오기 때문에 **화면에는 아무것도 나가지 않는다.**
스트리밍을 만든 의미가 사라진다.

`qwen3.5:2b` 에게 `"안녕"` 한 마디를 물었을 때 : thinking 7322자, content 66자.
첫 조각까지 `think-option: "true"` 9.2초 vs `"false"` 3.5초.

전체 시간이 아니라 **첫 조각까지의 침묵**이 사용자가 느끼는 속도다.

- Ollama 라면 `spring.ai.ollama.chat.options.think-option` 으로 조절한다
  (`true` / `low` / `medium` / `high` / `false`)
- Spring AI 1.1.8 은 이 프로퍼티의 **설정 바인딩 Converter 를 빠뜨렸다.**
  `Converter<String, ThinkOption>` 에 `@ConfigurationPropertiesBinding` 을 붙여 등록한다
- 값에 **따옴표가 필요하다.** 없으면 YAML 이 Boolean 으로 읽어 String 컨버터를 못 찾는다
- 시간을 잴 때는 **콜드 스타트를 분리한다.** 첫 요청에는 모델을 디스크에서 올리는 시간이 섞인다

## 마지막에 확인할 것

- [ ] 키 없이도 애플리케이션이 뜬다
- [ ] 같은 세션은 기억하고 다른 세션은 모른다 — **토큰 로그로** 확인
- [ ] 앱 컨테이너를 지웠다 띄워도 대화가 남는다
- [ ] 두 사용자가 같은 세션 이름을 써도 서로 안 보인다
- [ ] 인증 없이 `/api/**` → 401
- [ ] 한도 초과 → 429 와 읽을 수 있는 메시지
- [ ] 연결을 끊으면 취소 로그가 남는다
- [ ] 요청 한 건의 입력·출력 토큰이 로그에 남는다
- [ ] `docker history` 에 API 키가 없다
- [ ] 프롬프트가 자바 코드에 문자열로 박혀 있지 않다
