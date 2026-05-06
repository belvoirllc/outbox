// Copyright 2026 Outbox Contributors
// SPDX-License-Identifier: Apache-2.0 OR MIT
package io.outbox.service;

import jakarta.annotation.PostConstruct;
import org.apache.avro.specific.SpecificRecordBase;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Discovers Avro SpecificRecord classes on the classpath under the configured
 * scan package, and exposes them by simple name and FQCN so the UI/REST layer
 * can refer to them as strings.
 */
@Service
public class AvroClassScanner {

    private static final Logger log = LoggerFactory.getLogger(AvroClassScanner.class);

    @Value("${avro.scan-package}")
    private String scanPackage;

    private final Map<String, Class<? extends SpecificRecordBase>> classesByName = new ConcurrentHashMap<>();

    @PostConstruct
    public void scan() {
        log.info("Scanning '{}' for Avro SpecificRecord classes", scanPackage);
        Reflections reflections = new Reflections(scanPackage);
        Set<Class<? extends SpecificRecordBase>> found = reflections.getSubTypesOf(SpecificRecordBase.class);

        for (Class<? extends SpecificRecordBase> clazz : found) {
            classesByName.put(clazz.getName(), clazz);          // FQCN
            classesByName.put(clazz.getSimpleName(), clazz);    // short name
        }
        log.info("Discovered {} Avro classes: {}", found.size(),
                found.stream().map(Class::getSimpleName).collect(Collectors.toList()));
    }

    public List<String> listClassNames() {
        return classesByName.values().stream()
                .map(Class::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Optional<Class<? extends SpecificRecordBase>> findByName(String name) {
        return Optional.ofNullable(classesByName.get(name));
    }
}
