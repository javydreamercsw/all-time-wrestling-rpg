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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inbox_item_target")
@Getter
@Setter
public class InboxItemTarget extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "inbox_item_target_id")
  private Long id;

  @ManyToOne
  @JoinColumn(name = "inbox_item_id")
  private InboxItem inboxItem;

  @Column(name = "target_id", nullable = false)
  private String targetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false)
  private TargetType targetType = TargetType.ACCOUNT;

  @Override
  public Long getId() {
    return id;
  }

  public enum TargetType {
    ACCOUNT,
    WRESTLER,
    MATCH_FULFILLMENT,
    RIVALRY,
    SHOW,
    TITLE,
    FACTION,
    FEUD,
    OTHER
  }
}
