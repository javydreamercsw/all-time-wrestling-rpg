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
package com.github.javydreamercsw.base.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UpdateCheckServiceTest {

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherMinor() {
    assertThat(UpdateCheckService.isNewer("2.6.0", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherPatch() {
    assertThat(UpdateCheckService.isNewer("2.5.3", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherMajor() {
    assertThat(UpdateCheckService.isNewer("3.0.0", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsFalseForSameVersion() {
    assertThat(UpdateCheckService.isNewer("2.5.2", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_returnsFalseWhenCandidateIsOlder() {
    assertThat(UpdateCheckService.isNewer("2.4.0", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_handlesPreReleaseSuffix() {
    // Pre-release suffix stripped; numeric part compared only
    assertThat(UpdateCheckService.isNewer("2.6.0-RC1", "2.5.2")).isTrue();
    assertThat(UpdateCheckService.isNewer("2.5.2-RC1", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_returnsFalseForUnparseableVersions() {
    assertThat(UpdateCheckService.isNewer("not-a-version", "2.5.2")).isFalse();
    assertThat(UpdateCheckService.isNewer("2.6.0", "not-a-version")).isFalse();
  }
}
