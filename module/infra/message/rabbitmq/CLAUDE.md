# infra:message:rabbitmq

RabbitMQ 메시지 어댑터 (driven adapter). 도메인 `*MessagePort` (현재는 `SummonerMessagePort`) 의 RabbitMQ 구현체. **`message.broker=rabbitmq` 또는 미지정일 때 활성화** (`matchIfMissing = true`) — 기본 broker.

## Boundaries

- 허용: `core:lol-server-domain`, `spring-boot-starter-amqp`
- 금지: 다른 인프라 모듈 직접 호출, Kafka 어댑터와 동시 활성화 (둘 다 `SummonerMessagePort` 구현 시 빈 충돌)
- 컨슈머는 별도 워커 서비스에서 처리 — 이 모듈은 producer 만

## Layout

- `service/SummonerMessageAdapter.java` — `SummonerMessagePort` 구현 (`@ConditionalOnProperty(name = "message.broker", havingValue = "rabbitmq", matchIfMissing = true)`)
- `service/SummonerMessage.java` — 메시지 payload (Jackson2JsonMessageConverter 직렬화 대상)
- `config/RabbitMqConfig.java` — `ConnectionFactory`, `RabbitTemplate`, `Jackson2JsonMessageConverter` 빈 (모두 `@ConditionalOnProperty`)

## Key Files

- `service/SummonerMessageAdapter.java` — Producer 어댑터 reference (`rabbitTemplate.convertAndSend(exchange, routingKey, message)`)
- `config/RabbitMqConfig.java` — `CachingConnectionFactory` + JSON converter 설정. `spring.rabbitmq.*` + `rabbitmq.exchange.name`, `rabbitmq.routing.key` 프로퍼티

## Common Modifications

- **새 메시지 타입 추가**:
  1. 도메인에 `XxxMessagePort` out port 추가
  2. `service/Xxx*Message.java` payload 정의
  3. `service/Xxx*MessageAdapter.java implements XxxMessagePort` (`@ConditionalOnProperty(message.broker=rabbitmq, matchIfMissing=true)` 필수)
  4. 환경 yaml 에 exchange/routing key 프로퍼티 추가 (Exchange/Queue 선언이 필요하면 `RabbitMqConfig` 에 `@Bean Queue`, `@Bean Binding` 추가)
- **broker 전환**: 같은 port 의 Kafka 어댑터를 활성화하려면 `message.broker=kafka` 로 프로퍼티 변경 — 자동으로 RabbitMQ 비활성화

## Failure Patterns / Gotchas

- ❌ `@ConditionalOnProperty` 누락 — Kafka 어댑터와 같이 빈 등록되어 `NoUniqueBeanDefinitionException`
  ✅ 모든 어댑터/Config 에 `@ConditionalOnProperty(name = "message.broker", havingValue = "rabbitmq", matchIfMissing = true)` 명시
- ❌ Exchange / Queue 가 broker 에 없으면 message lost (publisher confirm 미설정)
  ✅ 환경 setup 시 declare 또는 `RabbitMqConfig` 에 `@Bean TopicExchange/Queue/Binding` 명시. Production 에선 publisher confirm 활성화 검토
- ❌ POJO payload 가 `Serializable` 미구현 — `Jackson2JsonMessageConverter` 가 잘 직렬화 못 하는 타입 (예: closure, JPA entity)
  ✅ 단순 record/POJO 만 메시지로 보낸다. JPA 엔티티/도메인 객체 금지
- ❌ `RabbitAutoConfiguration` 자동 활성화로 broker 부재 시 부팅 실패
  ✅ `app/application/src/main/resources/application.yml` 에 `RabbitAutoConfiguration` 이 `spring.autoconfigure.exclude` 로 제외됨 — 임포트는 환경 yaml 에서만

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (`SummonerMessagePort`)
- consumed by: `app:application` (런타임 빈 주입, 기본 broker)
- 형제: [infra:message:kafka](../kafka/CLAUDE.md) — 같은 port 의 다른 broker 구현체

## See Also

- [core:lol-server-domain](../../../core/lol-server-domain/CLAUDE.md) — `domain/summoner/application/port/out/SummonerMessagePort`
- [infra:message:kafka](../kafka/CLAUDE.md) — broker 전환 reference
- `app/application/src/main/resources/application.yml` — `message.broker` 프로퍼티 (기본 `rabbitmq`) + AutoConfiguration exclude
