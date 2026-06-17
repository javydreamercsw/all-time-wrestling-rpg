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
package com.github.javydreamercsw.management.domain.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniverseMembershipRepository extends JpaRepository<UniverseMembership, Long> {

  List<UniverseMembership> findByAccount(Account account);

  List<UniverseMembership> findByUniverse(Universe universe);

  Optional<UniverseMembership> findByAccountAndUniverse(Account account, Universe universe);

  boolean existsByAccountAndUniverse(Account account, Universe universe);

  Optional<UniverseMembership> findByAccount_IdAndUniverse(Long accountId, Universe universe);

  boolean existsByAccount_IdAndUniverseAndRole(
      Long accountId, Universe universe, UniverseMembership.UniverseMemberRole role);
}
