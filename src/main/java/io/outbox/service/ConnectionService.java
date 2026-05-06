// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.service;

import io.outbox.dto.ConnectionSettings;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConnectionService {

    public enum Status { DISCONNECTED, CONFIGURED }

    private ConnectionSettings current;

    // String-key producer (default)
    private DefaultKafkaProducerFactory<String, SpecificRecord> factory;
    private KafkaTemplate<String, SpecificRecord> template;

    // Avro-key producer (KafkaAvroSerializer on both key and value)
    private DefaultKafkaProducerFactory<SpecificRecord, SpecificRecord> avroKeyFactory;
    private KafkaTemplate<SpecificRecord, SpecificRecord> avroKeyTemplate;

    // Generic Avro value producer (string key, generic record value)
    private DefaultKafkaProducerFactory<String, Object> genericFactory;
    private KafkaTemplate<String, Object> genericTemplate;

    // Generic Avro key+value producer (generic record key, any value)
    private DefaultKafkaProducerFactory<Object, Object> genericKeyFactory;
    private KafkaTemplate<Object, Object> genericKeyTemplate;

    private Status status = Status.DISCONNECTED;

    public ConnectionService(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.schema-registry-url}") String schemaRegistryUrl,
            @Value("${kafka.security.protocol}") String securityProtocol,
            @Value("${kafka.security.sasl-mechanism}") String saslMechanism,
            @Value("${kafka.security.api-key}") String kafkaApiKey,
            @Value("${kafka.security.api-secret}") String kafkaApiSecret,
            @Value("${kafka.schema-registry.api-key}") String srApiKey,
            @Value("${kafka.schema-registry.api-secret}") String srApiSecret) {

        current = new ConnectionSettings();
        current.setBootstrapServers(bootstrapServers);
        current.setSchemaRegistryUrl(schemaRegistryUrl);
        current.setSecurityProtocol(securityProtocol);
        current.setSaslMechanism(saslMechanism);
        current.setKafkaApiKey(kafkaApiKey);
        current.setKafkaApiSecret(kafkaApiSecret);
        current.setSrUsername(srApiKey);
        current.setSrPassword(srApiSecret);
        // Don't auto-connect on startup — wait for explicit Apply from the user.
    }

    public synchronized void apply(ConnectionSettings settings) {
        destroyFactories();
        current = settings;
        rebuildTemplates();
        status = Status.CONFIGURED;
    }

    public synchronized void disconnect() {
        destroyFactories();
        status = Status.DISCONNECTED;
    }

    public ConnectionSettings getCurrent() { return current; }

    public synchronized Status getStatus() { return status; }

    public synchronized KafkaTemplate<String, SpecificRecord> getTemplate() {
        if (template == null) throw new IllegalStateException(
                "No active connection. Configure connection settings and click Apply.");
        return template;
    }

    public synchronized KafkaTemplate<SpecificRecord, SpecificRecord> getAvroKeyTemplate() {
        if (avroKeyTemplate == null) throw new IllegalStateException(
                "No active connection. Configure connection settings and click Apply.");
        return avroKeyTemplate;
    }

    public synchronized KafkaTemplate<String, Object> getGenericTemplate() {
        if (genericTemplate == null) throw new IllegalStateException(
                "No active connection. Configure connection settings and click Apply.");
        return genericTemplate;
    }

    public synchronized KafkaTemplate<Object, Object> getGenericKeyTemplate() {
        if (genericKeyTemplate == null) throw new IllegalStateException(
                "No active connection. Configure connection settings and click Apply.");
        return genericKeyTemplate;
    }

    private void destroyFactories() {
        if (factory != null) { factory.destroy(); factory = null; template = null; }
        if (avroKeyFactory != null) { avroKeyFactory.destroy(); avroKeyFactory = null; avroKeyTemplate = null; }
        if (genericFactory != null) { genericFactory.destroy(); genericFactory = null; genericTemplate = null; }
        if (genericKeyFactory != null) { genericKeyFactory.destroy(); genericKeyFactory = null; genericKeyTemplate = null; }
    }

    private void rebuildTemplates() {
        Map<String, Object> base = baseProps();

        // String-key template
        Map<String, Object> strProps = new HashMap<>(base);
        strProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        factory = new DefaultKafkaProducerFactory<>(strProps);
        template = new KafkaTemplate<>(factory);

        // Avro-key template — KafkaAvroSerializer on both key and value
        Map<String, Object> avroProps = new HashMap<>(base);
        avroProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        avroKeyFactory = new DefaultKafkaProducerFactory<>(avroProps);
        avroKeyTemplate = new KafkaTemplate<>(avroKeyFactory);

        // Generic Avro value template — string key, KafkaAvroSerializer on value
        Map<String, Object> genericProps = new HashMap<>(base);
        genericProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        genericFactory = new DefaultKafkaProducerFactory<>(genericProps);
        genericTemplate = new KafkaTemplate<>(genericFactory);

        // Generic Avro key+value template — KafkaAvroSerializer on both
        Map<String, Object> genericKeyProps = new HashMap<>(base);
        genericKeyProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        genericKeyFactory = new DefaultKafkaProducerFactory<>(genericKeyProps);
        genericKeyTemplate = new KafkaTemplate<>(genericKeyFactory);
    }

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, current.getBootstrapServers());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, current.getSchemaRegistryUrl());
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, current.isAutoRegisterSchemas());
        props.put(KafkaAvroSerializerConfig.USE_LATEST_VERSION, current.isUseLatestVersion());

        if (StringUtils.hasText(current.getKafkaApiKey()) || StringUtils.hasText(current.getSaslJaasConfig())) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, current.getSecurityProtocol());
            props.put(SaslConfigs.SASL_MECHANISM, current.getSaslMechanism());
            String jaas = StringUtils.hasText(current.getSaslJaasConfig())
                    ? current.getSaslJaasConfig()
                    : buildJaas(current.getSaslMechanism(), current.getKafkaApiKey(), current.getKafkaApiSecret());
            props.put(SaslConfigs.SASL_JAAS_CONFIG, jaas);
        }

        String srSource = StringUtils.hasText(current.getSrCredentialsSource())
                ? current.getSrCredentialsSource() : "USER_INFO";
        props.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, srSource);
        if ("USER_INFO".equals(srSource) && StringUtils.hasText(current.getSrUsername())) {
            props.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG,
                    current.getSrUsername() + ":" + current.getSrPassword());
        }

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    private static String buildJaas(String mechanism, String username, String password) {
        String loginModule = (mechanism != null && mechanism.startsWith("SCRAM"))
                ? "org.apache.kafka.common.security.scram.ScramLoginModule"
                : "org.apache.kafka.common.security.plain.PlainLoginModule";
        return String.format("%s required username=\"%s\" password=\"%s\";", loginModule, username, password);
    }
}
