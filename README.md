# 🧠 mybrain — Second Brain

공부한 개발 지식을 한 곳에 모아 쌓고, Claude Code와 연동해 관리하는 개인 지식 저장소입니다.

## 목적

- 강의·책·실습에서 배운 내용을 **구조화된 마크다운**으로 축적
- 흩어진 지식을 대주제별로 분류하고, 하루 단위로 정리·복습
- 시간이 지나도 **검색·연결이 쉬운** 형태로 유지

## 폴더 구조

```
mybrain/
├── frontend/         # 프론트엔드 지식
├── backend/          # 백엔드 지식
├── ai/               # AI / 머신러닝 / 딥러닝
├── statistics/       # 통계 / 수학
├── infrastructure/   # 인프라 / 데브옵스 / 클라우드
├── inbox/            # 분류가 애매한 내용을 일단 던져두는 곳
├── daily/            # 하루 정리 합본 (날짜별)
│   └── 2026/
│       └── 2026-08-12.md
├── _templates/       # 정리본 템플릿
│   ├── section-template.md   # 강의 1개(섹션) 정리용
│   └── daily-template.md     # 하루 정리(합본)용
└── README.md
```

- **대주제 5개** (`frontend` `backend` `ai` `statistics` `infrastructure`): 실제 지식이 들어가는 곳. 세부 폴더는 내용이 쌓이면서 필요에 따라 추가합니다.
- **`inbox/`**: 어디에 넣을지 애매하면 일단 여기에. 나중에 대주제로 옮깁니다.
- **`daily/`**: 하루에 공부한 섹션들을 합쳐 정리한 합본. `daily/<연도>/<YYYY-MM-DD>.md` 형식.
- **`_templates/`**: 새 정리본을 만들 때 복사해서 쓰는 템플릿.

## 분류 규칙

1. 지식 md 파일은 내용에 맞는 **대주제 폴더**에 둡니다.
2. 대주제가 애매하면 `inbox/`에 두고 나중에 옮깁니다.
3. 하루 정리 합본은 `daily/<연도>/` 아래에 날짜 파일로 만듭니다.
4. 빈 폴더는 `.gitkeep`으로 유지합니다.

## 네이밍 규칙

- 파일·폴더 이름은 **영어 소문자 + 하이픈**(kebab-case)
  - 예: `overfitting-and-regularization.md`, `react-hooks.md`
- 하루 정리 파일은 `YYYY-MM-DD.md` 형식
  - 예: `daily/2026/2026-08-12.md`

## 프론트매터 규칙

모든 지식 md 파일 맨 위에는 아래 프론트매터를 넣습니다.

```yaml
---
title: 제목
date: YYYY-MM-DD
category: ai
tags: [tag1, tag2]
source: 출처(예: 강의 3교시)
---
```

- `title`: 문서 제목
- `date`: 작성/학습 날짜 (`YYYY-MM-DD`)
- `category`: 대주제 (`frontend` `backend` `ai` `statistics` `infrastructure` 등)
- `tags`: 검색·연결용 태그 목록
- `source`: 출처 (강의명·교시, 책, URL 등)

## 새 정리본 만드는 법

1. `_templates/section-template.md`를 복사해 알맞은 대주제 폴더에 둡니다.
2. 파일 이름은 kebab-case로 짓습니다.
3. 프론트매터를 채우고 내용을 정리합니다.
4. 하루가 끝나면 `_templates/daily-template.md`로 그날의 합본을 만듭니다.
