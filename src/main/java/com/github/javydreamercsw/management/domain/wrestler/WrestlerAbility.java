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
package com.github.javydreamercsw.management.domain.wrestler;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "wrestler_ability")
@Getter
@Setter
public class WrestlerAbility extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "wrestler_ability_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  @Nullable private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "ability_type", nullable = false)
  private AbilityType abilityType;

  @Nullable @Column(name = "max_uses")
  private Integer maxUses;

  @Enumerated(EnumType.STRING)
  @Nullable private AbilityTiming timing;

  @Enumerated(EnumType.STRING)
  @Nullable private AbilityCategory category;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault = true;

  /** Groovy condition evaluated against game state; ability fires/unlocks when true. */
  @Column(name = "unlock_condition", columnDefinition = "TEXT")
  @Nullable private String unlockCondition;

  /** "onDemand" or a Groovy condition determining when this optional ability may be swapped in. */
  @Column(name = "swap_condition", columnDefinition = "TEXT")
  @Nullable private String swapCondition;

  /**
   * Groovy script evaluated before the effect; represents the activation cost (e.g. spend stamina,
   * discard a card).
   */
  @Column(name = "cost_script", columnDefinition = "TEXT")
  @Nullable private String costScript;

  /** Groovy script evaluated on activation; may modify wrestler and/or opponent state. */
  @Column(name = "effect_script", columnDefinition = "TEXT")
  @Nullable private String effectScript;
}
