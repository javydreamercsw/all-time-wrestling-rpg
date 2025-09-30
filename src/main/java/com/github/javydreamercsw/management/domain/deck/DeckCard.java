package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "deck_card",
    uniqueConstraints = @UniqueConstraint(columnNames = {"deck_id", "card_id", "set_id"}))
@Getter
@Setter
public class DeckCard extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "deck_id")
  private Deck deck;

  @ManyToOne(optional = false)
  @JoinColumn(name = "card_id")
  private Card card;

  @ManyToOne(optional = false)
  @JoinColumn(name = "set_id")
  private CardSet set;

  @Column(nullable = false, name = "amount")
  private int amount;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    DeckCard deckCard = (DeckCard) o;
    return Objects.equals(deck, deckCard.deck)
        && Objects.equals(card, deckCard.card)
        && Objects.equals(set, deckCard.set);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), deck, card, set);
  }
}
