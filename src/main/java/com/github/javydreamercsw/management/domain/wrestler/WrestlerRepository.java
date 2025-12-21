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
package com.github.javydreamercsw.management.domain.wrestler;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface WrestlerRepository
    extends JpaRepository<Wrestler, Long>, JpaSpecificationExecutor<Wrestler> {

  Optional<Wrestler> findByAccount(com.github.javydreamercsw.base.domain.account.Account account);

  // If you don't need a total row count, Slice is better than Page.
  Page<Wrestler> findAllBy(Pageable pageable);

  @Query("SELECT w FROM Wrestler w LEFT JOIN FETCH w.decks WHERE w.name = :name")
  Optional<Wrestler> findByName(@Param("name") String name);

  @Query("SELECT w FROM Wrestler w LEFT JOIN FETCH w.injuries WHERE w.id = :id")
  Optional<Wrestler> findByIdWithInjuries(@Param("id") Long id);

  Optional<Wrestler> findByExternalId(String externalId);

  List<Wrestler> findByFansBetween(long minFans, long maxFans);

  List<Wrestler> findByFansGreaterThanEqual(long minFans);

  Optional<Wrestler> findByAccountUsername(String username);
}
