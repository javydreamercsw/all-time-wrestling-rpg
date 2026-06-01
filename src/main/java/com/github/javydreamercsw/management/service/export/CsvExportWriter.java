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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class CsvExportWriter {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public byte[] write(ExportPayload payload, String universeName) throws IOException {
    String dateSuffix = DATE_FMT.format(LocalDate.now());
    String safeName = universeName.replaceAll("[^a-zA-Z0-9_-]", "_");

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      for (Map.Entry<ExportCategory, List<Map<String, Object>>> entry : payload.data().entrySet()) {
        String entryName =
            safeName + "-" + entry.getKey().name().toLowerCase() + "-" + dateSuffix + ".csv";
        zip.putNextEntry(new ZipEntry(entryName));
        writeCsv(zip, entry.getValue());
        zip.closeEntry();
      }
    }
    return out.toByteArray();
  }

  private void writeCsv(ZipOutputStream zip, List<Map<String, Object>> rows) throws IOException {
    Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);

    if (rows.isEmpty()) {
      writer.flush();
      return;
    }

    List<String> headers = new ArrayList<>(rows.get(0).keySet());
    writer.write(toCsvLine(headers));

    for (Map<String, Object> row : rows) {
      List<String> values = new ArrayList<>();
      for (String header : headers) {
        Object val = row.get(header);
        values.add(val != null ? val.toString() : "");
      }
      writer.write(toCsvLine(values));
    }

    writer.flush();
  }

  private String toCsvLine(List<String> fields) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escapeCsvField(fields.get(i)));
    }
    sb.append("\r\n");
    return sb.toString();
  }

  // RFC 4180: wrap in quotes if the field contains comma, double-quote, or newline;
  // escape inner double-quotes by doubling them.
  private String escapeCsvField(String value) {
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
