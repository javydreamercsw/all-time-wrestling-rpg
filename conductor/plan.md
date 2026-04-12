# Plan: Distribution and Packaging Improvements

## Objective

To significantly improve the onboarding and startup experience for non-technical users by providing automated browser launching, a system tray application, lightweight native images, portable ZIP distributions, and a simplified download portal.

## Background & Motivation

Currently, users must manually run Maven commands or execute a JAR, and then know to open their browser to `http://localhost:8080/atw-rpg`. If they close the terminal window, the application stops. Furthermore, the application relies on users having Java installed (unless they use the heavier `jpackage` installers). This plan introduces several quality-of-life features and alternative distribution formats to solve these issues.

## Scope & Impact

* **Java Code**: Add a new Spring Boot component (`DesktopIntegration`) to handle the system tray and browser auto-launch. This will be guarded by a specific profile (e.g., `desktop`) to avoid crashing in headless CI/CD or server environments.
* **Build Configuration (`pom.xml`)**:
  * Enable GraalVM Native Image support for generating standalone, fast-starting binaries.
  * Add a Maven Assembly plugin configuration to generate a "Zero-Install" ZIP archive with startup scripts.
* **Documentation/Website**: Update the VitePress documentation to serve as a user-friendly download portal.

## Proposed Solution & Implementation Steps

### Phase 1: Auto-launch Browser & System Tray Integration

1. **Create `DesktopIntegration` Component**:
   * Implement an `ApplicationListener<ApplicationReadyEvent>`.
   * Verify `java.awt.Desktop.isDesktopSupported()` and `!java.awt.GraphicsEnvironment.isHeadless()`.
   * Use `Desktop.getDesktop().browse(new URI("http://localhost:8080/atw-rpg"))` to open the game in the default browser.
2. **Add System Tray Icon**:
   * If `SystemTray.isSupported()`, add a tray icon with a simple popup menu ("Open Game", "Exit").
   * Use the existing icon at `src/main/resources/jpackage/mac/icon.icns` (converted to PNG) or `source-icon.png`.
3. **Profile Guarding**:
   * Ensure this logic only runs when a specific property or profile is active (e.g., `@ConditionalOnProperty(name = "atw.desktop.enabled", havingValue = "true")`).
   * Update the `desktop` profile in `pom.xml` to set `java.awt.headless=false` and `atw.desktop.enabled=true`.

### Phase 2: GraalVM Native Image Support

1. **Update `pom.xml`**:
   * Add the standard Spring Boot `native` profile.
   * Include the `native-maven-plugin` configuration.
2. **Document Native Build**:
   * Add instructions to `README.md` on how to build the native image (`mvn -Pnative,production package`).
   * Note requirements (GraalVM JDK 17+).

### Phase 3: Portable "Zero-Install" ZIP

1. **Assembly Descriptor**:
   * Create `src/main/assembly/portable.xml` to package the executable JAR alongside simple scripts.
2. **Startup Scripts**:
   * Create `start-windows.bat` and `start-mac-linux.sh`. These scripts will simply execute `java -jar atw-rpg.jar --atw.desktop.enabled=true`.
3. **Update `pom.xml`**:
   * Add an `assembly` profile that binds `maven-assembly-plugin` to the package phase.

### Phase 4: Simplified Download Portal

1. **Update VitePress Site**:
   * Modify `docs/site/index.md` (or create a dedicated `download.md`) to feature prominent download buttons for Windows, Mac, and Linux.
   * Explain the different formats (Installer, Portable ZIP, Native Image).

## Verification & Testing

* **Manual Testing**: Run the application with the `desktop` profile to ensure the browser opens and the tray icon appears and functions correctly.
* **Build Verification**: Execute `mvn clean package -Pproduction,desktop` and `mvn clean package -Pnative,production` to ensure the new build profiles do not break existing configurations.
* **Headless Testing**: Ensure the standard `mvn spring-boot:run` does not crash due to `java.awt.HeadlessException`.

## Alternatives Considered

* **Electron/Tauri Wrapper**: Wrapping the web app in Electron or Tauri was considered. However, this adds significant overhead and complexity since the backend relies heavily on Spring Boot and Java. The `jpackage` installers combined with the System Tray integration provide a similar "desktop app" feel without the Electron overhead.

