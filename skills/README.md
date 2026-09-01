---
title: Skills 모음
date: 2026-09-01
category: skills
tags: [skills, claude-code, tooling]
source: Claude Code 스킬 모음
---

# 🧩 Skills 모음

Claude Code에서 반복 작업을 자동화하는 **개인 스킬 라이브러리**입니다.
여기에 스킬을 쌓아두고, 필요한 컴퓨터·프로젝트에 복사해서 씁니다.

## 스킬이란

스킬은 `SKILL.md`(사용 지침) + 선택적 리소스(`references/`, `assets/`, `scripts/`)로 이뤄진 폴더입니다. Claude Code가 상황에 맞는 스킬을 자동으로 불러와 그 지침대로 작업합니다. 발동 여부는 `SKILL.md` 맨 위 프론트매터의 `description`으로 결정됩니다.

## 목록

| 스킬 | 하는 일 | 언제 발동 |
|---|---|---|
| [`msa-backend`](./msa-backend/) | Spring Boot + FastAPI MSA 백엔드 스캐폴딩 (Eureka·Kafka·MariaDB·Gateway, 템플릿 복제 모델) | 새 백엔드/MSA 프로젝트 시작, 서비스 추가, "MSA 뼈대 잡아줘" |

## 설치해서 쓰는 법

스킬 폴더를 대상 위치에 복사하면 끝입니다.

**A. 모든 프로젝트에서 쓰기 (개인 글로벌)**
```bash
cp -r skills/msa-backend ~/.claude/skills/msa-backend
```

**B. 특정 프로젝트에서만 쓰기 (프로젝트 스킬)**
```bash
cp -r skills/msa-backend <프로젝트>/.claude/skills/msa-backend
```

두 위치 모두 Claude Code가 세션 시작 시 자동으로 인식합니다.
(글로벌은 그 컴퓨터 전체, 프로젝트는 그 저장소를 여는 어디서든 적용)

## 새 스킬 추가하는 법

1. 스킬 폴더를 `skills/<skill-name>/` 로 추가 (`SKILL.md` 필수, 이름은 kebab-case).
2. 위 **목록** 표에 한 줄 추가.
3. 커밋·푸시.

> 스킬을 새로 만들거나 다듬을 때는 Claude Code의 `skill-creator` 스킬을 쓰면 구조·발동 정확도까지 잡아줍니다.
