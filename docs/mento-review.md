Kafka/Redis Streams 교체 시 수정 범위 확대

피드백:
RabbitTemplate을 직접 사용하고 있습니다.
RabbitTemplate은 인프라 어댑터 내부로 숨기고, 애플리케이션 계층에서는 MessagePublisherPort와 같은 포트 인터페이스만 호출하도록 수정해 주세요.

이렇게 하면 Kafka, Redis Streams 등으로 교체 시 애플리케이션 레이어 수정 없이 인프라 구현체만 교체할 수 있습니다.

코드 위치: NotificationOutboxPublisher

주요 정책 하드코딩 문제

피드백:
채널별 정책을 외부 설정으로 분리해 주세요.

현재 모든 채널에 대해 5초 / 3회 정책이 하드코딩되어 있는데,
이 정책은 application.yml 등 외부 설정으로 관리해야 profile별 분리 및 운영 환경별 관리가 용이합니다.

운영 환경에 따라 재시도 횟수나 간격이 달라질 수 있으므로, 코드에 고정하는 것은 유지보수 측면에서 부담이 됩니다.

코드 위치: NotificationOutboxPublisher.java

메시지 발행 실패 체크는 있으나 라우팅 체크 부재

피드백:
현재 confirm.ack()가 true이면 즉시 outbox.markAsPublished()를 호출하고 있습니다.

하지만 Confirm Ack는 “브로커가 메시지를 수신했다”는 의미일 뿐,
실제 큐로 라우팅이 성공했는지까지 보장하지는 않습니다.

따라서 Publisher Returns 등을 통해
라우팅 실패까지 체크하는 보완 로직이 필요합니다.

코드 위치: NotificationOutboxPublisher.java

다중 서버 환경에서 ID 유니크 보장 문제

피드백:
다중 서버 환경에서 “항상 유니크”가 보장되나요?

같은 nodeId를 가진 두 서버가
같은 millisecond에 동일한 sequence를 생성하면 ID 충돌 가능성이 있습니다.

운영 환경에서 nodeId 관리 전략 또는 충돌 방지 전략이 필요합니다.

코드 위치: Snowflake.java

Snowflake 직접 구현 사유

“Snowflake”는 원래 Twitter가 제안한 알고리즘 이름이며,
Java에서는 이를 구현한 다양한 검증된 라이브러리들이 존재합니다.

직접 구현한 이유가 있을까요?

학습 목적
커스터마이징 필요
외부 라이브러리 의존성 최소화

등의 명확한 의도가 없다면,
운영 안정성 측면에서는 검증된 라이브러리 사용도 고려해볼 수 있습니다.

Requeue 정책 및 장애 복구 전략

피드백:
컨슈머 재큐잉(requeue)에 대한 복구 시나리오가 정의되어 있나요?

정상 실패 경로

컨슈머가 정상 동작 중이라면 retry/wait 큐로 직접 이동 가능

비정상 중단 경로

컨슈머가 처리 중 다운될 경우

메시지가 wait 큐로 이동하지 못하고

in-flight 상태(unacked / PEL)에 남을 수 있음

이 경우에 대한 처리 전략이 필요합니다.

아직 컨슈머 쪽 개발은 진행되지 않은 상태인가요?

---

1. 예외 타입 중심 분류에서 상태코드+원인 기반 분류로 세분화 필요
   현재는 ResourceAccessException, HttpServerErrorException 같은 예외 타입 중심으로 Retry 정책이 걸려 있어 운영 상황별 제어가 제한됩니다.
   status code(429/5xx/4xx) + 예외 원인(timeout, DNS, connection reset 등)을 함께 기준으로 Retryable / NonRetryable을 명확히 분리하면, 불필요 재시도를 줄이고
   장애 대응 일관성을 높일 수 있습니다.

2. ResourceAccessException을 통해 내부 timeout / connection fail 대응은 GOOD
   다만 현재 장점이 코드에만 암묵적으로 남아 있으므로, “어떤 timeout/connection 오류를 재시도 대상으로 본다”는 세부적인 부분까지 고려하면 좋겠습니다!

3. 저트래픽 환경에서 Circuit Breaker가 OPEN→CLOSED로 늦게 복귀하는 케이스의 고민
   장애 해소 후에도 호출량이 낮으면 Half-Open 검증 기회가 부족해 정상 복귀가 지연될 수 있습니다.
   저트래픽 상황도 고려하여 해결책을 고민해보면 좋겠습니다!