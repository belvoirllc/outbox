// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.Map;

public class RestProxyRequest {
    private String method;
    private String url;
    private Map<String, String> headers;
    private String body;

    public String getMethod()  { return method; }
    public String getUrl()     { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody()    { return body; }

    public void setMethod(String method)   { this.method = method; }
    public void setUrl(String url)         { this.url = url; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setBody(String body)       { this.body = body; }
}
