---
title: Vue 실무 도구 — Pinia·Axios·UI 라이브러리·빌드/배포
date: 2026-08-21
category: frontend
tags: [vue, pinia, store, axios, http, element-plus, ui-library, eslint, prettier, vite, deployment, env, spa, vue-router]
source: 광주 SKALA 201 · Vue 3일차
---

# Vue 실무 도구 — Pinia·Axios·UI 라이브러리·빌드/배포

Vue 3일차. 라우터 복습 → 전역 상태관리(Pinia) → 서버 통신(Axios) → UI 라이브러리(Element Plus) → 코드 품질(ESLint/Prettier)·빌드(Vite)·환경변수·배포. 실무에서 바로 쓰는 도구들 총정리.

## 핵심 개념

- **Pinia (Store)** — 전역 반응형 상태를 관리하는 저장소 라이브러리. props 드릴링의 실무 해법.
- **State / Getter / Action** — 스토어의 3요소. 상태(데이터) / 파생 값(computed 성격) / 동작(메서드).
- **Axios** — 서버와 HTTP 통신하는 라이브러리. fetch보다 편의 기능이 많음.
- **인터셉터 (Interceptor)** — 요청/응답 중간에 끼어들어 처리(예: 토큰 자동 삽입).
- **UI 라이브러리** — 미리 만들어진 컴포넌트 모음. Element Plus, PrimeVue, Ant Design Vue, Vuetify 등.
- **ESLint** — JavaScript의 문제 패턴을 찾아 보고하는 정적 분석(linting) 도구.
- **Prettier** — 코드 포맷(공백·줄바꿈 등)을 일괄 정리하는 포매터.
- **Vite** — 빌드·번들링 도구. 설정은 `vite.config.js`.
- **환경변수 (.env)** — API 키 등 민감/환경별 값을 코드 밖으로 분리. 배포에 포함시키지 않음.

## 도메인·배경 지식

- **왜 Store(Pinia)인가**: 어제 배운 props/emits는 부모-자식 직통만 되고 먼 후손 전달은 드릴링이 필요. provide/inject도 한계. **전역 반응형 저장소(Store)**에 상태를 두면 어느 컴포넌트에서든 바로 접근·변경 → 앱이 커질수록 필수.
- **실무 라우팅 규모**: 비즈니스 앱은 메뉴가 100개 이상인 경우도 흔함. 이런 라우트를 하나하나 손으로 설정하지 않고 DB나 별도 설정에 모아 관리.
- **UI 라이브러리 선택**: 프로젝트당 보통 하나만 선택(여러 개 혼용 X). 주로 프론트엔드 엔지니어가 디자인 요구사항과의 매핑을 보고 결정. 기능은 대체로 비슷. PrimeVue가 가장 많이 쓰이고, Element Plus·Ant Design Vue 등은 중국산이며 무료로 많이 사용됨. (이번엔 쉬운 Element Plus 소개)
- **왜 JS에 Lint가 필요한가**: Java는 컴파일 단계에서 문법 오류를 잡지만, JavaScript는 인터프리터 언어라 컴파일 과정이 없어 실행 시점에야 문제가 드러남. 게다가 동적 타이핑이라 변수에 뭐가 들어올지 미리 알기 어려움. 그래서 실행 전에 **정적 분석(ESLint)**으로 잠재 오류·문법 문제를 미리 찾아 코드 일관성을 확보.
- **배포 전 검사(CI/CD 맥락)**: 배포는 모두가 보게 되므로 실수 배포를 막아야 함. CI/CD는 하루에도 여러 번 자동 배포하는데, 에러 있는 코드를 커밋하면 테스트가 중간에 멈춤 → 에러 상태로 커밋 금지. 그래서 Prettier 일괄 수정보다는 검사만 하는 식으로 운영하기도(일괄 수정은 깃 충돌 스트레스가 큼).

## 원리·메커니즘

- **Vue Router 복습(핵심)**: URL에 맞춰 해당 컴포넌트를 갈아끼우는 역할. 서버에 자원을 새로 요청하지 않고(이미 다 내려받음), JavaScript 엔진이 URL 변화를 가로채 로컬에서 컴포넌트를 찾아 바꿔치기 → SPA의 핵심. 설정은 `router/index.js`의 `createRouter`(+`createWebHistory`로 URL 기반 라우팅, `routes`에 경로↔컴포넌트 매핑).
- **Pinia 3단계**: ① `main.js`에서 `createPinia()`를 `use`로 등록(설치 시 자동), ② `defineStore('이름', 콜백)`으로 스토어 정의 — 콜백 안에 state/getter/action, ③ 컴포넌트에서 `useXxxStore()`로 가져와 사용. 변수 네이밍은 `use + 파일명 + Store` 컨벤션.
- **Store + 라우터 + 내비게이션 연동**: 라우팅 시 스토어의 로그인/권한 상태를 체크 → 미로그인이면 로그인 페이지로 리다이렉트, 로그인 상태면 원래 목적지나 대시보드로. (실제 구현은 백엔드 필요라 개념만)
- **Axios 사용**: 받은 JSON을 객체로 자동 파싱 → 객체 접근으로 바로 사용(JS가 JSON 처리에 최적화). `get/post/put/patch` 등 메서드, `create`로 기본 config(baseURL 등) 설정, 인터셉터로 토큰 주입. 여러 비동기를 한 번에 → `axios.all`(Promise.all 형태). async/await로 쓰면 로직이 깔끔(1일차 설명).
- **Vite 설정(`vite.config.js`)**: `defineConfig`로 구성. `plugins`(Vue·DevTools 등), `resolve`의 **alias(`@`)**로 절대 경로 import 설정 → 상대 경로(`../../`)의 혼란을 줄임. 환경 설정 시 자주 건드리는 파일.
- **빌드/미리보기**: `npm run build`로 정적 파일 생성 → `npm run preview`로 Node 없이 정적 서버로 로컬 확인 → 확인 후 Vercel 등에 호스팅.

## 실습·도구·코드

- **카운터 스토어(기초)**: state·getter·action 하나씩 있는 가장 단순한 Pinia 스토어를 직접 작성.
- **Config 스토어(과제 제출용)**: 날씨 앱의 설정 스토어. 온도 단위(섭씨/화씨) state를 만들고, 단위에 따라 computed로 표시 기호를 파생. (실무 스토어 코드는 공유·AI 생성이 흔하니 그때그때 확인)
- **Axios로 CRUD**: JSONPlaceholder로 GET/POST/PUT/PATCH를 테스트하는 프로토타입 코드 작성.
- **Element Plus**: 공식 사이트에서 컴포넌트 카탈로그 확인(Button, Form, Checkbox, DatePicker, Table, Tree, Carousel 등). `<el-button>`처럼 태그로 사용.
- **ESLint/Prettier 실습**: 일부러 에러를 내며 ESLint 동작 확인, Prettier가 공백/포맷을 어떻게 조정하는지 관찰. (Vue용 `eslint-plugin-vue`, Lint+Prettier 충돌 조정 도구도 존재)
- **환경변수(.env) — 과제 필수**: 샘플의 API 키를 본인 키로 바꿔 `.env`에 넣고, 그 `.env`는 배포/커밋에 포함시키지 않기. (소스나 배포본에서 키가 보이면 작업 안 한 것으로 간주)
- **배포**: `npm run build` → `npm run preview`로 로컬 확인 → Vercel 등에 호스팅해 과제 제출.

## 예시

- **온도 단위 Config 스토어** — 섭씨/화씨 state + computed 파생 기호(과제).
- **로그인 가드** — 라우팅 시 스토어의 로그인/권한 상태로 리다이렉트.
- **토큰 인터셉터** — Axios 요청 중간에 인증 토큰 자동 삽입.
- **`@` alias** — 상대 경로 대신 절대 경로로 컴포넌트·리소스 import.
- **el-button** — Element Plus 버튼 컴포넌트.

## 헷갈리는 점·질문

- Pinia 네이밍 — `defineStore` 이름 vs 변수 `useXxxStore` 컨벤션 구분.
- fetch vs Axios — Axios는 JSON 자동 파싱·인터셉터·`axios.all` 등 편의가 많음. 언제 뭘 쓸지는 프로젝트 나름.
- ESLint vs Prettier — 오류/패턴 분석(Lint) vs 포맷 정리(Prettier). 둘 다 품질 도구지만 역할이 다름. 일괄 수정은 깃 충돌 때문에 지양하기도.
- `@` alias(절대경로) — 왜 상대경로 대신 쓰나 → 경로 꼬임 방지. `vite.config.js`의 resolve에서 설정.
- .env 키 관리 — 반드시 배포/커밋에서 제외. 노출되면 과제 미이행 처리.
- Store 코드는 AI로도 잘 나옴 — 하지만 구조를 이해하고 검토할 것.

## 추가 학습·TODO

- [ ] 과제: Config 스토어(온도 단위) 완성 — state + computed 파생 기호.
- [ ] 과제 필수: API 키를 `.env`로 이동하고 배포/커밋에서 제외.
- [ ] `npm run build` → `npm run preview` 확인 → Vercel 등에 배포 제출(모레까지 마무리).
- [ ] 카운터 스토어 직접 만들어 state/getter/action 흐름 익히기.
- [ ] Axios로 CRUD 테스트, 인터셉터로 토큰 주입 실습.
- [ ] Element Plus 컴포넌트 하나 실제 화면에 적용.
- [ ] ESLint 에러 유발/Prettier 포맷 조정 관찰(코드 챌린지).
- [ ] `vite.config.js`의 `@` alias 설정 확인·활용.

## 한 줄 요약

> 전역 상태는 **Pinia(state/getter/action)**로, 서버 통신은 **Axios(JSON 자동 파싱·인터셉터)**로, 화면은 **UI 라이브러리(Element Plus)**로 빠르게 만들고, ESLint/Prettier로 품질을 지키며 Vite 빌드 → .env로 키 분리 → Vercel 배포로 마무리하는 것이 Vue 실무의 기본 흐름이다.
