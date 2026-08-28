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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.ContenderTieDetectedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Resolves contender ties by suggesting a tie-breaker match to the booker. When enough wrestlers
 * are tied, the configured multi-man match type (default Free-for-All) is proposed; with only two
 * tied wrestlers a one-on-one contender match is proposed instead.
 */
@Component
@Slf4j
public class ContenderTieResolver implements ApplicationListener<ContenderTieDetectedEvent> {

  private final InboxService inboxService;
  private final InboxEventType contenderTieDetected;
  private final SegmentTypeRepository segmentTypeRepository;
  private final GameSettingService gameSettingService;

  public ContenderTieResolver(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("contenderTieDetected") final InboxEventType contenderTieDetected,
      @NonNull final SegmentTypeRepository segmentTypeRepository,
      @NonNull final GameSettingService gameSettingService) {
    this.inboxService = inboxService;
    this.contenderTieDetected = contenderTieDetected;
    this.segmentTypeRepository = segmentTypeRepository;
    this.gameSettingService = gameSettingService;
  }

  @Override
  public void onApplicationEvent(@NonNull final ContenderTieDetectedEvent event) {
    List<RankedWrestlerDTO> tied = event.getTiedWrestlers();
    String matchTypeName = resolveMatchTypeName(tied.size());
    String participants =
        tied.stream().map(RankedWrestlerDTO::getName).collect(Collectors.joining(", "));

    log.info(
        "Suggesting {} tie-breaker for {} with participants: {}",
        matchTypeName,
        event.getTitle().getName(),
        participants);

    InboxItem inboxItem =
        inboxService.createInboxItem(
            contenderTieDetected,
            "Contender Tie: " + event.getTitle().getName(),
            ("The top contenders for '%s' are too close to call. Book a %s between %s to"
                    + " determine the next #1 contender.")
                .formatted(event.getTitle().getName(), matchTypeName, participants),
            InboxItem.Urgency.ACTION_REQUIRED,
            event.getTitle().getId().toString(),
            InboxItemTarget.TargetType.TITLE);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload("{\"route\":\"title-list\"}");
    inboxService.save(inboxItem);
  }

  /**
   * Picks the tie-breaker match type: the configured multi-man type when enough wrestlers are tied,
   * otherwise a one-on-one contender match.
   */
  private String resolveMatchTypeName(final int tiedCount) {
    String code =
        tiedCount >= gameSettingService.getContenderTieMinWrestlers()
            ? gameSettingService.getContenderTieMatchType()
            : WellKnownSegmentType.ONE_ON_ONE.getCode();
    return segmentTypeRepository.findByCode(code).map(type -> type.getName()).orElse(code);
  }
}
