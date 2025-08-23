package com.github.javydreamercsw.base.service.match;

import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchNarrationContext;

/**
 * Interface for providing match outcome calculations. This interface breaks the circular dependency
 * between base and management packages.
 */
public interface MatchOutcomeProvider {

  /**
   * Determines the match outcome if none is provided in the context.
   *
   * @param context The match narration context
   * @return The context with determined outcome
   */
  MatchNarrationContext determineOutcomeIfNeeded(MatchNarrationContext context);
}
