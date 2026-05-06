// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

public class SchemaReference {
    private String name;    // full Avro type name, e.g. "com.example.Address"
    private String schema;  // Avro schema JSON for this type

    public String getName()   { return name; }
    public void setName(String v) { this.name = v; }

    public String getSchema() { return schema; }
    public void setSchema(String v) { this.schema = v; }
}
