// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.service;

import io.outbox.dto.KafkaHeader;
import io.outbox.dto.ProduceRequest;
import io.outbox.dto.ProduceResult;
import io.outbox.dto.SchemaReference;
import io.outbox.dto.ValidateRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KafkaTestService {

    private static final Logger log = LoggerFactory.getLogger(KafkaTestService.class);

    private final ConnectionService connectionService;
    private final AvroClassScanner scanner;
    private final AvroDataGenerator generator;

    public KafkaTestService(ConnectionService connectionService,
                            AvroClassScanner scanner,
                            AvroDataGenerator generator) {
        this.connectionService = connectionService;
        this.scanner = scanner;
        this.generator = generator;
    }

    public ProduceResult produceAuto(ProduceRequest req) throws Exception {
        Class<? extends SpecificRecordBase> clazz = resolve(req.getClassName());
        List<ProduceResult.RecordMeta> records = new ArrayList<>();
        int count = req.getCount() == null ? 1 : req.getCount();
        for (int i = 0; i < count; i++) {
            SpecificRecord value = generator.generateRandom(clazz);
            records.add(send(req, value));
        }
        return ProduceResult.ok(records.size(), records);
    }

    public ProduceResult produceManual(ProduceRequest req) throws Exception {
        Class<? extends SpecificRecordBase> clazz = resolve(req.getClassName());
        SpecificRecord value = generator.fromJson(clazz, req.getJsonPayload());
        return ProduceResult.ok(1, List.of(send(req, value)));
    }

    public ProduceResult produceGeneric(ProduceRequest req) throws Exception {
        int count = req.getCount() == null ? 1 : req.getCount();
        List<ProduceResult.RecordMeta> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GenericRecord value = buildGenericRecord(req.getValueSchema(), req.getJsonPayload(), req.getValueReferences());
            records.add(send(req, value));
        }
        return ProduceResult.ok(records.size(), records);
    }

    public ProduceResult produceHybrid(ProduceRequest req) throws Exception {
        Class<? extends SpecificRecordBase> clazz = resolve(req.getClassName());
        List<ProduceResult.RecordMeta> records = new ArrayList<>();
        int count = req.getCount() == null ? 1 : req.getCount();
        for (int i = 0; i < count; i++) {
            SpecificRecord value = generator.generateWithOverrides(clazz, req.getOverrides());
            records.add(send(req, value));
        }
        return ProduceResult.ok(records.size(), records);
    }

    private ProduceResult.RecordMeta send(ProduceRequest req, Object value) throws Exception {
        List<Header> kafkaHeaders = buildHeaders(req.getHeaders());
        String valueJson = value instanceof GenericRecord
                ? renderGenericJson((GenericRecord) value) : renderJson((SpecificRecord) value);

        if (StringUtils.hasText(req.getKeySchema())) {
            GenericRecord keyRecord = buildGenericRecord(req.getKeySchema(), req.getKeyGenericPayload(), req.getKeyReferences());
            var record = new ProducerRecord<Object, Object>(req.getTopic(), null, null, keyRecord, value, kafkaHeaders);
            SendResult<Object, Object> result = connectionService.getGenericKeyTemplate().send(record).get();
            log.info("Sent (generic key) to {} partition={} offset={}", req.getTopic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return new ProduceResult.RecordMeta(req.getTopic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset(),
                    renderGenericJson(keyRecord), valueJson);

        } else if (StringUtils.hasText(req.getKeyClassName())) {
            SpecificRecord keyRecord = generateSpecificKey(req);
            var record = new ProducerRecord<SpecificRecord, SpecificRecord>(req.getTopic(), null, null, keyRecord, (SpecificRecord) value, kafkaHeaders);
            SendResult<SpecificRecord, SpecificRecord> result = connectionService.getAvroKeyTemplate().send(record).get();
            log.info("Sent (Avro key) to {} partition={} offset={}", req.getTopic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return new ProduceResult.RecordMeta(req.getTopic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset(),
                    renderJson(keyRecord), valueJson);

        } else {
            var record = new ProducerRecord<String, Object>(req.getTopic(), null, null, req.getKey(), value, kafkaHeaders);
            SendResult<String, Object> result = connectionService.getGenericTemplate().send(record).get();
            log.info("Sent to {} partition={} offset={}", req.getTopic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return new ProduceResult.RecordMeta(req.getTopic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset(),
                    null, valueJson);
        }
    }

    private List<Header> buildHeaders(List<KafkaHeader> headers) {
        if (headers == null || headers.isEmpty()) return List.of();
        return headers.stream()
                .filter(h -> StringUtils.hasText(h.getName()))
                .map(h -> (Header) new RecordHeader(h.getName(), h.getValue() != null ? h.getValue().getBytes() : new byte[0]))
                .toList();
    }

    public Map<String, Object> validate(ValidateRequest req) {
        try {
            buildGenericRecord(req.getSchema(), req.getPayload(), req.getReferences());
            return Map.of("valid", true);
        } catch (Exception e) {
            return Map.of("valid", false, "error", friendlyError(e));
        }
    }

    private GenericRecord buildGenericRecord(String schemaJson, String json, List<SchemaReference> refs) throws Exception {
        Schema.Parser parser = new Schema.Parser();
        if (refs != null) {
            for (SchemaReference ref : refs) {
                parser.parse(ref.getSchema());
            }
        }
        Schema schema = parser.parse(schemaJson);
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        return new GenericDatumReader<GenericRecord>(schema).read(null, decoder);
    }

    private static String friendlyError(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String msg = cause.getMessage();
        if (msg == null) msg = cause.getClass().getSimpleName();
        // trim verbose Avro stack context
        int nl = msg.indexOf('\n');
        return nl > 0 ? msg.substring(0, nl).trim() : msg.trim();
    }

    private SpecificRecord generateSpecificKey(ProduceRequest req) throws Exception {
        Class<? extends SpecificRecordBase> keyClazz = resolve(req.getKeyClassName());
        if ("manual".equals(req.getKeyMode()) && StringUtils.hasText(req.getKeyJsonPayload())) {
            return generator.fromJson(keyClazz, req.getKeyJsonPayload());
        }
        return generator.generateRandom(keyClazz);
    }

    private Class<? extends SpecificRecordBase> resolve(String name) {
        return scanner.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No SpecificRecord class found for '" + name +
                                "'. Available: " + scanner.listClassNames()));
    }

    private String renderGenericJson(GenericRecord record) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), out);
            new GenericDatumWriter<GenericRecord>(record.getSchema()).write(record, encoder);
            encoder.flush();
            return out.toString();
        } catch (Exception e) {
            return "<failed to render: " + e.getMessage() + ">";
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String renderJson(SpecificRecord record) {
        if (record == null) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), out);
            SpecificDatumWriter writer = new SpecificDatumWriter(record.getSchema());
            writer.write(record, encoder);
            encoder.flush();
            return out.toString();
        } catch (Exception e) {
            return "<failed to render: " + e.getMessage() + ">";
        }
    }
}
