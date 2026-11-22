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
package com.github.javydreamercsw.management.ui.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Coalesces rapid events into a single deferred UI update. Multiple {@link #schedule(Runnable)}
 * calls within the debounce window replace the pending command so that only the last one runs.
 *
 * <p>Contract: the scheduled command must be fast and non-blocking — it executes inside {@code
 * ui.access} while holding the Vaadin session lock, so a slow command stalls every other
 * interaction on that session.
 */
public final class DebouncedUiRunner {

  private static final ScheduledExecutorService SCHEDULER =
      new ScheduledThreadPoolExecutor(
          1,
          r -> {
            Thread t = new Thread(r, "ui-debounce");
            t.setDaemon(true);
            return t;
          });

  private final UI ui;
  private final LongSupplier debounceMsSupplier;
  private ScheduledFuture<?> pending;

  /**
   * @param ui target UI
   * @param debounceMsSupplier supplies the debounce window in milliseconds; read on every {@link
   *     #schedule} call so callers can retune the window after construction (e.g. tests)
   */
  public DebouncedUiRunner(final UI ui, final LongSupplier debounceMsSupplier) {
    this.ui = ui;
    this.debounceMsSupplier = debounceMsSupplier;
  }

  /**
   * Schedule the command to run on the UI thread after the debounce window. A previously scheduled
   * but not yet executed command is replaced. The command is dropped if the UI detaches before it
   * fires.
   *
   * @param uiCommand command to run inside {@code ui.access}
   */
  public synchronized void schedule(final Command uiCommand) {
    cancelPending();
    pending =
        SCHEDULER.schedule(
            () -> {
              if (ui.isAttached()) {
                ui.access(uiCommand);
              }
            },
            debounceMsSupplier.getAsLong(),
            TimeUnit.MILLISECONDS);
  }

  /** Cancel any pending command without running it. */
  public synchronized void cancelPending() {
    ScheduledFuture<?> existing = pending;
    if (existing != null) {
      existing.cancel(false);
      pending = null;
    }
  }

  /**
   * Convenience overload for callers that express the debounce window as a {@link Duration}.
   *
   * @param ui target UI
   * @param debounce minimum delay before the scheduled command runs
   */
  public static DebouncedUiRunner withDuration(final UI ui, final Duration debounce) {
    return new DebouncedUiRunner(ui, debounce::toMillis);
  }
}
