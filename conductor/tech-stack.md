# Tech Stack

The technology stack for the All Time Wrestling RPG project is as follows:

## Core Technologies

*   **Programming Language:** Java
*   **Backend Framework:** Spring Boot - For building robust, stand-alone, production-grade Spring-based applications.
*   **Frontend Framework:** Vaadin - For building modern web applications with a focus on rich user interfaces.
    *   **Frontend Technologies:** Web Components, Polymer, Lit, React - These are utilized within the Vaadin framework for various UI components and functionalities, offering a flexible and powerful frontend development environment.
*   **Database:** H2 Database - A lightweight, in-memory, and file-based database, ideal for development and testing. It is configured with Flyway for managing database migrations, ensuring schema evolution is controlled and versioned. The application is designed to be configurable for other databases in production environments.
*   **Build Tool:** Maven - For project automation, dependency management, and building.

## External Integrations

*   **AI Platforms:** Gemini, Claude, OpenAI - Integrated for AI-powered features, primarily for generating dynamic and context-aware narrations for wrestling matches and events.
*   **Notion:** Utilized for data synchronization, allowing the application to import and manage game content (such as wrestlers, shows, and rules) directly from Notion databases. This provides a flexible content management system.