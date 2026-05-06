// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

public class KafkaHeader {
    private String name;
    private String value;

    public String getName()  { return name; }
    public void setName(String v)  { this.name = v; }

    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
}
