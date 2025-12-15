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
package com.github.javydreamercsw.base.ui.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

/** Login view for the application. */
@Route("login")
@PageTitle("Login | All Time Wrestling RPG")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

  private final LoginForm loginForm = new LoginForm();

  public LoginView() {
    addClassName("login-view");
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    // Create login form with custom i18n
    LoginI18n i18n = LoginI18n.createDefault();
    i18n.setHeader(new LoginI18n.Header());
    i18n.getHeader().setTitle("All Time Wrestling RPG");
    i18n.getHeader().setDescription("Wrestling Promotion Management System");

    LoginI18n.Form i18nForm = i18n.getForm();
    i18nForm.setTitle("");
    i18nForm.setUsername("Username");
    i18nForm.setPassword("Password");
    i18nForm.setSubmit("Sign In");
    i18nForm.setForgotPassword("Forgot password?");

    LoginI18n.ErrorMessage i18nError = i18n.getErrorMessage();
    i18nError.setTitle("Login Failed");
    i18nError.setMessage(
        "Invalid username or password. After 5 failed attempts, your account will be locked for 15"
            + " minutes.");
    i18nError.setUsername("Username is required");
    i18nError.setPassword("Password is required");

    i18n.setAdditionalInformation(
        "Default accounts: admin/admin123, booker/booker123, player/player123, viewer/viewer123");

    loginForm.setI18n(i18n);
    loginForm.setAction("login");
    loginForm.setForgotPasswordButtonVisible(
        false); // TODO: Enable when forgot password is implemented

    // Create header with logo placeholder
    Div header = createHeader();

    // Add remember me note
    Paragraph rememberMeNote = new Paragraph("Use 'Remember me' to stay signed in for 7 days");
    rememberMeNote.addClassNames(
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.FontSize.SMALL,
        LumoUtility.TextAlignment.CENTER);

    add(header, loginForm, rememberMeNote);
  }

  private Div createHeader() {
    Div header = new Div();
    header.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.AlignItems.CENTER,
        LumoUtility.Gap.MEDIUM,
        LumoUtility.Margin.Bottom.LARGE);

    // Placeholder logo - will be replaced later
    // For now, using a styled div with text
    Div logoPlaceholder = new Div();
    logoPlaceholder.setText("🤼");
    logoPlaceholder.addClassNames(
        LumoUtility.FontSize.XXXLARGE,
        LumoUtility.Padding.LARGE,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.LARGE);
    logoPlaceholder.getStyle().set("font-size", "4rem");

    H1 appName = new H1("All Time Wrestling RPG");
    appName.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XXLARGE);

    Paragraph tagline = new Paragraph("Manage your wrestling promotion");
    tagline.addClassNames(
        LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

    header.add(logoPlaceholder, appName, tagline);
    return header;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // Show error message if login failed
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      loginForm.setError(true);
    }
  }
}
