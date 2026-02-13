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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentaryServiceTest {

  private CommentatorRepository commentatorRepository;
  private CommentaryTeamRepository commentaryTeamRepository;
  private NpcRepository npcRepository;
  private CommentaryService commentaryService;

  @BeforeEach
  void setUp() {
    commentatorRepository = mock(CommentatorRepository.class);
    commentaryTeamRepository = mock(CommentaryTeamRepository.class);
    npcRepository = mock(NpcRepository.class);
    commentaryService =
        new CommentaryService(commentatorRepository, commentaryTeamRepository, npcRepository);
  }

  @Test
  void testCreateOrUpdateCommentator() {
    String name = "Test Commentator";
    Npc npc = new Npc();
    npc.setName(name);

    when(npcRepository.findByName(name)).thenReturn(Optional.of(npc));
    when(npcRepository.save(any(Npc.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(commentatorRepository.save(any(Commentator.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Commentator result =
        commentaryService.createOrUpdateCommentator(
            name,
            Gender.MALE,
            AlignmentType.FACE,
            "Description",
            "Play-by-play",
            "What a maneuver!",
            "Analytical and professional");

    assertNotNull(result);
    assertEquals(name, result.getNpc().getName());
    assertEquals("Play-by-play", result.getStyle());
    assertEquals("What a maneuver!", result.getCatchphrase());

    verify(npcRepository).save(any(Npc.class));
    verify(commentatorRepository).save(any(Commentator.class));
  }

  @Test
  void testCreateOrUpdateTeam() {
    String teamName = "Main Event Team";
    String commName = "Dara Hoshiko";
    Npc npc = new Npc();
    npc.setName(commName);
    Commentator commentator = new Commentator();
    commentator.setNpc(npc);

    when(commentaryTeamRepository.findAll()).thenReturn(List.of());
    when(commentatorRepository.findAll()).thenReturn(List.of(commentator));
    when(commentaryTeamRepository.save(any(CommentaryTeam.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CommentaryTeam result = commentaryService.createOrUpdateTeam(teamName, List.of(commName));

    assertNotNull(result);
    assertEquals(teamName, result.getName());
    assertEquals(1, result.getCommentators().size());
    assertEquals(commName, result.getCommentators().get(0).getNpc().getName());

    verify(commentaryTeamRepository).save(any(CommentaryTeam.class));
  }

  @Test
  void testFindCommentatorByNpcName() {
    String name = "Dara Hoshiko";
    Npc npc = new Npc();
    npc.setName(name);
    Commentator commentator = new Commentator();
    commentator.setNpc(npc);

    when(commentatorRepository.findAll()).thenReturn(List.of(commentator));

    Optional<Commentator> result = commentaryService.findCommentatorByNpcName(name);

    assertTrue(result.isPresent());
    assertEquals(name, result.get().getNpc().getName());
  }
}
