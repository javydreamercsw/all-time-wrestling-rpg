package com.github.javydreamercsw.management.domain.inbox;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import java.time.Instant;
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

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "description", nullable = false, length = 1024)
  private String description;

  @Column(name = "event_timestamp", nullable = false)
  private Instant eventTimestamp;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @Override
  public Long getId() {
    return id;
  }
}
