// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.List;

public class ValidateRequest {
    private String schema;
    private String payload;
    private List<SchemaReference> references;

    public String getSchema()   { return schema; }
    public void setSchema(String v) { this.schema = v; }

    public String getPayload()  { return payload; }
    public void setPayload(String v) { this.payload = v; }

    public List<SchemaReference> getReferences() { return references; }
    public void setReferences(List<SchemaReference> v) { this.references = v; }
}
