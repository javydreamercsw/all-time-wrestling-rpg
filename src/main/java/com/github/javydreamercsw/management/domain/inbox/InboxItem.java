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
package com.github.javydreamercsw.management.domain.inbox;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inbox_item")
@Getter
@Setter
public class InboxItem extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "inbox_item_id")
  private Long id;

  @Convert(converter = InboxEventTypeConverter.class)
  @Column(name = "event_type", nullable = false)
  private InboxEventType eventType;

  @Column(name = "description", nullable = false)
  @Size(max = 1024) private String description;

  @Column(name = "event_timestamp", nullable = false)
  private Instant eventTimestamp;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @OneToMany(
      mappedBy = "inboxItem",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<InboxItemTarget> targets = new ArrayList<>();

  @Override
  public Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (eventTimestamp == null) {
      eventTimestamp = Instant.now();
    }
  }

  public void addTarget(String targetId) {
    InboxItemTarget target = new InboxItemTarget();
    target.setInboxItem(this);
    target.setTargetId(targetId);
    targets.add(target);
  }
}
