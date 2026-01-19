# Specification: Title Reign Visibility and History Tracking

## Overview

The "Title Reigns" feature aims to expose and visualize historical and current championship data that is already stored in the system. Users will be able to track the legacy of each championship and the personal accomplishments of every wrestler through dedicated history views.

## Functional Requirements

### 1. Enhanced Wrestler Profile View

- **Title History Section:** A new section on the Wrestler Profile page.
- **Visual Timeline:** A graphical timeline showing all championships held by the wrestler over their career.
- **History Cards:** Detailed cards for each specific reign held by the wrestler, including:
  - Championship name and tier.
  - Start and End dates (displaying "Present" for active reigns).
  - Total Duration (calculated automatically in days).
  - **Match Reference:** A clickable link to the show/match where the title was won, with a hover tooltip showing a summary of that match.

### 2. Championship Detail View (Legacy History)

- **Championship Overview:** A dedicated or updated view for each title.
- **Lineal History Timeline:** A chronological visual representation of all holders of the title from its inception.
- **Reign Detail Cards:** Cards for every champion in the title's history, showing:
  - Wrestler(s) name.
  - Dates and Duration.
  - **Change Event:** Clickable navigation to the show where the title change occurred.

### 3. Dynamic Logic & UI State

- **Current Champion Highlighting:** Active reigns must be visually distinct (e.g., specific color, "Current Champion" badge).
- **Ongoing Duration:** For active reigns, the duration should be calculated dynamically relative to the current game date.
- **Tooltips:** Implementation of hover tooltips for Match/Event references to provide immediate context without leaving the page.

## Non-Functional Requirements

- **Performance:** History timelines should load efficiently even for titles with long histories.
- **UI Consistency:** Visuals must adhere to Material Design principles and existing Vaadin theme.

## Acceptance Criteria

- [ ] Users can view a full history of a wrestler's title wins on their profile.
- [ ] Users can view the complete lineal history of a championship.
- [ ] All "Won At" events are clickable and correctly navigate to the corresponding Show view.
- [ ] Active reigns are clearly labeled and durations are up-to-date.
- [ ] Unit tests verify the duration calculation logic (especially for "Present" dates).
- [ ] Integration tests verify that clicking a history reference correctly redirects the user.

## Out of Scope

- External sharing/exporting of title history.
- Management/Editing of history entries from these views (this is a read-only display feature).

