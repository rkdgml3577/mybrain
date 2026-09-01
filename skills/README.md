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

> **이 폴더가 모든 스킬의 정본(source of truth)입니다.**
> 스킬을 고칠 때는 항상 여기 `skills/<name>/` 을 고치고, `install.sh` 로 다시 배포합니다.
> 프로젝트나 `~/.claude/skills` 에 있는 것은 여기서 배포된 **사본**이라, 직접 고치지 말고 이 저장소에서 다시 설치하세요.

## 스킬이란

스킬은 `SKILL.md`(사용 지침) + 선택적 리소스(`references/`, `assets/`, `scripts/`)로 이뤄진 폴더입니다. Claude Code가 상황에 맞는 스킬을 자동으로 불러와 그 지침대로 작업합니다. 발동 여부는 `SKILL.md` 맨 위 프론트매터의 `description`으로 결정됩니다.

## 목록

| 스킬 | 하는 일 | 언제 발동 |
|---|---|---|
| [`msa-backend`](./msa-backend/) | Spring Boot + FastAPI MSA 백엔드 스캐폴딩 (Eureka·Kafka·MariaDB·Gateway, 템플릿 복제 모델) | 새 백엔드/MSA 프로젝트 시작, 서비스 추가, "MSA 뼈대 잡아줘" |

## 설치해서 쓰는 법

`install.sh` 로 정본을 대상 위치에 배포합니다. (대상 폴더는 덮어씁니다)

```bash
cd skills

./install.sh                          # 사용 가능한 스킬 목록
./install.sh msa-backend              # 글로벌(~/.claude/skills)에 설치
./install.sh msa-backend --project ~/work/my-app   # 그 프로젝트의 .claude/skills 에 설치
./install.sh --all                    # 모든 스킬을 글로벌에 설치
```

- **글로벌**(`~/.claude/skills`): 그 컴퓨터의 모든 프로젝트에서 발동
- **프로젝트**(`<프로젝트>/.claude/skills`): 그 저장소를 여는 어디서든 발동

Claude Code가 두 위치 모두 세션 시작 시 자동으로 인식합니다.
(스크립트 없이 `cp -r skills/<name> ~/.claude/skills/` 로 직접 복사해도 됩니다.)

### 다른 컴퓨터에서 처음 세팅할 때

```bash
git clone https://github.com/rkdgml3577/mybrain
cd mybrain/skills && ./install.sh --all
```

## 새 스킬 추가하는 법

1. 스킬 폴더를 `skills/<skill-name>/` 로 추가 (`SKILL.md` 필수, 이름은 kebab-case).
2. 위 **목록** 표에 한 줄 추가.
3. 커밋·푸시.
4. 쓰는 곳에 `./install.sh <skill-name>` 으로 배포.

## 스킬을 수정할 때

1. 정본(`skills/<name>/`)을 수정하고 커밋·푸시.
2. `./install.sh <name>` (또는 `--all`)으로 사용하는 위치에 다시 배포.

> 사본을 직접 고치면 다음 배포 때 덮어써져 사라집니다. 항상 정본을 고치세요.

> 스킬을 새로 만들거나 다듬을 때는 Claude Code의 `skill-creator` 스킬을 쓰면 구조·발동 정확도까지 잡아줍니다.
