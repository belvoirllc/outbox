# Outbox

A lightweight developer tool for producing Avro messages to Kafka and proxying REST API calls — all from a browser UI.

![Java 17+](https://img.shields.io/badge/Java-17%2B-blue) ![Maven 3.8+](https://img.shields.io/badge/Maven-3.8%2B-blue) ![License](https://img.shields.io/badge/license-Apache%202.0%20OR%20MIT-green)

---

## What it does

- **Kafka Producer** — produce messages to any topic using Avro (SpecificRecord or generic), JSON, or plain string payloads. Supports Confluent Schema Registry, SASL/SSL auth, and custom headers.
- **Fake data generation** — auto-generate realistic test payloads from your Avro schema with one click.
- **REST API client** — proxy HTTP requests (GET, POST, PUT, DELETE) through the backend, useful for testing APIs behind internal networks.
- **Environments** — save and switch between connection profiles (bootstrap servers, Schema Registry URLs, credentials).
- **Collections** — save frequently used requests and replay them instantly.
- **History** — every produce/request is logged so you can re-run or inspect past calls.

---

## Requirements

- Java 17 or later
- Maven 3.8 or later

---

## Quick start

```bash
git clone https://github.com/belvoirllc/outbox.git
cd outbox
mvn clean package -q
java -jar target/outbox-1.0.0.jar
```

Open [http://localhost:7070](http://localhost:7070) in your browser.

---

## Configuration

All settings can be overridden with environment variables — no need to edit any files.

| Environment variable        | Default                      | Description                              |
|-----------------------------|------------------------------|------------------------------------------|
| `KAFKA_BOOTSTRAP`           | `localhost:9092`             | Kafka bootstrap servers                  |
| `SCHEMA_REGISTRY_URL`       | `http://localhost:8081`      | Schema Registry URL                      |
| `KAFKA_SECURITY_PROTOCOL`   | `PLAINTEXT`                  | `PLAINTEXT` or `SASL_SSL`               |
| `KAFKA_SASL_MECHANISM`      | `PLAIN`                      | SASL mechanism                           |
| `KAFKA_API_KEY`             | _(empty)_                    | Kafka API key (Confluent Cloud)          |
| `KAFKA_API_SECRET`          | _(empty)_                    | Kafka API secret (Confluent Cloud)       |
| `SR_API_KEY`                | _(empty)_                    | Schema Registry API key                  |
| `SR_API_SECRET`             | _(empty)_                    | Schema Registry API secret               |
| `AVRO_SCAN_PACKAGE`         | `io.outbox.avro`             | Package to scan for SpecificRecord classes |

### Confluent Cloud example

```bash
KAFKA_BOOTSTRAP=pkc-xxx.us-east-1.aws.confluent.cloud:9092 \
KAFKA_SECURITY_PROTOCOL=SASL_SSL \
KAFKA_API_KEY=my-key \
KAFKA_API_SECRET=my-secret \
SCHEMA_REGISTRY_URL=https://psrc-xxx.us-east-1.aws.confluent.cloud \
SR_API_KEY=sr-key \
SR_API_SECRET=sr-secret \
java -jar target/outbox-1.0.0.jar
```

### Adding your own Avro schemas

Drop `.avsc` files into `src/main/resources/avro/` and rebuild. Outbox will code-generate the SpecificRecord classes and make them available in the UI automatically.

---

## License

Outbox source code is dual-licensed — you may use it under either:

- [Apache License, Version 2.0](LICENSE-APACHE)
- [MIT License](LICENSE-MIT)

See [LICENSE](LICENSE) for details.

### Third-party dependencies

Outbox depends on the following notable third-party libraries:

| Library | License |
|---|---|
| Spring Boot, Spring Kafka | Apache 2.0 |
| Apache Avro | Apache 2.0 |
| Apache Kafka Clients | Apache 2.0 |
| DataFaker | Apache 2.0 |
| Reflections | Apache 2.0 |
| Confluent `kafka-avro-serializer` | [Confluent Community License 1.0](https://www.confluent.io/confluent-community-license) |
| Confluent `kafka-schema-registry-client` | [Confluent Community License 1.0](https://www.confluent.io/confluent-community-license) |

The Confluent Community License permits free use, modification, and distribution for any purpose **other than** providing a competing hosted Schema Registry service. For full terms see the [Confluent Community License](https://www.confluent.io/confluent-community-license). Full third-party attribution is in [NOTICE](NOTICE).
