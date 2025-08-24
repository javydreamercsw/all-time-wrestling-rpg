package com.github.javydreamercsw.management.domain.card;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
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

  @Column(name = "set_code", nullable = false)
  @Size(max = 3) private String name;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Instant creationDate) {
    this.creationDate = creationDate;
  }
}
