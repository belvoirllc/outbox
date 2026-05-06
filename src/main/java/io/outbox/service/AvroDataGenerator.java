// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.datafaker.Faker;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AvroDataGenerator {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public <T extends SpecificRecord> T generateRandom(Class<T> clazz) {
        Schema schema = SpecificData.get().getSchema(clazz);
        GenericRecord generic = buildRecord(schema);
        // Binary round-trip: GenericDatumWriter accepts raw primitives for logical
        // types; SpecificDatumReader converts them (e.g. Long → Instant).
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            new GenericDatumWriter<>(schema).write(generic, encoder);
            encoder.flush();
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(out.toByteArray(), null);
            return new SpecificDatumReader<>(clazz).read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate record for " + clazz.getName(), e);
        }
    }

    public <T extends SpecificRecord> T fromJson(Class<T> clazz, String json) throws IOException {
        Schema schema = SpecificData.get().getSchema(clazz);
        return new SpecificDatumReader<>(clazz).read(null, DecoderFactory.get().jsonDecoder(schema, json));
    }

    public <T extends SpecificRecord> T generateWithOverrides(Class<T> clazz,
                                                              Map<String, Object> overrides) throws IOException {
        if (overrides == null || overrides.isEmpty()) return generateRandom(clazz);
        T random = generateRandom(clazz);
        Schema schema = random.getSchema();
        String randomJson = toAvroJson(random, schema, clazz);
        ObjectNode node = (ObjectNode) mapper.readTree(randomJson);
        for (Map.Entry<String, Object> e : overrides.entrySet()) {
            JsonNode valueNode = mapper.valueToTree(e.getValue());
            Schema.Field field = schema.getField(e.getKey());
            if (field != null && isNullableUnion(field.schema()) && !valueNode.isNull()) {
                ObjectNode wrapped = mapper.createObjectNode();
                wrapped.set(unionBranchName(field.schema()), valueNode);
                node.set(e.getKey(), wrapped);
            } else {
                node.set(e.getKey(), valueNode);
            }
        }
        return fromJson(clazz, mapper.writeValueAsString(node));
    }

    // -------------------------------------------------------------------------
    // Record / value builders
    // -------------------------------------------------------------------------

    private GenericRecord buildRecord(Schema schema) {
        GenericData.Record record = new GenericData.Record(schema);
        for (Schema.Field field : schema.getFields()) {
            record.put(field.name(), generateValue(field.schema(), field.name()));
        }
        return record;
    }

    private Object generateValue(Schema schema, String fieldName) {
        return switch (schema.getType()) {
            case RECORD  -> buildRecord(schema);
            case UNION   -> generateUnion(schema, fieldName);
            case ARRAY   -> generateArray(schema, fieldName);
            case MAP     -> generateMap(schema, fieldName);
            case ENUM    -> new GenericData.EnumSymbol(schema, pickEnum(schema));
            case STRING  -> generateString(fieldName, schema);
            case INT     -> generateInt(fieldName, schema);
            case LONG    -> generateLong(fieldName, schema);
            case FLOAT   -> (float) faker.number().randomDouble(2, 0, 10_000);
            case DOUBLE  -> faker.number().randomDouble(2, 0, 10_000);
            case BOOLEAN -> faker.bool().bool();
            case BYTES   -> ByteBuffer.wrap(faker.lorem().characters(8).getBytes());
            case FIXED   -> new GenericData.Fixed(schema, new byte[schema.getFixedSize()]);
            case NULL    -> null;
            default      -> null;
        };
    }

    private Object generateUnion(Schema schema, String fieldName) {
        List<Schema> nonNull = schema.getTypes().stream()
                .filter(s -> s.getType() != Schema.Type.NULL)
                .collect(Collectors.toList());
        // Return null ~15% of the time for nullable unions; always generate for non-nullable
        boolean isNullable = nonNull.size() < schema.getTypes().size();
        if (isNullable && faker.number().numberBetween(0, 100) < 15) return null;
        return nonNull.isEmpty() ? null : generateValue(nonNull.get(0), fieldName);
    }

    private GenericData.Array<Object> generateArray(Schema schema, String fieldName) {
        Schema elem = schema.getElementType();
        int size = faker.number().numberBetween(1, 4);
        GenericData.Array<Object> arr = new GenericData.Array<>(size, schema);
        for (int i = 0; i < size; i++) arr.add(generateValue(elem, fieldName));
        return arr;
    }

    private Map<String, Object> generateMap(Schema schema, String fieldName) {
        Schema valSchema = schema.getValueType();
        Map<String, Object> map = new HashMap<>();
        int size = faker.number().numberBetween(1, 3);
        for (int i = 0; i < size; i++) {
            map.put(faker.lorem().word() + "_" + i, generateValue(valSchema, fieldName));
        }
        return map;
    }

    private String pickEnum(Schema schema) {
        List<String> symbols = schema.getEnumSymbols();
        return symbols.get(faker.number().numberBetween(0, symbols.size()));
    }

    // -------------------------------------------------------------------------
    // Type-specific generators with field-name hints
    // -------------------------------------------------------------------------

    private String generateString(String fieldName, Schema schema) {
        if (schema.getLogicalType() != null && "uuid".equals(schema.getLogicalType().getName())) {
            return UUID.randomUUID().toString();
        }
        String n = normalize(fieldName);
        if (fieldIs(n, "id", "uuid", "guid", "identifier"))    return UUID.randomUUID().toString();
        if (fieldIs(n, "email", "emailaddress"))                return faker.internet().emailAddress();
        if (fieldIs(n, "firstname", "fname"))                   return faker.name().firstName();
        if (fieldIs(n, "lastname", "lname", "surname"))         return faker.name().lastName();
        if (fieldIs(n, "fullname", "displayname"))              return faker.name().fullName();
        if (fieldIs(n, "name") && !n.contains("user"))         return faker.name().fullName();
        if (fieldIs(n, "username", "login", "handle"))          return faker.internet().username();
        if (fieldIs(n, "phone", "phonenumber", "mobile", "cell")) return faker.phoneNumber().phoneNumber();
        if (fieldIs(n, "address", "street", "streetaddress"))   return faker.address().streetAddress();
        if (fieldIs(n, "city"))                                 return faker.address().city();
        if (fieldIs(n, "state", "province", "region"))          return faker.address().stateAbbr();
        if (fieldIs(n, "country"))                              return faker.address().country();
        if (fieldIs(n, "zipcode", "postalcode", "zip"))        return faker.address().zipCode();
        if (fieldIs(n, "company", "organization", "employer")) return faker.company().name();
        if (fieldIs(n, "department", "dept"))                   return faker.commerce().department();
        if (fieldIs(n, "jobtitle", "title", "position"))       return faker.job().title();
        if (fieldIs(n, "password", "passwd"))                   return faker.internet().password(8, 16);
        if (fieldIs(n, "url", "website", "homepage", "link"))  return "https://" + faker.internet().domainName();
        if (fieldIs(n, "ipaddress", "ip"))                     return faker.internet().ipV4Address();
        if (fieldIs(n, "description", "desc", "bio", "summary", "about", "notes", "note", "comment", "text", "message", "content"))
                                                                return faker.lorem().sentence();
        if (fieldIs(n, "productname", "product", "item"))      return faker.commerce().productName();
        if (fieldIs(n, "color", "colour"))                     return faker.color().name();
        if (fieldIs(n, "currency", "currencycode"))            return faker.options().option("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "BRL");
        if (fieldIs(n, "sku", "code"))                         return faker.commerce().promotionCode(3);
        if (fieldIs(n, "category"))                            return faker.commerce().department();
        return faker.lorem().word();
    }

    private int generateInt(String fieldName, Schema schema) {
        if (schema.getLogicalType() != null && "date".equals(schema.getLogicalType().getName())) {
            return (int) (System.currentTimeMillis() / 86_400_000L)
                    - faker.number().numberBetween(0, 365);
        }
        String n = normalize(fieldName);
        if (fieldIs(n, "age"))                        return faker.number().numberBetween(18, 85);
        if (fieldIs(n, "year"))                       return faker.number().numberBetween(2000, 2025);
        if (fieldIs(n, "month"))                      return faker.number().numberBetween(1, 12);
        if (fieldIs(n, "day"))                        return faker.number().numberBetween(1, 28);
        if (fieldIs(n, "count", "quantity", "qty"))   return faker.number().numberBetween(1, 500);
        if (fieldIs(n, "score", "rating", "rank"))    return faker.number().numberBetween(1, 10);
        if (fieldIs(n, "price", "amount", "cost"))    return faker.number().numberBetween(1, 999);
        return faker.number().numberBetween(1, 10_000);
    }

    private long generateLong(String fieldName, Schema schema) {
        if (schema.getLogicalType() != null) {
            String lt = schema.getLogicalType().getName();
            if (lt.startsWith("timestamp")) {
                // Random timestamp within last 90 days
                long now = System.currentTimeMillis();
                return now - (long) faker.number().numberBetween(0, 90) * 86_400_000L;
            }
        }
        String n = normalize(fieldName);
        if (n.contains("timestamp") || n.contains("time") || n.endsWith("at") || n.endsWith("date")) {
            return System.currentTimeMillis() - (long) faker.number().numberBetween(0, 90) * 86_400_000L;
        }
        if (fieldIs(n, "id", "userid", "orderid", "customerid")) return (long) faker.number().numberBetween(1_000, 999_999);
        if (fieldIs(n, "price", "amount", "cost"))               return (long) faker.number().numberBetween(100, 999_99);
        return (long) faker.number().numberBetween(1, 1_000_000);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Lowercase + strip underscores/hyphens for field-name matching. */
    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase().replaceAll("[_\\-]", "");
    }

    private static boolean fieldIs(String normalized, String... keywords) {
        for (String kw : keywords) {
            if (normalized.equals(kw) || normalized.endsWith(kw) || normalized.contains(kw)) return true;
        }
        return false;
    }

    private boolean isNullableUnion(Schema schema) {
        return schema.getType() == Schema.Type.UNION
                && schema.getTypes().stream().anyMatch(s -> s.getType() == Schema.Type.NULL);
    }

    private String unionBranchName(Schema unionSchema) {
        return unionSchema.getTypes().stream()
                .filter(s -> s.getType() != Schema.Type.NULL)
                .findFirst()
                .map(Schema::getFullName)
                .orElseThrow(() -> new IllegalStateException("No non-null branch in union"));
    }

    private <T extends SpecificRecord> String toAvroJson(T record, Schema schema, Class<T> clazz) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, out);
        new SpecificDatumWriter<>(clazz).write(record, encoder);
        encoder.flush();
        return out.toString();
    }
}
