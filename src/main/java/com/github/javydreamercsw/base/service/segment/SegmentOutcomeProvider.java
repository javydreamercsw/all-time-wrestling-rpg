package com.github.javydreamercsw.base.service.segment;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;

/**
 * Interface for providing segment outcome calculations. This interface breaks the circular
 * dependency between base and management packages.
 */
public interface SegmentOutcomeProvider {

  /**
   * Determines the segment outcome if none is provided in the context.
   *
   * @param context The segment narration context
   * @return The context with determined outcome
   */
  SegmentNarrationContext determineOutcomeIfNeeded(SegmentNarrationContext context);
}
