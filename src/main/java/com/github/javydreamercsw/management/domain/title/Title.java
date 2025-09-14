package com.github.javydreamercsw.management.domain.title;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

  @Column(name = "is_vacant", nullable = false)
  private Boolean isVacant = true;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contender_id")
  private Wrestler contender;

  @OneToMany(mappedBy = "title", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnoreProperties({"title"})
  private Set<TitleReign> titleReigns = new HashSet<>();

  @ManyToMany(mappedBy = "titles", fetch = FetchType.LAZY)
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

    this.isVacant = newChampions.isEmpty();
  }

  public void vacateTitle() {
    getCurrentReign().ifPresent(reign -> reign.endReign(Instant.now()));
    this.isVacant = true;
  }

  @JsonIgnore
  public java.util.Optional<TitleReign> getCurrentReign() {
    return getTitleReigns().stream().filter(TitleReign::isCurrentReign).findFirst();
  }

  @JsonIgnore
  public List<Wrestler> getCurrentChampions() {
    return getCurrentReign().map(TitleReign::getChampions).orElse(new ArrayList<>());
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
    if (isVacant) {
      return name + " (Vacant)";
    }
    return name + " (Champion: " + getChampionNames() + ")";
  }

  private String getChampionNames() {
    return getCurrentChampions().stream().map(Wrestler::getName).collect(Collectors.joining(" & "));
  }

  public String getStatusEmoji() {
    if (!isActive) return "🚫";
    if (isVacant) return "👑❓";
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
}
