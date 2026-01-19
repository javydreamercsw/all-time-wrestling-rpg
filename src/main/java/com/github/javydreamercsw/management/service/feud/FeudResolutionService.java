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
package com.github.javydreamercsw.management.service.feud;

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.event.FeudResolvedEvent;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeudResolutionService {

  @Autowired private final MultiWrestlerFeudRepository feudRepository;
  @Autowired private final ApplicationEventPublisher eventPublisher;
  @Autowired private final Random random;

  /**
   * Attempt to resolve a multi-wrestler feud.
   *
   * @param feud The feud to attempt to resolve.
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void attemptFeudResolution(@NonNull MultiWrestlerFeud feud) {
    if (!feud.canAttemptResolution()) {
      log.debug(
          "Feud {} is not eligible for resolution (Heat: {})", feud.getName(), feud.getHeat());
      return;
    }

    int numberOfParticipants = feud.getActiveParticipantCount();
    if (numberOfParticipants <= 0) {
      return;
    }
    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = 0;
    for (int i = 0; i < numberOfParticipants; i++) {
      roll += d20.roll();
    }

    int threshold = 10 * numberOfParticipants;
    if (roll > threshold) {
      log.info(
          "Feud {} resolved with a roll of {} (threshold: {})", feud.getName(), roll, threshold);
      feud.endFeud("Resolved after PLE match.");
      feudRepository.save(feud);
      eventPublisher.publishEvent(new FeudResolvedEvent(this, feud));
    } else {
      log.info(
          "Feud {} not resolved with a roll of {} (threshold: {})",
          feud.getName(),
          roll,
          threshold);
    }
  }
}
