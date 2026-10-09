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
package com.github.javydreamercsw.management.ui.component;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Karibu tests for the shared briefcase cash-in dialog (ATW-8p72, ATW-3fhh). */
class BriefcaseCashInDialogTest extends AbstractViewTest {

  private TitleOpportunityService titleOpportunityService;
  private TitleService titleService;
  private ShowService showService;
  private TitleOpportunity held;
  private Title worldTitle;
  private Title womensTitle;
  private Title retiredTitle;
  private Show upcomingShow;

  @BeforeEach
  void setup() {
    titleOpportunityService = mock(TitleOpportunityService.class);
    titleService = mock(TitleService.class);
    showService = mock(ShowService.class);

    Wrestler holder = new Wrestler();
    holder.setId(8L);
    holder.setName("Mukundi Shumba");
    held = new TitleOpportunity();
    held.setId(10L);
    held.setName("Time Vault briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(holder);
    held.setEarnedAt(LocalDate.now().minusDays(30));

    worldTitle = title(2L, "ATW World", Gender.MALE, true);
    Wrestler champion = new Wrestler();
    champion.setId(7L);
    champion.setName("Champion");
    worldTitle.getCurrentChampions().add(champion);

    womensTitle = title(3L, "ATW Women's", Gender.FEMALE, true);
    womensTitle.getCurrentChampions().add(champion);

    retiredTitle = title(4L, "Retired Belt", null, false);
    retiredTitle.getCurrentChampions().add(champion);

    when(titleService.findAll()).thenReturn(List.of(worldTitle, womensTitle, retiredTitle));

    upcomingShow = new Show();
    upcomingShow.setId(5L);
    upcomingShow.setName("Saturday Slam");
    upcomingShow.setShowDate(LocalDate.now().plusDays(7));
    when(showService.getUpcomingShows(50)).thenReturn(List.of(upcomingShow));
  }

  private static Title title(Long id, String name, Gender gender, boolean active) {
    Title t = new Title();
    t.setId(id);
    t.setName(name);
    t.setGender(gender);
    t.setIsActive(active);
    return t;
  }

  private BriefcaseCashInDialog buildDialog() {
    BriefcaseCashInDialog dialog =
        new BriefcaseCashInDialog(
            held, titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(dialog);
    return dialog;
  }

  @Test
  @DisplayName("Dialog opens with the case name in the header and the cashable filter applied")
  void dialog_opens_withCashableTitlesOnly() {
    BriefcaseCashInDialog dialog = buildDialog();

    assertThat(dialog.getHeaderTitle()).isEqualTo("Cash In: Time Vault briefcase");

    @SuppressWarnings("unchecked")
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-title-combo"));
    // Retired belt filtered out; both active titles with champions offered.
    assertThat(titleCombo.getListDataView().getItemCount()).isEqualTo(2);

    @SuppressWarnings("unchecked")
    ComboBox<Show> showCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-show-combo"));
    assertThat(showCombo.getListDataView().getItemCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("A gendered briefcase only offers its division and ungendered titles")
  void genderedBriefcase_filtersOppositeDivision() {
    held.setGender(Gender.MALE);
    BriefcaseCashInDialog dialog = buildDialog();

    @SuppressWarnings("unchecked")
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-title-combo"));
    assertThat(titleCombo.getListDataView().getItemCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Confirm is disabled until both championship and show are selected")
  void confirm_disabledUntilBothSelected() {
    BriefcaseCashInDialog dialog = buildDialog();
    Button confirm = _get(dialog, Button.class, spec -> spec.withId("cash-in-confirm"));
    assertThat(confirm.isEnabled()).isFalse();

    @SuppressWarnings("unchecked")
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-title-combo"));
    titleCombo.setValue(worldTitle);
    assertThat(confirm.isEnabled()).isFalse();

    @SuppressWarnings("unchecked")
    ComboBox<Show> showCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-show-combo"));
    showCombo.setValue(upcomingShow);
    assertThat(confirm.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("Confirm books the cash-in through the service, closes and runs onBooked")
  void confirm_booksCashIn_andRunsCallback() {
    boolean[] refreshed = {false};
    BriefcaseCashInDialog dialog =
        new BriefcaseCashInDialog(
            held, titleOpportunityService, titleService, showService, () -> refreshed[0] = true);
    UI.getCurrent().add(dialog);

    @SuppressWarnings("unchecked")
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-title-combo"));
    @SuppressWarnings("unchecked")
    ComboBox<Show> showCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-show-combo"));
    titleCombo.setValue(worldTitle);
    showCombo.setValue(upcomingShow);

    Button confirm = _get(dialog, Button.class, spec -> spec.withId("cash-in-confirm"));
    confirm.click();

    verify(titleOpportunityService).cashIn(10L, 2L, 5L);
    assertThat(refreshed[0]).isTrue();
  }

  @Test
  @DisplayName("A service validation error surfaces as a notification and skips the callback")
  void confirm_serviceError_showsNotification() {
    when(titleOpportunityService.cashIn(anyLong(), anyLong(), anyLong()))
        .thenThrow(new IllegalStateException("This briefcase is not cashable"));
    boolean[] refreshed = {true};
    BriefcaseCashInDialog dialog =
        new BriefcaseCashInDialog(
            held, titleOpportunityService, titleService, showService, () -> refreshed[0] = false);
    UI.getCurrent().add(dialog);

    @SuppressWarnings("unchecked")
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-title-combo"));
    @SuppressWarnings("unchecked")
    ComboBox<Show> showCombo =
        _get(dialog, ComboBox.class, spec -> spec.withId("cash-in-show-combo"));
    titleCombo.setValue(worldTitle);
    showCombo.setValue(upcomingShow);

    Button confirm = _get(dialog, Button.class, spec -> spec.withId("cash-in-confirm"));
    confirm.click();

    verify(titleOpportunityService).cashIn(10L, 2L, 5L);
    // The service threw, so the success callback never runs and the dialog stays usable.
    assertThat(refreshed[0]).isTrue();
  }

  @Test
  @DisplayName("Cancel closes the dialog without calling the service")
  void cancel_closesWithoutBooking() {
    BriefcaseCashInDialog dialog = buildDialog();
    dialog.open();

    Button cancel = _get(dialog, Button.class, spec -> spec.withText("Cancel"));
    cancel.click();

    assertThat(dialog.isOpened()).isFalse();
    verify(titleOpportunityService, never()).cashIn(anyLong(), anyLong(), anyLong());
  }
}
