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
package com.github.javydreamercsw.management.domain.league;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class League {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull @Column(unique = true)
  private String name;

  @NotNull @ManyToOne
  @JoinColumn(name = "commissioner_id")
  private Account commissioner;

  @Enumerated(EnumType.STRING)
  private LeagueStatus status = LeagueStatus.PRE_DRAFT;

  private int maxPicksPerPlayer = 1;

  @ManyToMany
  @JoinTable(
      name = "league_excluded_wrestler",
      joinColumns = @JoinColumn(name = "league_id"),
      inverseJoinColumns = @JoinColumn(name = "wrestler_id"))
  private Set<Wrestler> excludedWrestlers = new HashSet<>();

  public enum LeagueStatus {
    PRE_DRAFT,
    DRAFTING,
    SEASON_ACTIVE,
    COMPLETED
  }
}
