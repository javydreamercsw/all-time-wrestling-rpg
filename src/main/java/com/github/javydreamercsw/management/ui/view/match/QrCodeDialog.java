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
package com.github.javydreamercsw.management.ui.view.match;

import com.github.javydreamercsw.management.util.QrCodeUtil;
import com.github.javydreamercsw.management.util.UrlUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QrCodeDialog extends Dialog {

  public QrCodeDialog(Long matchId) {
    setHeaderTitle("Share Match");

    String matchUrl = UrlUtil.getBaseUrl() + "/match/" + matchId;

    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(true);
    layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
    layout.setWidth("300px");

    try {
      String base64 = QrCodeUtil.toBase64Png(matchUrl, 256);
      Image qrImage = new Image("data:image/png;base64," + base64, "QR code for match");
      qrImage.setWidth("256px");
      qrImage.setHeight("256px");
      layout.add(qrImage);
    } catch (Exception e) {
      log.error("Failed to generate QR code for match {}", matchId, e);
      Notification.show("Could not generate QR code.")
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    Span urlLabel = new Span(matchUrl);
    urlLabel.getStyle().set("word-break", "break-all").set("font-size", "var(--lumo-font-size-s)");
    layout.add(urlLabel);

    add(layout);
    getFooter().add(new Button("Close", e -> close()));
  }
}
