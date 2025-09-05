package com.github.javydreamercsw.management.service.npc;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NpcService {

  @Autowired private NpcRepository npcRepository;

  public List<Npc> findAll() {
    return npcRepository.findAll();
  }

  public Page<Npc> findAll(Pageable pageable) {
    return npcRepository.findAll(pageable);
  }

  public List<Npc> findAllByType(String type) {
    return npcRepository.findAllByNpcType(type);
  }

  public Npc save(Npc npc) {
    return npcRepository.save(npc);
  }

  public void delete(Npc npc) {
    npcRepository.delete(npc);
  }

  public Npc findByName(String name) {
    return npcRepository.findByName(name);
  }

  public Npc findByExternalId(String externalId) {
    return npcRepository.findByExternalId(externalId);
  }
}
