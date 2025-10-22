package com.github.javydreamercsw.management.domain.wrestler;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.Move;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.MoveSet;
import com.github.javydreamercsw.management.domain.card.Card;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class WrestlerDTO {
  private String name;
  private String description;
  private String gender;
  private String tier;
  private MoveSet moveSet; // Add MoveSet field

  public WrestlerDTO(@NonNull Wrestler wrestler) {
    this.name = wrestler.getName();
    this.description = wrestler.getDescription();
    this.gender = wrestler.getGender() != null ? wrestler.getGender().name() : Gender.MALE.name();
    this.tier = wrestler.getTier().name();
    this.moveSet = convertToMoveSet(wrestler); // Populate MoveSet
  }

  private MoveSet convertToMoveSet(Wrestler wrestler) {
    MoveSet ms = new MoveSet();
    List<Move> finishers = new ArrayList<>();
    List<Move> trademarks = new ArrayList<>();
    List<Move> commonMoves = new ArrayList<>();

    wrestler
        .getDecks()
        .forEach(
            deck -> {
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
                        } else if (card.getSignature()) { // Assuming signature cards are trademarks
                          trademarks.add(move);
                        } else { // All other cards can be considered common moves
                          commonMoves.add(move);
                        }
                      });
            });

    ms.setFinishers(finishers);
    ms.setTrademarks(trademarks);
    ms.setCommonMoves(commonMoves);

    return ms;
  }
}
