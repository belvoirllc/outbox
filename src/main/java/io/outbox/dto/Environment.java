// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.List;

public class Environment {
    private String name;
    private boolean active;
    private List<EnvVariable> variables;

    public String getName()               { return name; }
    public boolean isActive()             { return active; }
    public List<EnvVariable> getVariables() { return variables; }
    public void setName(String name)      { this.name = name; }
    public void setActive(boolean active) { this.active = active; }
    public void setVariables(List<EnvVariable> variables) { this.variables = variables; }
}
