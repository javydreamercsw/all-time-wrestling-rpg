# Specification: Multiplayer & League Features

## 1. Introduction

This specification defines the "Multiplayer & League Features" update. This update transforms the application into a platform where users can participate in managed Leagues.

## 2. Core Workflows

### 2.1. League Management (The Container)

**Goal:** A Booker/Admin creates and manages a season-long competition.
- **Roles:**
- **Commissioner (Admin/Booker):** Sets up the league, schedules shows, finalizes results.
- **Player:** Drafts a stable of wrestlers, plays matches, reports results.
- **League Settings:**
- Participants (List of Users).
- Match Result Deadline (e.g., 24 hours after show posting).
- Draft Configuration (Snake/Standard, Timers).

### 2.2. Draft Phase (The Setup)

**Goal:** Distribute wrestlers among Players to form `LeagueRosters`.
- **Flow:**
1.  Commissioner starts Draft.
2.  Players take turns picking wrestlers from the global pool.
3.  **Outcome:** A `LeagueRoster` entry is created, linking User + Wrestler + League.
4.  Drafted wrestlers are exclusive to that User within this League.

### 2.3. Booking Phase (The Booker's Job)

**Goal:** Commissioner creates shows using the drafted rosters.
- **Constraint:** When booking a match, the Commissioner can only use wrestlers drafted by the League participants (or Free Agents, if allowed).
- **Automation:** When a match is booked (e.g., "The Rock (Player A) vs. Steve Austin (Player B)"), the system detects the Players involved.

### 2.4. Match Fulfillment Phase (The Player's Job)

**Goal:** Players perform the actual matches and report outcomes.
- **Notification:**
- When the Commissioner "Publishes" a show, Players receive an **Inbox Notification**.
- Notification contains: "You have a pending match on [Show Name] vs [Opponent Name]."
- **Action:**
- Players play the match (IRL, simulated, or via other means).
- Players access a "Match Report" form via the notification.
- Input: Winner, Duration, Method (Pin/Sub), Notes.
- **Status:** Matches track states: `SCHEDULED` -> `PENDING_RESULTS` -> `RESULTS_SUBMITTED` -> `FINALIZED`.

### 2.5. Show Finalization (The Adjudication)

**Goal:** Commissioner wraps up the show.
- Once all match results are in (or deadline passes), the Commissioner reviews them.
- Commissioner adds Narration/Flavor text.
- Commissioner clicks "Adjudicate" to apply ratings, title changes, and league standings.

## 3. Data Model Changes

### New Entities

- **`League`**: ID, Name, CommissionerID, Settings.
- **`LeagueMembership`**: Link between League and User.
- **`Draft`**: Linked to `League`.
- **`DraftPick`**: Records the selection history.
- **`LeagueRoster`**: The active mapping of User -> Wrestler for a specific League.
- **`MatchRequest`**: (Or columns on `Segment`) Tracks who needs to report results for a specific match segment.

## 4. UI Requirements

- **League Dashboard:** Hub for standings, schedule, and roster management.
- **Draft Room:** Real-time picking interface.
- **Booker Interface:** Enhanced `ShowDetailView` showing which Player owns which wrestler.
- **Player Interface:** "Pending Matches" view (Inbox integration).

## 5. Security

- Only League Members can access internal League details.
- Only the specific Player (or Commissioner) can submit results for their match.

