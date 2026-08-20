---
title: Vue 심화 — Composition API·컴포넌트 통신·슬롯·라우터
date: 2026-08-20
category: frontend
tags: [vue, composition-api, ref, reactive, computed, watch, props, emits, provide-inject, slots, vue-router, spa, pinia]
source: 광주 SKALA 201 · Vue 2일차
---

# Vue 심화 — Composition API·컴포넌트 통신·슬롯·라우터

Vue 2일차. 반응형 상태(Composition API) 복습 → 컴포넌트 간 데이터/이벤트 전달(props·emits) → provide/inject·슬롯 → Vue Router. 오늘 개념, 실습은 내일. (중간에 시험 예정)

## 핵심 개념

- **Composition API** — Vue가 제공하는 함수들로 반응형 로직을 구성하는 방식. `createApp`, `ref`, `reactive`, `computed`, `watch`/`watchEffect` 등.
- **ref** — primitive·object 타입 모두 반응형으로 만듦. `script setup`에서는 `.value`로 접근, template(interpolation)에서는 `.value` 불필요.
- **reactive** — object 타입만 반응형. 반응형이 끊기는(단절) 케이스가 있어, 실무에선 대부분 ref를 씀.
- **computed** — 기존 반응형 데이터를 바탕으로 미리 계산된 파생 반응형 데이터. 메모리에 캐싱되어, 의존 데이터가 바뀔 때만 재계산.
- **watch / watchEffect** — 데이터 변화에 반응해 동작 실행.
- **props / emits** — 부모→자식은 props(데이터 내림), 자식→부모는 emits(이벤트 올림).
- **provide / inject** — 조상이 값을 제공하고 후손이 주입받음. props를 층층이 뚫는 걸 줄여줌.
- **슬롯 (Slot)** — 부모가 자식 태그 사이에 넣은 템플릿이 자식의 지정 위치에 꽂히는 구조. default / named / scoped 세 종류가 함께 쓰임.
- **Vue Router** — SPA에서 URL ↔ 컴포넌트를 매핑해 화면(페이지)을 전환.
- **SPA (Single Page Application)** — 서버에 매번 페이지를 요청하는 방식이 아니라, 하나의 페이지 안에서 컴포넌트를 갈아끼우는 앱.

## 도메인·배경 지식

- **왜 computed/watch가 "뷰스러운" 앱을 만드나**: 단순 반응형 데이터를 바인딩·양방향 연결하는 건 이미 됨. 여기에 computed(파생 상태)와 watch(변화 감지)를 더하면, "하나가 바뀌면 다른 것들이 알아서 순차적으로 갱신되는" 구조를 미리 선언해둘 수 있음 → 더 선언적이고 유지보수 쉬운 앱.
- **왜 ref를 주로 쓰나**: reactive는 object만 되고 반응형이 끊기는 경우가 있음. ref는 모든 타입에 되고 일관적이라 대부분 ref로 처리.
- **provide/inject vs Store(Pinia)**: props 드릴링을 피하는 방법 중 provide/inject가 있으나, 실무에선 자식이 조상이 뭘 꽂았는지 다 알아야 해서 소스가 헷갈림. 그래서 전역 반응형 저장소인 **Store(Pinia, 내일 학습)**를 더 많이 씀. provide/inject는 "루트가 아니라 특정 지점부터 특정 후손까지 값을 내리고 싶을 때" 정도로 제한적으로 사용.
- **views 폴더 vs components 폴더 (구조 규칙)**: 라우터에 매핑되는 **페이지 단위 컴포넌트는 `views/`**에, 그 안에 들어가는 **재사용 가능한 부품 컴포넌트는 `components/`**에 둠. 페이지는 각각 달라 재사용성이 낮고, 부품은 재사용성이 높기 때문. views의 페이지 컴포넌트는 끝에 **`View`**를 붙이는 게 컨벤션(예: `DashboardView`, `UserProfileView`).

## 원리·메커니즘

- **컴포넌트 등록 2가지**: ① 지역 등록 — 부모가 자식을 `import`해 태그로 사용(부모-자식 성립). ② 전역 등록 — `main.js`에서 `app.component(...)`로 등록(체이닝 가능)하면, 어느 `.vue`에서도 `import` 없이 사용. 단, 전역 등록은 "사용 가능"하게 할 뿐 부모-자식 관계 자체는 태그로 호출할 때 성립. (태그 이름은 PascalCase·kebab-case 모두 허용)
- **props (부모→자식)**: 자식 태그의 속성에 콜론(`:`)으로 바인딩해 주입. 자식은 `defineProps`로 "무슨 데이터를 받을지" 정의.
- **emits (자식→부모)**: 자식은 `defineEmits`로 "무슨 이벤트를 보낼지" 정의 → `emit(이벤트명, payload)`로 발생. 부모는 자식 태그에 `@이벤트명="핸들러"`로 등록해두면, 자식이 emit할 때 핸들러가 실행되며 payload(두 번째 인자)를 전달받음.
- **compiler macro**: `defineProps`·`defineEmits`(·`defineExpose`)는 import가 필요 없는 특수 예약어. 런타임이 아니라 빌드 시점에 Vue 컴파일러가 변환. `script setup`에서만 사용 가능.
- **흐름을 볼 땐 props와 emits를 분리해서 보기**: 하나의 컴포넌트에 props 수십 개, emits 수십 개가 섞이면 헷갈림. "props는 데이터 내림, emits는 이벤트 올림"으로 따로 떼어 이해하는 게 요령.
- ⭐ **props 드릴링 문제(오늘의 최난관)**: 조상(할아버지)에서 먼 후손(손자)으로 바로 전달하는 방법이 없음. 중간에 있는 부모 역할 컴포넌트들을 props·emits로 전부 뚫어야 함 → 이 지점이 가장 어렵고, 그래서 provide/inject·Store가 등장.
- **슬롯 배치의 부모-자식 판별**: 슬롯으로 A 안에 B가 "들어가 보여도", 관계는 누가 누구를 호출(import해서 태그로 사용)하느냐로 정해짐. 시각적 중첩이 아니라 호출 관계가 부모-자식.
- **Vue Router 동작**: `routes`가 경로 → 컴포넌트를 매핑하고, 매칭된 컴포넌트가 `<router-view>` 자리에 렌더됨. 못 찾으면 에러 없이 그 자리가 빈 채로 뜸 → 이를 막으려 정규식 패턴의 **Catch-All Route(NotFoundView)**를 맨 끝에 둠(SPA라 직접 경로 입력이 드물어 실사용은 적음).
- **router vs route 구분(중요)**: `useRouter()`가 주는 router = 시스템(전체 페이지 이동 제어), `useRoute()`가 주는 route = 현재 활성 경로 정보(params·query). 이름이 비슷해 반드시 구분.

## 실습·도구·코드

- **전역 컴포넌트 등록**: `main.js`에서 `createApp(App).component('Comp', Comp)...` 체이닝 → 자주 쓰는 컴포넌트를 import 없이 사용.
- **props/emits 예시**: 자식 버튼 클릭 → `emit('childEvent', '안녕하세요 부모 컴포넌트')` → 부모는 `<Child @childEvent="handleChildEvent">`로 받고, 핸들러 인자로 payload 수신.
- **슬롯 3종(함께 사용)**:
  - **default slot** — 이름 없는 `<slot>`. 부모가 자식 태그 사이에 넣은 템플릿이 여기로 들어감. 아무것도 안 넣으면 기본값 표시.
  - **named slot** — 이름 붙은 슬롯으로 여러 위치에 각각 배치.
  - **scoped slot** — 슬롯에 데이터를 실어 전달.
  - 예: `CardBase`를 3번 호출 → 각 호출의 시작~종료 태그 사이 내용(p / h2+button / 없음)이 각 슬롯에 꽂힘.
- **provide/inject**: `script setup`의 `provide(...)`로 내리거나, `main.js`의 앱에서 `app.provide(...)`로 전역 제공. 전자는 선언한 조상부터, 후자는 루트라 전체가 사용.
- **Vue Router**:
  - 선언적 이동: 템플릿에서 `<router-link>`로 링크.
  - 프로그래매틱 내비게이션: 스크립트에서 페이지 전환. `useRouter()` → `router.push`(특정 페이지 이동), `router.replace`, `router.go(n)`, `back`, `forward`. 예: 로그인 성공 시 `router.push('/')`.
  - params 뽑기: `useRoute()` → `route.params.id`.
  - query string 뽑기: URL `?key=value`를 `route.query.search`, `route.query.page`처럼 추출(매핑은 `/weather`처럼 단순히 두고 컴포넌트만 지정).
  - 폴더 규칙: 라우터 매핑 페이지는 `views/`(끝에 `View`), 재사용 부품은 `components/`.

## 예시

- **WeatherParent / BaseDashboardCard / SearchBar** — 실습 컴포넌트 분리 예시. 슬롯/호출 관계로 부모-자식이 정해짐.
- **로그인 성공 → 메인 이동** — `router.push`로 스크립트에서 페이지 전환.
- **CardBase ×3 + 슬롯** — 같은 자식을 여러 번 호출하고 서로 다른 내용을 슬롯으로 주입.
- **NotFoundView (Catch-All)** — 매핑 안 된 경로에서 빈 화면 대신 404 표시.

## 헷갈리는 점·질문

- props vs emits — 한 컴포넌트에 섞여 있어 흐름이 헷갈림. 데이터 내림(props) / 이벤트 올림(emits)으로 분리해서 볼 것.
- props 드릴링 — 왜 조상→후손 직통이 안 되고 중간을 다 뚫어야 하나 → provide/inject·Store로 완화(내일 Pinia).
- router vs route — 시스템(이동 제어) vs 현재 경로 정보. 이름 비슷, 역할 다름.
- 호출 관계 = 부모-자식 — 슬롯으로 안에 들어가 보여도, 실제 관계는 누가 호출하느냐로 결정.
- defineProps/defineEmits는 왜 import가 없나 — compiler macro라 빌드 시점에 변환, script setup 전용.
- ref vs reactive — 왜 주로 ref? reactive의 반응형 단절 때문.
- views vs components 폴더 — 페이지 단위(재사용 낮음) vs 부품(재사용 높음).

## 추가 학습·TODO

- [ ] 내일: Pinia(Store) 학습 — 전역 반응형 상태 관리. props 드릴링의 실무 해법.
- [ ] 내일: 컴포넌트 분리 실습 — props/emits로 4개 컴포넌트 나누고 이벤트 2개 emit(반응형 데이터 변경·alert). "이 과정 중 가장 어려운 부분"이라 예고됨 → 제공 소스 참고하며 조정.
- [ ] ref/reactive/computed/watch를 "왜 쓰는지" 중심으로 다시 정리(문법보다 목적).
- [ ] Vue Router: `router.push`/`route.params`/`route.query` 직접 써보기.
- [ ] views/ vs components/ 규칙에 맞춰 내 파일 배치 + 페이지 컴포넌트에 `View` 접미사.
- [ ] 시험 대비: Composition API 3인방(computed/watch/watchEffect)과 props·emits 흐름 확실히.

## 한 줄 요약

> Vue의 반응형은 ref 중심으로 만들고, 부모-자식은 **props(내림)·emits(올림)**로 통신하되 먼 후손 전달은 props 드릴링이라 **provide/inject·Store(Pinia)**로 푼다 — 그리고 Vue Router로 URL↔컴포넌트를 매핑해 SPA의 페이지 전환을 처리한다.
