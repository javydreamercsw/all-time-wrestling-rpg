package com.github.javydreamercsw.base.domain;

import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

@MappedSuperclass
public abstract class AbstractEntity<ID> {

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
    return 31;
  }
}
