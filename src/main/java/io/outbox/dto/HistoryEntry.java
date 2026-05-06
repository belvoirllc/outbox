// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.List;

public class HistoryEntry {
    private String id;
    private String timestamp;
    private String method;
    private String url;
    private List<CollectionHeader> requestHeaders;
    private String requestBody;
    private int responseStatus;
    private String responseStatusText;
    private long responseDurationMs;
    private boolean success;
    private String responseBodyPreview;

    public String getId()                          { return id; }
    public String getTimestamp()                   { return timestamp; }
    public String getMethod()                      { return method; }
    public String getUrl()                         { return url; }
    public List<CollectionHeader> getRequestHeaders() { return requestHeaders; }
    public String getRequestBody()                 { return requestBody; }
    public int getResponseStatus()                 { return responseStatus; }
    public String getResponseStatusText()          { return responseStatusText; }
    public long getResponseDurationMs()            { return responseDurationMs; }
    public boolean isSuccess()                     { return success; }
    public String getResponseBodyPreview()         { return responseBodyPreview; }

    public void setId(String id)                                        { this.id = id; }
    public void setTimestamp(String timestamp)                          { this.timestamp = timestamp; }
    public void setMethod(String method)                                { this.method = method; }
    public void setUrl(String url)                                      { this.url = url; }
    public void setRequestHeaders(List<CollectionHeader> requestHeaders){ this.requestHeaders = requestHeaders; }
    public void setRequestBody(String requestBody)                      { this.requestBody = requestBody; }
    public void setResponseStatus(int responseStatus)                   { this.responseStatus = responseStatus; }
    public void setResponseStatusText(String responseStatusText)        { this.responseStatusText = responseStatusText; }
    public void setResponseDurationMs(long responseDurationMs)          { this.responseDurationMs = responseDurationMs; }
    public void setSuccess(boolean success)                             { this.success = success; }
    public void setResponseBodyPreview(String responseBodyPreview)      { this.responseBodyPreview = responseBodyPreview; }
}
