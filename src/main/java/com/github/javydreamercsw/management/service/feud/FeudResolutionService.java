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
    DiceBag diceBag = new DiceBag(random, new int[numberOfParticipants]);
    int roll = diceBag.roll(20);

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
