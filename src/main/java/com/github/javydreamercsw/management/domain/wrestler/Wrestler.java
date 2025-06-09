package com.github.javydreamercsw.management.domain.wrestler;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.card.Card;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "wrestler")
public class Wrestler extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "wrestler_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = Card.DESCRIPTION_MAX_LENGTH) private String name;

  @Column(name = "starting_stamina", nullable = false)
  private Integer startingStamina;

  @Column(name = "low_stamina", nullable = false)
  private Integer lowStamina;

  @Column(name = "starting_health", nullable = false)
  private Integer startingHealth;

  @Column(name = "low_health", nullable = false)
  private Integer lowHealth;

  @Column(name = "deck_size", nullable = false)
  private Integer deckSize;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCreationDate(Instant creationDate) {
    this.creationDate = creationDate;
  }

  public Instant getCreationDate() {
    return creationDate;
  }

  public Integer getStartingStamina() {
    return startingStamina;
  }

  public void setStartingStamina(Integer startingStamina) {
    this.startingStamina = startingStamina;
  }

  public Integer getLowStamina() {
    return lowStamina;
  }

  public void setLowStamina(Integer lowStamina) {
    this.lowStamina = lowStamina;
  }

  public Integer getStartingHealth() {
    return startingHealth;
  }

  public void setStartingHealth(Integer startingHealth) {
    this.startingHealth = startingHealth;
  }

  public Integer getLowHealth() {
    return lowHealth;
  }

  public void setLowHealth(Integer lowHealth) {
    this.lowHealth = lowHealth;
  }

  public Integer getDeckSize() {
    return deckSize;
  }

  public void setDeckSize(Integer deckSize) {
    this.deckSize = deckSize;
  }
}
