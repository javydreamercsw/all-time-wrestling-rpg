package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "deck")
public class Deck extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deck_id")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @OneToMany(
      fetch = FetchType.EAGER,
      mappedBy = "deck",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<DeckCard> cards = new ArrayList<>();

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  public void setCreationDate(Instant creationDate) {
    this.creationDate = creationDate;
  }

  public Instant getCreationDate() {
    return creationDate;
  }

  public void setWrestler(Wrestler wrestler) {
    this.wrestler = wrestler;
  }

  public Wrestler getWrestler() {
    return wrestler;
  }

  public List<DeckCard> getCards() {
    return cards;
  }

  public void setCards(List<DeckCard> cards) {
    this.cards = cards;
  }

  public void addCard(@NonNull Card card, int amount) {
    DeckCard deckCard = new DeckCard();
    deckCard.setCard(card);
    deckCard.setAmount(amount);
    deckCard.setDeck(this);
    getCards().add(deckCard);
  }
}
