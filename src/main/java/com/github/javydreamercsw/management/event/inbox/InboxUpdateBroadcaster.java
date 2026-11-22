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
package com.github.javydreamercsw.management.event.inbox;

import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Pushes inbox update events to registered UI listeners. Listeners are independent consumers with
 * no ordering guarantee between them; each is dispatched on its own virtual thread so a slow
 * listener cannot delay delivery to the others. Listener contract: never block inside {@link
 * #broadcast(InboxUpdateEvent)} handling — schedule work and return.
 */
@Component
public class InboxUpdateBroadcaster {
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private final List<Consumer<InboxUpdateEvent>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(final Consumer<InboxUpdateEvent> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void broadcast(final InboxUpdateEvent event) {
    for (Consumer<InboxUpdateEvent> listener : listeners) {
      executor.execute(() -> listener.accept(event));
    }
  }

  @PreDestroy
  public void destroy() {
    executor.shutdown();
  }
}
