// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.outbox.dto.HistoryEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private static final int MAX_ENTRIES = 200;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path DATA_FILE;

    static {
        Path dir = Path.of(System.getProperty("user.home"), ".avro-studio");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        DATA_FILE = dir.resolve("history.json");
    }

    @GetMapping
    public List<HistoryEntry> getAll() {
        if (!Files.exists(DATA_FILE)) return new ArrayList<>();
        try {
            return MAPPER.readValue(DATA_FILE.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @PostMapping
    public ResponseEntity<Void> append(@RequestBody HistoryEntry entry) {
        try {
            List<HistoryEntry> history = getAll();
            history.add(0, entry); // newest first
            if (history.size() > MAX_ENTRIES) history = history.subList(0, MAX_ENTRIES);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(DATA_FILE.toFile(), history);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(DATA_FILE.toFile(), new ArrayList<>());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
