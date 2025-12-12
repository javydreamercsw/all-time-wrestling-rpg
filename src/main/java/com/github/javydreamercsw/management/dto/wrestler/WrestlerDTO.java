/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.dto.wrestler;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import java.io.Serializable;
import lombok.Data;

@Data
public class WrestlerDTO implements Serializable {
  private Long id;
  private String name;
  private Long fans;
  private WrestlerTier tier;
  private String externalId;
  // Add other fields as needed for API response
}
