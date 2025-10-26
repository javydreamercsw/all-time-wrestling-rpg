package com.github.javydreamercsw.base.event;

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
