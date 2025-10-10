package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Integration test for FactionListView that tests real database interactions. This test would catch
 * LazyInitializationException that unit tests with mocks cannot detect.
 */
@DisplayName("Faction ListView Integration Tests")
@EnabledIf("isNotionTokenAvailable")
class FactionListViewIntegrationTest extends AbstractIntegrationTest {}
