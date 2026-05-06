// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.outbox.dto.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentsController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path DATA_FILE;

    static {
        Path dir = Path.of(System.getProperty("user.home"), ".avro-studio");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        DATA_FILE = dir.resolve("environments.json");
    }

    @GetMapping
    public List<Environment> getAll() {
        if (!Files.exists(DATA_FILE)) return new ArrayList<>();
        try {
            return MAPPER.readValue(DATA_FILE.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @PostMapping
    public ResponseEntity<List<Environment>> saveAll(@RequestBody List<Environment> environments) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(DATA_FILE.toFile(), environments);
            return ResponseEntity.ok(environments);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
