---
title: Docker Compose와 Kubernetes 입문 — 오케스트레이션·Deployment·Service·Ingress
date: 2026-08-25
category: infrastructure
tags: [docker, docker-compose, kubernetes, orchestration, deployment, service, ingress, pod, dockerfile, msa, agile, eks]
source: 광주 SKALA · 컨테이너 2일차
---

# Docker Compose와 Kubernetes 입문 — 오케스트레이션·Deployment·Service·Ingress

컨테이너 2일차. 어제(VM vs 컨테이너·이미지·볼륨) 복습 → Dockerfile 명령어 심화 + 멀티스테이지 빌드 → Docker Compose(다중 컨테이너) → 왜 오케스트레이션인가 → Kubernetes 핵심 리소스(Pod·Deployment·Service·Ingress). 조직론(MSA·Agile)도 곁들임.

## 핵심 개념

- **Docker Compose** — 여러 컨테이너를 하나의 YAML로 정의해 한 번에 올리고 관리하는 도구.
- **멀티스테이지 빌드 (Multi-stage Build)** — 하나의 Dockerfile에서 빌드용 단계와 실행용 단계를 나눠, 최종 이미지를 가볍게 만드는 방식.
- **오케스트레이션 (Orchestration)** — 다수의 컨테이너를 자동으로 배포·복구·확장·분배해주는 것. 도구 = Kubernetes.
- **Pod** — 컨테이너를 담는 최소 실행 단위. 고유 IP를 가지나 임시적(temporary).
- **Deployment** — Pod의 개수(복제본)와 스펙(Pod 템플릿)을 정의·관리하는 리소스. 가장 중요.
- **Service** — 바뀌지 않는 고정 주소로 Pod들을 묶어 로드밸런싱해주는 리소스(부하분산·불변 주소).
- **Ingress** — 외부에서 들어오는 요청을 도메인·경로 기준으로 라우팅해 적절한 Service로 보냄.
- **ConfigMap** — 설정 값을 컨테이너에 주입하는 리소스.
- **Selector / Label** — Service가 어떤 Pod를 대상으로 할지 라벨로 찾음.

## 도메인·배경 지식

- **왜 컨테이너만으론 부족한가**: 컨테이너 3개까지는 사람이 손으로 관리 가능하지만, 30개부터는 불가능. 손으로 하던 일(재시작·재배치·트래픽 분배)이 곧 오케스트레이터의 기능 목록이 됨. → Kubernetes 필요.
- **Docker만 할 때 vs Kubernetes**: Docker는 프로세스가 죽으면 restart 옵션 정도. Kubernetes는 다른 노드로 자동 재스케줄링, 트래픽을 여러 복제본에 자동 로드밸런싱, Pod 주소가 바뀌어도 엔드포인트 자동 갱신(사람이 Nginx 설정 고칠 필요 없음).
- **MSA·Agile·2피자팀(조직론)**: 큰 앱 덩어리를 마이크로서비스로 잘게 쪼개 개별·빠른 배포를 하려는 흐름. 이를 뒷받침하는 조직이 2피자 팀(피자 두 판으로 한 끼 해결 가능한 3~7명 규모) — 작게 만들어 "2:8 법칙(소수만 일함)"을 막고 모두가 기여하게 함. Agile(Scrum, Sprint)과 결합. 현장은 폭포수 + Agile 하이브리드가 많음.
- **레지스트리·CI/CD는 케바케(현업)**: 회사·계열사마다 Harbor / ECR / Azure DevOps 등 제각각. 정답 없음.
- **입체적 사고**: 하나의 기술 요소를 배울 때 다른 쪽(스프링 IoC, 객체 생명주기 등)과 연결해서 이해하면 이해도가 올라감. (교수님 반복 강조)

## 원리·메커니즘

- **이미지↔컨테이너↔레지스트리 흐름(복습)**: Dockerfile → build → 이미지 → push → 레지스트리(Harbor 등) 저장 → run(레지스트리에서 pull) → 컨테이너 실행. (Docker Hub에서 `docker run`하면 자동 pull되는 것과 같음)
- **포트 터널링(복습)**: 호스트 포트 ↔ 컨테이너 포트를 연결(`-p 8080:80`). 왼쪽=외부(내 컴퓨터/호스트), 오른쪽=내부(컨테이너). 이 매핑이 있어야 브라우저 요청이 컨테이너까지 도달.
- **볼륨(복습)**: 컨테이너가 죽어도 데이터 유지(영속성). 컨테이너=임시라 데이터를 밖에 둠.
- **Dockerfile 명령어 정리**:
  - `FROM`(베이스 이미지) → `WORKDIR`(컨테이너 내부 작업 디렉토리, 필수) → `COPY`(호스트 파일을 이미지로) → `ENTRYPOINT`/`CMD`(실행).
  - `ADD` — build 중 호스트 파일을 컨테이너로 복사(원격/압축 해제 등 추가 기능).
  - `EXPOSE` — 어떤 포트를 쓸 "의도"를 문서화하는 역할(필수 아님, 실제 노출은 `-p`).
  - `LABEL` — key=value 메타데이터로 이미지 설명(현장에선 거의 안 씀, Dockerfile 가독성이 좋아서).
- **멀티스테이지 빌드**: 예를 들어 프론트엔드 빌드 스테이지(베이스 이미지·`npm` 설치·빌드)와 실행 스테이지를 분리 → 빌드 도구는 최종 이미지에서 빼서 가볍게. 스테이지별로 베이스 이미지·WORKDIR가 다름.
- **Kubernetes 요청 흐름**: 외부 요청 → Ingress(도메인/경로 라우팅) → Service(로드밸런싱, 불변 주소) → Pod(실제 컨테이너 처리). 배포 시엔 Harbor 이미지 → Deployment → Pod 생성.
- **왜 Service가 필요한가**: Pod는 임시라 죽고 살 때마다 IP가 바뀜. 매번 Pod IP를 쫓아다닐 수 없으니, Service가 불변 주소로 Pod들을 대표하고, Selector/Label로 대상 Pod를 자동 추적(주소 바뀌어도 엔드포인트 자동 반영).
- **왜 격리 단위가 작을수록 빠른가**: 컨테이너는 커널을 공유해 OS 부팅 시간이 없음 → VM보다 부팅이 빠르고 더 많이 올릴 수 있음. (물리 서버 → VM → 컨테이너 순으로 가벼워짐)

## 실습·도구·코드

- **Docker Compose 실습**:
  - `docker-compose.yml`에 서비스(예: DB)·환경변수(password 등)·볼륨(DB 데이터)·초기화 SQL·healthcheck(interval 5s, retry 10회)를 정의.
  - `docker compose up -d`(백그라운드)로 한 번에 기동 → `docker compose ps`로 서비스 목록, `docker compose logs`로 로그, `exec -it`로 컨테이너 진입.
  - Compose가 전용 네트워크를 만들고 서비스 이름을 호스트명으로 붙여줌 → 서비스 간 이름으로 통신.
- **컨테이너 확인 명령(복습)**: `docker ps` / `docker ps -a`, `docker exec -it <컨테이너> sh`로 진입.
- **Kubernetes 실습 환경 준비**:
  - 어제 배포한 가이드대로 실습 환경 설치·구성 → 오늘 `kubectl` 접속이 정상 동작하는지 최종 검증.
  - alias 설정: `~/.zshrc`에서 `kubectl`을 `k`로 alias(예: `k get pod`). 매번 `kubectl` 치는 수고 절감. (덤으로 셸 테마를 `agnoster`로 변경 권장)
  - AWS 연동에서 IRSA(IAM Roles for Service Accounts)가 중요.
- **핵심 리소스 8종(뒤에 상세 예정)**: Pod / Deployment / Service / Ingress / ConfigMap 등이 핵심. (전체 리소스는 수십 종)

## 예시

- **컨테이너 3개 vs 30개** — 손 관리의 한계 → 오케스트레이션 필요성의 직관.
- **2피자 팀** — 3~7명 소규모 팀 + 마이크로서비스로 개별 배포.
- **healthcheck(interval 5s, retry 10)** — 컨테이너 상태를 주기적으로 확인해 이상 시 재시도.
- **Service 불변 주소** — Pod IP가 바뀌어도 Service 주소는 그대로라 호출이 안 끊김.
- **`k get pod` alias** — `kubectl`을 `k`로 줄여 생산성↑.

## 헷갈리는 점·질문

- Pod vs Deployment — Pod는 컨테이너 담는 최소 단위(임시), Deployment는 그 Pod의 복제본·스펙을 관리(그래서 실무에선 Pod를 직접 안 만들고 Deployment로 관리).
- Service vs Ingress — Service는 내부에서 Pod를 묶어 로드밸런싱(불변 주소), Ingress는 외부 도메인/경로를 Service로 라우팅.
- 왜 Service의 주소는 안 바뀌나 — Pod가 임시라 IP가 바뀌므로, 불변 주소가 필요해 Service가 대표 역할.
- EXPOSE는 필수인가 — 아님. 의도 문서화용. 실제 노출은 `-p`.
- 멀티스테이지는 왜 — 빌드 도구를 최종 이미지에서 제외해 경량화.
- Docker vs Kubernetes 역할 — 하나 실행(Docker) vs 다수 자동 운영(Kubernetes).

## 추가 학습·TODO

- [ ] 실습 환경 최종 검증 — `kubectl` 접속 정상 동작 확인, 안 되는 동료 서포트.
- [ ] `~/.zshrc`에 `k`=`kubectl` alias 설정 (+ agnoster 테마).
- [ ] `docker-compose.yml` 작성 — DB 서비스 + 볼륨 + healthcheck + init SQL → `up -d`로 기동, `ps`/`logs`/`exec` 확인.
- [ ] 멀티스테이지 Dockerfile로 빌드/실행 분리해 이미지 경량화 실습.
- [ ] Kubernetes 핵심 8리소스(Pod/Deployment/Service/Ingress/ConfigMap 등) 개념 예습.
- [ ] 요청 흐름(Ingress → Service → Pod)과 Deployment의 복제본 개념 손으로 그려보기.
- [ ] (연계) DevOps 파트에서 CI/CD 파이프라인·GitOps(ArgoCD) 예습.

## 한 줄 요약

> 컨테이너가 수십 개로 늘면 사람이 못 하므로, Kubernetes가 자동 배포·복구·분배를 맡는다 — Deployment가 Pod 복제본을 관리하고, 임시적인 Pod를 Service의 불변 주소가 대표하며, Ingress가 외부 요청을 도메인/경로로 라우팅한다. 그 전 단계로 Docker Compose는 여러 컨테이너를 YAML 하나로 묶어 관리한다.
