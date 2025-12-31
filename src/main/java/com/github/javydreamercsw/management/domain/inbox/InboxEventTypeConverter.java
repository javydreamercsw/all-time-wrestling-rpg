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
package com.github.javydreamercsw.management.domain.inbox;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@Converter(autoApply = true)
@Slf4j
public class InboxEventTypeConverter
    implements AttributeConverter<InboxEventType, String>, ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    log.info("Setting application context in InboxEventTypeConverter");
    context = applicationContext;
  }

  private InboxEventTypeRegistry getRegistry() {
    if (context == null) {
      log.error("Application context is null in InboxEventTypeConverter!");
      throw new IllegalStateException("Application context not initialized");
    }
    return context.getBean(InboxEventTypeRegistry.class);
  }

  @Override
  public String convertToDatabaseColumn(InboxEventType attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.getName();
  }

  @Override
  public InboxEventType convertToEntityAttribute(String eventType) {
    if (eventType == null) {
      return null;
    }
    return getRegistry().getEventTypes().stream()
        .filter(type -> type.getName().equals(eventType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + eventType));
  }
}
