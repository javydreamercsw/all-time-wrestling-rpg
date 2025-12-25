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
package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

public interface DeckRepository extends JpaRepository<Deck, Long>, JpaSpecificationExecutor<Deck> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Deck> findAllBy(Pageable pageable);

  List<Deck> findByWrestler(Wrestler wrestler);

  @Transactional
  void deleteByWrestler(Wrestler wrestler);
}
