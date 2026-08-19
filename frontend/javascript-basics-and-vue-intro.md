---
title: JavaScript 기초와 Vue 입문 — 함수·비동기·모듈부터 반응형·컴포넌트까지
date: 2026-08-19
category: frontend
tags: [javascript, vue, arrow-function, async, fetch, promise, modules, event-bubbling, virtual-dom, data-binding, mvvm, vite, npm, components, directives, v-bind]
source: 광주 SKALA 201 · JavaScript/Vue
---

# JavaScript 기초와 Vue 입문 — 함수·비동기·모듈부터 반응형·컴포넌트까지

프론트엔드 과정. 오전 = JavaScript 문법 총정리, 오후 = Vue 입문(환경 설정·구조·반응형·컴포넌트). 내일 v-model 등 본격 문법으로 이어짐.

## 핵심 개념

### JavaScript

- **함수 선언 vs 함수 표현식** — 표현식은 함수를 변수에 할당하는 방식(함수도 참조값). 화살표 함수는 그 축약형.
- **화살표 함수 (Arrow Function)** — `function` 키워드·블록·`return`을 생략해 짧게 쓰는 함수. Vue에서 매우 자주 씀.
- **동적 타이핑 (Dynamic Typing)** — 타입 명시가 없어, 하나의 배열에 문자·숫자·객체를 섞어 담을 수 있음(heterogeneous).
- **객체(Object)** — property와 method를 가짐. `obj.length`처럼 property로 값 접근.
- **이벤트 버블링/캡처 (Bubbling/Capturing)** — 중첩된 요소에 이벤트가 어떤 순서로 전파되는지.
- **localStorage** — 브라우저에 데이터를 저장(껐다 켜도 유지). 보안 데이터는 저장 금지.
- **비동기(Async): fetch / Promise / async·await** — 서버 요청 등 시간이 걸리는 작업 처리. fetch의 반환값은 Promise.
- **모듈(Module): export / import** — 파일에서 필요한 것만 밖으로 노출(은닉성). `export`한 것만 `import` 가능.
- **불변성(Immutability)** — `toReversed()` 등은 원본을 건드리지 않고 새 배열을 반환.
- **shorthand / computed property key** — `{name, age}`처럼 축약, 그리고 객체의 키(key)조차 변수로 지정 가능.

### Vue

- **Virtual DOM** — 실제 DOM을 직접 조작하지 않고 가상 DOM에서 변경을 모아 한 번에 반영.
- **양방향 데이터 바인딩 (Two-way Binding) / MVVM** — Model(JS 데이터) ↔ View(화면)를 엮어줌. Model-View-ViewModel 패턴.
- **디렉티브 (Directive)** — `v-`로 시작하는 속성(attribute). 템플릿과 JS 데이터를 연결.
- **텍스트 인터폴레이션 `{{ }}`** — 변수 값을 문자열로 화면에 그대로 출력.
- **v-bind** — 요소의 속성(src, disabled 등)을 JS 변수와 바인딩.
- **컴포넌트 (Component)** — UI를 이루는 재사용 단위. 컴포넌트 안에 컴포넌트를 중첩.

## 도메인·배경 지식

- **왜 Node.js가 필요한가 (3가지, 개발 환경 전용)**: ① Vite가 Vue(.vue) 파일을 순수 HTML/CSS/JS로 빌드·번들링하는데 Vite가 Node 위에서 돎, ② npm(Node Package Manager)으로 라이브러리를 설치·관리, ③ 로컬 개발 서버로 .vue를 브라우저가 바로 볼 수 있게 함. → 프로덕션에서는 Node를 안 씀, 개발 도구에서만.
- **왜 Virtual DOM이 중요해졌나**: 과거 프론트는 한 번 뿌리면 끝이라 중요치 않았음. 하지만 실무 앱은 데이터가 많아짐(그리드 수천 행, 엑셀식 표의 컬럼 계산 등). 실제 DOM을 매번 조작하면 느려지므로, 변경을 모아 한 번에 그리는 Virtual DOM이 성능상 이점. → "화면 그리는 건 생각보다 어렵고 비싼 작업."
- **package.json 이해**: 외부 의존성을 dependencies(실행 시 필요: Pinia, Vue Router 등)와 devDependencies(개발 시에만 필요한 품질관리 도구: Prettier, Vite, 빌드 도구)로 나눠 관리. engines로 Node 실행 버전 명시.
- **AI 사용 가이드(과제 규칙)**: 3일차까지는 AI로 소스 generation 금지, 질의응답(모르는 것 물어보기)만 허용. 4일차에 generation 1회 허용. AI에 다 맡기면 "안 가르친 걸 써서" 이해 여부가 드러남 → 학습 효과를 위해 직접 칠 것.

## 원리·메커니즘

- **이벤트 버블링/캡처**: 요소는 서로 감싸는 구조(예: `div > p`). `p`를 클릭하면 `p`와 이를 감싼 `div` 양쪽에 이벤트가 걸림. 기본은 버블링 — 안쪽(`p`)이 먼저, 바깥(`div`)이 나중에 실행(버블이 퍼지듯 밖으로). 반대 순서로 처리하려면 리스너 등록 시 **캡처 옵션(true)**을 줌. → 실습에 자주 나오니 필수.
- **비동기와 Promise**: fetch는 결과를 바로 주지 않고 Promise를 반환 → `.then`/`.catch`로 이어 처리하거나, async/await로 동기 코드처럼 간결하게 작성. 서버에서 받은 JSON은 문자열 형태라 파싱해서 사용.
- **모듈의 은닉성**: 예전엔 JS를 HTML에 통째로 넣어 전역 접근이 됐음. 모듈은 `export`한 것만 밖으로 노출 → 필요한 것만 열고 나머지는 감춤(캡슐화).
- **Virtual DOM 배치 처리**: 한 이벤트 안에서 데이터가 여러 번 바뀌어도, DOM을 매번 직접 조작하지 않고 끝난 시점에 한 번에 실제 DOM에 반영 → 비용 절감.
- **데이터 바인딩 흐름**: "이 화면 속성은 저 데이터와 묶여 있다"고 선언만 하면, Model이 바뀌면 View가 자동 갱신. 역으로 사용자가 화면에서 입력하면 그 값이 Model로 반영(양방향).
- **v-bind 동작**: `{{ }}`가 변수 값을 텍스트로 출력한다면, `v-bind`는 그 값을 속성에 넣음. 예: `disabled` 변수(true/false)를 버튼의 disabled 속성에 바인딩 → 변수만 토글하면 버튼 활성/비활성이 자동으로 바뀜.

## 실습·도구·코드

- **환경 설정**: OS 무관(Windows는 WSL 등). Node.js 설치 → 이후 npm으로 라이브러리 관리, Vite로 빌드/개발 서버 실행.
- **프로젝트 구조와 실행 흐름(제일 중요)**:
  - `index.html` — 유일한 HTML. `<body>` 안에 사실상 `<div id="app">` 하나뿐 + `main.js`를 모듈로 로드. (favicon·viewport·title은 제출 시 본인 것으로 수정)
  - `main.js` — 초기화 작업 담당. Vue의 `createApp`으로 앱 생성·마운트.
  - `App.vue` — 루트 컴포넌트. `template` / `script setup` / `style` 구성.
  - 흐름: `index.html → main.js → App.vue`. 이 흐름을 정확히 이해하는 게 핵심.
- **스캐폴딩(scaffolding)**: 프로젝트 생성 시 기본 폴더·파일 자동 구성. 이후 install로 라이브러리 설치, Vue Devtools도 함께 실행.
- **컴포넌트 만들기 실습**: `App.vue`의 template/script를 비우고 `<h1>Hello SKALA Vue</h1>` 등을 넣어 `div#app` 아래로 렌더링되는지 확인 → 컴포넌트를 하나 분리해 중첩 구조 체험.
- **v-bind 실습**: 이미지 `src`를 변수와 바인딩, 버튼 `disabled`를 boolean 변수와 바인딩, 클릭 시 `true/false`를 뒤집는 토글링 함수 작성 → 변수 변화가 인터폴레이션·바인딩으로 즉시 반영되는 것 확인.
- **JS 문법 실습 포인트**: 화살표 함수, 배열(다른 타입 혼합), 이벤트 버블링/캡처, localStorage(set/get/remove), fetch+async, export/import, `flat()`, `toReversed()`, 객체 shorthand.

## 예시

- **혼합 배열** — `[1, "BMW", {...}]`처럼 타입이 다른 값을 한 배열에 담기(동적 타이핑).
- **엑셀식 그리드** — 수천 행·컬럼 사이즈 계산을 매번 하면 브라우저가 느려짐 → Virtual DOM이 필요한 맥락.
- **동의 버튼 토글** — `disabled` 변수를 클릭으로 뒤집어 버튼 잠금/해제(v-bind + 토글링).
- **다차원 배열 평탄화** — `flat()`으로 2차원 → 1차원. AI/파이썬의 행렬 데이터 처리와 연결되는 개념.

## 헷갈리는 점·질문

- 버블링 vs 캡처 — 기본은 버블링(안→밖). 캡처는 옵션으로 반대(밖→안). 실습에 자주 나오니 확실히.
- 함수 선언 vs 표현식 vs 화살표 — 셋 다 함수지만 쓰임과 문법이 다름. Vue에선 화살표를 주로 씀.
- dependencies vs devDependencies — 실행에 필요 vs 개발에만 필요. 프로덕션 빌드에 뭐가 들어가는지 구분.
- 왜 Node가 필요한가 — 프론트인데 왜? → 빌드(Vite)·패키지(npm)·개발서버 때문. 프로덕션엔 불필요.
- `{{ }}` vs `v-bind` — 텍스트로 출력 vs 속성에 값 주입. 헷갈리기 쉬움.
- 불변성 — `toReversed()`는 새 배열 반환(원본 유지). 원본을 바꾸는 메서드와 구분 필요.

## 추가 학습·TODO

- [ ] 배포 과제 — 완성본을 원하는 서버(Vercel·GitHub Pages 등)에 배포해 제출. (개별 학습 정도를 평가)
- [ ] 3일차까지 AI 소스 generation 금지 — 질의응답만. 에러 두려워 말고 직접 쳐볼 것.
- [ ] Vue Devtools로 컴포넌트 안의 컴포넌트 구조 확인해보기.
- [ ] `index.html → main.js → App.vue` 흐름 손으로 따라가며 완전히 이해.
- [ ] 내일: v-model(양방향 바인딩) 등 디렉티브 본격 학습 예정 — 오늘 한두 번 써본 감 유지.
- [ ] JS 문법 30분씩 반복(특히 화살표 함수, 비동기, 모듈) — 강사 권장.

## 한 줄 요약

> 오전엔 화살표 함수·비동기(fetch/Promise)·모듈·불변성 같은 모던 JavaScript 문법을 훑고, 오후엔 Vue로 넘어가 Virtual DOM·MVVM 반응형·컴포넌트와 `index.html → main.js → App.vue` 구조, 그리고 v-bind 바인딩까지 큰 그림을 잡았다.
