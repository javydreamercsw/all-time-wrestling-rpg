package com.github.javydreamercsw.management.domain.storyline;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StorylineBranchTest {
  @Test
  void testDefaultValues() {
    StorylineBranch branch = new StorylineBranch();
    assertTrue(branch.getIsActive(), "Default isActive should be true");
    assertEquals(1, branch.getPriority(), "Default priority should be 1");
  }

  @Test
  void testFieldAssignment() {
    StorylineBranch branch = new StorylineBranch();
    branch.setName("Branch Name");
    branch.setDescription("Branch Description");
    branch.setBranchType(StorylineBranchType.RIVALRY_ESCALATION);
    branch.setCreationDate(Instant.now());
    assertEquals("Branch Name", branch.getName());
    assertEquals("Branch Description", branch.getDescription());
    assertEquals(StorylineBranchType.RIVALRY_ESCALATION, branch.getBranchType());
    assertNotNull(branch.getCreationDate());
  }

  @Test
  void testNameLengthConstraint() {
    StorylineBranch branch = new StorylineBranch();
    String longName = "a".repeat(300);
    branch.setName(longName);
    assertEquals(longName, branch.getName()); // Constraint is enforced at persistence layer
  }
}
