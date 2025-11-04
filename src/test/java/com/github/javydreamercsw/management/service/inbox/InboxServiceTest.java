package com.github.javydreamercsw.management.service.inbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

  @Mock private InboxItemRepository inboxItemRepository;

  @Mock private TitleRepository titleRepository;

  @Mock private WrestlerRepository wrestlerRepository;

  @InjectMocks private InboxService inboxService;

  @Test
  void handleChampionshipDefendedEvent() {
    // Given
    Title title = mock(Title.class);
    when(title.getId()).thenReturn(1L);
    when(title.getName()).thenReturn("Test Title");
    when(titleRepository.findById(1L)).thenReturn(java.util.Optional.of(title));

    Wrestler wrestler = mock(Wrestler.class);
    when(wrestler.getName()).thenReturn("Test Wrestler");

    ChampionshipDefendedEvent event = new ChampionshipDefendedEvent(this, title, List.of(wrestler));

    // When
    inboxService.handleChampionshipDefendedEvent(event);

    // Then
    verify(inboxItemRepository, times(1)).save(any(InboxItem.class));
  }

  @Test
  void handleChampionshipChangeEvent() {
    // Given
    Title title = mock(Title.class);
    when(title.getId()).thenReturn(1L);
    when(title.getName()).thenReturn("Test Title");
    when(titleRepository.findById(1L)).thenReturn(java.util.Optional.of(title));

    Wrestler wrestler = mock(Wrestler.class);
    when(wrestler.getName()).thenReturn("Test Wrestler");

    Wrestler oldWrestler = mock(Wrestler.class);
    when(oldWrestler.getName()).thenReturn("Old Wrestler");

    ChampionshipChangeEvent event =
        new ChampionshipChangeEvent(this, title, List.of(wrestler), List.of(oldWrestler));

    // When
    inboxService.handleChampionshipChangeEvent(event);
    verify(inboxItemRepository, times(1)).save(any(InboxItem.class));
  }

  @Test
  void testDeleteSelected() {
    // Given
    InboxItem item1 = new InboxItem();
    item1.setId(1L);
    InboxItem item2 = new InboxItem();
    item2.setId(2L);
    List<InboxItem> items = List.of(item1, item2);

    // When
    inboxService.deleteSelected(items);

    // Then
    verify(inboxItemRepository, times(1)).deleteAll(items);
  }
}
