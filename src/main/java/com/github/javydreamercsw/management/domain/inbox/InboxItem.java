package com.github.javydreamercsw.management.domain.inbox;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private InboxEventType eventType;

  @Column(name = "description", nullable = false)
  @Size(max = 1024) private String description;

  @Column(name = "event_timestamp", nullable = false)
  private Instant eventTimestamp;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @Column(name = "reference_id")
  private String referenceId;

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
}
