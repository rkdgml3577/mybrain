---
title: Kubernetes 운영 리소스 — 배포 전략·Service 타입·ConfigMap/Secret·PV/PVC
date: 2026-08-27
category: infrastructure
tags: [kubernetes, deployment, rolling-update, blue-green, canary, service, clusterip, nodeport, loadbalancer, configmap, secret, pv, pvc, storageclass, requests-limits, kubectl, spring-ai]
source: 광주 SKALA · Kubernetes 2일차
---

# Kubernetes 운영 리소스 — 배포 전략·Service 타입·ConfigMap/Secret·PV/PVC

Kubernetes 2일차. 컨트롤 플레인 복습 → kubectl 디버깅 → 배포 전략(롤링/블루그린/카나리)과 롤백 → Service 4타입 → ConfigMap/Secret → PV/PVC/StorageClass → request/limit. (중간에 Spring AI 3대 추상화 언급) — 어제·오늘의 필수 리소스가 심화의 핵심.

## 핵심 개념

- **Deployment → ReplicaSet → Pod** — 배포의 계층 구조. Deployment가 ReplicaSet을, ReplicaSet이 Pod 개수를 관리.
- **롤링 업데이트 (Rolling Update)** — Pod를 하나씩 순차적으로 새 버전으로 교체(기본 전략).
- **블루-그린 (Blue-Green)** — 구/신 버전을 동시에 띄워놓고 트래픽을 한 방에 전환.
- **카나리 (Canary)** — 신버전으로 트래픽을 10%→점진적으로 늘려가며 검증.
- **Rollout Undo** — 이전 ReplicaSet을 다시 키워 되돌리는 롤백(빠름).
- **Service 4타입** — ClusterIP / NodePort / LoadBalancer / ExternalName (+ Headless).
- **ConfigMap / Secret** — 일반 설정 / 민감 정보(Base64 인코딩). 컨테이너에 주입.
- **PV / PVC / StorageClass** — 실제 스토리지(제공자) / 스토리지 요청 / 동적 프로비저닝 정의.
- **request / limit** — 자원 최소 요청(스케줄러가 봄) / 최대 상한(커널이 강제).
- **Spring AI 3대 추상화** — ChatClient(+ChatModel) / Embedding Model / Vector Store.

## 도메인·배경 지식

- **EKS 비용 구조(중요)**: AWS EKS는 컨트롤 플레인(마스터 노드)을 AWS가 대신 운영 → 사용자는 마스터 노드를 만질 일이 없지만, 워커 노드가 없어도 클러스터가 존재하는 것만으로 과금(월 약 65~80달러). 클러스터 하나 = 고정 비용.
- **LoadBalancer 타입의 비용 함정**: Service를 LoadBalancer로 만들면 서비스당 AWS LB(NLB 등) 하나가 자동 생성 → 비용이 큼. 무작정 쓰면 안 되고 지양하는 방향(Ingress로 묶는 등).
- **롤링 업데이트의 한계(DB 스키마)**: 앱 단 롤백은 쉬움. 하지만 DB 스키마 변경·마이그레이션이 동반되면 매우 난해. 롤링 중엔 구/신 버전이 공존하는데, 구버전은 기존 스키마, 신버전은 변경된 스키마를 봐야 함 → 스키마 변경 시점·롤백 전략이 어려움. (롤백해도 DB 스키마는 자동으로 안 되돌아감)
- **왜 어제·오늘 리소스가 심화의 핵심인가**: 컨테이너는 학교/개인 스터디로 접해봤어도 Kubernetes는 처음인 사람이 대부분. 이틀에 다 얻긴 불가능하니, Deployment·ReplicaSet·Pod / Ingress·Service·Pod / PV·PVC / ConfigMap·Secret 이 필수 세트를 확실히 잡는 게 다른 무엇보다 중요.

## 원리·메커니즘

- **컨트롤 플레인 6요소(복습·보강)**: API Server(관문)·etcd(KV DB)·Scheduler(노드 배치)·Controller Manager(리소스별 컨트롤러)·Cloud Controller Manager(CCM, AWS 연동) — 여기에 사용자가 셈에 넣기도 하는 것 포함. 워커 노드: kubelet·kube-proxy·컨테이너 런타임·CNI·CSI·Node Agent(총 6).
- **Pod 이름에 유니크 문자가 붙는 이유**: Pod는 임시적이고 스케일 아웃/인으로 개수가 계속 변동(2개↔100개) → 유니크 네이밍이 필수. 네이밍은 `type/이름`(예: `svc/...`, `pod/...`, `deployment/...`) 형태로도 지정 가능.
- **배포 전략 3종 비교**:
  - 롤링: 하나씩 순차 교체 → 구/신 공존 구간 존재(스키마 문제의 원인).
  - 블루-그린: 구/신 동시 기동 → Service의 Selector 라벨을 신버전으로 바꾸거나, 서비스를 둘 만들어 Ingress 라우팅을 전환해 한 방에 교체.
  - 카나리: 트래픽을 100%(구)→신버전 10%씩 점진 이동하며 검증.
- **Service 4타입 상세**:
  - ClusterIP(기본): 클러스터 내부용 가상 IP.
  - NodePort: 모든 노드에 포트를 열어 노드 IP로 접근(특이 케이스, 상대적으로 덜 씀).
  - LoadBalancer: 클라우드 LB 자동 생성(AWS ELB→CLB/NLB/ALB 중 NLB). 서비스당 LB 1개라 비쌈.
  - ExternalName: DNS의 CNAME을 만들어 외부(RDS 등)를 이름으로 부름.
  - Headless: ClusterIP 없음(None) → Pod IP를 직접 반환.
- **Service 포트 매핑**: `8080:80`처럼 외부(호스트/내 컴퓨터)↔내부(컨테이너) 연결 = Docker의 `-p`와 동일. Service가 여러 Pod 중 하나로 부하분산해 연결.
- **ConfigMap/Secret 주입 방식**: 개별 키를 env로 주입하거나, `configMapRef`/`secretRef`로 전체 키를 한 번에 주입. 볼륨 마운트로 ConfigMap을 디렉토리에 파일로 마운트(key/path로 특정 항목 선택). DB 패스워드 등 민감정보는 Secret(Base64).
- **PV/PVC/StorageClass**: PVC=요청, PV=실물(제공자), StorageClass=동적 프로비저닝 정의. 접근 모드 ReadWriteOnce(EBS, 단일 노드) vs 여러 노드(EFS). PVC를 지워야 볼륨이 삭제됨.
- **request vs limit**: request는 스케줄러가 보는 값(어느 노드에 배치할지 결정, 안 적으면 "0으로 간주"), limit은 노드 커널이 강제하는 상한(스케줄 결정엔 무관).
- **Spring AI 연계**: 스프링 부트(계층·어노테이션·IoC 컨테이너/컴포넌트 스캔) 위에 딱 3가지 추상화만 추가 — ChatClient(ChatModel에 프롬프팅), Embedding Model(텍스트→벡터), Vector Store(벡터 DB 반영). 나머지는 의존성이 알아서 처리(추상화=인터페이스).

## 실습·도구·코드

- **kubectl 디버깅 세트**:
  - `describe` — 리소스 상세·이벤트(문제 원인 파악). `logs`(+`--previous` 죽기 전 로그, `-c` 특정 컨테이너), `exec -it` 진입.
  - `top` — 자원 사용량(Metric Server 필요). `explain`/`api-resources` — 필드/리소스 종류 확인. `auth can-i` — 권한 확인. `-o` — 출력 포맷/특정 값 추출.
  - 상태 필터로 Running이 아닌 것만 보기 등. `k` alias로 축약.
- **배포/롤백**: 롤링 업데이트로 새 버전 반영, 문제 시 `rollout undo`로 이전 ReplicaSet 복구. 블루-그린은 Selector/Ingress 전환으로 실습.
- **Service 실습**: 타입별(ClusterIP/NodePort/LoadBalancer) 동작 확인. LoadBalancer가 AWS NLB를 만드는지 관찰(비용 인지).
- **ConfigMap/Secret 실습**: env 주입 + 볼륨 마운트(key/path). DB 패스워드는 Secret으로.
- **스토리지 실습**: PVC 생성 → StorageClass 동적 프로비저닝 → EBS(RWO)/EFS(다중 노드) 차이 확인. PVC 삭제로 볼륨 삭제.

## 예시

- **DB 스키마 마이그레이션 + 롤백** — 앱은 롤백 쉬워도 스키마는 난해(구/신 공존 구간).
- **블루-그린 = Ingress 라우팅 전환** — 구/신 동시 기동 후 한 방에 전환.
- **카나리 10%** — 신버전으로 트래픽 조금씩 이동.
- **Service=LoadBalancer → NLB 자동 생성** — 서비스당 LB 1개, 비용 주의.
- **Secret으로 DB 패스워드** — 민감정보는 Base64 Secret.

## 헷갈리는 점·질문

- 롤링 vs 블루그린 vs 카나리 — 순차 교체 / 한 방 전환 / 트래픽 점진 이동.
- 롤링의 스키마 함정 — 구/신 공존 구간에서 DB 스키마가 애매. 롤백해도 스키마는 자동 복구 X.
- Service 타입 선택 — 내부(ClusterIP), 노드 포트(NodePort), 클라우드 LB(LoadBalancer·비쌈), 외부 이름(ExternalName), IP 직접(Headless).
- request vs limit — request=스케줄러 판단(배치), limit=커널 강제(상한). request 미기재 시 0으로 간주.
- PV vs PVC vs StorageClass — 실물 / 요청 / 동적 프로비저닝. PVC 삭제해야 볼륨 삭제.
- ConfigMap vs Secret — 일반 설정 vs 민감정보(Base64).

## 추가 학습·TODO

- [ ] 필수 리소스 세트 확실히: Deployment·ReplicaSet·Pod / Ingress·Service·Pod / PV·PVC / ConfigMap·Secret.
- [ ] 롤링 업데이트 → `rollout undo` 롤백 실습. 블루-그린(Selector/Ingress 전환)도 시도.
- [ ] Service 4타입 동작 비교, LoadBalancer의 AWS LB 생성·비용 관찰.
- [ ] ConfigMap/Secret을 env·볼륨 마운트로 주입(DB 패스워드는 Secret).
- [ ] PVC 생성 → StorageClass 동적 프로비저닝 → EBS(RWO)/EFS 차이 확인.
- [ ] `describe`/`logs --previous`/`top`/`auth can-i` 등 디버깅 명령 익히기.
- [ ] (연계) Spring AI 3대 추상화(ChatClient·Embedding·Vector Store) 개념 복습.

## 한 줄 요약

> Kubernetes 실전은 **Deployment(롤링/블루그린/카나리 배포)**로 안전하게 바꾸고, Service 4타입으로 노출하며, 설정은 ConfigMap/Secret, 데이터는 PV/PVC/StorageClass로 다룬다 — 그리고 request/limit로 자원을 조절하되, EKS의 LoadBalancer·클러스터 고정비 같은 AWS 비용을 늘 함께 고려해야 한다.
