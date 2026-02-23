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
package com.github.javydreamercsw.management.domain.campaign;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "storyline_milestone")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorylineMilestone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "storyline_id", nullable = false)
  private CampaignStoryline storyline;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "narrative_goal", nullable = false, length = 2000)
  private String narrativeGoal;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private MilestoneStatus status = MilestoneStatus.PENDING;

  @Column(name = "display_order", nullable = false)
  private int order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_on_success_id")
  private StorylineMilestone nextMilestoneOnSuccess;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_on_failure_id")
  private StorylineMilestone nextMilestoneOnFailure;

  public enum MilestoneStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED
  }
}
