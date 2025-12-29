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
package com.github.javydreamercsw.base.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service
public class LocalAIStatusService {

  @Getter @Setter private Status status = Status.NOT_STARTED;
  @Getter @Setter private String message = "LocalAI is not initialized.";

  public enum Status {
    NOT_STARTED,
    STARTING,
    DOWNLOADING_MODEL,
    READY,
    FAILED
  }

  public boolean isReady() {
    return status == Status.READY;
  }
}
