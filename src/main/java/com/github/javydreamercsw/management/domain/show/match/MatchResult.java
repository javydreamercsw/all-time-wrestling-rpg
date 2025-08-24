package com.github.javydreamercsw.management.domain.show.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.stipulation.MatchRule;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents the result of a wrestling match in the ATW RPG system. Tracks participants, winner,
 * and match details for storyline continuity and statistics.
 */
@Entity
@Table(name = "match_result")
@Getter
@Setter
public class MatchResult extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "match_result_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "show_id", nullable = false)
  @JsonIgnoreProperties({"season"})
  private Show show;

  @ManyToOne(optional = false)
  @JoinColumn(name = "match_type_id", nullable = false)
  private MatchType matchType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_id")
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns"})
  private Wrestler winner;

  @Column(name = "match_date", nullable = false)
  private Instant matchDate;

  @Column(name = "duration_minutes")
  @Min(1) @Max(180) private Integer durationMinutes;

  @Column(name = "match_rating")
  @Min(1) @Max(5) private Integer matchRating;

  // Match rules (many-to-many relationship)
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "match_result_rule",
      joinColumns = @JoinColumn(name = "match_result_id"),
      inverseJoinColumns = @JoinColumn(name = "match_rule_id"))
  @JsonIgnoreProperties({"description", "creationDate"})
  private List<MatchRule> matchRules = new ArrayList<>();

  @Lob
  @Column(name = "narration")
  private String narration;

  @Column(name = "is_title_match", nullable = false)
  private Boolean isTitleMatch = false;

  @Column(name = "is_npc_generated", nullable = false)
  private Boolean isNpcGenerated = false;

  // Match participants
  @OneToMany(mappedBy = "matchResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"matchResult"})
  private List<MatchParticipant> participants = new ArrayList<>();

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (matchDate == null) {
      matchDate = Instant.now();
    }
    if (isTitleMatch == null) {
      isTitleMatch = false;
    }
    if (isNpcGenerated == null) {
      isNpcGenerated = false;
    }
  }

  /** Add a participant to the match. */
  public void addParticipant(Wrestler wrestler, boolean isWinner) {
    MatchParticipant participant = new MatchParticipant();
    participant.setMatchResult(this);
    participant.setWrestler(wrestler);
    participant.setIsWinner(isWinner);
    participants.add(participant);

    if (isWinner) {
      this.winner = wrestler;
    }
  }

  /** Get all wrestlers participating in the match. */
  public List<Wrestler> getWrestlers() {
    return participants.stream().map(MatchParticipant::getWrestler).toList();
  }

  /** Check if this was a singles match (2 participants). */
  public boolean isSinglesMatch() {
    return participants.size() == 2;
  }

  /** Check if this was a multi-person match (3+ participants). */
  public boolean isMultiPersonMatch() {
    return participants.size() > 2;
  }

  /** Add a match rule to this match. */
  public void addMatchRule(MatchRule matchRule) {
    if (matchRule != null && !matchRules.contains(matchRule)) {
      matchRules.add(matchRule);
    }
  }

  /** Remove a match rule from this match. */
  public void removeMatchRule(MatchRule matchRule) {
    matchRules.remove(matchRule);
  }

  /** Check if this match has a specific rule. */
  public boolean hasMatchRule(MatchRule matchRule) {
    return matchRules.contains(matchRule);
  }

  /** Check if this match has any rules. */
  public boolean hasMatchRules() {
    return !matchRules.isEmpty();
  }

  /** Get match rules as a formatted string. */
  public String getMatchRulesAsString() {
    if (matchRules.isEmpty()) {
      return "Standard Match";
    }
    return matchRules.stream()
        .map(MatchRule::getName)
        .collect(java.util.stream.Collectors.joining(", "));
  }
}
