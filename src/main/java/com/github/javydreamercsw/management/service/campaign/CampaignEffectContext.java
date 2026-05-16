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

  public void spendStamina(final int amount) {
    // TODO: Integrate with Match Engine to reduce current stamina
    log.debug("[Script] Spending {} Stamina", amount);
  }

  public void gainStamina(final int amount) {
    // TODO: Integrate with Match Engine to increase current stamina
    log.debug("[Script] Gaining {} Stamina", amount);
  }

  public void gainHitPoints(final int amount) {
    // TODO: Integrate with Match Engine to heal
    log.debug("[Script] Gaining {} HP", amount);
  }

  public void damage(final int amount) {
    // TODO: Integrate with Match Engine to damage opponent
    log.debug("[Script] Dealing {} Damage to opponent", amount);
  }

  public void gainMomentum(final int amount) {
    // TODO: Integrate with Match Engine momentum tracker
    log.debug("[Script] Gaining {} Momentum", amount);
  }

  public void drawCard(final int amount) {
    // TODO: Integrate with Deck/Hand management
    log.debug("[Script] Drawing {} cards", amount);
  }

  // ==================== Match Flow Control ====================

  public void gainInitiative() {
    // TODO: Set initiative to player
    log.debug("[Script] Gaining Initiative");
  }

  public void pin() {
    // TODO: Attempt pinfall
    log.debug("[Script] Attempting Pin");
  }

  public void breakPin() {
    // TODO: Force pin break
    log.debug("[Script] Breaking Pin");
  }

  public void negateAttack() {
    // TODO: Cancel incoming attack
    log.debug("[Script] Negating Attack");
  }

  // ==================== Modifiers ====================

  public void modifyOpponentRoll(final int modifier) {
    // TODO: Add temporary modifier to opponent's next roll
    log.debug("[Script] Modifying Opponent Roll by {}", modifier);
  }

  public void modifyRoll(final int modifier) {
    // TODO: Add temporary modifier to player's roll
    log.debug("[Script] Modifying Player Roll by {}", modifier);
  }

  public void modifyBackstageDice(final int amount) {
    // TODO: Add bonus dice for backstage checks
    log.debug("[Script] Adding {} dice to Backstage Check", amount);
  }

  public void modifyAttribute(final String attribute, final int amount) {
    // TODO: Temporary or permanent attribute buff
    log.debug("[Script] Modifying attribute {} by {}", attribute, amount);
  }
}
