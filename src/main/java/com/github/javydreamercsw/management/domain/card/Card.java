package com.github.javydreamercsw.management.domain.card;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "card", uniqueConstraints = @UniqueConstraint(columnNames = {"set_id", "number"}))
public class Card extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "card_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Column(name = "type", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String type;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "set_id", nullable = false)
  private CardSet set;

  @Column(name = "target", nullable = false)
  private Integer target;

  @Column(name = "stamina", nullable = false)
  private Integer stamina;

  @Column(name = "damage", nullable = false)
  private Integer damage;

  @Column(name = "momentum", nullable = false)
  private Integer momentum;

  @Column(name = "number")
  private Integer number;

  @Column(name = "signature", nullable = false)
  private Boolean signature = false;

  @Column(name = "finisher", nullable = false)
  private Boolean finisher = false;

  @Column(name = "taunt", nullable = false)
  private Boolean taunt = false;

  @Column(name = "recover", nullable = false)
  private Boolean recover = false;

  @Column(name = "pin", nullable = false)
  private Boolean pin = false;

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

  public CardSet getSet() {
    return set;
  }

  public void setSet(CardSet set) {
    this.set = set;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }

  public Boolean getTaunt() {
    return taunt;
  }

  public void setTaunt(Boolean taunt) {
    this.taunt = taunt;
  }

  public Boolean getRecover() {
    return recover;
  }

  public void setRecover(Boolean recover) {
    this.recover = recover;
  }

  public Boolean getPin() {
    return pin;
  }

  public void setPin(Boolean pin) {
    this.pin = pin;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  @PreUpdate
  private void ensureDefaults() {
    if (number == null) {
      number = 0;
    }
  }
}
