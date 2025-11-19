package com.github.javydreamercsw.management.domain.npc;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NpcRepository extends JpaRepository<Npc, Long> {

  List<Npc> findAllByNpcType(String npcType);

  Npc findByName(String name);

  Npc findByExternalId(String externalId);
}
