package com.github.javydreamercsw.management.domain.card;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "card", uniqueConstraints = @UniqueConstraint(columnNames = {"set_id", "number"}))
@Setter
@Getter
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
}
