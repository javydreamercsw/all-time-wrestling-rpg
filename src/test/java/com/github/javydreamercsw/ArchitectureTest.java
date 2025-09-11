package com.github.javydreamercsw;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  static final String BASE_PACKAGE = "com.github.javydreamercsw";

  private final JavaClasses importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

  // TODO Add your own rules and remove those that don't apply to your project

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
  void there_should_not_be_circular_dependencies_between_feature_packages() {
    slices().matching(BASE_PACKAGE + ".(*)..").should().beFreeOfCycles().check(importedClasses);
  }
}
