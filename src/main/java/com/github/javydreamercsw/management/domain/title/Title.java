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
package com.github.javydreamercsw.management.domain.title;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "title", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class Title extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "title_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @Column(name = "tier", nullable = false)
  @Enumerated(EnumType.STRING)
  private WrestlerTier tier;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  @Column(name = "championship_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ChampionshipType championshipType;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "title_contender",
      joinColumns = @JoinColumn(name = "title_id"),
      inverseJoinColumns = @JoinColumn(name = "wrestler_id"))
  private List<Wrestler> challengers = new ArrayList<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "title_champion",
      joinColumns = @JoinColumn(name = "title_id"),
      inverseJoinColumns = @JoinColumn(name = "wrestler_id"))
  private List<Wrestler> champion = new ArrayList<>();

  @OneToMany(
      mappedBy = "title",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JsonIgnoreProperties({"title"})
  private Set<TitleReign> titleReigns = new HashSet<>();

  @ManyToMany(mappedBy = "titles", fetch = FetchType.EAGER)
  private List<Segment> segments = new ArrayList<>();

  public void awardTitleTo(@NonNull List<Wrestler> newChampions, @NonNull Instant awardDate) {
    // End the current reign if one exists.
    getCurrentReign().ifPresent(reign -> reign.endReign(awardDate));

    // Create a new title reign for the new champions.
    TitleReign newReign = new TitleReign();
    newReign.setTitle(this);
    newReign.getChampions().addAll(newChampions);
    newReign.setStartDate(awardDate);
    getTitleReigns().add(newReign);

    this.champion = new ArrayList<>(newChampions); // Ensure champion field is updated
  }

  public void vacateTitle(Instant now) {
    getCurrentReign().ifPresent(reign -> reign.endReign(now));
    this.champion.clear();
  }

  @JsonIgnore
  public java.util.Optional<TitleReign> getCurrentReign() {
    return getTitleReigns().stream().filter(TitleReign::isCurrentReign).findFirst();
  }

  @JsonProperty("currentChampions")
  public List<Wrestler> getCurrentChampions() {
    return champion;
  }

  public long getCurrentReignDays(Instant now) {
    return getCurrentReign().map(reign -> reign.getReignLengthDays(now)).orElse(0L);
  }

  public int getTotalReigns() {
    return titleReigns.size();
  }

  public boolean isTopTier() {
    return getTier() == WrestlerTier.MAIN_EVENTER;
  }

  public String getDisplayName() {
    if (isVacant()) {
      return name + " (Vacant)";
    }
    return name + " (Champion: " + getChampionNames() + ")";
  }

  public String getChampionNames() {
    return getCurrentChampions().stream().map(Wrestler::getName).collect(Collectors.joining(" & "));
  }

  public String getChallengerNames() {
    return getChallengers().stream().map(Wrestler::getName).collect(Collectors.joining(" & "));
  }

  public String getStatusEmoji() {
    if (!isActive) {
      return "🚫";
    }
    if (isVacant()) {
      return "👑❓";
    }
    return "👑";
  }

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }

  @JsonProperty("isVacant")
  public boolean isVacant() {
    return getCurrentChampions().isEmpty();
  }

  /**
   * Adds a challenger to the title.
   *
   * @param wrestler Wrestler to add as a challenger.
   */
  public void addChallenger(@NonNull Wrestler wrestler) {
    if (!this.challengers.contains(wrestler)) {
      this.challengers.add(wrestler);
    }
  }
}
