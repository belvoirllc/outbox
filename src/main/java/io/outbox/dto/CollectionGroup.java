// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.List;

public class CollectionGroup {
    private String name;
    private List<SavedRequest> requests;
    private boolean hidden;

    public String getName() { return name; }
    public List<SavedRequest> getRequests() { return requests; }
    public boolean isHidden() { return hidden; }
    public void setName(String name) { this.name = name; }
    public void setRequests(List<SavedRequest> requests) { this.requests = requests; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
