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
package com.github.javydreamercsw.management.domain.wrestler;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.Move;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.MoveSet;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.card.Card;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class WrestlerDTO implements Serializable {
  private Long id; // Add id field
  private String name;
  private String description;
  private String gender;
  private WrestlerTier tier;
  private MoveSet moveSet; // Add MoveSet field
  private Long fans;
  private String externalId;
  private String imageUrl;
  private String managerName;
  private boolean injured;

  public WrestlerDTO(@NonNull Wrestler wrestler) {
    this.id = wrestler.getId(); // Initialize id
    this.name = wrestler.getName();
    this.description = wrestler.getDescription();
    this.gender = wrestler.getGender() != null ? wrestler.getGender().name() : Gender.MALE.name();
    this.tier = wrestler.getTier();
    this.moveSet = convertToMoveSet(wrestler); // Populate MoveSet
    this.fans = wrestler.getFans();
    this.externalId = wrestler.getExternalId();
    this.imageUrl = wrestler.getImageUrl();
    this.injured = !wrestler.getActiveInjuries().isEmpty();
    if (wrestler.getManager() != null) {
      this.managerName = wrestler.getManager().getName();
    }
  }

  private MoveSet convertToMoveSet(Wrestler wrestler) {
    MoveSet ms = new MoveSet();
    List<Move> finishers = new ArrayList<>();
    List<Move> trademarks = new ArrayList<>();
    List<Move> commonMoves = new ArrayList<>();

    wrestler
        .getDecks()
        .forEach(
            deck ->
                deck.getCards()
                    .forEach(
                        deckCard -> { // deckCard is of type DeckCard
                          Card card = deckCard.getCard(); // Get the actual Card object
                          String moveDescription =
                              card.getName(); // Start with the card name as description

                          // Enhance description based on card properties
                          if (card.getFinisher()) {
                            moveDescription += " (Finisher)";
                          } else if (card.getSignature()) {
                            moveDescription += " (Signature)";
                          } else if (card.getTaunt()) {
                            moveDescription += " (Taunt)";
                          } else if (card.getRecover()) {
                            moveDescription += " (Recovery)";
                          } else if (card.getPin()) {
                            moveDescription += " (Pin Attempt)";
                          }

                          // Use card.getType() for the Move's type
                          Move move = new Move(card.getName(), moveDescription, card.getType());

                          if (card.getFinisher()) {
                            finishers.add(move);
                          } else if (card
                              .getSignature()) { // Assuming signature cards are trademarks
                            trademarks.add(move);
                          } else { // All other cards can be considered common moves
                            commonMoves.add(move);
                          }
                        }));

    ms.setFinishers(finishers);
    ms.setTrademarks(trademarks);
    ms.setCommonMoves(commonMoves);

    return ms;
  }
}
