package com.github.javydreamercsw.management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TitleServiceTest {

  @Mock private TitleRepository titleRepository;
  @Mock private Clock clock;

  @InjectMocks private TitleService titleService;

  private Title testTitle;
  private Wrestler testWrestler;

  @BeforeEach
  void setUp() {
    testTitle = new Title();
    testTitle.setId(1L);
    testTitle.setName("Test Title");
    testTitle.setIsActive(false); // Ensure title is inactive for deletion
    testTitle.vacateTitle(); // Ensure title is vacant for deletion

    testWrestler = new Wrestler();
    testWrestler.setId(1L);
    testWrestler.setName("Test Wrestler");
  }

  @Test
  void testSave() {
    when(titleRepository.save(any(Title.class))).thenReturn(testTitle);
    Title saved = titleService.save(new Title());
    assertNotNull(saved);
    verify(titleRepository, times(1)).save(any(Title.class));
  }

  @Test
  void testDelete() {
    when(titleRepository.findById(anyLong())).thenReturn(Optional.of(testTitle));
    doNothing().when(titleRepository).deleteById(anyLong());
    assertNotNull(testTitle.getId());
    titleService.deleteTitle(testTitle.getId());
    verify(titleRepository, times(1)).deleteById(anyLong());
  }

  @Test
  void testFindAll() {
    List<Title> titles = new ArrayList<>();
    titles.add(testTitle);
    when(titleRepository.findAll()).thenReturn(titles);
    List<Title> result = titleService.findAll();
    assertEquals(1, result.size());
    verify(titleRepository, times(1)).findAll();
  }

  @Test
  void testFindByName() {
    when(titleRepository.findByName(anyString())).thenReturn(Optional.of(testTitle));
    Optional<Title> result = titleService.findByName("Test Title");
    assertTrue(result.isPresent());
    assertEquals("Test Title", result.get().getName());
    verify(titleRepository, times(1)).findByName(anyString());
  }

  @Test
  void testAwardTitleTo() {
    when(clock.instant()).thenReturn(java.time.Instant.now());
    when(titleRepository.saveAndFlush(any(Title.class))).thenReturn(testTitle);
    List<Wrestler> champions = new ArrayList<>();
    champions.add(testWrestler);
    titleService.awardTitleTo(testTitle, champions);
    verify(titleRepository, times(1)).saveAndFlush(any(Title.class));
    assertFalse(testTitle.getChampion().isEmpty());
    assertEquals(testWrestler, testTitle.getChampion().get(0));
  }

  @Test
  void testVacateTitle() {
    List<Wrestler> champions = new ArrayList<>();
    champions.add(testWrestler);
    testTitle.setChampion(champions);
    when(titleRepository.findById(anyLong())).thenReturn(Optional.of(testTitle));
    when(titleRepository.saveAndFlush(any(Title.class))).thenReturn(testTitle);
    assertNotNull(testTitle.getId());
    titleService.vacateTitle(testTitle.getId());
    verify(titleRepository, times(1)).saveAndFlush(any(Title.class));
    assertTrue(testTitle.getChampion().isEmpty());
  }
}
