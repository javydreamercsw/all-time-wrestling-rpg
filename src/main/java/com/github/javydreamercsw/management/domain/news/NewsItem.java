/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.news;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "news_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItem extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "news_item_id")
  private Long id;

  @NotNull @Size(max = 255) @Column(name = "headline", nullable = false)
  private String headline;

  @NotNull @Size(max = 2000) @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @NotNull @Column(name = "publish_date", nullable = false)
  private Instant publishDate;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  private NewsCategory category;

  @Builder.Default
  @Column(name = "is_rumor", nullable = false)
  private Boolean isRumor = false;

  @Min(1) @Max(5) @Builder.Default
  @Column(name = "importance", nullable = false)
  private Integer importance = 3;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (publishDate == null) {
      publishDate = Instant.now();
    }
    if (isRumor == null) {
      isRumor = false;
    }
    if (importance == null) {
      importance = 3;
    }
  }
}
