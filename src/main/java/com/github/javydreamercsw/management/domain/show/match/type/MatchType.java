package com.github.javydreamercsw.management.domain.show.match.type;

import static com.github.javydreamercsw.management.domain.card.Card.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "match_type", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class MatchType extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "match_type_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Override
  public @Nullable Long getId() {
    return id;
  }
}
