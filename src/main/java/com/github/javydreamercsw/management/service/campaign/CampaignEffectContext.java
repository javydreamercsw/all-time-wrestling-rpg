/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Context object exposed to Groovy scripts in Campaign Ability Cards. Provides methods to
 * manipulate campaign state, wrestler stats, and match flow.
 */
@Slf4j
public class CampaignEffectContext {

  // featureData keys consumed by sub-issues (match engine wiring)
  public static final String KEY_INITIATIVE_GRANTED = "initiativeGranted";
  public static final String KEY_PENDING_PIN_ATTEMPT = "pendingPinAttempt";
  public static final String KEY_BREAK_PIN_GRANTED = "breakPinGranted";
  public static final String KEY_ATTACK_NEGATED = "attackNegated";
  public static final String KEY_PLAYER_ROLL_MODIFIER = "playerRollModifier";
  public static final String KEY_OPPONENT_ROLL_MODIFIER = "opponentRollModifier";
  public static final String KEY_BACKSTAGE_DICE_BONUS = "backstageDiceBonus";
  public static final String KEY_ATTRIBUTE_MODIFIER_PREFIX = "attributeModifier_";

  private final Campaign campaign;
  private final CampaignStateRepository stateRepository;
  private final ObjectMapper objectMapper;

  public CampaignEffectContext(
      final Campaign campaign,
      final CampaignStateRepository stateRepository,
      final ObjectMapper objectMapper) {
    this.campaign = campaign;
    this.stateRepository = stateRepository;
    this.objectMapper = objectMapper;
  }

  // ==================== Resource Management ====================

  public void spendStamina(final int amount) {
    CampaignState state = campaign.getState();
    state.setStaminaPenalty(state.getStaminaPenalty() + amount);
    stateRepository.save(state);
    log.debug(
        "[Script] Spending {} Stamina. New staminaPenalty={}", amount, state.getStaminaPenalty());
  }

  public void gainStamina(final int amount) {
    CampaignState state = campaign.getState();
    state.setStaminaPenalty(Math.max(0, state.getStaminaPenalty() - amount));
    stateRepository.save(state);
    log.debug(
        "[Script] Gaining {} Stamina. New staminaPenalty={}", amount, state.getStaminaPenalty());
  }

  public void gainHitPoints(final int amount) {
    CampaignState state = campaign.getState();
    state.setHealthPenalty(Math.max(0, state.getHealthPenalty() - amount));
    stateRepository.save(state);
    log.debug("[Script] Gaining {} HP. New healthPenalty={}", amount, state.getHealthPenalty());
  }

  public void damage(final int amount) {
    CampaignState state = campaign.getState();
    state.setOpponentHealthPenalty(state.getOpponentHealthPenalty() + amount);
    stateRepository.save(state);
    log.debug(
        "[Script] Dealing {} damage to opponent. New opponentHealthPenalty={}",
        amount,
        state.getOpponentHealthPenalty());
  }

  public void gainMomentum(final int amount) {
    CampaignState state = campaign.getState();
    state.setMomentumBonus(state.getMomentumBonus() + amount);
    stateRepository.save(state);
    log.debug(
        "[Script] Gaining {} Momentum. New momentumBonus={}", amount, state.getMomentumBonus());
  }

  public void drawCard(final int amount) {
    CampaignState state = campaign.getState();
    state.setHandSizePenalty(Math.max(0, state.getHandSizePenalty() - amount));
    stateRepository.save(state);
    log.debug(
        "[Script] Drawing {} cards. New handSizePenalty={}", amount, state.getHandSizePenalty());
  }

  // ==================== Match Flow Control ====================

  public void gainInitiative() {
    setFeatureFlag(KEY_INITIATIVE_GRANTED, true);
    log.debug("[Script] Initiative granted to player");
  }

  public void pin() {
    setFeatureFlag(KEY_PENDING_PIN_ATTEMPT, true);
    log.debug("[Script] Pending pin attempt flagged");
  }

  public void breakPin() {
    setFeatureFlag(KEY_BREAK_PIN_GRANTED, true);
    log.debug("[Script] Break pin flagged");
  }

  public void negateAttack() {
    setFeatureFlag(KEY_ATTACK_NEGATED, true);
    log.debug("[Script] Attack negation flagged");
  }

  // ==================== Modifiers ====================

  public void modifyOpponentRoll(final int modifier) {
    accumulateFeatureInt(KEY_OPPONENT_ROLL_MODIFIER, modifier);
    log.debug("[Script] Opponent roll modifier: {}", modifier);
  }

  public void modifyRoll(final int modifier) {
    accumulateFeatureInt(KEY_PLAYER_ROLL_MODIFIER, modifier);
    log.debug("[Script] Player roll modifier: {}", modifier);
  }

  public void modifyBackstageDice(final int amount) {
    accumulateFeatureInt(KEY_BACKSTAGE_DICE_BONUS, amount);
    log.debug("[Script] Backstage dice bonus: {}", amount);
  }

  public void modifyAttribute(final String attribute, final int amount) {
    // Map well-known attributes to CampaignState fields; store the rest in featureData.
    switch (attribute.toLowerCase()) {
      case "health", "hp" -> {
        if (amount >= 0) {
          gainHitPoints(amount);
        } else {
          CampaignState state = campaign.getState();
          state.setHealthPenalty(state.getHealthPenalty() + Math.abs(amount));
          stateRepository.save(state);
        }
      }
      case "stamina" -> {
        if (amount >= 0) {
          gainStamina(amount);
        } else {
          spendStamina(Math.abs(amount));
        }
      }
      case "momentum" -> gainMomentum(amount);
      case "handsize", "hand_size" -> {
        if (amount >= 0) {
          drawCard(amount);
        } else {
          CampaignState state = campaign.getState();
          state.setHandSizePenalty(state.getHandSizePenalty() + Math.abs(amount));
          stateRepository.save(state);
        }
      }
      default -> {
        accumulateFeatureInt(KEY_ATTRIBUTE_MODIFIER_PREFIX + attribute.toLowerCase(), amount);
        log.debug("[Script] Stored attribute modifier: {}={}", attribute, amount);
      }
    }
  }

  // ==================== featureData helpers ====================

  private Map<String, Object> readFeatureData() {
    CampaignState state = campaign.getState();
    if (state.getFeatureData() == null) {
      return new HashMap<>();
    }
    try {
      return objectMapper.readValue(
          state.getFeatureData(), new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.warn("Failed to parse featureData in CampaignEffectContext", e);
      return new HashMap<>();
    }
  }

  private void writeFeatureData(final Map<String, Object> data) {
    CampaignState state = campaign.getState();
    try {
      state.setFeatureData(objectMapper.writeValueAsString(data));
      stateRepository.save(state);
    } catch (Exception e) {
      log.error("Failed to write featureData in CampaignEffectContext", e);
    }
  }

  private void setFeatureFlag(final String key, final boolean value) {
    Map<String, Object> data = readFeatureData();
    data.put(key, value);
    writeFeatureData(data);
  }

  private void accumulateFeatureInt(final String key, final int delta) {
    Map<String, Object> data = readFeatureData();
    int current = data.containsKey(key) ? ((Number) data.get(key)).intValue() : 0;
    data.put(key, current + delta);
    writeFeatureData(data);
  }
}
