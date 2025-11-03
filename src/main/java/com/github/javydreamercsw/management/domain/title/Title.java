package com.github.javydreamercsw.management.domain.title;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
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

  @Column(name = "external_id")
  private String externalId;

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

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "title_contender",
      joinColumns = @JoinColumn(name = "title_id"),
      inverseJoinColumns = @JoinColumn(name = "wrestler_id"))
  private List<Wrestler> contender = new ArrayList<>();

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

  public void vacateTitle() {
    getCurrentReign().ifPresent(reign -> reign.endReign(Instant.now()));
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

  public long getCurrentReignDays() {
    return getCurrentReign().map(TitleReign::getReignLengthDays).orElse(0L);
  }

  public int getTotalReigns() {
    return titleReigns.size();
  }

  public boolean isWrestlerEligible(Wrestler wrestler) {
    return wrestler.isEligibleForTitle(tier);
  }

  public Long getChallengeCost() {
    return tier.getChallengeCost();
  }

  public Long getContenderEntryFee() {
    return tier.getContenderEntryFee();
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

  public String getContenderNames() {
    return getContender().stream().map(Wrestler::getName).collect(Collectors.joining(" & "));
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

  /** Sets the #1 contender for the title. Clears any existing contenders. */
  public void setNumberOneContender(@NonNull Wrestler wrestler) {
    this.contender.clear();
    this.contender.add(wrestler);
  }

  /** Gets the #1 contender for the title, or null if none is set. */
  @Nullable public Wrestler getNumberOneContender() {
    return this.contender.isEmpty() ? null : this.contender.get(0);
  }
}
