package com.github.javydreamercsw.management.event.league;

import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class DraftBroadcaster {
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final List<Consumer<DraftUpdateEvent>> listeners = new CopyOnWriteArrayList<>();

  public Registration register(Consumer<DraftUpdateEvent> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  public void broadcast(DraftUpdateEvent event) {
    for (Consumer<DraftUpdateEvent> listener : listeners) {
      executor.execute(() -> listener.accept(event));
    }
  }

  @PreDestroy
  public void destroy() {
    executor.shutdown();
  }
}
