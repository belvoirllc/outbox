// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.controller;

import io.outbox.dto.ConnectionSettings;
import io.outbox.dto.ProduceRequest;
import io.outbox.dto.ProduceResult;
import io.outbox.dto.ValidateRequest;
import io.outbox.service.AvroClassScanner;
import io.outbox.service.AvroDataGenerator;
import io.outbox.service.ConnectionService;
import io.outbox.service.KafkaTestService;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestProducerController {

    private static final Logger log = LoggerFactory.getLogger(TestProducerController.class);

    private final KafkaTestService service;
    private final AvroClassScanner scanner;
    private final AvroDataGenerator generator;
    private final ConnectionService connectionService;

    public TestProducerController(KafkaTestService service,
                                  AvroClassScanner scanner,
                                  AvroDataGenerator generator,
                                  ConnectionService connectionService) {
        this.service = service;
        this.scanner = scanner;
        this.generator = generator;
        this.connectionService = connectionService;
    }

    @GetMapping("/connection")
    public ConnectionSettings getConnection() {
        return connectionService.getCurrent();
    }

    @GetMapping("/connection/status")
    public Map<String, String> connectionStatus() {
        return Map.of("status", connectionService.getStatus().name());
    }

    @PostMapping("/connection")
    public ResponseEntity<Map<String, String>> applyConnection(@RequestBody ConnectionSettings settings) {
        connectionService.apply(settings);
        log.info("Connection configured: bootstrap={} sr={}", settings.getBootstrapServers(), settings.getSchemaRegistryUrl());
        return ResponseEntity.ok(Map.of("status", "CONFIGURED"));
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Map<String, String>> disconnect() {
        connectionService.disconnect();
        log.info("Connection disconnected");
        return ResponseEntity.ok(Map.of("status", "DISCONNECTED"));
    }

    @GetMapping("/avro/classes")
    public List<String> listClasses() {
        return scanner.listClassNames();
    }

    @GetMapping("/avro/schema/{className}")
    public ResponseEntity<String> getSchema(@PathVariable String className) {
        return scanner.findByName(className)
                .map(clazz -> ResponseEntity.ok(SpecificData.get().getSchema(clazz).toString(true)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/avro/generate/{className}")
    public ResponseEntity<String> generateSample(@PathVariable String className) {
        return scanner.findByName(className)
                .map(clazz -> {
                    try {
                        SpecificRecord record = generator.generateRandom(clazz);
                        return ResponseEntity.ok(toAvroJson(record));
                    } catch (Exception e) {
                        return ResponseEntity.status(500).<String>body(e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String toAvroJson(SpecificRecord record) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), out);
        new SpecificDatumWriter(record.getSchema()).write(record, encoder);
        encoder.flush();
        return out.toString();
    }

    @PostMapping("/avro/validate")
    public Map<String, Object> validateGeneric(@RequestBody ValidateRequest req) {
        return service.validate(req);
    }

    @PostMapping("/produce/generic")
    public ResponseEntity<ProduceResult> produceGeneric(@RequestBody ProduceRequest req) {
        return run(() -> service.produceGeneric(req));
    }

    @PostMapping("/produce/auto")
    public ResponseEntity<ProduceResult> produceAuto(@RequestBody ProduceRequest req) {
        return run(() -> service.produceAuto(req));
    }

    @PostMapping("/produce/manual")
    public ResponseEntity<ProduceResult> produceManual(@RequestBody ProduceRequest req) {
        return run(() -> service.produceManual(req));
    }

    @PostMapping("/produce/hybrid")
    public ResponseEntity<ProduceResult> produceHybrid(@RequestBody ProduceRequest req) {
        return run(() -> service.produceHybrid(req));
    }

    private interface ThrowingSupplier { ProduceResult get() throws Exception; }

    private ResponseEntity<ProduceResult> run(ThrowingSupplier fn) {
        try {
            return ResponseEntity.ok(fn.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ProduceResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Produce failed", e);
            return ResponseEntity.status(500).body(
                    ProduceResult.error(e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }
}
