---
title: Kubernetes 아키텍처 — 매니페스트·컨트롤 플레인·배포 흐름·EKS 연동
date: 2026-08-26
category: infrastructure
tags: [kubernetes, manifest, yaml, declarative, control-plane, api-server, etcd, scheduler, controller, deployment, pod, sidecar, eks, aws, irsa, cni, csi]
source: 광주 SKALA · Kubernetes 심화 (오전·오후 통합)
---

# Kubernetes 아키텍처 — 매니페스트·컨트롤 플레인·배포 흐름·EKS 연동

Kubernetes 심화. 왜 K8s인가 복습 → 선언형 매니페스트(YAML)와 리소스 → 컨트롤 플레인 5요소 → `kubectl apply` 배포 흐름 → 스펙 vs 스테이터스(컨트롤러의 일) → Pod 내부(사이드카) → EKS/AWS 연동(IRSA·로드밸런서·EBS/EFS)·클러스터 필수 애드온.

## 핵심 개념

- **매니페스트 (Manifest)** — 원하는 상태를 적은 YAML 명세(내 노트북/Git에 존재). 이걸 실행해 생기는 게 리소스(실물).
- **선언형 vs 명령형** — 선언형: "원하는 상태"를 YAML로 선언(권장). 명령형: 명령어로 직접 지시. 실무는 선언형 연습이 유리.
- **etcd** — 컨트롤 플레인(마스터 노드)의 Key-Value DB. 모든 리소스 상태·히스토리를 저장.
- **API Server** — 모든 컴포넌트가 거쳐가는 중심. 컴포넌트끼리 직접 통신하지 않고 전부 API Server를 통함.
- **Scheduler** — 어떤 Pod를 어느 노드에 올릴지 결정.
- **Controller Manager** — 각 리소스(Deployment·ReplicaSet·Service·ConfigMap 등)의 컨트롤러 모음. 스펙과 스테이터스의 차이를 없애는 역할.
- **kubelet / kube-proxy** — 워커 노드에서 Pod를 실제로 띄우고(kubelet) 네트워크를 처리(kube-proxy).
- **사이드카 (Sidecar)** — Pod 안에서 메인 컨테이너 옆에 붙는 보조 컨테이너(로그 수집 등). (Init 컨테이너도 있으나 잘 안 씀)
- **IRSA** — IAM Roles for Service Accounts. K8s 서비스어카운트에 AWS 권한을 부여(EKS 연동의 핵심).
- **CNI / CSI** — 컨테이너 네트워크 인터페이스 / 스토리지 인터페이스. 없으면 Pod가 안 뜨거나 볼륨 할당이 안 됨.

## 도메인·배경 지식

- **왜 인프라/클라우드가 어렵지만 값진가**: 프론트·백엔드·AI 코딩은 **바이브 코딩(AI 보조)**으로 어느 정도 커버되지만, 인프라는 용어·아키텍처를 모르면 바이브 코딩 자체가 불가능. 그만큼 진입장벽이 있고, 대신 인프라는 "정직"해서 내 의도대로 동작함. 클라우드가 학생들이 가장 어려워하는 코스지만, 커리어로 삼겠다는 사람도 많음.
- **K8s의 존재 이유(복습)**: 컨테이너가 많아 사람 관리 범위를 넘어설 때 필요. 한두 개면 오히려 배보다 배꼽.
- **자격증(참고)**: K8s 이해에 관심 있으면 CKA / CKAD(개발자 관점) 준비도 나쁘지 않음(비용은 듦).
- **실습 클러스터 사양**: AWS EKS(스칼라 광주), 워커 노드 M6i.xlarge(4vCPU/16GB) 2대로 시작 → 자원 부족 시 노드 스케일링(추가) 가능. 네임스페이스는 개인별(예: `scala-gj-1234`).

## 원리·메커니즘

- **매니페스트 4칸(YAML 읽는 법)**: `apiVersion`(API 그룹/버전) → `kind`(리소스 종류) → `metadata`(name·namespace·label) → `spec`(원하는 상태). 여기까지가 내가 쓰는 부분. `status`는 Kubernetes가 채움(현재 상태). Pod·Service·Ingress 등 모든 오브젝트가 이 틀을 공유.
- **컨트롤 플레인 5요소(AWS 기준)**: ① API Server(중심 관문) ② etcd(상태 DB) ③ Scheduler(노드 배치 결정) ④ Controller Manager(리소스별 컨트롤러) ⑤ Cloud Controller Manager(AWS 연동). API Server가 멈추면 새 배포는 막히지만, 이미 스케줄돼 도는 Pod는 계속 유지.
- **`kubectl apply` 배포 흐름**: `kubectl apply` → API Server가 받아 검증 → etcd 저장 → Deployment 컨트롤러가 ReplicaSet 생성 → ReplicaSet 컨트롤러가 Pod 개수를 맞춤 → Scheduler가 노드 결정 → 워커 노드의 kubelet이 컨테이너 런타임에 생성 요청 → 런타임이 이미지 pull·컨테이너 실행 → kubelet이 상태 확인·보고 → 엔드포인트 컨트롤러가 그 Pod를 Service 목록에 추가(부하분산 대상 등록).
- **스펙 vs 스테이터스 = 컨트롤러의 일**: 스펙(원하는 상태)과 스테이터스(현실)의 차이를 줄이는 게 컨트롤러. 예: 스펙에 replicas 3인데 2개만 떠 있으면 하나 더 생성.
- **요청 흐름(복습)**: 외부 요청 → Ingress(도메인·경로 라우팅) → Service(불변 주소·로드밸런싱) → Pod. 배포는 Harbor 이미지 → Deployment → Pod.
- **Pod 내부**: 메인 웹 컨테이너 + 사이드카(로그 수집 등) + (드물게) Init 컨테이너(초기 실행). Pod가 안 뜨면 아무것도 못 뜸.
- **오버레이 네트워크**: 물리적으로 떨어진 노드 간 Pod-to-Pod 통신을 가능케 하는 가상 네트워크(CNI, 예: Calico).

## 실습·도구·코드

- **kubectl 기본**: 리소스 대상 명령은 전부 `kubectl`. `k` alias 권장. `k get deploy`, `k get pod`, `-n <네임스페이스>` 옵션으로 특정 네임스페이스 조회.
- **선언형 실습**: 가급적 명령형보다 **manifest(YAML)**로 연습 → `kubectl apply -f`.
- **EKS/AWS 연동(중요)**: EKS는 IRSA·ALB·EBS CSI를 표준 제공. IRSA가 가장 중요(서비스어카운트 ↔ AWS 권한).
  - Service 타입 4종: ClusterIP / NodePort / LoadBalancer / (ExternalName). 타입을 LoadBalancer로 주면 AWS에 NLB가 자동 생성됨 → "K8s에서 Service 하나 만들었을 뿐인데 AWS 자원(NLB)이 생기는" 상황을 인지해야 함(모르면 자원이 무수히 생김).
  - Cloud Controller Manager가 AWS 자원을 생성: 노드 추가→EC2 인스턴스 등록, 노드 삭제→클러스터 제거, PVC→EBS 볼륨 생성, Pod 네트워크→VPC CNI로 VPC IP 할당.
- **클러스터 필수 애드온**: CNI(없으면 Pod 안 뜸), CoreDNS(도메인 네임 통신), Metric Server(`kubectl top`·HPA 자원 상태), Nginx Ingress Controller(경로/도메인 라우팅), CSI 드라이버(EBS/EFS)(없으면 PVC가 Pending → 볼륨 할당 실패), cert-manager(TLS 인증서 자동 발급·갱신, 443). 로그 수집기는 이번엔 제외.
- **환경 세팅 팁**: `~/.zshrc`에서 `k`=`kubectl` alias, 셸 테마 `agnoster` 적용.

## 예시

- **오토바이 사이드카** — Pod 안 메인 컨테이너 옆의 보조 컨테이너 비유.
- **Service=LoadBalancer → AWS NLB 자동 생성** — 매니페스트 한 줄이 실제 AWS 자원으로 입금되는 지점.
- **replicas 2 → 3** — 스펙과 현실의 차이를 컨트롤러가 메꿈(Pod 하나 추가).
- **API Server 다운** — 새 배포는 막혀도 기존 Pod는 계속 돎.

## 헷갈리는 점·질문

- 명세(매니페스트) vs 실물(리소스) — YAML은 Git/노트북에, 실제 객체는 etcd에. 컨트롤러가 명세→실물을 만듦.
- spec vs status — 내가 쓰는 건 spec(원하는 상태), status는 K8s가 채우는 현재 상태.
- 컨트롤 플레인 5요소 역할 — API Server(관문)/etcd(DB)/Scheduler(배치)/Controller Manager(정합)/Cloud Controller(클라우드 연동).
- 왜 컴포넌트가 직접 통신 안 하나 — 전부 API Server 경유(단일 관문). 그래서 API Server가 병목이자 핵심.
- Service LoadBalancer의 부작용 — AWS NLB가 자동 생성됨 → 비용·자원 인지 필요.
- CNI/CSI 없으면 — Pod 자체가 안 뜨거나(CNI) PVC가 Pending(CSI).

## 추가 학습·TODO

- [ ] 실습 환경 최종 검증 — `kubectl`(→`k`) 접속·alias 확인, 안 되는 동료 서포트.
- [ ] 매니페스트 4칸(apiVersion/kind/metadata/spec) 손으로 써보고 `kubectl apply`로 배포.
- [ ] `kubectl apply` 배포 흐름(API Server→etcd→Deployment→ReplicaSet→Scheduler→kubelet→엔드포인트) 그려보기.
- [ ] Service 타입 4종 차이, 특히 LoadBalancer→AWS NLB 자동 생성 실습·관찰.
- [ ] 클러스터 애드온 역할 확인(CNI/CoreDNS/Metric Server/Ingress Controller/CSI/cert-manager).
- [ ] Pod에 사이드카(로그 수집) 붙여보기.
- [ ] (관심 시) CKA/CKAD 자격증 개요 살펴보기.
- [ ] (예고) 다음: Kubernetes 아키텍처 세부 컨트롤러, Service/Ingress 상세.

## 한 줄 요약

> Kubernetes는 **YAML 매니페스트(apiVersion/kind/metadata/spec)**로 "원하는 상태"를 선언하면, **컨트롤 플레인(API Server·etcd·Scheduler·Controller·Cloud Controller)**이 그 스펙과 현실의 차이를 메꾸며 배포를 이룬다 — EKS에서는 Service 타입만 바꿔도 AWS 자원(NLB·EBS)이 자동 생성되므로 클라우드 연동(IRSA·CNI·CSI)을 함께 이해해야 한다.
