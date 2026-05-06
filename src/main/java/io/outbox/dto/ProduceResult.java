// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.dto;

import java.util.ArrayList;
import java.util.List;

public class ProduceResult {
    private boolean success;
    private String message;
    private int sentCount;
    private List<RecordMeta> records = new ArrayList<>();

    public static ProduceResult ok(int count, List<RecordMeta> records) {
        ProduceResult r = new ProduceResult();
        r.success = true;
        r.sentCount = count;
        r.records = records;
        r.message = "Sent " + count + " record(s)";
        return r;
    }

    public static ProduceResult error(String msg) {
        ProduceResult r = new ProduceResult();
        r.success = false;
        r.message = msg;
        return r;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getSentCount() { return sentCount; }
    public List<RecordMeta> getRecords() { return records; }

    public static class RecordMeta {
        private final String topic;
        private final int partition;
        private final long offset;
        private final String keyPayload;   // Avro-JSON of the key (null for string keys)
        private final String payload;      // Avro-JSON of the value

        public RecordMeta(String topic, int partition, long offset, String keyPayload, String payload) {
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.keyPayload = keyPayload;
            this.payload = payload;
        }

        public String getTopic() { return topic; }
        public int getPartition() { return partition; }
        public long getOffset() { return offset; }
        public String getKeyPayload() { return keyPayload; }
        public String getPayload() { return payload; }
    }
}
