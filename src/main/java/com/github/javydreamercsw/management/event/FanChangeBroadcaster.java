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

import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.vaadin.flow.shared.Registration;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FanChangeBroadcaster {
  static final Executor executor = Executors.newSingleThreadExecutor();

  private static final LinkedList<Consumer<FanAwardedEvent>> listeners = new LinkedList<>();

  public static synchronized Registration register(Consumer<FanAwardedEvent> listener) {
    listeners.add(listener);

    return () -> {
      synchronized (FanChangeBroadcaster.class) {
        listeners.remove(listener);
      }
    };
  }

  public static synchronized void broadcast(FanAwardedEvent event) {
    for (final Consumer<FanAwardedEvent> listener : listeners) {
      executor.execute(() -> listener.accept(event));
    }
  }
}
