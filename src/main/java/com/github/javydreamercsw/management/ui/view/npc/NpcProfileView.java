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
package com.github.javydreamercsw.management.ui.view.npc;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Route("npc-profile/:npcId?")
@PageTitle("NPC Profile")
@PermitAll
public class NpcProfileView extends Main implements BeforeEnterObserver {

  private final NpcService npcService;
  private final NpcRepository npcRepository;
  private final ImageGenerationServiceFactory imageGenerationServiceFactory;
  private final ImageStorageService imageStorageService;
  private final AiSettingsService aiSettingsService;
  private final SecurityUtils securityUtils;

  private Npc npc;

  private final H2 npcName = new H2();
  private final Paragraph npcDetails = new Paragraph();
  private final VerticalLayout biographyLayout = new VerticalLayout();
  private final Image npcImage = new Image();
  private final Button generateImageButton = new Button("Generate Image");
  private final ImageUploadComponent uploadComponent;

  @Autowired
  public NpcProfileView(
      NpcService npcService,
      NpcRepository npcRepository,
      ImageGenerationServiceFactory imageGenerationServiceFactory,
      ImageStorageService imageStorageService,
      AiSettingsService aiSettingsService,
      SecurityUtils securityUtils) {
    this.npcService = npcService;
    this.npcRepository = npcRepository;
    this.imageGenerationServiceFactory = imageGenerationServiceFactory;
    this.imageStorageService = imageStorageService;
    this.aiSettingsService = aiSettingsService;
    this.securityUtils = securityUtils;

    npcName.setId("npc-name");
    npcImage.setSrc("https://via.placeholder.com/150");
    npcImage.setAlt("NPC Image");
    npcImage.setId("npc-image");
    npcImage.setMaxWidth("300px");

    generateImageButton.setId("generate-image-button");
    generateImageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateImageButton.addClickListener(
        e -> {
          new NpcImageGenerationDialog(
                  npc,
                  npcService,
                  imageGenerationServiceFactory,
                  imageStorageService,
                  aiSettingsService,
                  this::updateView)
              .open();
        });

    uploadComponent =
        new ImageUploadComponent(
            imageStorageService,
            url -> {
              npc.setImageUrl(url);
              npcService.save(npc);
              updateView();
            });
    uploadComponent.setUploadButtonText("Upload Image");

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(new ViewToolbar("NPC Profile", new RouterLink("Back to List", NpcListView.class)));

    VerticalLayout nameAndDetailsLayout = new VerticalLayout();
    nameAndDetailsLayout.add(npcName, npcDetails);

    VerticalLayout imageLayout = new VerticalLayout(npcImage, generateImageButton, uploadComponent);
    imageLayout.setAlignItems(Alignment.CENTER);
    imageLayout.setPadding(false);
    imageLayout.setSpacing(true);

    HorizontalLayout header = new HorizontalLayout(imageLayout, nameAndDetailsLayout);
    header.setAlignItems(Alignment.CENTER);

    add(header, biographyLayout);
  }

  @Override
  @Transactional
  public void beforeEnter(BeforeEnterEvent event) {
    RouteParameters parameters = event.getRouteParameters();
    if (parameters.get("npcId").isPresent()) {
      Long npcId = Long.valueOf(parameters.get("npcId").get());
      Optional<Npc> foundNpc = npcRepository.findById(npcId);
      if (foundNpc.isPresent()) {
        npc = foundNpc.get();
        updateView();
      } else {
        event.rerouteTo(NpcListView.class);
      }
    } else {
      event.rerouteTo(NpcListView.class);
    }
  }

  private void updateView() {
    if (npc != null && npc.getId() != null) {
      npcName.setText(npc.getName());
      npcDetails.setText(String.format("Type: %s", npc.getNpcType()));

      if (npc.getImageUrl() != null && !npc.getImageUrl().isEmpty()) {
        npcImage.setSrc(npc.getImageUrl());
      } else {
        npcImage.setSrc("https://via.placeholder.com/150");
      }

      biographyLayout.removeAll();
      biographyLayout.add(new H3("Biography"));
      if (npc.getDescription() != null && !npc.getDescription().isEmpty()) {
        biographyLayout.add(new Paragraph(npc.getDescription()));
      } else {
        biographyLayout.add(new Paragraph("No biography available."));
      }

      generateImageButton.setVisible(securityUtils.canEdit());
      uploadComponent.setVisible(securityUtils.canEdit());
    }
  }
}
