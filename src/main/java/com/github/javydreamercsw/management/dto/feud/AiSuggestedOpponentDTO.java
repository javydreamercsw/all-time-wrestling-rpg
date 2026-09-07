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
package com.github.javydreamercsw.management.dto.feud;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * AI's single opponent suggestion for a story arc beat. Tolerates extra fields and accepts {@code
 * "id"} as an alias for {@code wrestlerId} — smaller local models (e.g. llama3.2:1b on the nightly
 * Ollama run) don't always echo the exact schema. The wrestler id is still validated against the
 * candidate list server-side.
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSuggestedOpponentDTO {
  @JsonAlias("id")
  private Long wrestlerId;

  private String name;
  private String rationale;
}
