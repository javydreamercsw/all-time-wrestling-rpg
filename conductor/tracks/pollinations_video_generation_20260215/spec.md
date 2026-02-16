# Specification: Pollinations AI Video Generation

## Overview

Integrate Pollinations AI to provide dynamic video generation capabilities for the wrestling RPG. This feature enhances immersion by bringing match narrations and summaries to life through AI-generated visual content, leveraging existing wrestler and NPC assets.

## Functional Requirements

- **Integration:** Implement a service to interact with Pollinations AI for video generation.
- **Trigger Points:**
  - Add a "Generate Video" action in the Booker Dashboard for completed shows/matches.
  - Provide a general-purpose prompt-to-video interface for Admins/Bookers.
- **Visual Prompting:**
  - If participants (Wrestlers/NPCs) have configured images, use those URLs as visual references for the AI.
  - If images are unavailable, generate detailed text prompts based on character descriptions and match context.
- **Persistence:**
  - Utilize the existing storage infrastructure to save generated video files.
  - Store metadata and file paths in the database, linked to the relevant Match or Show entities.
- **Distribution & Visibility (Inbox Integration):**
  - Generate a "Show Summary Newsletter" for players after a show is processed.
  - Include the generated video(s) within this newsletter, delivered to the **Player Inbox**.
  - Ensure videos are also accessible via the Match History and Match Report views.
- **UI/UX:**
  - Display generation progress (loading state) in the dashboard.
  - Embed a video player in the Inbox, Match Report, and History views.

## Non-Functional Requirements

- **Error Handling:** Gracefully handle API timeouts or failures from Pollinations AI with user notifications.
- **Fallback:** Ensure narration remains available even if video generation fails or is skipped.
- **Performance:** Perform video generation asynchronously to avoid blocking the main UI thread.

## Acceptance Criteria

- [ ] Users can trigger video generation for a match from the Booker Dashboard.
- [ ] Generated videos are correctly saved to the server's storage directory.
- [ ] A newsletter containing the video is delivered to relevant player inboxes.
- [ ] Videos can be played directly within the application (Inbox/Reports).
- [ ] The system uses character images when available to guide the video aesthetic.

## Out of Scope

- Real-time video generation during live match simulation.
- User-side video editing or filtering.
- Public sharing of videos to social media platforms directly from the RPG.

