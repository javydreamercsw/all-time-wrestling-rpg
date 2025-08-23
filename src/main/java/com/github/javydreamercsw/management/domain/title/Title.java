package com.github.javydreamercsw.management.domain.title;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.wrestler.TitleTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a championship title in the ATW RPG system. Tracks the current champion, title
 * history, and championship-specific rules.
 */
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
  private TitleTier tier;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "is_vacant", nullable = false)
  private Boolean isVacant = true;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // Current champion (null if vacant)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_champion_id")
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns"})
  private Wrestler currentChampion;

  @Column(name = "title_won_date")
  private Instant titleWonDate;

  // Title history
  @OneToMany(mappedBy = "title", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"title"})
  private List<TitleReign> titleHistory = new ArrayList<>();

  // ==================== ATW RPG METHODS ====================

  /** Award the title to a new champion. */
  public void awardTitle(Wrestler newChampion) {
    // End current reign if there is one
    if (currentChampion != null && !isVacant) {
      getCurrentReign().ifPresent(reign -> reign.endReign(Instant.now()));
    }

    // Set new champion
    this.currentChampion = newChampion;
    this.isVacant = false;
    this.titleWonDate = Instant.now();

    // Create new title reign
    TitleReign newReign = new TitleReign();
    newReign.setTitle(this);
    newReign.setChampion(newChampion);
    newReign.setStartDate(titleWonDate);
    titleHistory.add(newReign);
  }

  /** Vacate the title (no current champion). */
  public void vacateTitle() {
    if (currentChampion != null && !isVacant) {
      getCurrentReign().ifPresent(reign -> reign.endReign(Instant.now()));
    }

    this.currentChampion = null;
    this.isVacant = true;
    this.titleWonDate = null;
  }

  /** Get the current title reign. */
  @JsonIgnore
  public java.util.Optional<TitleReign> getCurrentReign() {
    return titleHistory.stream().filter(reign -> reign.getEndDate() == null).findFirst();
  }

  /** Get the length of the current reign in days. */
  public long getCurrentReignDays() {
    if (isVacant || titleWonDate == null) {
      return 0;
    }
    return java.time.Duration.between(titleWonDate, Instant.now()).toDays();
  }

  /** Get the total number of title reigns. */
  public int getTotalReigns() {
    return titleHistory.size();
  }

  /** Check if a wrestler is eligible to challenge for this title. */
  public boolean isWrestlerEligible(Wrestler wrestler) {
    return wrestler.isEligibleForTitle(tier);
  }

  /** Get the fan cost to challenge for this title. */
  public Long getChallengeCost() {
    return tier.getChallengeCost();
  }

  /** Get the #1 contender entry fee for this title. */
  public Long getContenderEntryFee() {
    return tier.getContenderEntryFee();
  }

  /** Get display name with current champion info. */
  public String getDisplayName() {
    if (isVacant) {
      return name + " (Vacant)";
    }
    return name + " (Champion: " + currentChampion.getName() + ")";
  }

  /** Get title status emoji. */
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
