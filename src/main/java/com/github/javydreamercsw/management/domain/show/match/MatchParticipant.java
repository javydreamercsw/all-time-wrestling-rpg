package com.github.javydreamercsw.management.domain.show.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Represents a wrestler's participation in a match. Links wrestlers to match results and tracks
 * whether they won or lost.
 */
@Entity
@Table(name = "match_participant")
@Getter
@Setter
public class MatchParticipant extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "match_participant_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "match_id", nullable = false)
  @JsonIgnoreProperties({"participants"})
  private Match match;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "wrestler_id", nullable = false)
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns"})
  private Wrestler wrestler;

  @Column(name = "is_winner", nullable = false)
  private Boolean isWinner = false;

  @Override
  public @Nullable Long getId() {
    return id;
  }
}
