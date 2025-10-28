package com.github.javydreamercsw.management.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class DataMigrationService {

  private final ApplicationContext context;
  private final ObjectMapper objectMapper;
  private final EntityDependencyAnalyzer dependencyAnalyzer;

  public DataMigrationService(
      ApplicationContext context,
      ObjectMapper objectMapper,
      EntityDependencyAnalyzer dependencyAnalyzer) {
    this.context = context;
    this.objectMapper = objectMapper;
    this.dependencyAnalyzer = dependencyAnalyzer;
  }

  public enum DataFormat {
    JSON,
    CSV
  }

  public byte[] exportData(DataFormat format) throws IOException {
    if (DataFormat.JSON.equals(format)) {
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

  public void importData(DataFormat format, byte[] file) throws IOException {
    if (DataFormat.JSON.equals(format)) {
      importDataAsJson(file);
    } else {
      throw new IllegalArgumentException("Unsupported format: " + format);
    }
  }

  private void importDataAsJson(byte[] file) throws IOException {
    List<String> entityNames = dependencyAnalyzer.getAutomaticSyncOrder();

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(file))) {
      Map<String, byte[]> zipEntryContents = new HashMap<>();
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        String fileName = zipEntry.getName();
        ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
          entryBaos.write(buffer, 0, len);
        }
        zipEntryContents.put(
            fileName.substring(0, fileName.lastIndexOf('.')), entryBaos.toByteArray());
        zipEntry = zis.getNextEntry();
      }

      // Now process the entries in the correct dependency order
      for (String repositoryName : entityNames) {
        if (zipEntryContents.containsKey(repositoryName)) {
          byte[] jsonBytes = zipEntryContents.get(repositoryName);
          JpaRepository repository = context.getBean(repositoryName, JpaRepository.class);
          Class<?> entityClass =
              ResolvableType.forClass(repository.getClass())
                  .as(JpaRepository.class)
                  .getGeneric(0)
                  .resolve();

          JavaType type =
              objectMapper.getTypeFactory().constructCollectionType(List.class, entityClass);
          List<?> entities =
              objectMapper.readValue(new String(jsonBytes, StandardCharsets.UTF_8), type);

          repository.saveAll(entities);
          // Flush and clear the entity manager to ensure changes are persisted and visible
          context.getBean(EntityManager.class).flush();
          context.getBean(EntityManager.class).clear();
        }
      }
    }
  }
}
