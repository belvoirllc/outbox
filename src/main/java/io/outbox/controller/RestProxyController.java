// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.controller;

import io.outbox.dto.RestProxyRequest;
import io.outbox.dto.RestProxyResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rest")
public class RestProxyController {

    private final RestClient restClient;

    public RestProxyController() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @PostMapping("/send")
    public RestProxyResponse send(@RequestBody RestProxyRequest req) {
        long start = System.currentTimeMillis();
        RestProxyResponse result = new RestProxyResponse();
        try {
            HttpMethod method = HttpMethod.valueOf(req.getMethod().toUpperCase());

            var spec = restClient.method(method).uri(req.getUrl());

            if (req.getHeaders() != null) {
                req.getHeaders().forEach((k, v) -> { if (k != null && !k.isBlank()) spec.header(k, v); });
            }

            if (req.getBody() != null && !req.getBody().isBlank()) {
                String contentType = req.getHeaders() != null
                        ? req.getHeaders().getOrDefault("Content-Type", "application/json")
                        : "application/json";
                spec.contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(req.getBody().getBytes(StandardCharsets.UTF_8));
            }

            ResponseEntity<byte[]> response = spec
                    .retrieve()
                    .onStatus(code -> true, (request, r) -> {}) // capture all statuses, never throw
                    .toEntity(byte[].class);

            long duration = System.currentTimeMillis() - start;

            Map<String, String> respHeaders = new LinkedHashMap<>();
            response.getHeaders().forEach((k, v) -> respHeaders.put(k, String.join(", ", v)));

            byte[] bodyBytes = response.getBody();
            String bodyStr = bodyBytes != null
                    ? new String(bodyBytes, StandardCharsets.UTF_8)
                    : "";

            result.setSuccess(response.getStatusCode().is2xxSuccessful());
            result.setStatus(response.getStatusCode().value());
            result.setStatusText(response.getStatusCode().toString());
            result.setHeaders(respHeaders);
            result.setBody(bodyStr);
            result.setDurationMs(duration);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setDurationMs(System.currentTimeMillis() - start);
            result.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }
}
