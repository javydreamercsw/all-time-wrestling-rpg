package com.github.javydreamercsw.management.domain.show.segment;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

/**
 * Represents a wrestling segment in the ATW RPG system. Tracks participants, winner, and segment
 * details for storyline continuity and statistics.
 */
@Entity
@Table(name = "segment")
@Getter
@Setter
@ToString
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Segment extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "segment_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "show_id", nullable = false)
  @JsonIgnore
  @ToString.Exclude
  private Show show;

  @ManyToOne(optional = false)
  @JoinColumn(name = "segment_type_id", nullable = false)
  private SegmentType segmentType;

  @Column(name = "segment_date", nullable = false)
  private Instant segmentDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SegmentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "adjudication_status", nullable = false)
  private AdjudicationStatus adjudicationStatus;

  // Segment rules (many-to-many relationship)
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "segment_segment_rule",
      joinColumns = @JoinColumn(name = "segment_id"),
      inverseJoinColumns = @JoinColumn(name = "segment_rule_id"))
  @JsonIgnoreProperties({"description", "creationDate"})
  private List<SegmentRule> segmentRules = new ArrayList<>();

  @Lob
  @Column(name = "narration")
  private String narration;

  @Lob
  @Column(name = "summary")
  private String summary;

  @Column(name = "is_title_segment", nullable = false)
  private Boolean isTitleSegment = false;

  @Column(name = "is_npc_generated", nullable = false)
  private Boolean isNpcGenerated = false;

  @Column(name = "external_id", unique = true)
  private String externalId;

  @Column(name = "segment_order", nullable = false)
  private int segmentOrder;

  @Column(name = "is_main_event", nullable = false)
  private boolean isMainEvent;

  // Segment participants
  @OneToMany(
      mappedBy = "segment",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JsonIgnoreProperties({"segment"})
  private List<SegmentParticipant> participants = new ArrayList<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "segment_title",
      joinColumns = @JoinColumn(name = "segment_id"),
      inverseJoinColumns = @JoinColumn(name = "title_id"))
  @JsonIgnore
  private Set<Title> titles = new HashSet<>();

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (segmentDate == null) {
      segmentDate = Instant.now();
    }
    if (isTitleSegment == null) {
      isTitleSegment = false;
    }
    if (isNpcGenerated == null) {
      isNpcGenerated = false;
    }
    if (status == null) {
      status = SegmentStatus.BOOKED;
    }
    if (adjudicationStatus == null) {
      adjudicationStatus = AdjudicationStatus.PENDING;
    }
    if (segmentOrder == 0) {
      segmentOrder = 0;
    }
    if (!isMainEvent) {
      isMainEvent = false;
    }
  }

  /** Add a participant to the segment. */
  public void addParticipant(@NonNull Wrestler wrestler) {
    SegmentParticipant participant = new SegmentParticipant();
    participant.setSegment(this);
    participant.setWrestler(wrestler);
    participant.setIsWinner(false); // Winner is set separately
    participants.add(participant);
  }

  /** Get all wrestlers participating in the segment. */
  public List<Wrestler> getWrestlers() {
    return participants.stream()
        .map(SegmentParticipant::getWrestler)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }

  public List<Wrestler> getWinners() {
    return participants.stream()
        .filter(SegmentParticipant::getIsWinner)
        .map(SegmentParticipant::getWrestler)
        .toList();
  }

  public List<Wrestler> getLosers() {
    return participants.stream()
        .filter(participant -> !participant.getIsWinner())
        .map(SegmentParticipant::getWrestler)
        .toList();
  }

  public void setWinners(List<Wrestler> winners) {
    if (winners == null || winners.isEmpty()) {
      for (SegmentParticipant participant : participants) {
        participant.setIsWinner(false);
      }
    } else {
      for (SegmentParticipant participant : participants) {
        participant.setIsWinner(winners.contains(participant.getWrestler()));
      }
    }
    this.status = SegmentStatus.COMPLETED;
  }

  public void syncParticipants(List<Wrestler> newParticipantWrestlers) {
    // Remove participants that are no longer in the new list
    participants.removeIf(
        existingParticipant ->
            !newParticipantWrestlers.contains(existingParticipant.getWrestler()));

    // Add new participants that are not already in the list
    for (Wrestler newWrestler : newParticipantWrestlers) {
      boolean alreadyExists =
          participants.stream()
              .anyMatch(
                  existingParticipant -> existingParticipant.getWrestler().equals(newWrestler));
      if (!alreadyExists) {
        addParticipant(newWrestler);
      }
    }
  }

  /** Add a segment rule to this segment. */
  public void addSegmentRule(SegmentRule segmentRule) {
    if (segmentRule != null && !segmentRules.contains(segmentRule)) {
      segmentRules.add(segmentRule);
    }
  }

  public void syncSegmentRules(List<SegmentRule> newSegmentRules) {
    // Remove rules that are no longer in the new list
    segmentRules.removeIf(existingRule -> !newSegmentRules.contains(existingRule));

    // Add new rules that are not already in the list
    for (SegmentRule newRule : newSegmentRules) {
      if (!segmentRules.contains(newRule)) {
        addSegmentRule(newRule);
      }
    }
  }

  /** Check if this segment has any rules. */
  public boolean hasSegmentRules() {
    return !segmentRules.isEmpty();
  }

  /** Get segment rules as a formatted string. */
  public String getSegmentRulesAsString() {
    if (segmentRules.isEmpty()) {
      return "Standard Match";
    }
    return segmentRules.stream()
        .map(SegmentRule::getName)
        .collect(java.util.stream.Collectors.joining(", "));
  }
}
