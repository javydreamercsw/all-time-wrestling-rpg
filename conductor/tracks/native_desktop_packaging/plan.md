# Implementation Plan - Native Desktop Packaging

## Phase 1: POM Refactoring

- [x] Create branch `feature/native-desktop-packaging`.
- [x] Modify `pom.xml`:
  - [x] Change `<packaging>` to `jar`.
  - [x] Move `spring-boot-starter-tomcat` to default `compile` scope (remove `provided`).
  - [x] Add a `war` profile that sets packaging to `war` and marks Tomcat as `provided`.
  - [x] Add a `desktop` profile with `jpackage-maven-plugin` configuration.
- [x] Verify local build:
  - [x] `mvn clean package` (Verify JAR works).
  - [x] `mvn clean package -Pwar` (Verify WAR works).

## Phase 2: Workflow Updates

- [x] Update `.github/workflows/maven.yml`:
  - [x] Ensure CI runs for the new JAR-based build.
- [x] Update `.github/workflows/release.yml`:
  - [x] Adjust artifact naming/paths if necessary.
  - [x] Ensure both JAR and WAR (if needed) are considered for release.
  - [x] Add matrix job for Native Installers (Linux/macOS).

## Phase 3: Documentation

- [x] Update `README.md` with instructions for the new packaging options.
- [x] Update `STARTUP_GUIDE.md`.

## Phase 4: Verification

- [x] Run local verification of executable JAR.
- [x] Run local verification of Native Installer (macOS DMG).
- [ ] Run E2E tests against the executable JAR (Optional but recommended).
- [ ] Verify WAR deployment still works (Local verification done, CI will also verify).

