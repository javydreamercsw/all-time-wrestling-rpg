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
package com.github.javydreamercsw.management.sync.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the committed seed-provenance manifest into the seed-update review feature. Loading is
 * lenient: when the manifest resource is absent or malformed (e.g. before the generation task has
 * ever run), the bean is an empty manifest and seed-review detection simply degrades — startup
 * never breaks.
 */
@Configuration
public class SeedReviewConfig {

  @Bean
  public SeedProvenanceManifest seedProvenanceManifest(ObjectMapper objectMapper) {
    return SeedProvenanceManifest.load(objectMapper);
  }
}
