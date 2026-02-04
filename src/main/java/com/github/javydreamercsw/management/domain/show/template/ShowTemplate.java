/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.domain.show.template;

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

  @Column(name = "image_url")
  @Size(max = 512) private String imageUrl;

  @Column(name = "expected_matches")
  private Integer expectedMatches;

  @Column(name = "expected_promos")
  private Integer expectedPromos;

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
