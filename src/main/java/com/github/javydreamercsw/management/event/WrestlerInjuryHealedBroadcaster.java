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
