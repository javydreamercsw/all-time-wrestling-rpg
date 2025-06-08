package com.github.javydreamercsw.management.domain.card;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "card")
public class Card extends AbstractEntity<Long> {

  public static final int DESCRIPTION_MAX_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "card_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Column(name = "target", nullable = false)
  private Integer target;

  @Column(name = "stamina", nullable = false)
  private Integer stamina;

  @Column(name = "damage", nullable = false)
  private Integer damage;

  @Column(name = "momentum", nullable = false)
  private Integer momentum;

  @Column(name = "signature", nullable = false)
  private Boolean signature;

  @Column(name = "finisher", nullable = false)
  private Boolean finisher;

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

  public Integer getTarget() {
    return target;
  }

  public void setTarget(Integer target) {
    this.target = target;
  }

  public Integer getStamina() {
    return stamina;
  }

  public void setStamina(Integer stamina) {
    this.stamina = stamina;
  }

  public Integer getDamage() {
    return damage;
  }

  public void setDamage(Integer damage) {
    this.damage = damage;
  }

  public Integer getMomentum() {
    return momentum;
  }

  public void setMomentum(Integer momentum) {
    this.momentum = momentum;
  }

  public Boolean getSignature() {
    return signature;
  }

  public void setSignature(Boolean signature) {
    this.signature = signature;
  }

  public Boolean getFinisher() {
    return finisher;
  }

  public void setFinisher(Boolean finisher) {
    this.finisher = finisher;
  }

  public void setCreationDate(Instant creationDate) {
    this.creationDate = creationDate;
  }

  public Instant getCreationDate() {
    return creationDate;
  }
}
