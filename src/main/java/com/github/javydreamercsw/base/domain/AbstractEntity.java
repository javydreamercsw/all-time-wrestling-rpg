package com.github.javydreamercsw.base.domain;

import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

@MappedSuperclass
public abstract class AbstractEntity<ID> {

  /** Maximum length for description fields across all entities */
  public static final int DESCRIPTION_MAX_LENGTH = 255;

  public abstract @Nullable ID getId();

  @Override
  public String toString() {
    return "%s{id=%s}".formatted(getClass().getSimpleName(), getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractEntity<?> that = (AbstractEntity<?>) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
