package com.github.javydreamercsw.base.event;

import com.vaadin.flow.shared.Registration;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WrestlerInjuryHealedBroadcaster {
  static final Executor executor = Executors.newSingleThreadExecutor();

  private static final LinkedList<Consumer<WrestlerInjuryHealedEvent>> listeners =
      new LinkedList<>();

  public static synchronized Registration register(Consumer<WrestlerInjuryHealedEvent> listener) {
    listeners.add(listener);

    return () -> {
      synchronized (WrestlerInjuryHealedBroadcaster.class) {
        listeners.remove(listener);
      }
    };
  }

  public static synchronized void broadcast(WrestlerInjuryHealedEvent event) {
    for (final Consumer<WrestlerInjuryHealedEvent> listener : listeners) {
      executor.execute(() -> listener.accept(event));
    }
  }
}
