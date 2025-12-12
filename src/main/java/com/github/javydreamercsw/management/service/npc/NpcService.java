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
