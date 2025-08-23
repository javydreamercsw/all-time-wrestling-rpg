package com.github.javydreamercsw.management.domain.show.template;

import static com.github.javydreamercsw.management.domain.card.Card.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Entity representing a show template in the ATW RPG system. Templates define the structure and
 * characteristics of different types of wrestling shows.
 */
@Entity
@Table(name = "show_template", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class ShowTemplate extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "template_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Column(name = "description")
  @Size(max = DESCRIPTION_MAX_LENGTH) private String description;

  @ManyToOne(optional = false)
  @JoinColumn(name = "show_type_id", nullable = false)
  private ShowType showType;

  @Column(name = "notion_url")
  @Size(max = 500) private String notionUrl;

  @Column(name = "external_id", unique = true)
  @Size(max = 255) private String externalId;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /**
   * Check if this is a Premium Live Event (PLE) template.
   *
   * @return true if this template is for a PLE
   */
  public boolean isPremiumLiveEvent() {
    return showType != null && "Premium Live Event (PLE)".equals(showType.getName());
  }

  /**
   * Check if this is a Weekly show template.
   *
   * @return true if this template is for a weekly show
   */
  public boolean isWeeklyShow() {
    return showType != null && "Weekly".equals(showType.getName());
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }
}
