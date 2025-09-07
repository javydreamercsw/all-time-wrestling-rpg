package com.github.javydreamercsw.management.service.storyline;

import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.storyline.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing storyline branching based on match outcomes in the ATW RPG system. Handles
 * creation, activation, and execution of storyline branches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorylineBranchingService {

  @Autowired private final StorylineBranchRepository storylineBranchRepository;
  @Autowired private final Clock clock;

  /** Get all storyline branches with pagination. */
  @Transactional(readOnly = true)
  public Page<StorylineBranch> getAllBranches(Pageable pageable) {
    return storylineBranchRepository.findAllBy(pageable);
  }

  /** Get storyline branch by ID. */
  @Transactional(readOnly = true)
  public Optional<StorylineBranch> getBranchById(@NonNull Long id) {
    return storylineBranchRepository.findById(id);
  }

  /** Get all active storyline branches. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getActiveBranches() {
    return storylineBranchRepository.findByIsActiveTrue();
  }

  /** Get branches by type. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getBranchesByType(@NonNull StorylineBranchType branchType) {
    return storylineBranchRepository.findByIsActiveTrueAndBranchType(branchType);
  }

  /** Create a new storyline branch. */
  public Optional<StorylineBranch> createBranch(
      @NonNull String name,
      @NonNull String description,
      @NonNull StorylineBranchType branchType,
      int priority) {
    StorylineBranch branch = new StorylineBranch();
    branch.setName(name);
    branch.setDescription(description);
    branch.setBranchType(branchType);
    branch.setPriority(priority > 0 ? priority : branchType.getDefaultPriority());
    branch.setIsActive(true);
    branch.setCreationDate(clock.instant());

    StorylineBranch savedBranch = storylineBranchRepository.saveAndFlush(branch);

    log.info(
        "Created storyline branch: {} (Type: {}, Priority: {})",
        savedBranch.getName(),
        branchType,
        savedBranch.getPriority());

    return Optional.of(savedBranch);
  }

  /** Add a condition to a storyline branch. */
  public Optional<StorylineBranch> addCondition(
      @NonNull Long branchId,
      @NonNull String conditionType,
      @NonNull String conditionKey,
      @NonNull String conditionValue,
      @NonNull String description) {
    Optional<StorylineBranch> branchOpt = storylineBranchRepository.findById(branchId);

    if (branchOpt.isEmpty()) {
      return Optional.empty();
    }

    StorylineBranch branch = branchOpt.get();

    StorylineBranchCondition condition = new StorylineBranchCondition();
    condition.setConditionType(conditionType);
    condition.setConditionKey(conditionKey);
    condition.setConditionValue(conditionValue);
    condition.setConditionDescription(description);
    condition.setIsConditionMet(false);
    condition.setCreationDate(clock.instant());

    branch.addCondition(condition);
    StorylineBranch savedBranch = storylineBranchRepository.saveAndFlush(branch);

    log.info("Added condition to branch {}: {} - {}", branch.getName(), conditionType, description);

    return Optional.of(savedBranch);
  }

  /** Add an effect to a storyline branch. */
  public Optional<StorylineBranch> addEffect(
      @NonNull Long branchId,
      @NonNull String effectType,
      @NonNull String effectKey,
      @NonNull String effectValue,
      @NonNull String description,
      int executionOrder) {
    Optional<StorylineBranch> branchOpt = storylineBranchRepository.findById(branchId);

    if (branchOpt.isEmpty()) {
      return Optional.empty();
    }

    StorylineBranch branch = branchOpt.get();

    StorylineBranchEffect effect = new StorylineBranchEffect();
    effect.setEffectType(effectType);
    effect.setEffectKey(effectKey);
    effect.setEffectValue(effectValue);
    effect.setEffectDescription(description);
    effect.setExecutionOrder(executionOrder > 0 ? executionOrder : 1);
    effect.setIsExecuted(false);
    effect.setCreationDate(clock.instant());

    branch.addEffect(effect);
    StorylineBranch savedBranch = storylineBranchRepository.saveAndFlush(branch);

    log.info("Added effect to branch {}: {} - {}", branch.getName(), effectType, description);

    return Optional.of(savedBranch);
  }

  /** Get branches ready to activate. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getBranchesReadyToActivate() {
    return storylineBranchRepository.findBranchesReadyToActivate();
  }

  /** Get branches waiting for conditions. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getBranchesWaitingForConditions() {
    return storylineBranchRepository.findBranchesWaitingForConditions();
  }

  /** Get activated branches with pending effects. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getActivatedBranchesWithPendingEffects() {
    return storylineBranchRepository.findActivatedBranchesWithPendingEffects();
  }

  /** Activate a storyline branch. */
  public Optional<StorylineBranch> activateBranch(
      @NonNull Long branchId, Match triggeringMatch) {
    Optional<StorylineBranch> branchOpt = storylineBranchRepository.findById(branchId);

    if (branchOpt.isEmpty()) {
      return Optional.empty();
    }

    StorylineBranch branch = branchOpt.get();

    if (!branch.areConditionsMet()) {
      log.warn("Cannot activate branch {} - conditions not met", branch.getName());
      return Optional.empty();
    }

    if (branch.isActivated()) {
      log.warn("Branch {} is already activated", branch.getName());
      return Optional.of(branch);
    }

    branch.activate(triggeringMatch);
    StorylineBranch savedBranch = storylineBranchRepository.saveAndFlush(branch);

    log.info(
        "Activated storyline branch: {} (triggered by match: {})",
        branch.getName(),
        triggeringMatch != null ? triggeringMatch.getId() : "manual");

    return Optional.of(savedBranch);
  }

  /** Complete a storyline branch. */
  public Optional<StorylineBranch> completeBranch(@NonNull Long branchId, @NonNull String reason) {
    Optional<StorylineBranch> branchOpt = storylineBranchRepository.findById(branchId);

    if (branchOpt.isEmpty()) {
      return Optional.empty();
    }

    StorylineBranch branch = branchOpt.get();

    if (branch.isCompleted()) {
      return Optional.of(branch); // Already completed
    }

    branch.complete(reason);
    StorylineBranch savedBranch = storylineBranchRepository.saveAndFlush(branch);

    log.info("Completed storyline branch: {} (reason: {})", branch.getName(), reason);

    return Optional.of(savedBranch);
  }

  /** Process match outcome for storyline branching. */
  public void processMatchOutcome(@NonNull Match match) {
    log.info("Processing match outcome for storyline branching: Match ID {}", match.getId());

    // Get all match outcome branches
    List<StorylineBranch> matchOutcomeBranches =
        getBranchesByType(StorylineBranchType.MATCH_OUTCOME);

    for (StorylineBranch branch : matchOutcomeBranches) {
      if (branch.areConditionsMet()) {
        activateBranch(branch.getId(), match);
      }
    }

    // Check for other branch types that might be triggered by match outcomes
    processRivalryEscalationBranches(match);
    processTitleChangeBranches(match);
  }

  /** Process rivalry escalation branches. */
  private void processRivalryEscalationBranches(@NonNull Match match) {
    List<StorylineBranch> rivalryBranches =
        getBranchesByType(StorylineBranchType.RIVALRY_ESCALATION);

    for (StorylineBranch branch : rivalryBranches) {
      if (branch.areConditionsMet()) {
        activateBranch(branch.getId(), match);
      }
    }
  }

  /** Process title change branches. */
  private void processTitleChangeBranches(@NonNull Match match) {
    if (!match.getIsTitleMatch()) {
      return; // Not a title match
    }

    List<StorylineBranch> titleBranches = getBranchesByType(StorylineBranchType.TITLE_CHANGE);

    for (StorylineBranch branch : titleBranches) {
      if (branch.areConditionsMet()) {
        activateBranch(branch.getId(), match);
      }
    }
  }

  /** Get highest priority branches. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getHighestPriorityBranches(int limit) {
    return storylineBranchRepository.findHighestPriorityBranches(Pageable.ofSize(limit));
  }

  /** Get recent branches. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getRecentBranches(int days) {
    Instant sinceDate = clock.instant().minusSeconds(days * 24L * 3600L);
    return storylineBranchRepository.findRecentBranches(sinceDate);
  }

  /** Get expired branches. */
  @Transactional(readOnly = true)
  public List<StorylineBranch> getExpiredBranches(int days) {
    Instant expirationDate = clock.instant().minusSeconds(days * 24L * 3600L);
    return storylineBranchRepository.findExpiredBranches(expirationDate);
  }

  /** Clean up expired branches. */
  public int cleanupExpiredBranches(int expirationDays) {
    List<StorylineBranch> expiredBranches = getExpiredBranches(expirationDays);

    int cleanedUp = 0;
    for (StorylineBranch branch : expiredBranches) {
      completeBranch(branch.getId(), "Expired after " + expirationDays + " days");
      cleanedUp++;
    }

    log.info("Cleaned up {} expired storyline branches", cleanedUp);
    return cleanedUp;
  }

  /** Get branch statistics. */
  @Transactional(readOnly = true)
  public BranchStatistics getBranchStatistics() {
    List<StorylineBranch> activeBranches = getActiveBranches();
    long readyToActivate = storylineBranchRepository.countBranchesReadyToActivate();

    int totalActive = activeBranches.size();
    int waitingForConditions =
        (int) activeBranches.stream().filter(branch -> !branch.areConditionsMet()).count();
    int activated = (int) activeBranches.stream().filter(StorylineBranch::isActivated).count();

    return new BranchStatistics(
        totalActive, (int) readyToActivate, waitingForConditions, activated);
  }

  /** Record class for branch statistics. */
  public record BranchStatistics(
      int totalActiveBranches, int readyToActivate, int waitingForConditions, int activated) {}
}