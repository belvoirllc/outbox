// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.List;
import java.util.Map;

public class ProduceRequest {
    private String className;                // FQCN or simple name of value SpecificRecord
    private String valueSchema;              // generic mode: Avro schema JSON
    private String topic;
    private Integer count = 1;
    private String jsonPayload;              // manual / generic mode: Avro-JSON value
    private Map<String, Object> overrides;   // hybrid mode: value field overrides

    // Key — String mode (default)
    private String key;

    // Key — Avro mode (set keyClassName to enable)
    private String keyClassName;             // if set, key is an Avro SpecificRecord
    private String keyMode = "auto";         // "auto" or "manual"
    private String keyJsonPayload;           // manual Avro key JSON

    // Key — Generic Avro mode
    private String keySchema;               // Avro schema JSON for generic key
    private String keyGenericPayload;       // JSON value for generic key

    // Referenced schemas (named types used by value or key schema)
    private List<SchemaReference> valueReferences;
    private List<SchemaReference> keyReferences;

    // Kafka message headers
    private List<KafkaHeader> headers;

    public String getClassName() { return className; }
    public void setClassName(String v) { this.className = v; }

    public String getValueSchema() { return valueSchema; }
    public void setValueSchema(String v) { this.valueSchema = v; }

    public String getTopic() { return topic; }
    public void setTopic(String v) { this.topic = v; }

    public Integer getCount() { return count; }
    public void setCount(Integer v) { this.count = v; }

    public String getJsonPayload() { return jsonPayload; }
    public void setJsonPayload(String v) { this.jsonPayload = v; }

    public Map<String, Object> getOverrides() { return overrides; }
    public void setOverrides(Map<String, Object> v) { this.overrides = v; }

    public String getKey() { return key; }
    public void setKey(String v) { this.key = v; }

    public String getKeyClassName() { return keyClassName; }
    public void setKeyClassName(String v) { this.keyClassName = v; }

    public String getKeyMode() { return keyMode; }
    public void setKeyMode(String v) { this.keyMode = v; }

    public String getKeyJsonPayload() { return keyJsonPayload; }
    public void setKeyJsonPayload(String v) { this.keyJsonPayload = v; }

    public String getKeySchema() { return keySchema; }
    public void setKeySchema(String v) { this.keySchema = v; }

    public String getKeyGenericPayload() { return keyGenericPayload; }
    public void setKeyGenericPayload(String v) { this.keyGenericPayload = v; }

    public List<SchemaReference> getValueReferences() { return valueReferences; }
    public void setValueReferences(List<SchemaReference> v) { this.valueReferences = v; }

    public List<SchemaReference> getKeyReferences() { return keyReferences; }
    public void setKeyReferences(List<SchemaReference> v) { this.keyReferences = v; }

    public List<KafkaHeader> getHeaders() { return headers; }
    public void setHeaders(List<KafkaHeader> v) { this.headers = v; }
}
