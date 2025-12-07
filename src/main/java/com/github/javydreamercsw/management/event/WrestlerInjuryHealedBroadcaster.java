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
package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.vaadin.flow.shared.Registration;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class WrestlerInjuryHealedBroadcaster {
  private final Executor executor = Executors.newSingleThreadExecutor();

  private final LinkedList<Consumer<WrestlerInjuryHealedEvent>> listeners = new LinkedList<>();

  public synchronized Registration register(Consumer<WrestlerInjuryHealedEvent> listener) {
    listeners.add(listener);

    return () -> {
      synchronized (WrestlerInjuryHealedBroadcaster.class) {
        listeners.remove(listener);
      }
    };
  }

  public synchronized void broadcast(WrestlerInjuryHealedEvent event) {
    for (final Consumer<WrestlerInjuryHealedEvent> listener : listeners) {
      executor.execute(() -> listener.accept(event));
    }
  }
}
