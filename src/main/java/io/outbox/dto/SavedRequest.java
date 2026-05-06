// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.List;

public class SavedRequest {
    private String name;
    private String method;
    private String url;
    private List<CollectionHeader> headers;
    private String body;
    private boolean hidden;

    public String getName()   { return name; }
    public String getMethod() { return method; }
    public String getUrl()    { return url; }
    public List<CollectionHeader> getHeaders() { return headers; }
    public String getBody()   { return body; }
    public boolean isHidden() { return hidden; }

    public void setName(String name)     { this.name = name; }
    public void setMethod(String method) { this.method = method; }
    public void setUrl(String url)       { this.url = url; }
    public void setHeaders(List<CollectionHeader> headers) { this.headers = headers; }
    public void setBody(String body)     { this.body = body; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
