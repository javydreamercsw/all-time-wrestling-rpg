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
package com.github.javydreamercsw.management.event.inbox;

import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Broadcasts a signal to open the profile drawer in the current UI session. */
@Component
public class OpenProfileDrawerBroadcaster {
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final List<Consumer<Void>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(final Consumer<Void> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void broadcast() {
    for (Consumer<Void> listener : listeners) {
      executor.execute(() -> listener.accept(null));
    }
  }

  @PreDestroy
  public void destroy() {
    executor.shutdown();
  }
}
