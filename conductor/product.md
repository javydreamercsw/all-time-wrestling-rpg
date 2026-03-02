# Initial Concept

To provide an AI-powered storytelling platform for wrestling events.

## Key User Roles and Interactions

* **Admin:** Full system access, including user account management.
* **Booker:** Can manage all game content (shows, wrestlers, etc.).
* **Player:** Can manage their own wrestler and related items.
* **Viewer:** Read-only access to public data.

## Core Gameplay Mechanics and Features

The core gameplay mechanics and features of All Time Wrestling RPG include:

* **Show Management:** Users can manage wrestling shows, including ordering segments and designating main events. Features automatic scheduling of empty show "shells" for entire seasons based on recurrence rules.
* **AI Narration & Planning:** An AI narration service generates context-aware narrations. Enhanced "Smart Book" logic automates show planning, ensuring 100% roster participation, respecting title defense frequencies, and prioritizing active feuds.
* **Dynamic Ranking System:** Wrestlers are ranked based on fan count, with a refined fan acquisition and loss system that adds consequence to match outcomes.
* **Season Management:** Options for "soft reset" of roster fan standings and tier definitions at the end of each season, including fan recalibration and tier boundary resets.
* **Segment Rules with Bump Addition:** Configurable segment rules to automatically add bumps to participants based on winners, losers, or all participants.
* **Notion Synchronization:** Advanced synchronization engine providing real-time UI feedback, detailed logging, and intelligent duplicate detection when bridging local data with Notion databases.
* **Multi-Database Support & Migration:** Support for multiple database engines (H2, MySQL) with a built-in data transfer wizard for seamless migration between environments.
* **Title History & Lineal tracking:** Comprehensive visualization of championship histories and wrestler accomplishments through chronological timelines and detailed reign records.
* **Solo Campaign Mode:** A persistent, narrative-driven single-player experience ("All or Nothing") featuring character progression, alignment tracking (Face/Heel), backstage actions, and AI-driven story branching.
* **League Management:** Multiplayer-focused league system supporting snake drafts, automated standings, match reporting, and season history tracking.
* **Interactive Promos:** Role-playing elements where players can cut promos to influence feud heat, alignment, and match hype, with AI providing dynamic feedback and rewards.
* **Match Narration Feedback:** Players can rate and provide specific feedback on AI-generated match narration to train the system for better future storytelling.
* **On-Demand Theme Switching:** User-configurable UI themes allowing instant switching between Light, Dark, and custom color presets.
* **AI Image Generation:** Integrated generation of wrestler, NPC, and show template visual assets using local or remote AI models.

## Technical Considerations

Primary technical considerations for the All Time Wrestling RPG project include:

* **AI Platform Integration:** Seamlessly integrating with various AI platforms (Gemini, Claude, OpenAI) to power the AI narration service.
* **Notion Data Synchronization:** Efficiently synchronizing data from Notion databases to local JSON files and the application's database, ensuring data consistency and performance.
* **Scalability:** Designing the application to handle a growing number of users and AI-driven processes without performance degradation.
* **Security:** Implementing robust security measures for user authentication, authorization, and data protection, given the sensitive nature of user accounts and game data.
* **Maintainability:** Ensuring the codebase is well-structured, documented, and easy to maintain and extend for future development.

## Long-term Vision and Future Aspirations

The long-term vision for the All Time Wrestling RPG project is to integrate advanced machine learning for even more realistic AI interactions and dynamic storylines. This involves continuously enhancing the AI's ability to generate compelling narratives, adapt to in-game events, and create unique wrestling experiences. The goal is to push the boundaries of AI-driven storytelling in simulation games, providing players with an unparalleled level of immersion and replayability. This could also involve exploring procedural content generation and more sophisticated character AI to make each playthrough feel fresh and unpredictable.
