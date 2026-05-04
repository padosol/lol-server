# infra:message:kafka

Kafka 메시지 어댑터 (driven adapter). 도메인 `*MessagePort` (현재는 `SummonerMessagePort`) 의 Kafka 구현체. **`message.broker=kafka` 일 때만 활성화** — RabbitMQ 와는 `@ConditionalOnProperty` 로 상호 배타.

## Boundaries

- 허용: `core:lol-server-domain`, `spring-boot-starter`, `spring-kafka`
- 금지: 다른 인프라 모듈 직접 호출, RabbitMQ 어댑터와 동시 활성화 (둘 다 `SummonerMessagePort` 를 구현하면 빈 충돌)
- 컨슈머는 별도 워커 서비스에서 실행 (이 모듈은 producer 만)

## Layout

- `service/SummonerKafkaMessageAdapter.java` — `SummonerMessagePort` 구현 (`@ConditionalOnProperty(name = "message.broker", havingValue = "kafka")`)
- `service/SummonerKafkaMessage.java` — Kafka payload (record/POJO, JsonSerializer 직렬화 대상)
- `config/KafkaProducerConfig.java` — `KafkaTemplate<String, Object>` + `ProducerFactory` 빈 (역시 `@ConditionalOnProperty`)

## Key Files

- `service/SummonerKafkaMessageAdapter.java` — 표준 Producer 어댑터 reference (`KafkaTemplate.send(...).whenComplete(...)` 비동기 + 실패 로깅)
- `config/KafkaProducerConfig.java` — Bootstrap servers, `JsonSerializer` 설정. `spring.kafka.bootstrap-servers` 프로퍼티 + `kafka.topic.*` 토픽명

## Common Modifications

- **새 토픽/메시지 추가**:
  1. 도메인에 `XxxMessagePort` out port 추가
  2. `service/Xxx*KafkaMessage.java` payload 정의 (Jackson 직렬화 가능 형태)
  3. `service/Xxx*KafkaMessageAdapter.java implements XxxMessagePort` (`@ConditionalOnProperty(message.broker=kafka)` 필수)
  4. 환경 yaml 에 `kafka.topic.<name>` 추가
- **다른 broker 와 동기화**: 같은 port 를 RabbitMQ 어댑터에도 추가하고 `@ConditionalOnProperty` 로 분기 (`infra:message:rabbitmq` 의 `SummonerMessageAdapter` 가 reference)

## Failure Patterns / Gotchas

- ❌ `@ConditionalOnProperty` 누락 — RabbitMQ 어댑터와 같이 빈 등록되어 `NoUniqueBeanDefinitionException`
  ✅ 모든 어댑터/Config 에 `@ConditionalOnProperty(name = "message.broker", havingValue = "kafka")` 명시
- ❌ `kafkaTemplate.send(...)` 결과 무시 — 발행 실패가 silent
  ✅ `whenComplete((result, ex) -> { if (ex != null) log.error(...) })` 로 실패 로깅 (재처리는 도메인 책임)
- ❌ `@KafkaListener` 컨슈머를 같은 모듈에 추가 — 본 서비스가 워커 역할까지 떠안음
  ✅ 컨슈머는 별도 워커에서 처리, 이 모듈은 producer 전용
- ❌ Kafka key 없이 `kafkaTemplate.send(topic, value)` — 파티션 분산 어긋남
  ✅ key 는 `puuid` 등 도메인 식별자로 명시 (현재 `SummonerKafkaMessageAdapter` 패턴)
- ❌ RabbitAutoConfiguration / KafkaAutoConfiguration 자동 활성화로 미사용 broker 도 부팅 시 시도
  ✅ `app/application/src/main/resources/application.yml` 에 두 AutoConfiguration 모두 `spring.autoconfigure.exclude` 처리됨 — 임포트 옵션은 환경 yaml 에서만 켠다

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (`SummonerMessagePort`)
- consumed by: `app:application` (런타임 빈 주입, `message.broker=kafka` 일 때)
- 형제: [infra:message:rabbitmq](../rabbitmq/CLAUDE.md) — 같은 port 의 다른 broker 구현체

## See Also

- [core:lol-server-domain](../../../core/lol-server-domain/CLAUDE.md) — `domain/summoner/application/port/out/SummonerMessagePort`
- [infra:message:rabbitmq](../rabbitmq/CLAUDE.md) — broker 전환 reference
- `app/application/src/main/resources/application.yml` — `message.broker` 프로퍼티 + AutoConfiguration exclude
