// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

public class EnvVariable {
    private String key;
    private String value;

    public String getKey()   { return key; }
    public String getValue() { return value; }
    public void setKey(String key)     { this.key = key; }
    public void setValue(String value) { this.value = value; }
}
