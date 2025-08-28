package com.github.javydreamercsw.management.domain.show;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "show")
@Getter
@Setter
public class Show extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "show_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description", nullable = false)
  private String description;

  @ManyToOne(optional = false)
  @JoinColumn(name = "show_type_id", nullable = false)
  private ShowType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "season_id")
  private Season season;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  private ShowTemplate template;

  @Column(name = "show_date")
  private LocalDate showDate;

  @Column(name = "external_id", unique = true)
  @Size(max = 255) private String externalId;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /**
   * Check if this show is based on a Premium Live Event template.
   *
   * @return true if this show uses a PLE template
   */
  public boolean isPremiumLiveEvent() {
    return template != null && template.isPremiumLiveEvent();
  }

  /**
   * Check if this show is based on a Weekly show template.
   *
   * @return true if this show uses a weekly show template
   */
  public boolean isWeeklyShow() {
    return template != null && template.isWeeklyShow();
  }

  /**
   * Get the template name if available.
   *
   * @return template name or null if no template is set
   */
  public @Nullable String getTemplateName() {
    return template != null ? template.getName() : null;
  }

  /**
   * Check if this show has a scheduled date.
   *
   * @return true if showDate is set
   */
  public boolean hasScheduledDate() {
    return showDate != null;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }
}
