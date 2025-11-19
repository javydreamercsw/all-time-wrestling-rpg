package com.github.javydreamercsw.management.service.inbox;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

  @Mock private InboxRepository inboxRepository;

  @Mock private TitleRepository titleRepository;

  @Mock private WrestlerRepository wrestlerRepository;

  @InjectMocks private InboxService inboxService;

  @Test
  void testDeleteSelected() {
    // Given
    InboxItem item1 = new InboxItem();
    item1.setId(1L);
    InboxItem item2 = new InboxItem();
    item2.setId(2L);
    Set<InboxItem> items = Set.of(item1, item2);

    // When
    inboxService.deleteSelected(items);

    // Then
    verify(inboxRepository, times(1)).deleteAll(items);
  }
}
