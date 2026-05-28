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
package com.github.javydreamercsw.management.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Marks a test or class as requiring a live Notion connection.
 *
 * <p>Tests annotated with {@code @NotionLiveTest} are skipped by default. To run them, set the
 * system property {@code -Dnotion.live.tests.enabled=true}.
 *
 * <p>Combine with {@link com.github.javydreamercsw.management.extension.NotionTestCleanupExtension}
 * to ensure any pages created during the test are archived afterwards.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("notion-live")
@EnabledIfSystemProperty(
    named = "notion.live.tests.enabled",
    matches = "true",
    disabledReason =
        "Live Notion tests disabled by default. Run with -Dnotion.live.tests.enabled=true to"
            + " enable.")
public @interface NotionLiveTest {}
