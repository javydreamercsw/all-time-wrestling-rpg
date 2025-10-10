package com.github.javydreamercsw.management.service.storyline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.storyline.StorylineBranch;
import com.github.javydreamercsw.management.domain.storyline.StorylineBranchRepository;
import com.github.javydreamercsw.management.domain.storyline.StorylineBranchType;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class StorylineBranchingServiceTest {

  @Mock private StorylineBranchRepository branchRepository;
  @Mock private Clock clock;
  private StorylineBranchingService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new StorylineBranchingService(branchRepository, clock);
  }

  @Test
  void testCreateBranch() {
    when(clock.instant()).thenReturn(Instant.now());
    StorylineBranchType type = StorylineBranchType.RIVALRY_ESCALATION;
    StorylineBranch branch = new StorylineBranch();
    branch.setName("Test");
    branch.setDescription("Desc");
    branch.setBranchType(type);
    branch.setPriority(type.getDefaultPriority());
    branch.setIsActive(true);
    branch.setCreationDate(Instant.now());
    when(branchRepository.saveAndFlush(any())).thenReturn(branch);
    Optional<StorylineBranch> result = service.createBranch("Test", "Desc", type, 0);
    assertTrue(result.isPresent());
    assertEquals("Test", result.get().getName());
  }

  @Test
  void testGetAllBranches() {
    Page<StorylineBranch> page = new PageImpl<>(java.util.List.of(new StorylineBranch()));
    when(branchRepository.findAllBy(any())).thenReturn(page);
    Page<StorylineBranch> result = service.getAllBranches(PageRequest.of(0, 10));
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void testGetBranchById() {
    StorylineBranch branch = new StorylineBranch();
    branch.setName("Test");
    when(branchRepository.findById(anyLong())).thenReturn(Optional.of(branch));
    Optional<StorylineBranch> result = service.getBranchById(1L);
    assertTrue(result.isPresent());
    assertEquals("Test", result.get().getName());
  }

  @Test
  void testGetActiveBranches() {
    StorylineBranch branch = new StorylineBranch();
    branch.setIsActive(true);
    when(branchRepository.findByIsActiveTrue()).thenReturn(java.util.List.of(branch));
    var result = service.getActiveBranches();
    assertEquals(1, result.size());
    assertTrue(result.get(0).getIsActive());
  }

  @Test
  void testGetBranchesByType() {
    StorylineBranchType type = StorylineBranchType.FACTION_DYNAMICS;
    StorylineBranch branch = new StorylineBranch();
    branch.setBranchType(type);
    when(branchRepository.findByIsActiveTrueAndBranchType(type))
        .thenReturn(java.util.List.of(branch));
    var result = service.getBranchesByType(type);
    assertEquals(1, result.size());
    assertEquals(type, result.get(0).getBranchType());
  }

  @Test
  void testAddConditionBranchNotFound() {
    when(branchRepository.findById(anyLong())).thenReturn(Optional.empty());
    var result = service.addCondition(1L, "type", "key", "value", "desc");
    assertTrue(result.isEmpty());
  }
}
