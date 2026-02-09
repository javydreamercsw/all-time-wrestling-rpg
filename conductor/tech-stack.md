# Tech Stack

## Backend

* **Language:** Java 25
* **Framework:** Spring Boot 4.0.1
* **Build Tool:** Maven
* **Database:**
  * **Development/Testing:** H2 (Embedded)
  * **Production:** MySQL 8.0+
* **Migration Tool:** Flyway
* **ORM:** Hibernate 7.2.0 (via Spring Data JPA)
* **Testing:** JUnit 5, Mockito, Testcontainers, Karibu Testing (Vaadin)
* **Security:** Spring Security (OAuth2, Crypto)
* **Utilities:** Lombok, MapStruct, Jackson, Groovy (Scripting)

## Frontend

* **Framework:** Vaadin Flow 25.0.4 (Java-based UI)
* **Client-Side:**
  * **React:** 19 (Enabled via Vaadin React Components)
  * **Lit:** 3.3.2
  * **TypeScript:** 5.9.3
* **Build Tool:** Vite 7.3.1 (integrated with Vaadin)
* **Styling:** Vaadin Lumo Theme
* **Components:** Vaadin Components, FullCalendar
* **Documentation:** VitePress (Docs-as-Code)

## Infrastructure & DevOps

* **Containerization:** Docker (Jib for image building)
* **CI/CD:** GitHub Actions (inferred from `.github/workflows`)
* **Code Quality:** Spotless (Code Formatting), OWASP Dependency Check, JaCoCo (Coverage)

## External Integrations

* **AI Services:**
  * Gemini
  * Claude
  * OpenAI
  * LocalAI (Self-hosted model support)
* **Productivity:** Notion (via Notion SDK JVM)

