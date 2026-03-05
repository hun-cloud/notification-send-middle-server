# 알림 중계 시스템 (Notification Relay System)

Spring Boot 기반의 분산 알림 중계 시스템으로, Transactional Outbox 패턴과 RabbitMQ Consistent Hash Exchange를 활용하여 높은 안정성과 사용자별 순서 보장을 제공합니다.

## 목차
- [주요 포인트 (설계 고민 사항)](#주요-포인트-설계-고민-사항)
- [2주차 추가 구현 사항](#2주차-추가-구현-사항)
- [프로젝트 구조](#프로젝트-구조)
- [아키텍처](#아키텍처)
- [기술 스택](#기술-스택)
- [실행 방법](#실행-방법)
- [API 명세서](#api-명세서)

---

## 주요 포인트 (설계 고민 사항)

### 1. 메시지 손실 방지 - Transactional Outbox 패턴

**문제점**
- 알림 전송 요청 처리 중 네트워크 장애나 서버 다운 시 메시지 손실 가능
- RabbitMQ 직접 전송 방식은 트랜잭션과 메시지 발행의 원자성 보장 불가

**해결 방법**
- Transactional Outbox 패턴을 도입하여 데이터베이스 트랜잭션 내에서 메시지를 Outbox 테이블에 저장
- 별도의 스케줄러(NotificationOutboxPublisher)가 주기적으로 PENDING 상태의 메시지를 조회하여 RabbitMQ로 발행
- 발행 성공 시 PUBLISHED 상태로 변경, 실패 시 재시도 로직(최대 3회) 수행

**핵심 코드 위치**
- `relay-infrastructure/src/main/java/com/notification/relay/infrastructure/adapter/out/NotificationSenderAdapter.java`
- `relay-infrastructure/src/main/java/com/notification/relay/infrastructure/adapter/out/secheduler/NotificationOutboxPublisher.java`

### 2. 사용자별 순서 보장 - Consistent Hash Exchange

**문제점**
- 같은 사용자에게 전송되는 메시지(예: "주문 접수" → "결제 완료" → "배송 시작")의 순서가 보장되지 않으면 사용자 경험 저하
- 일반적인 RabbitMQ Topic Exchange는 여러 컨슈머가 병렬로 메시지를 처리하므로 순서 보장 불가

**해결 방법**
- RabbitMQ의 **Consistent Hash Exchange** 플러그인 활용
- `userId` 헤더를 해시 키로 사용하여 동일 사용자의 메시지는 항상 같은 큐로 라우팅
- 각 알림 타입(SMS, Email, Kakao)별로 여러 개의 큐를 생성하여 부하 분산 및 순서 보장 동시 달성
  - SMS: 2개 큐
  - Email: 4개 큐
  - Kakao: 4개 큐

**핵심 코드 위치**
- `relay-infrastructure/src/main/java/com/notification/relay/infrastructure/adapter/out/config/RabbitMqConfig.java`
- `docker-compose.yml` (Consistent Hash Exchange 플러그인 활성화)

### 3. 동시성 제어 - Optimistic Locking

**문제점**
- 여러 워커 인스턴스가 동시에 실행될 경우 같은 메시지를 중복 발행할 가능성

**해결 방법**
- JPA의 `@Version`을 사용한 낙관적 잠금(Optimistic Locking) 적용
- 동일 메시지에 대한 동시 업데이트 시 `OptimisticLockingFailureException` 발생
- 예외 발생 시 안전하게 무시하고 다음 메시지 처리

**핵심 코드 위치**
- `relay-infrastructure/src/main/java/com/notification/relay/infrastructure/persistence/entity/NotificationOutbox.java:85` (@Version 필드)

### 4. 재시도 전략

**문제점**
- 일시적 네트워크 장애나 RabbitMQ 부하로 메시지 발행이 실패할 수 있음

**해결 방법**
- 발행 실패 시 최대 3회까지 재시도
- Outbox 테이블의 `retryCount` 필드를 통한 재시도 횟수 관리
- 스케줄러가 1초마다 실행되면서 PENDING 상태의 메시지를 재시도
- 3회 재시도 후에도 실패하면 FAILED 상태로 변경하여 별도 알림/모니터링 가능

**핵심 코드 위치**
- `relay-infrastructure/src/main/java/com/notification/relay/infrastructure/adapter/out/secheduler/NotificationOutboxPublisher.java:86-92` (재시도 로직)
- `relay-infrastructure/src/main/java/com/notification/relay/infrastructure/persistence/entity/NotificationOutbox.java` (canRetry, incrementRetry 메서드)

### 5. 멀티모듈 아키텍처 - 클린 아키텍처 기반

**문제점**
- 도메인 로직과 인프라 로직의 혼재로 인한 유지보수성 저하
- 테스트 어려움 및 의존성 관리 복잡도 증가

**해결 방법**
- 헥사고날 아키텍처(Hexagonal Architecture) 원칙에 따라 7개 모듈로 분리
  - **relay-core**: 순수 도메인 로직 (외부 의존성 없음)
  - **relay-application**: 유스케이스 및 비즈니스 흐름 조율
  - **relay-infrastructure**: 데이터베이스, 메시징 등 외부 시스템 연동
  - **relay-api**: REST API 컨트롤러 및 HTTP 요청 처리
- 의존성 방향을 단방향으로 유지: API → Application → Core ← Infrastructure

**효과**
- 도메인 로직의 독립성 확보로 테스트 용이성 향상
- 인프라 기술 교체 시 도메인 레이어에 영향 없음
- 각 모듈의 책임 명확화

---

## 2주차 추가 구현 사항

### 1. Consumer Requeue 정책 - DLX + Wait Queue + Dead Queue

**구현 내용**
- RabbitMQ Dead Letter Exchange(DLX) 패턴을 활용한 실패 메시지 재처리 파이프라인 구현
- 메시지 처리 실패 시 Wait Queue(TTL: 30초)로 이동 → TTL 만료 후 원래 Exchange로 재진입
- 최대 재시도 횟수(기본 3회) 초과 시 Dead Queue로 영구 보관

**재처리 흐름**
```
메인 큐 → [처리 실패] → Wait Queue (TTL: 30초) → 원래 Exchange → 메인 큐 재진입
                                                                   ↓ (3회 초과)
                                                              Dead Queue (영구 보관)
```

**`x-death` 헤더 기반 재시도 횟수 추적**
- RabbitMQ가 DLX로 메시지를 이동시킬 때 `x-death` 헤더에 실패 이력을 자동 기록
- `MessageParser.getDeathCount()`로 현재 재시도 횟수를 추출하여 정책 적용

**핵심 코드 위치**
- `relay-worker/src/main/java/com/notification/relay/worker/config/RabbitMqConsumerConfig.java`
- `relay-infrastructure/.../config/RabbitMqConfig.java` (메인 큐 DLX 설정 추가)

**기술적 결정: Wait Queue를 채널 타입당 1개로 구성**
- 초기 설계에서는 메인 큐마다 Wait Queue를 두는 방식(예: `sms.0.wait`, `sms.1.wait`) 검토
- Wait Queue에서 TTL 만료 시 Consistent Hash Exchange로 재진입하므로, userId 기반으로 재라우팅되어 순서 보장이 유지됨
- 따라서 Wait Queue는 채널 타입당 1개(`notification.sms.wait`)로 충분하며 큐 수를 최소화

### 2. Manual Acknowledge 모드

**구현 내용**
- `acknowledge-mode: manual` 설정으로 메시지 처리 완료 후 명시적 ACK/NACK 전송
- `prefetch: 1`로 한 번에 하나의 메시지만 처리하여 순서 보장 강화

**기술적 결정: 왜 Manual Ack인가**
- Auto ACK는 메시지 수신 즉시 RabbitMQ에서 삭제하므로, 처리 중 서버 다운 시 메시지 유실
- Manual ACK는 발송 성공 후 ACK를 보내므로 처리 보장 가능
- 실무에서도 외부 API 호출이 포함된 컨슈머는 Manual ACK가 표준적인 방식

### 3. 외부 API 신뢰성 - Timeout / Retry / Circuit Breaker

**구현 내용**
- **Read Timeout**: RestClient에 연결 3초, 읽기 5초 타임아웃 적용 (무한 대기 방지)
- **Retry**: Resilience4j `@Retry`로 외부 API 실패 시 최대 3회 자동 재시도
- **Circuit Breaker**: 실패율 50% 초과 시 회로 차단, 10초 후 Half-Open 상태로 복구 시도

**장애 흐름**
```
send() 호출
  → Retry: 최대 3회 재시도
    → 모두 실패 시 Circuit Breaker 실패로 기록
      → 실패율 50% 초과 시 Circuit Open (이후 즉시 fallback)
        → fallback: ExternalApiUnavailableException 발생
          → Consumer: NACK 전송 → Wait Queue로 이동
```

**기술적 결정: Resilience4j 선택 이유**
- Spring Boot 4.0에서 Spring Retry는 AOP 기반 Circuit Breaker를 기본 제공하지 않음
- Resilience4j는 `@CircuitBreaker` + `@Retry` 애노테이션으로 선언적 적용 가능
- 경량 라이브러리로 추가 인프라 없이 단일 서비스 내에서 동작

**핵심 코드 위치**
- `relay-worker/src/main/java/com/notification/relay/worker/sender/HttpNotificationSender.java`
- `relay-worker/src/main/java/com/notification/relay/worker/config/RestClientConfig.java`
- `relay-api/src/main/resources/application.yml` (resilience4j 설정)

### 4. 멱등성 처리 - Redis 기반 중복 방지

**구현 내용**
- 메시지 처리 전 Redis에서 `notificationId` 존재 여부 확인
- 처리 완료 후 Redis에 `processed:notification:{id}` 키를 TTL 1일로 저장
- Wait Queue 재처리 시나리오에서 동일 메시지 중복 발송 방지

**기술적 결정: 인터페이스(`IdempotencyChecker`) 분리**
- Redis가 아닌 다른 저장소(DB, Memcached 등)로 변경 시 구현체만 교체
- Consumer가 구체 구현에 의존하지 않아 테스트 시 Mock 주입 용이

**핵심 코드 위치**
- `relay-worker/src/main/java/com/notification/relay/worker/idempotency/IdempotencyChecker.java` (인터페이스)
- `relay-worker/src/main/java/com/notification/relay/worker/idempotency/RedisIdempotencyChecker.java`

### 5. 발송 API 연동 - Port/Adapter 패턴

**구현 내용**
- `NotificationSender` 인터페이스로 발송 수단 추상화
- `HttpNotificationSender`: Mock Send API를 RestClient로 호출하는 HTTP 구현체

**기술적 결정: `NotificationSender` 인터페이스 분리**
- 현재 HTTP REST 방식이지만 향후 gRPC, SDK 방식으로 변경 가능
- Consumer가 구체 구현이 아닌 인터페이스에만 의존 → 발송 수단 교체 시 Consumer 코드 무변경

**핵심 코드 위치**
- `relay-worker/src/main/java/com/notification/relay/worker/sender/NotificationSender.java` (인터페이스)
- `relay-worker/src/main/java/com/notification/relay/worker/sender/HttpNotificationSender.java`

### 6. relay-worker 통합 구조

**기술적 결정: 별도 서버 없이 relay-api에 통합**
- Worker를 별도 애플리케이션으로 분리하면 배포 복잡도 증가
- 현 단계에서는 relay-worker 모듈을 relay-api 의존성으로 추가하여 단일 프로세스로 실행
- 향후 트래픽 증가 시 독립 서버로 분리 가능한 구조 유지 (모듈 경계 명확히 분리)

---

## 프로젝트 구조

### 멀티모듈 구성

```
relay/
├── relay-api/              # REST API 계층
│   ├── controller/         # API 엔드포인트
│   ├── dto/                # 요청/응답 DTO
│   └── config/             # 스케줄링 설정
│
├── relay-application/      # 응용 계층 (유스케이스)
│   ├── service/            # 비즈니스 로직 조율
│   ├── port/               # 포트 인터페이스 (in/out)
│   └── dto/                # 커맨드/쿼리 DTO
│
├── relay-core/             # 도메인 계층 (순수 비즈니스 로직)
│   ├── domain/             # 도메인 모델 (Notification, NotificationType)
│   └── exception/          # 도메인 예외
│
├── relay-infrastructure/   # 인프라 계층 (외부 시스템 연동)
│   ├── adapter/
│   │   └── out/
│   │       ├── config/     # RabbitMQ, 데이터베이스 설정
│   │       ├── messaging/  # 메시지 라우팅 전략
│   │       └── scheduler/  # Outbox 발행 스케줄러
│   ├── persistence/
│   │   ├── entity/         # JPA 엔티티 (NotificationOutbox)
│   │   └── repository/     # JPA 리포지토리
│   └── util/               # ID 생성기 등 유틸리티
│
├── relay-common/           # 공통 모듈 (공유 유틸리티)
├── relay-event/            # 이벤트 처리 모듈 (향후 확장)
└── relay-worker/           # 워커 모듈 (컨슈머, 발송 처리)
    ├── consumer/           # RabbitMQ 메시지 컨슈머 (SMS/Email/Kakao)
    ├── sender/             # 외부 발송 API 연동 (NotificationSender 인터페이스)
    ├── idempotency/        # 멱등성 처리 (Redis 기반 중복 방지)
    ├── publisher/          # Dead Letter 발행
    └── config/             # Wait Queue, Dead Queue, RestClient 설정
```

### 모듈별 의존성 관계

```
relay-api
  ├─> relay-application
  ├─> relay-infrastructure
  └─> relay-core

relay-application
  ├─> relay-core
  └─> relay-common

relay-infrastructure
  ├─> relay-application
  ├─> relay-core
  └─> relay-common

relay-core
  └─> relay-common

relay-api
  └─> relay-worker (통합 실행)

relay-worker
  (독립 모듈, Spring Boot BOM 의존)
```

---

## 아키텍처

### 전체 시스템 흐름

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP POST /api/v1/notifications
       ▼
┌─────────────────────────────────────────┐
│         relay-api (Controller)          │
│  NotificationController                 │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│    relay-application (Use Case)         │
│  SendNotificationService                │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│    relay-core (Domain Model)            │
│  Notification.create()                  │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  relay-infrastructure (Adapter)         │
│  NotificationSenderAdapter              │
│    └─> NotificationOutbox 저장 (PENDING) │
└─────────────────────────────────────────┘
               │
               │ 1초마다 스케줄 실행
               ▼
┌─────────────────────────────────────────┐
│  NotificationOutboxPublisher            │
│    1. PENDING 메시지 조회 (최대 100건)     │
│    2. RabbitMQ로 발행                    │
│    3. ACK 확인 (5초 타임아웃)              │
│    4. 상태 업데이트 (PUBLISHED/FAILED)     │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         RabbitMQ                        │
│  ┌─────────────────────────────────┐    │
│  │ Consistent Hash Exchange        │    │
│  │  (userId 헤더 기반 해시)          │   │
│  └────┬──────────┬────────┬─────────┘   │
│       │          │        │             │
│    ┌──▼───┐  ┌──▼───┐ ┌──▼───┐          │
│    │SMS.0 │  │SMS.1 │ │EMAIL │ ...      │
│    └──────┘  └──────┘ └──────┘          │
└─────────────────────────────────────────┘
               │
               ▼
       ┌──────────────────────────────────────────────┐
       │              relay-worker (relay-api 내 실행) │
       │                                              │
       │  SmsNotificationConsumer                     │
       │    1. IdempotencyChecker (Redis 중복 확인)    │
       │    2. NotificationSender (외부 API 호출)      │
       │       └─> @Retry + @CircuitBreaker           │
       │    3. ACK / NACK                             │
       │                                              │
       │  [실패 시]                                    │
       │  NACK → Wait Queue (TTL: 30초)               │
       │       → 원래 Exchange 재진입 (재시도)           │
       │  [3회 초과]                                   │
       │  Dead Queue (영구 보관)                       │
       └──────────────────────────────────────────────┘
```

### 계층별 책임

| 계층 | 모듈 | 책임 |
|------|------|------|
| **Presentation** | relay-api | HTTP 요청 수신, DTO 변환, 응답 반환 |
| **Application** | relay-application | 유스케이스 실행, 트랜잭션 경계 관리 |
| **Domain** | relay-core | 비즈니스 규칙, 도메인 모델, 포트 정의 |
| **Infrastructure** | relay-infrastructure | DB 연동, 메시징, 스케줄링, 외부 API 호출 |

---

## 기술 스택

### Backend
- **Java 25** (LTS)
- **Spring Boot 4.0.3**
  - Spring Web MVC
  - Spring Data JPA
  - Spring AMQP (RabbitMQ)
  - Spring Scheduling
- **Gradle 8.x** (멀티모듈 빌드)

### Database
- **MySQL 8.0**
  - Outbox 패턴 구현을 위한 트랜잭션 지원
  - 인덱스 최적화 (status + createdAt, userId + createdAt)

### Message Broker
- **RabbitMQ 3.12** (with Management Plugin)
  - Consistent Hash Exchange Plugin
  - Publisher Confirms 활성화
  - Durable Queue 설정

### Cache / Idempotency
- **Redis** - 멱등성 체크 (중복 메시지 처리 방지, TTL 1일)

### Resilience
- **Resilience4j 2.3.0** - Circuit Breaker + Retry (외부 API 신뢰성)
- **Apache HttpClient 5** - RestClient 커넥션 풀, Timeout 설정

### Libraries
- **Lombok** - 보일러플레이트 코드 제거
- **SpringDoc OpenAPI 3.0.1** - API 문서 자동 생성
- **JUnit 5** - 단위 테스트
- **Mockito** - 모킹 프레임워크 (RETURNS_SELF 활용 Fluent API 테스트)

### Infrastructure
- **Docker & Docker Compose** - 로컬 개발 환경 구성

---

## 실행 방법

### 사전 요구사항

- **Java 25 이상** 설치
- **Docker & Docker Compose** 설치
- **Gradle** (또는 프로젝트 내장 Gradle Wrapper 사용)

### 1. 프로젝트 클론

```bash
git clone <repository-url>
cd relay
```

### 2. Docker 환경 시작 (MySQL, RabbitMQ)

```bash
docker-compose up -d
```

**포트 확인**
- MySQL: `localhost:3306`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672` (guest/guest)

**서비스 상태 확인**
```bash
docker-compose ps
```

### 3. 애플리케이션 빌드

```bash
# Windows
.\gradlew clean build

# Linux/Mac
./gradlew clean build
```

### 4. 애플리케이션 실행

```bash
# Windows
.\gradlew :relay-api:bootRun

# Linux/Mac
./gradlew :relay-api:bootRun
```

애플리케이션이 `http://localhost:8080`에서 실행됩니다.

### 5. API 테스트

**Swagger UI 접속**
```
http://localhost:8080/swagger-ui/index.html
```

**curl 예제**
```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "message": "주문이 접수되었습니다.",
    "notificationType": "SMS"
  }'
```

**응답 예시**
```json
{
  "code": 200,
  "result": true,
  "message": "성공",
  "data": null
}
```

### 6. RabbitMQ 관리 UI에서 확인

1. `http://localhost:15672` 접속 (guest/guest)
2. **Exchanges** 탭에서 `notification.sms.exchange` 확인
3. **Queues** 탭에서 메시지 라우팅 확인

### 7. 데이터베이스 확인

```bash
# MySQL 컨테이너 접속
docker exec -it notification-relay-mysql mysql -urelay_user -prelay_password

# Outbox 테이블 조회
USE notification_relay;
SELECT * FROM notification_outbox ORDER BY created_at DESC LIMIT 10;
```

### Docker 환경 종료

```bash
# 컨테이너 정지
docker-compose stop

# 컨테이너 및 네트워크 삭제
docker-compose down

# 볼륨까지 완전 삭제 (데이터 초기화)
docker-compose down -v
```

---

## API 명세서

### 기본 정보

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`

### 엔드포인트

#### 1. 알림 전송

**요청**

```http
POST /api/v1/notifications
Content-Type: application/json

{
  "userId": "string",         // 필수: 사용자 ID
  "message": "string",        // 필수: 알림 메시지 내용
  "notificationType": "enum"  // 필수: SMS | EMAIL | KAKAO
}
```

**요청 예시**

```json
{
  "userId": "user123",
  "message": "결제가 완료되었습니다.",
  "notificationType": "SMS"
}
```

**응답 (성공)**

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "result": true,
  "message": "성공",
  "data": null
}
```

**응답 (실패)**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": 400,
  "result": false,
  "message": "잘못된 요청입니다.",
  "data": null
}
```

**NotificationType Enum 값**

| 값 | 설명 |
|------|------|
| `SMS` | 문자 메시지 |
| `EMAIL` | 이메일 |
| `KAKAO` | 카카오톡 알림 |

### Swagger UI

더 자세한 API 명세는 Swagger UI에서 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

## 추가 개선 사항 (향후 계획)

- [ ] 알림 내역 조회 API 구현
- [ ] Dead Queue 모니터링 및 수동 재처리 API
- [ ] relay-worker 독립 서버 분리 (트래픽 증가 시)
- [ ] 발송 수단별 gRPC 전환 (NotificationSender 구현체 교체)

---