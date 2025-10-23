package com.github.javydreamercsw.management.domain.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "card_set")
@Getter
@Setter
public class CardSet extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "set_id")
  private Long id;

  @Column(name = "name", nullable = false, unique = true)
  @JsonProperty("name")
  private String name;

  @Column(name = "set_code", nullable = false, unique = true, length = 3)
  @JsonProperty("set_code")
  private String setCode;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }
}
