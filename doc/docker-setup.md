# Docker로 MySQL과 RabbitMQ 로컬 환경 구성

## 1. docker-compose.yml 파일 생성

프로젝트 루트 디렉토리에 `docker-compose.yml` 파일을 생성합니다.

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: notification-relay-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: notification_relay
      MYSQL_USER: relay_user
      MYSQL_PASSWORD: relay_password
      TZ: Asia/Seoul
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    networks:
      - notification-network

  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: notification-relay-rabbitmq
    restart: always
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"   # AMQP 포트
      - "15672:15672" # Management UI 포트
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - notification-network

volumes:
  mysql_data:
    driver: local
  rabbitmq_data:
    driver: local

networks:
  notification-network:
    driver: bridge
```

## 2. init.sql 파일 생성 (선택사항)

초기 테이블 생성이 필요한 경우 `init.sql` 파일을 생성합니다.

```sql
-- init.sql
CREATE DATABASE IF NOT EXISTS notification_relay CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE notification_relay;

-- 알림 히스토리 테이블 예시
CREATE TABLE IF NOT EXISTS notification_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 3. Docker 컨테이너 실행

### 모든 서비스 시작
```bash
docker-compose up -d
```

### 로그 확인
```bash
# 모든 서비스 로그
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f mysql
docker-compose logs -f rabbitmq
```

### 서비스 상태 확인
```bash
docker-compose ps
```

## 4. 접속 정보

### MySQL
- **Host**: localhost
- **Port**: 3306
- **Database**: notification_relay
- **Username**: relay_user (또는 root)
- **Password**: relay_password (또는 root)
- **Root Password**: root

**연결 URL**:
```
jdbc:mysql://localhost:3306/notification_relay?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

### RabbitMQ
- **AMQP Port**: 5672
- **Management UI**: http://localhost:15672
- **Username**: guest
- **Password**: guest

**Management UI 접속**:
- 브라우저에서 `http://localhost:15672` 접속
- guest/guest로 로그인

## 5. RabbitMQ Exchange 및 Queue 설정

Management UI에서 수동으로 설정하거나, 애플리케이션 코드에서 자동 생성하도록 구성할 수 있습니다.

### Management UI에서 설정 (수동)

1. http://localhost:15672 접속
2. **Exchanges** 탭 → **Add a new exchange**
   - Name: `notification.exchange`
   - Type: `topic` (또는 `direct`)
   - Durability: `Durable`
   - Auto delete: `No`

3. **Queues** 탭 → **Add a new queue**
   - Name: `notification.sms.queue`
   - Durability: `Durable`

   동일하게 다음 큐들도 생성:
   - `notification.kakao.queue`
   - `notification.email.queue`

4. **Exchanges** 탭 → `notification.exchange` 클릭 → **Bindings**
   - To queue: `notification.sms.queue`, Routing key: `notification.sms`
   - To queue: `notification.kakao.queue`, Routing key: `notification.kakao`
   - To queue: `notification.email.queue`, Routing key: `notification.email`

### 애플리케이션에서 자동 생성 (코드)

`relay-infrastructure`에 설정 클래스 추가:

```java
package com.notification.relay.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqQueueConfig {

    public static final String EXCHANGE_NAME = "notification.exchange";

    public static final String SMS_QUEUE = "notification.sms.queue";
    public static final String KAKAO_QUEUE = "notification.kakao.queue";
    public static final String EMAIL_QUEUE = "notification.email.queue";

    public static final String SMS_ROUTING_KEY = "notification.sms";
    public static final String KAKAO_ROUTING_KEY = "notification.kakao";
    public static final String EMAIL_ROUTING_KEY = "notification.email";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue smsQueue() {
        return new Queue(SMS_QUEUE, true);
    }

    @Bean
    public Queue kakaoQueue() {
        return new Queue(KAKAO_QUEUE, true);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(smsQueue)
                .to(notificationExchange)
                .with(SMS_ROUTING_KEY);
    }

    @Bean
    public Binding kakaoBinding(Queue kakaoQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(kakaoQueue)
                .to(notificationExchange)
                .with(KAKAO_ROUTING_KEY);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(notificationExchange)
                .with(EMAIL_ROUTING_KEY);
    }
}
```

## 6. 유용한 Docker 명령어

### 컨테이너 중지
```bash
docker-compose stop
```

### 컨테이너 재시작
```bash
docker-compose restart
```

### 컨테이너 중지 및 삭제
```bash
docker-compose down
```

### 볼륨까지 함께 삭제 (데이터 완전 초기화)
```bash
docker-compose down -v
```

### 특정 서비스만 재시작
```bash
docker-compose restart mysql
docker-compose restart rabbitmq
```

### MySQL 컨테이너 접속
```bash
docker exec -it notification-relay-mysql mysql -uroot -proot
```

## 7. 트러블슈팅

### MySQL 연결 오류
- 포트 3306이 이미 사용 중인 경우: docker-compose.yml에서 포트 변경 (예: "3307:3306")
- 컨테이너가 시작되지 않는 경우: `docker-compose logs mysql` 로그 확인

### RabbitMQ 연결 오류
- 포트 5672 또는 15672가 이미 사용 중인 경우: 포트 변경
- Management UI 접속 안 되는 경우: 컨테이너가 완전히 시작될 때까지 30초 정도 대기

### 데이터 초기화가 필요한 경우
```bash
docker-compose down -v
docker-compose up -d
```

## 8. 헬스 체크

### MySQL 연결 테스트
```bash
docker exec notification-relay-mysql mysql -uroot -proot -e "SELECT 1"
```

### RabbitMQ 상태 확인
```bash
docker exec notification-relay-rabbitmq rabbitmqctl status
```
