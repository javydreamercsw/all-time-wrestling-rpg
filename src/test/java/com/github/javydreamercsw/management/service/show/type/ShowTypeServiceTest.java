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
package com.github.javydreamercsw.management.service.show.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowTypeServiceTest {

  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private Clock clock;

  @InjectMocks private ShowTypeService service;

  @Test
  void list_delegatesToRepository() {
    Pageable pageable = PageRequest.of(0, 10);
    @SuppressWarnings("unchecked")
    Page<ShowType> slice = mock(Page.class);
    ShowType st = new ShowType();
    when(showTypeRepository.findAllBy(pageable)).thenReturn(slice);
    when(slice.toList()).thenReturn(List.of(st));

    List<ShowType> result = service.list(pageable);

    assertThat(result).containsExactly(st);
    verify(showTypeRepository).findAllBy(pageable);
  }

  @Test
  void count_returnsRepositoryCount() {
    when(showTypeRepository.count()).thenReturn(42L);

    assertThat(service.count()).isEqualTo(42L);
  }

  @Test
  void save_setsCreationDateAndDelegatesToRepository() {
    Instant fixedInstant = Instant.parse("2025-01-01T00:00:00Z");
    when(clock.instant()).thenReturn(fixedInstant);
    ShowType showType = new ShowType();
    when(showTypeRepository.saveAndFlush(showType)).thenReturn(showType);

    ShowType result = service.save(showType);

    verify(clock).instant();
    verify(showTypeRepository).saveAndFlush(showType);
    assertThat(showType.getCreationDate()).isEqualTo(fixedInstant);
    assertThat(result).isSameAs(showType);
  }

  @Test
  void findAll_returnsList() {
    ShowType st1 = new ShowType();
    ShowType st2 = new ShowType();
    when(showTypeRepository.findAll()).thenReturn(List.of(st1, st2));

    List<ShowType> result = service.findAll();

    assertThat(result).containsExactly(st1, st2);
  }

  @Test
  void delete_delegatesToRepository() {
    ShowType showType = new ShowType();

    service.delete(showType);

    verify(showTypeRepository).delete(showType);
  }

  @Test
  void findByName_found_returnsOptional() {
    ShowType st = new ShowType();
    st.setName("PPV");
    when(showTypeRepository.findByName("PPV")).thenReturn(Optional.of(st));

    Optional<ShowType> result = service.findByName("PPV");

    assertThat(result).isPresent().contains(st);
  }

  @Test
  void findByName_notFound_returnsEmpty() {
    when(showTypeRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<ShowType> result = service.findByName("Unknown");

    assertThat(result).isEmpty();
  }

  @Test
  void existsByName_true() {
    when(showTypeRepository.existsByName("PPV")).thenReturn(true);

    assertThat(service.existsByName("PPV")).isTrue();
  }

  @Test
  void existsByName_false() {
    when(showTypeRepository.existsByName("Unknown")).thenReturn(false);

    assertThat(service.existsByName("Unknown")).isFalse();
  }

  @Test
  void createOrUpdateShowType_newType_createsAndSaves() {
    Instant fixedInstant = Instant.parse("2025-06-01T00:00:00Z");
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(clock.instant()).thenReturn(fixedInstant);
    when(showTypeRepository.findByName("House Show")).thenReturn(Optional.empty());
    ShowType saved = new ShowType();
    when(showTypeRepository.saveAndFlush(any(ShowType.class))).thenReturn(saved);

    ShowType result = service.createOrUpdateShowType("House Show", "A house show", 5, 2);

    verify(showTypeRepository).saveAndFlush(any(ShowType.class));
    assertThat(result).isSameAs(saved);
  }

  @Test
  void createOrUpdateShowType_existingTypeUnchanged_returnsExisting() {
    ShowType existing = new ShowType();
    existing.setName("PPV");
    existing.setDescription("Pay-per-view event");
    existing.setExpectedMatches(8);
    existing.setExpectedPromos(3);
    when(showTypeRepository.findByName("PPV")).thenReturn(Optional.of(existing));

    ShowType result = service.createOrUpdateShowType("PPV", "Pay-per-view event", 8, 3);

    verify(showTypeRepository, never()).saveAndFlush(any());
    assertThat(result).isSameAs(existing);
  }

  @Test
  void createOrUpdateShowType_existingTypeChanged_updatesAndSaves() {
    Instant fixedInstant = Instant.parse("2025-06-01T00:00:00Z");
    when(clock.instant()).thenReturn(fixedInstant);
    ShowType existing = new ShowType();
    existing.setName("PPV");
    existing.setDescription("Old description");
    existing.setExpectedMatches(5);
    existing.setExpectedPromos(2);
    when(showTypeRepository.findByName("PPV")).thenReturn(Optional.of(existing));
    ShowType saved = new ShowType();
    when(showTypeRepository.saveAndFlush(any(ShowType.class))).thenReturn(saved);

    ShowType result = service.createOrUpdateShowType("PPV", "New description", 6, 3);

    verify(showTypeRepository).saveAndFlush(existing);
    assertThat(result).isSameAs(saved);
  }
}
