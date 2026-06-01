/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonExportWriter {

  private final ObjectMapper objectMapper;

  public byte[] write(ExportPayload payload, String universeName) throws IOException {
    ObjectMapper mapper =
        objectMapper
            .copy()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("universe", universeName);
    root.put("exportedAt", Instant.now().toString());

    Map<String, List<Map<String, Object>>> categories = new LinkedHashMap<>();
    for (Map.Entry<ExportCategory, List<Map<String, Object>>> entry : payload.data().entrySet()) {
      categories.put(entry.getKey().name(), entry.getValue());
    }
    root.put("categories", categories);

    return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
  }
}
