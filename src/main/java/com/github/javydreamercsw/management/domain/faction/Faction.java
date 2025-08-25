package com.github.javydreamercsw.management.domain.faction;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
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
 * Represents a faction (stable) of wrestlers in the ATW RPG system. Factions can have rivalries
 * with other factions and participate in multi-wrestler feuds.
 */
@Entity
@Table(name = "faction", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class Faction extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "faction_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leader_id")
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns", "faction"})
  private Wrestler leader;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "formed_date", nullable = false)
  private Instant formedDate;

  @Column(name = "disbanded_date")
  private Instant disbandedDate;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "external_id")
  private String externalId; // External system ID (e.g., Notion page ID)

  // Faction members
  @OneToMany(mappedBy = "faction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction", "rivalries", "injuries", "deck", "titleReigns"})
  private List<Wrestler> members = new ArrayList<>();

  // Faction rivalries where this faction is faction1
  @OneToMany(mappedBy = "faction1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction1", "faction2"})
  private List<FactionRivalry> rivalriesAsFaction1 = new ArrayList<>();

  // Faction rivalries where this faction is faction2
  @OneToMany(mappedBy = "faction2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction1", "faction2"})
  private List<FactionRivalry> rivalriesAsFaction2 = new ArrayList<>();

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (formedDate == null) {
      formedDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Add a member to the faction. */
  public void addMember(Wrestler wrestler) {
    if (wrestler != null && !members.contains(wrestler)) {
      members.add(wrestler);
      wrestler.setFaction(this);
    }
  }

  /** Remove a member from the faction. */
  public void removeMember(Wrestler wrestler) {
    if (wrestler != null && members.contains(wrestler)) {
      members.remove(wrestler);
      wrestler.setFaction(null);
    }
  }

  /** Check if a wrestler is a member of this faction. */
  public boolean hasMember(Wrestler wrestler) {
    return members.contains(wrestler);
  }

  /** Get the number of active members. */
  public int getMemberCount() {
    return members.size();
  }

  /** Check if this faction is a singles faction (1 member). */
  public boolean isSinglesFaction() {
    return members.size() == 1;
  }

  /** Check if this faction is a tag team (2 members). */
  public boolean isTagTeam() {
    return members.size() == 2;
  }

  /** Check if this faction is a stable (3+ members). */
  public boolean isStable() {
    return members.size() >= 3;
  }

  /** Disband the faction. */
  public void disband(String reason) {
    this.isActive = false;
    this.disbandedDate = Instant.now();

    // Remove all members from faction
    for (Wrestler member : new ArrayList<>(members)) {
      removeMember(member);
    }
  }

  /** Get all active faction rivalries involving this faction. */
  public List<FactionRivalry> getActiveRivalries() {
    List<FactionRivalry> activeRivalries = new ArrayList<>();

    for (FactionRivalry rivalry : rivalriesAsFaction1) {
      if (rivalry.getIsActive()) {
        activeRivalries.add(rivalry);
      }
    }

    for (FactionRivalry rivalry : rivalriesAsFaction2) {
      if (rivalry.getIsActive()) {
        activeRivalries.add(rivalry);
      }
    }

    return activeRivalries;
  }

  /** Get the opposing faction in a rivalry. */
  public Faction getOpposingFaction(FactionRivalry rivalry) {
    if (rivalry.getFaction1().equals(this)) {
      return rivalry.getFaction2();
    } else if (rivalry.getFaction2().equals(this)) {
      return rivalry.getFaction1();
    }
    throw new IllegalArgumentException("Faction is not part of this rivalry");
  }

  /** Check if this faction has a rivalry with another faction. */
  public boolean hasRivalryWith(Faction otherFaction) {
    return getActiveRivalries().stream()
        .anyMatch(
            rivalry ->
                rivalry.getFaction1().equals(otherFaction)
                    || rivalry.getFaction2().equals(otherFaction));
  }

  /** Get display name with member count. */
  public String getDisplayName() {
    if (!isActive) {
      return name + " (Disbanded)";
    }
    return name + " (" + members.size() + " members)";
  }

  /** Get faction type based on member count. */
  public String getFactionType() {
    if (members.size() == 1) return "Singles";
    if (members.size() == 2) return "Tag Team";
    return "Stable";
  }
}
