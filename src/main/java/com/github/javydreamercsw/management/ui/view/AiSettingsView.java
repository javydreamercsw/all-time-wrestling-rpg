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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.config.LocalAIContainerConfig;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@PageTitle("AI Settings")
@Component
@RolesAllowed("ADMIN")
@Lazy
@UIScope
public class AiSettingsView extends VerticalLayout {

  private final AiSettingsService aiSettingsService;
  private final GameSettingService gameSettingService;
  private final LocalAIContainerConfig localAIContainerConfig;
  private final LocalAIStatusService localAIStatusService;

  private Checkbox aiProviderAuto;
  private NumberField aiTimeout;

  // OpenAI fields
  private Checkbox openAIEnabled;
  private TextField openAIApiUrl;
  private PasswordField openAIApiKey;
  private TextField openAIDefaultModel;
  private TextField openAIPremiumModel;
  private TextField openAIImageModel;
  private NumberField openAIMaxTokens;
  private NumberField openAITemperature;

  // Claude fields
  private Checkbox claudeEnabled;
  private TextField claudeApiUrl;
  private PasswordField claudeApiKey;
  private TextField claudeModelName;

  // Gemini fields
  private Checkbox geminiEnabled;
  private TextField geminiApiUrl;
  private PasswordField geminiApiKey;
  private TextField geminiModelName;

  // LocalAI fields
  private Checkbox localAIEnabled;
  private TextField localAIBaseUrl;
  private ComboBox<String> localAIModel;
  private TextField localAIImageModel;
  private TextField localAIModelUrl;
  private Span localAIStatusLabel;
  private Button openLocalAiUiBtn;

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    // Poll every 5 seconds to update status
    attachEvent.getUI().setPollInterval(5000);
    attachEvent.getUI().addPollListener(event -> updateLocalAIStatus());
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    if (detachEvent.getUI() != null) {
      detachEvent.getUI().setPollInterval(-1);
    }
  }

  @Autowired
  public AiSettingsView(
      AiSettingsService aiSettingsService,
      GameSettingService gameSettingService,
      LocalAIContainerConfig localAIContainerConfig,
      LocalAIStatusService localAIStatusService) {
    this.aiSettingsService = aiSettingsService;
    this.gameSettingService = gameSettingService;
    this.localAIContainerConfig = localAIContainerConfig;
    this.localAIStatusService = localAIStatusService;
    init();
  }

  private void init() {
    add(new H3("Common AI Settings"));
    FormLayout commonSettingsLayout = new FormLayout();
    aiProviderAuto = new Checkbox("Auto Select Provider", aiSettingsService.isAiProviderAuto());
    aiProviderAuto.addValueChangeListener(
        event -> saveSetting("AI_PROVIDER_AUTO", String.valueOf(event.getValue())));
    aiTimeout = new NumberField("Timeout (seconds)");
    aiTimeout.setValue((double) aiSettingsService.getAiTimeout());
    aiTimeout.addValueChangeListener(
        event -> saveSetting("AI_TIMEOUT", String.valueOf(event.getValue().intValue())));
    commonSettingsLayout.add(aiProviderAuto, aiTimeout);
    add(commonSettingsLayout);

    add(new H3("OpenAI Settings"));
    FormLayout openAISettingsLayout = new FormLayout();
    openAIEnabled = new Checkbox("Enabled", aiSettingsService.isOpenAIEnabled());
    openAIEnabled.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_ENABLED", String.valueOf(event.getValue())));
    openAIApiUrl = new TextField("API URL", aiSettingsService.getOpenAIApiUrl(), "");
    openAIApiUrl.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_API_URL", event.getValue()));
    openAIApiKey =
        new PasswordField(
            "API KEY",
            aiSettingsService.getOpenAIApiKey(),
            event -> saveSetting("AI_OPENAI_API_KEY", event.getValue()));
    openAIDefaultModel =
        new TextField("Default Model", aiSettingsService.getOpenAIDefaultModel(), "");
    openAIDefaultModel.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_DEFAULT_MODEL", event.getValue()));
    openAIPremiumModel =
        new TextField("Premium Model", aiSettingsService.getOpenAIPremiumModel(), "");
    openAIPremiumModel.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_PREMIUM_MODEL", event.getValue()));
    openAIImageModel = new TextField("Image Model", aiSettingsService.getOpenAIImageModel(), "");
    openAIImageModel.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_IMAGE_MODEL", event.getValue()));
    openAIMaxTokens = new NumberField("Max Tokens");
    openAIMaxTokens.setValue((double) aiSettingsService.getOpenAIMaxTokens());
    openAIMaxTokens.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_MAX_TOKENS", String.valueOf(event.getValue().intValue())));
    openAITemperature = new NumberField("Temperature");
    openAITemperature.setValue((double) aiSettingsService.getOpenAITemperature());
    openAITemperature.addValueChangeListener(
        event -> saveSetting("AI_OPENAI_TEMPERATURE", String.valueOf(event.getValue())));
    openAISettingsLayout.add(
        openAIEnabled,
        openAIApiUrl,
        openAIApiKey,
        openAIDefaultModel,
        openAIPremiumModel,
        openAIImageModel,
        openAIMaxTokens,
        openAITemperature);
    add(openAISettingsLayout);

    add(new H3("Claude Settings"));
    FormLayout claudeSettingsLayout = new FormLayout();
    claudeEnabled = new Checkbox("Enabled", aiSettingsService.isClaudeEnabled());
    claudeEnabled.addValueChangeListener(
        event -> saveSetting("AI_CLAUDE_ENABLED", String.valueOf(event.getValue())));
    claudeApiUrl = new TextField("API URL", aiSettingsService.getClaudeApiUrl(), "");
    claudeApiUrl.addValueChangeListener(
        event -> saveSetting("AI_CLAUDE_API_URL", event.getValue()));
    claudeApiKey =
        new PasswordField(
            "API KEY",
            aiSettingsService.getClaudeApiKey(),
            event -> saveSetting("AI_CLAUDE_API_KEY", event.getValue()));
    claudeModelName = new TextField("Model Name", aiSettingsService.getClaudeModelName(), "");
    claudeModelName.addValueChangeListener(
        event -> saveSetting("AI_CLAUDE_MODEL_NAME", event.getValue()));
    claudeSettingsLayout.add(claudeEnabled, claudeApiUrl, claudeApiKey, claudeModelName);
    add(claudeSettingsLayout);

    add(new H3("Gemini Settings"));
    FormLayout geminiSettingsLayout = new FormLayout();
    geminiEnabled = new Checkbox("Enabled", aiSettingsService.isGeminiEnabled());
    geminiEnabled.addValueChangeListener(
        event -> saveSetting("AI_GEMINI_ENABLED", String.valueOf(event.getValue())));
    geminiApiUrl = new TextField("API URL", aiSettingsService.getGeminiApiUrl(), "");
    geminiApiUrl.addValueChangeListener(
        event -> saveSetting("AI_GEMINI_API_URL", event.getValue()));
    geminiApiKey =
        new PasswordField(
            "API KEY",
            aiSettingsService.getGeminiApiKey(),
            event -> saveSetting("AI_GEMINI_API_KEY", event.getValue()));
    geminiModelName = new TextField("Model Name", aiSettingsService.getGeminiModelName(), "");
    geminiModelName.addValueChangeListener(
        event -> saveSetting("AI_GEMINI_MODEL_NAME", event.getValue()));
    geminiSettingsLayout.add(geminiEnabled, geminiApiUrl, geminiApiKey, geminiModelName);
    add(geminiSettingsLayout);

    add(new H3("LocalAI Settings"));
    localAIStatusLabel = new Span();
    updateLocalAIStatus();
    add(localAIStatusLabel);

    FormLayout localAISettingsLayout = new FormLayout();
    localAIEnabled = new Checkbox("Enabled", aiSettingsService.isLocalAIEnabled());
    localAIEnabled.addValueChangeListener(
        event -> {
          saveSetting("AI_LOCALAI_ENABLED", String.valueOf(event.getValue()));
          if (event.getValue()) {
            localAIContainerConfig.startLocalAiContainer();
            Notification.show("LocalAI enabled. Starting container if necessary...")
                .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
          } else {
            localAIContainerConfig.stopLocalAiContainer();
            Notification.show("LocalAI disabled and container stopped.")
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
          }
          updateLocalAIStatus();
        });
    localAIBaseUrl = new TextField("Base URL", aiSettingsService.getLocalAIBaseUrl(), "");
    localAIBaseUrl.addValueChangeListener(
        event -> saveSetting("AI_LOCALAI_BASE_URL", event.getValue()));

    localAIModel = new ComboBox<>("Model");
    localAIModel.setItems(
        "llama-3.2-1b-instruct:q4_k_m", "gpt-4", "gpt-oss-120b", "mistral", "phi-2");
    localAIModel.setAllowCustomValue(true);
    localAIModel.addCustomValueSetListener(
        e -> {
          localAIModel.setValue(e.getDetail());
        });
    localAIModel.setValue(aiSettingsService.getLocalAIModel());
    localAIModel.addValueChangeListener(event -> saveSetting("AI_LOCALAI_MODEL", event.getValue()));

    localAIImageModel = new TextField("Image Model", aiSettingsService.getLocalAIImageModel(), "");
    localAIImageModel.addValueChangeListener(
        event -> saveSetting("AI_LOCALAI_IMAGE_MODEL", event.getValue()));
    localAIModelUrl = new TextField("Model URL", aiSettingsService.getLocalAIModelUrl(), "");
    localAIModelUrl.addValueChangeListener(
        event -> saveSetting("AI_LOCALAI_MODEL_URL", event.getValue()));
    localAISettingsLayout.add(
        localAIEnabled, localAIBaseUrl, localAIModel, localAIImageModel, localAIModelUrl);
    add(localAISettingsLayout);

    HorizontalLayout localAiControls = new HorizontalLayout();
    Button startBtn = new Button("Start Container", new Icon(VaadinIcon.PLAY));
    startBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    startBtn.addClickListener(
        e -> {
          // Ensure enabled
          if (!localAIEnabled.getValue()) {
            localAIEnabled.setValue(true);
          }
          // Force start container even if already "enabled" (but maybe stopped)
          localAIContainerConfig.startLocalAiContainer(true);
          Notification.show("Starting LocalAI container...");
          updateLocalAIStatus();
        });

    Button stopBtn = new Button("Stop Container", new Icon(VaadinIcon.STOP));
    stopBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
    stopBtn.addClickListener(
        e -> {
          // Disabling the toggle triggers the listener which stops the container
          localAIEnabled.setValue(false);
          updateLocalAIStatus();
        });

    Button restartBtn = new Button("Restart Container", new Icon(VaadinIcon.REFRESH));
    restartBtn.addClickListener(
        e -> {
          if (!localAIEnabled.getValue()) {
            localAIEnabled.setValue(true);
          }
          localAIContainerConfig.forceRestartLocalAiContainer();
          Notification.show("Restarting LocalAI container...");
          updateLocalAIStatus();
        });

    Button checkHealthBtn = new Button("Check Health", new Icon(VaadinIcon.DOCTOR));
    checkHealthBtn.addClickListener(
        e -> {
          localAIStatusService.checkHealth();
          updateLocalAIStatus();
          Notification.show("LocalAI Status: " + localAIStatusService.getMessage());
        });

    openLocalAiUiBtn = new Button("Open LocalAI UI", new Icon(VaadinIcon.EXTERNAL_LINK));
    openLocalAiUiBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    openLocalAiUiBtn.addClickListener(
        e -> {
          String url = aiSettingsService.getLocalAIBaseUrl();
          UI.getCurrent().getPage().open(url, "_blank");
        });

    localAiControls.add(startBtn, stopBtn, restartBtn, checkHealthBtn, openLocalAiUiBtn);
    add(localAiControls);

    add(new H3("Pollinations Settings"));
    FormLayout pollinationsSettingsLayout = new FormLayout();

    Checkbox pollinationsEnabled =
        new Checkbox("Enabled", aiSettingsService.isPollinationsEnabled());
    pollinationsEnabled.addValueChangeListener(
        event -> saveSetting("AI_POLLINATIONS_ENABLED", String.valueOf(event.getValue())));

    PasswordField pollinationsApiKey =
        new PasswordField(
            "API Key",
            aiSettingsService.getPollinationsApiKey(),
            event -> saveSetting("AI_POLLINATIONS_API_KEY", event.getValue()));

    pollinationsSettingsLayout.add(pollinationsEnabled, pollinationsApiKey);
    add(pollinationsSettingsLayout);
  }

  private void updateLocalAIStatus() {
    localAIStatusLabel.setText("LocalAI Status: " + localAIStatusService.getMessage());
    localAIStatusLabel.getStyle().set("font-weight", "bold");

    // Refresh Base URL field to show dynamic port
    if (localAIBaseUrl != null) {
      localAIBaseUrl.setValue(aiSettingsService.getLocalAIBaseUrl());
    }

    if (openLocalAiUiBtn != null) {
      openLocalAiUiBtn.setEnabled(
          localAIStatusService.getStatus() == LocalAIStatusService.Status.READY);
    }

    switch (localAIStatusService.getStatus()) {
      case READY -> {
        localAIStatusLabel.getElement().getThemeList().clear();
        localAIStatusLabel.addClassNames(LumoUtility.TextColor.SUCCESS);
      }
      case FAILED -> {
        localAIStatusLabel.getElement().getThemeList().clear();
        localAIStatusLabel.addClassNames(LumoUtility.TextColor.ERROR);
      }
      case STARTING, DOWNLOADING_MODEL -> {
        localAIStatusLabel.getElement().getThemeList().clear();
        localAIStatusLabel.addClassNames(LumoUtility.TextColor.PRIMARY);
      }
      default -> {
        localAIStatusLabel.getElement().getThemeList().clear();
        localAIStatusLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
      }
    }
  }

  private void saveSetting(String key, String value) {
    GameSetting setting = gameSettingService.findById(key).orElseGet(GameSetting::new);
    setting.setId(key);
    setting.setValue(value);
    gameSettingService.save(setting);
    Notification.show("Setting '" + key + "' updated to: " + value)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
