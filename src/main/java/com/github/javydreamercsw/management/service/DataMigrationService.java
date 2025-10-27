package com.github.javydreamercsw.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class DataMigrationService {

    private final ApplicationContext context;
    private final ObjectMapper objectMapper;

    public DataMigrationService(ApplicationContext context, ObjectMapper objectMapper) {
        this.context = context;
        this.objectMapper = objectMapper;
    }

    public byte[] exportData(String format) throws IOException {
        if ("JSON".equals(format)) {
            return exportDataAsJson();
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private byte[] exportDataAsJson() throws IOException {
        Map<String, JpaRepository> repositories = context.getBeansOfType(JpaRepository.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, JpaRepository> entry : repositories.entrySet()) {
                String repositoryName = entry.getKey();
                JpaRepository repository = entry.getValue();
                List<?> entities = repository.findAll();

                if (!entities.isEmpty()) {
                    String fileName = repositoryName + ".json";
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zos.putNextEntry(zipEntry);
                    zos.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(entities));
                    zos.closeEntry();
                }
            }
        }
        return baos.toByteArray();
    }

    public void importData(String format, byte[] file) {
        // TODO: Implement data import
    }
}
