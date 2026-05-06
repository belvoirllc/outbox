// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.Map;

public class RestProxyResponse {
    private boolean success;
    private int status;
    private String statusText;
    private Map<String, String> headers;
    private String body;
    private long durationMs;
    private String error;

    public boolean isSuccess()              { return success; }
    public int getStatus()                  { return status; }
    public String getStatusText()           { return statusText; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody()                 { return body; }
    public long getDurationMs()             { return durationMs; }
    public String getError()                { return error; }

    public void setSuccess(boolean success)             { this.success = success; }
    public void setStatus(int status)                   { this.status = status; }
    public void setStatusText(String statusText)        { this.statusText = statusText; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public void setBody(String body)                    { this.body = body; }
    public void setDurationMs(long durationMs)          { this.durationMs = durationMs; }
    public void setError(String error)                  { this.error = error; }
}
