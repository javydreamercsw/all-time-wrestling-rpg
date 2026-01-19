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
package com.github.javydreamercsw.management.service.campaign;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CampaignScriptService {

  private final Map<String, groovy.lang.Script> scriptCache = new ConcurrentHashMap<>();

  public Object executeScript(String scriptPath, Map<String, Object> variables) {
    try {
      Binding binding = new Binding(variables);
      groovy.lang.Script script = getScript(scriptPath, binding);
      return script.run();
    } catch (Exception e) {
      log.error("Error executing campaign script: {}", scriptPath, e);
      throw new RuntimeException("Failed to execute campaign script: " + scriptPath, e);
    }
  }

  private groovy.lang.Script getScript(String scriptPath, Binding binding) throws IOException {
    // For now, we recreate the script object with the new binding.
    // In a more advanced implementation, we might cache the Script class and re-instantiate it.
    // However, GroovyShell.parse returns a Script object which is bound to a specific Binding.
    // To reuse compiled classes, we would need to use GroovyClassLoader.

    // Simple caching of raw script content or pre-parsed script logic would be better.
    // Let's use GroovyShell to parse and run for simplicity in Phase 2.

    Resource resource = new ClassPathResource("scripts/campaign/" + scriptPath);
    if (!resource.exists()) {
      throw new IOException("Script not found: " + scriptPath);
    }

    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      GroovyShell shell = new GroovyShell(binding);
      return shell.parse(reader);
    }
  }
}
