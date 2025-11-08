package com.github.javydreamercsw.management.ui.view.deck;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.deck.DeckCardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;

@Route("deck-list")
@PageTitle("Deck List")
@Menu(order = 1, icon = "vaadin:records", title = "Deck List")
@PermitAll // When security is enabled, allow all authenticated users
public class DeckListView extends VerticalLayout {

  private final DeckService deckService;
  private final DeckCardService deckCardService;
  private final CardService cardService;

  public DeckListView(
      DeckService deckService, DeckCardService deckCardService, CardService cardService) {
    this.deckService = deckService;
    this.deckCardService = deckCardService;
    this.cardService = cardService;

    Grid<Deck> deckGrid = new Grid<>(Deck.class, false);
    deckGrid.addColumn(Deck::getId).setHeader("Deck ID");
    deckGrid
        .addColumn(deck -> deck.getWrestler().getName())
        .setHeader("Wrestler")
        .setSortable(true);
    deckGrid
        .addComponentColumn(
            deck -> {
              Button viewBtn = new Button("View");
              viewBtn.addClickListener(e -> openDeckView(deck));
              return viewBtn;
            })
        .setHeader("View");
    deckGrid
        .addComponentColumn(
            deck -> {
              Button edit = new Button("Edit");
              edit.addClickListener(e -> openDeckEditor(deck));
              return edit;
            })
        .setHeader("Actions");

    deckGrid.setItems(deckService.findAll());
    deckGrid.getStyle().set("height", "100vh");
    add(deckGrid);
    setSizeFull();
  }

  // Java
  private void openDeckEditor(@NonNull Deck deck) {
    Dialog dialog = new Dialog();
    dialog.setWidth("1000px");

    VerticalLayout layout = new VerticalLayout();
    Grid<DeckCard> cardGrid = new Grid<>(DeckCard.class, false);
    cardGrid.addColumn(dc -> dc.getCard().getName()).setHeader("Card").setSortable(true);
    cardGrid.addColumn(DeckCard::getAmount).setHeader("Amount").setSortable(true);

    // Editor for amount
    Binder<DeckCard> binder = new Binder<>(DeckCard.class);
    Editor<DeckCard> editor = cardGrid.getEditor();
    editor.setBinder(binder);

    cardGrid
        .addComponentColumn(
            dc -> {
              Button editBtn = new Button("Edit");
              editBtn.addClickListener(e -> openDeckCardEditor(dc, cardGrid, deck, dialog));
              return editBtn;
            })
        .setHeader("Edit");

    cardGrid
        .addComponentColumn(
            dc -> {
              Button removeBtn = new Button("Remove");
              removeBtn.addClickListener(
                  e -> {
                    deckCardService.delete(dc);
                    deck.getCards().remove(dc);
                    deckService.save(deck);
                    cardGrid.setItems(deckService.findById(deck.getId()).getCards());
                  });
              return removeBtn;
            })
        .setHeader("Remove");

    // Save/Cancel buttons for editor
    Button saveBtn =
        new Button(
            "Save",
            e -> {
              editor.save();
              dialog.close();
            });
    Button cancelBtn =
        new Button(
            "Cancel",
            e -> {
              editor.cancel();
              dialog.close();
            });

    editor.addSaveListener(
        event -> {
          DeckCard updated = event.getItem();
          deckCardService.save(updated);
          cardGrid.setItems(deckService.findById(deck.getId()).getCards());
        });

    cardGrid.setItems(deckService.findById(deck.getId()).getCards());

    // Add new card section (unchanged)
    ComboBox<Card> cardCombo = new ComboBox<>("Card", cardService.findAll());
    cardCombo.setItemLabelGenerator(Card::getName);
    IntegerField amountInput = new IntegerField("Amount");
    Button addBtn =
        new Button(
            "Add",
            e -> {
              Card selected = cardCombo.getValue();
              Integer amount = amountInput.getValue();
              if (selected != null && amount != null && amount > 0) {
                DeckCard dc = new DeckCard();
                dc.setDeck(deck);
                dc.setCard(selected);
                dc.setSet(selected.getSet());
                dc.setAmount(amount);
                deckCardService.save(dc);
                cardGrid.setItems(deckService.findById(deck.getId()).getCards());
              }
            });

    HorizontalLayout addLayout = new HorizontalLayout(cardCombo, amountInput, addBtn);

    layout.add(cardGrid, addLayout);

    Button closeBtn = new Button("Close", e -> dialog.close());
    HorizontalLayout actions = new HorizontalLayout(saveBtn, cancelBtn, closeBtn);
    layout.add(actions);

    dialog.add(layout);
    dialog.open();
  }

  private void openDeckCardEditor(
      DeckCard deckCard, Grid<DeckCard> cardGrid, Deck deck, Dialog parentDialog) {
    Dialog editDialog = new Dialog();
    editDialog.setHeaderTitle("Edit Card Amount");

    VerticalLayout layout = new VerticalLayout();
    layout.add("Card: " + deckCard.getCard().getName());

    IntegerField amountField = new IntegerField("Amount");
    amountField.setValue(deckCard.getAmount());
    layout.add(amountField);

    Button saveBtn =
        new Button(
            "Save",
            e -> {
              Integer newAmount = amountField.getValue();
              if (newAmount != null && newAmount > 0) {
                deckCard.setAmount(newAmount);
                deckCardService.save(deckCard);
                cardGrid.setItems(deckService.findById(deck.getId()).getCards());
                editDialog.close();
              }
            });
    Button cancelBtn = new Button("Cancel", e -> editDialog.close());
    layout.add(new HorizontalLayout(saveBtn, cancelBtn));

    editDialog.add(layout);
    editDialog.open();
  }

  private void openDeckView(Deck deck) {
    Dialog viewDialog = new Dialog();
    viewDialog.setWidth("1000px");
    viewDialog.setHeaderTitle("Deck Details");

    VerticalLayout layout = new VerticalLayout();
    layout.add("Wrestler: " + deck.getWrestler().getName());

    Grid<DeckCard> cardGrid = new Grid<>(DeckCard.class, false);
    cardGrid.addColumn(dc -> dc.getCard().getName()).setHeader("Card").setSortable(true);
    cardGrid.addColumn(DeckCard::getAmount).setHeader("Amount").setSortable(true);
    cardGrid.setItems(deck.getCards());

    layout.add(cardGrid);

    Button closeBtn = new Button("Close", e -> viewDialog.close());
    layout.add(closeBtn);

    viewDialog.add(layout);
    viewDialog.open();
  }
}
