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
package com.github.javydreamercsw.management.service.commentator;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CommentaryService {

  private final CommentatorRepository commentatorRepository;
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final NpcRepository npcRepository;

  @Autowired
  public CommentaryService(
      CommentatorRepository commentatorRepository,
      CommentaryTeamRepository commentaryTeamRepository,
      NpcRepository npcRepository) {
    this.commentatorRepository = commentatorRepository;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.npcRepository = npcRepository;
  }

  @Transactional
  public Commentator createOrUpdateCommentator(
      @NonNull String npcName,
      @NonNull Gender gender,
      @NonNull AlignmentType alignment,
      String description,
      String style,
      String catchphrase,
      String personaDescription) {
    Npc npc = npcRepository.findByName(npcName).orElse(null);
    if (npc == null) {
      log.info("Creating new NPC for commentator: {}", npcName);
      npc = new Npc();
      npc.setName(npcName);
      npc.setNpcType("Commentator");
    }
    npc.setGender(gender);
    npc.setAlignment(alignment);
    npc.setDescription(description);
    npc = npcRepository.save(npc);

    Optional<Commentator> existing =
        commentatorRepository.findAll().stream()
            .filter(c -> c.getNpc().getName().equals(npcName))
            .findFirst();

    Commentator commentator = existing.orElse(new Commentator());
    commentator.setNpc(npc);
    commentator.setStyle(style);
    commentator.setCatchphrase(catchphrase);
    commentator.setPersonaDescription(personaDescription);

    return commentatorRepository.save(commentator);
  }

  @Transactional
  public CommentaryTeam createOrUpdateTeam(
      @NonNull String teamName, @NonNull List<String> commentatorNames) {
    Optional<CommentaryTeam> existing =
        commentaryTeamRepository.findAll().stream()
            .filter(t -> t.getName().equals(teamName))
            .findFirst();

    CommentaryTeam team = existing.orElse(new CommentaryTeam());
    team.setName(teamName);

    List<Commentator> commentators = new ArrayList<>();
    for (String name : commentatorNames) {
      commentatorRepository.findAll().stream()
          .filter(c -> c.getNpc().getName().equals(name))
          .findFirst()
          .ifPresent(commentators::add);
    }
    team.setCommentators(commentators);

    return commentaryTeamRepository.save(team);
  }

  public List<CommentaryTeam> getAllTeams() {
    return commentaryTeamRepository.findAll();
  }

  public Optional<Commentator> findCommentatorByNpcName(@NonNull String npcName) {
    return commentatorRepository.findAll().stream()
        .filter(c -> c.getNpc().getName().equals(npcName))
        .findFirst();
  }
}
