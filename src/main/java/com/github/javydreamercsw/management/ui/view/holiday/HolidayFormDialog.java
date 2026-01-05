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
package com.github.javydreamercsw.management.ui.view.holiday;

import com.github.javydreamercsw.management.domain.Holiday;
import com.github.javydreamercsw.management.domain.HolidayType;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import java.time.DayOfWeek;
import java.time.Month;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HolidayFormDialog extends Div {

  private final TextField description = new TextField("Description");
  private final TextField theme = new TextField("Theme");
  private final TextArea decorations = new TextArea("Decorations");
  private final ComboBox<HolidayType> type = new ComboBox<>("Holiday Type");
  private final IntegerField dayOfMonth = new IntegerField("Day of Month");
  private final ComboBox<Month> month = new ComboBox<>("Month");
  private final ComboBox<DayOfWeek> dayOfWeek = new ComboBox<>("Day of Week");
  private final IntegerField weekOfMonth = new IntegerField("Week of Month (1-5, or -1 for last)");

  private final Button save = new Button("Save");
  private final Button cancel = new Button("Cancel");

  private final Binder<Holiday> binder = new Binder<>(Holiday.class);
  private Holiday holiday;

  public HolidayFormDialog() {
    addClassName("holiday-form-dialog");

    type.setItems(HolidayType.values());
    type.setId("type");
    type.setItemLabelGenerator(t -> t.name().toLowerCase());

    month.setItems(Month.values());
    month.setItemLabelGenerator(m -> m.name().toLowerCase());
    month.setId("month");

    dayOfWeek.setItems(DayOfWeek.values());
    dayOfWeek.setItemLabelGenerator(d -> d.name().toLowerCase());
    dayOfWeek.setId("day-of-week");

    dayOfMonth.setId("day-of-month");
    theme.setId("holiday-theme");
    description.setId("description");
    decorations.setId("decorations");
    weekOfMonth.setId("week-of-month");
    save.setId("holiday-save");

    binder.bindInstanceFields(this);

    type.addValueChangeListener(event -> configureFields(event.getValue()));

    add(createFormLayout(), createButtonsLayout());
  }

  private FormLayout createFormLayout() {
    FormLayout formLayout = new FormLayout();
    formLayout.add(
        description, theme, decorations, type, dayOfMonth, month, dayOfWeek, weekOfMonth);
    return formLayout;
  }

  private HorizontalLayout createButtonsLayout() {
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    save.addClickListener(event -> validateAndSave());
    cancel.addClickListener(event -> fireEvent(new CloseEvent(this)));

    binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

    return new HorizontalLayout(save, cancel);
  }

  public void setHoliday(Holiday holiday) {
    this.holiday = holiday;
    binder.readBean(holiday);
    configureFields(holiday.getType());
  }

  private void configureFields(HolidayType holidayType) {
    boolean isFixed = holidayType == HolidayType.FIXED;
    dayOfMonth.setVisible(isFixed);
    month.setVisible(true); // Month is always visible
    dayOfWeek.setVisible(!isFixed);
    weekOfMonth.setVisible(!isFixed);

    if (isFixed) {
      dayOfMonth.setRequiredIndicatorVisible(true);
      month.setRequiredIndicatorVisible(true);
      dayOfWeek.setRequiredIndicatorVisible(false);
      weekOfMonth.setRequiredIndicatorVisible(false);
    } else {
      dayOfMonth.setRequiredIndicatorVisible(false);
      month.setRequiredIndicatorVisible(true);
      dayOfWeek.setRequiredIndicatorVisible(true);
      weekOfMonth.setRequiredIndicatorVisible(true);
    }
  }

  private void validateAndSave() {
    try {
      binder.writeBean(holiday);
      fireEvent(new SaveEvent(this, holiday));
    } catch (ValidationException e) {
      log.error("Validation error during holiday save", e);
    }
  }

  // Events
  @Getter
  public abstract static class HolidayFormEvent extends ComponentEvent<HolidayFormDialog> {
    private final Holiday holiday;

    protected HolidayFormEvent(HolidayFormDialog source, Holiday holiday) {
      super(source, false);
      this.holiday = holiday;
    }
  }

  public static class SaveEvent extends HolidayFormEvent {
    SaveEvent(HolidayFormDialog source, Holiday holiday) {
      super(source, holiday);
    }
  }

  public static class CloseEvent extends HolidayFormEvent {
    CloseEvent(HolidayFormDialog source) {
      super(source, null);
    }
  }

  public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
    return addListener(SaveEvent.class, listener);
  }

  public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
    return addListener(CloseEvent.class, listener);
  }
}
