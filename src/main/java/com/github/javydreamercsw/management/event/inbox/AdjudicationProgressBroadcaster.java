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

import com.github.javydreamercsw.management.event.ShowAdjudicationProgressEvent;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Forwards {@link ShowAdjudicationProgressEvent}s from the adjudication run's async thread to
 * attached show views, so the floating progress overlay updates live (ATW-v0vh).
 *
 * <p>Both an {@link ApplicationListener} and a listener registry (same shape as {@link
 * AiSettingsChangeBroadcaster}): the event multicast is synchronous on the run's ForkJoinPool
 * worker inside the open adjudication transaction, so this class only enqueues the fan-out onto its
 * own executor — the view's callback wraps UI work in {@code ui.access()}.
 *
 * <p>Keeps the last event per show so a user who navigates away mid-run and returns gets the
 * overlay restored from {@link #lastState}; views call {@link #clear} on terminal completion.
 */
@Component
public class AdjudicationProgressBroadcaster
    implements ApplicationListener<ShowAdjudicationProgressEvent> {
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final List<Consumer<ShowAdjudicationProgressEvent>> listeners =
      new CopyOnWriteArrayList<>();
  private final Map<Long, ShowAdjudicationProgressEvent> lastStateByShowId =
      new ConcurrentHashMap<>();

  /** Registers a listener; callers remove the returned Registration on view detach. */
  public Registration register(final Consumer<ShowAdjudicationProgressEvent> listener) {
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  @Override
  public void onApplicationEvent(final ShowAdjudicationProgressEvent event) {
    lastStateByShowId.put(event.getShowId(), event);
    for (Consumer<ShowAdjudicationProgressEvent> listener : listeners) {
      executor.execute(() -> listener.accept(event));
    }
  }

  /** The most recent progress event for the show, for restoring the overlay on re-attach. */
  public Optional<ShowAdjudicationProgressEvent> lastState(final Long showId) {
    return Optional.ofNullable(lastStateByShowId.get(showId));
  }

  /**
   * Called on terminal completion (success, failure, or retry done) so a stale run never restores.
   */
  public void clear(final Long showId) {
    lastStateByShowId.remove(showId);
  }

  @PreDestroy
  public void destroy() {
    executor.shutdown();
  }
}
