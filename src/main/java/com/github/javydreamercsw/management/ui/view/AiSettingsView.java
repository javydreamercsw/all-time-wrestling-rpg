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

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.config.LocalAIContainerConfig;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
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
  private TextField localAIModel;
  private TextField localAIImageModel;
  private TextField localAIModelUrl;

  @Autowired
  public AiSettingsView(
      AiSettingsService aiSettingsService,
      GameSettingService gameSettingService,
      LocalAIContainerConfig localAIContainerConfig) {
    this.aiSettingsService = aiSettingsService;
    this.gameSettingService = gameSettingService;
    this.localAIContainerConfig = localAIContainerConfig;
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
    openAIImageModel =
        new TextField("Image Model", aiSettingsService.getOpenAIImageModel(), "");
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
    FormLayout localAISettingsLayout = new FormLayout();
    localAIEnabled = new Checkbox("Enabled", aiSettingsService.isLocalAIEnabled());
    localAIEnabled.addValueChangeListener(
        event -> {
          saveSetting("AI_LOCALAI_ENABLED", String.valueOf(event.getValue()));
          if (event.getValue()) {
            localAIContainerConfig.startLocalAiContainer();
          } else {
            localAIContainerConfig.stopLocalAiContainer();
          }
        });
    localAIBaseUrl = new TextField("Base URL", aiSettingsService.getLocalAIBaseUrl(), "");
    localAIBaseUrl.addValueChangeListener(
        event -> saveSetting("AI_LOCALAI_BASE_URL", event.getValue()));
    localAIModel = new TextField("Model", aiSettingsService.getLocalAIModel(), "");
    localAIModel.addValueChangeListener(event -> saveSetting("AI_LOCALAI_MODEL", event.getValue()));
    localAIImageModel = new TextField("Image Model", aiSettingsService.getLocalAIImageModel(), "");
    localAIImageModel.addValueChangeListener(event -> saveSetting("AI_LOCALAI_IMAGE_MODEL", event.getValue()));
    localAIModelUrl = new TextField("Model URL", aiSettingsService.getLocalAIModelUrl(), "");
    localAIModelUrl.addValueChangeListener(
        event -> saveSetting("AI_LOCALAI_MODEL_URL", event.getValue()));
    localAISettingsLayout.add(localAIEnabled, localAIBaseUrl, localAIModel, localAIImageModel, localAIModelUrl);
    add(localAISettingsLayout);
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
