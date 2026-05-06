// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

public class ConnectionSettings {
    private String bootstrapServers = "localhost:9092";
    private String schemaRegistryUrl = "http://localhost:8081";
    private String securityProtocol = "PLAINTEXT";
    private String saslMechanism = "PLAIN";

    // Broker auth — used to auto-build JAAS config if saslJaasConfig is blank
    private String kafkaApiKey = "";
    private String kafkaApiSecret = "";

    // If set, used as-is for SASL_JAAS_CONFIG (overrides auto-build from key/secret)
    private String saslJaasConfig = "";

    // Schema Registry basic auth
    private String srCredentialsSource = "USER_INFO";
    private String srUsername = "";
    private String srPassword = "";

    private boolean autoRegisterSchemas = true;
    private boolean useLatestVersion = false;

    public boolean isAutoRegisterSchemas() { return autoRegisterSchemas; }
    public void setAutoRegisterSchemas(boolean v) { this.autoRegisterSchemas = v; }

    public boolean isUseLatestVersion() { return useLatestVersion; }
    public void setUseLatestVersion(boolean v) { this.useLatestVersion = v; }

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String v) { this.bootstrapServers = v; }

    public String getSchemaRegistryUrl() { return schemaRegistryUrl; }
    public void setSchemaRegistryUrl(String v) { this.schemaRegistryUrl = v; }

    public String getSecurityProtocol() { return securityProtocol; }
    public void setSecurityProtocol(String v) { this.securityProtocol = v; }

    public String getSaslMechanism() { return saslMechanism; }
    public void setSaslMechanism(String v) { this.saslMechanism = v; }

    public String getKafkaApiKey() { return kafkaApiKey; }
    public void setKafkaApiKey(String v) { this.kafkaApiKey = v; }

    public String getKafkaApiSecret() { return kafkaApiSecret; }
    public void setKafkaApiSecret(String v) { this.kafkaApiSecret = v; }

    public String getSaslJaasConfig() { return saslJaasConfig; }
    public void setSaslJaasConfig(String v) { this.saslJaasConfig = v; }

    public String getSrCredentialsSource() { return srCredentialsSource; }
    public void setSrCredentialsSource(String v) { this.srCredentialsSource = v; }

    public String getSrUsername() { return srUsername; }
    public void setSrUsername(String v) { this.srUsername = v; }

    public String getSrPassword() { return srPassword; }
    public void setSrPassword(String v) { this.srPassword = v; }
}
