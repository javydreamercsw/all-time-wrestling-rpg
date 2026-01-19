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

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Context object exposed to Groovy scripts in Campaign Ability Cards. Provides methods to
 * manipulate campaign state, wrestler stats, and match flow.
 */
@Slf4j
@RequiredArgsConstructor
public class CampaignEffectContext {

  private final Campaign campaign;

  // ==================== Resource Management ====================

  public void spendStamina(int amount) {
    // TODO: Integrate with Match Engine to reduce current stamina
    log.info("[Script] Spending {} Stamina", amount);
  }

  public void gainStamina(int amount) {
    // TODO: Integrate with Match Engine to increase current stamina
    log.info("[Script] Gaining {} Stamina", amount);
  }

  public void gainHitPoints(int amount) {
    // TODO: Integrate with Match Engine to heal
    log.info("[Script] Gaining {} HP", amount);
  }

  public void damage(int amount) {
    // TODO: Integrate with Match Engine to damage opponent
    log.info("[Script] Dealing {} Damage to opponent", amount);
  }

  public void gainMomentum(int amount) {
    // TODO: Integrate with Match Engine momentum tracker
    log.info("[Script] Gaining {} Momentum", amount);
  }

  public void drawCard(int amount) {
    // TODO: Integrate with Deck/Hand management
    log.info("[Script] Drawing {} cards", amount);
  }

  // ==================== Match Flow Control ====================

  public void gainInitiative() {
    // TODO: Set initiative to player
    log.info("[Script] Gaining Initiative");
  }

  public void pin() {
    // TODO: Attempt pinfall
    log.info("[Script] Attempting Pin");
  }

  public void breakPin() {
    // TODO: Force pin break
    log.info("[Script] Breaking Pin");
  }

  public void negateAttack() {
    // TODO: Cancel incoming attack
    log.info("[Script] Negating Attack");
  }

  // ==================== Modifiers ====================

  public void modifyOpponentRoll(int modifier) {
    // TODO: Add temporary modifier to opponent's next roll
    log.info("[Script] Modifying Opponent Roll by {}", modifier);
  }

  public void modifyRoll(int modifier) {
    // TODO: Add temporary modifier to player's roll
    log.info("[Script] Modifying Player Roll by {}", modifier);
  }

  public void modifyBackstageDice(int amount) {
    // TODO: Add bonus dice for backstage checks
    log.info("[Script] Adding {} dice to Backstage Check", amount);
  }

  public void modifyAttribute(String attribute, int amount) {
    // TODO: Temporary or permanent attribute buff
    log.info("[Script] Modifying attribute {} by {}", attribute, amount);
  }
}
