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
package com.github.javydreamercsw.management.domain.commentator;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CommentatorRepositoryTest extends AbstractJpaTest {

  @Autowired private CommentatorRepository commentatorRepository;
  @Autowired private CommentaryTeamRepository commentaryTeamRepository;
  @Autowired private NpcRepository npcRepository;

  @Test
  void testCreateAndSaveCommentator() {
    Npc npc = new Npc();
    npc.setName("Test NPC");
    npc.setNpcType("Commentator");
    npc.setAlignment(AlignmentType.FACE);
    npc = npcRepository.save(npc);

    Commentator commentator = new Commentator();
    commentator.setNpc(npc);
    commentator.setStyle("Play-by-play");
    commentator.setCatchphrase("Wait for it!");
    commentator.setPersonaDescription("Energetic and supportive.");
    commentator = commentatorRepository.save(commentator);

    assertThat(commentator.getId()).isNotNull();
    assertThat(commentator.getNpc().getName()).isEqualTo("Test NPC");
    assertThat(commentator.getNpc().getAlignment()).isEqualTo(AlignmentType.FACE);
    assertThat(commentator.getCatchphrase()).isEqualTo("Wait for it!");
  }

  @Test
  void testCreateAndSaveCommentaryTeam() {
    Npc npc1 = new Npc();
    npc1.setName("Face Commentator");
    npc1.setNpcType("Commentator");
    npc1.setAlignment(AlignmentType.FACE);
    npc1 = npcRepository.save(npc1);

    Commentator face = new Commentator();
    face.setNpc(npc1);
    face = commentatorRepository.save(face);

    Npc npc2 = new Npc();
    npc2.setName("Heel Commentator");
    npc2.setNpcType("Commentator");
    npc2.setAlignment(AlignmentType.HEEL);
    npc2 = npcRepository.save(npc2);

    Commentator heel = new Commentator();
    heel.setNpc(npc2);
    heel = commentatorRepository.save(heel);

    CommentaryTeam team = new CommentaryTeam();
    team.setName("Dynamic Duo");
    team.setCommentators(List.of(face, heel));
    team = commentaryTeamRepository.save(team);

    assertThat(team.getId()).isNotNull();
    assertThat(team.getName()).isEqualTo("Dynamic Duo");
    assertThat(team.getCommentators()).hasSize(2);
    assertThat(team.getCommentators()).containsExactlyInAnyOrder(face, heel);
  }
}
