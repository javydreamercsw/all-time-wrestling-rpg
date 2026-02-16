# Specification - Native Desktop Packaging with JPackage

## Goal

The goal is to make the application more accessible to non-technical end-users by providing native installers (MSI, DMG, DEB/RPM) instead of requiring Docker or manual Tomcat installation.

## Requirements

1. **JAR Packaging by Default**: The application should produce an executable JAR by default.
2. **WAR Support**: A `war` profile should be available to generate a WAR file for traditional server deployments.
3. **Embedded Server**: The executable JAR must include the embedded Tomcat server (current `provided` scope for `spring-boot-starter-tomcat` must be handled).
4. **JPackage Integration**: A `desktop` profile should be added to generate platform-specific installers.
5. **CI/CD Updates**: GitHub workflows must be updated to:
   - Build and test both JAR and WAR where appropriate.
   - Include the correct artifact in the release process.
   - Potentially generate installers during the release workflow (though cross-platform jpackage requires multiple runners).

## Success Criteria

- `mvn package` produces an executable JAR.
- `mvn package -Pwar` produces a deployable WAR.
- `java -jar target/*.jar` starts the application successfully.
- GitHub Release workflow continues to function and uploads the expected artifacts.

