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
package com.github.javydreamercsw;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  static final String BASE_PACKAGE = "com.github.javydreamercsw";
  private final JavaClasses importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

  @Test
  void domain_model_should_not_depend_on_application_services() {
    noClasses()
        .that()
        .resideInAPackage(BASE_PACKAGE + "..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE_PACKAGE + "..service..")
        .check(importedClasses);
  }

  @Test
  void domain_model_should_not_depend_on_the_user_interface() {
    noClasses()
        .that()
        .resideInAPackage(BASE_PACKAGE + "..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(BASE_PACKAGE + "..ui..")
        .check(importedClasses);
  }

  @Test
  void application_services_should_not_depend_on_the_user_interface() {
    noClasses()
        .that()
        .resideInAPackage(BASE_PACKAGE + "..service..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(BASE_PACKAGE + "..ui..")
        .check(importedClasses);
  }

  @Test
  @Disabled
  void there_should_not_be_circular_dependencies_between_feature_packages() {
    slices().matching(BASE_PACKAGE + ".(*)..").should().beFreeOfCycles().check(importedClasses);
  }
}
