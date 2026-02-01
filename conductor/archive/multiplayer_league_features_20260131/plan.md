# Implementation Plan: Multiplayer & League Features

## Phase 1: Core Data Model & League Structure

- [x] **Entities:**

  - `League` (Entity)

  - `LeagueMembership` (Entity)

  - `LeagueRoster` (Entity)

- [x] **Repositories:** Create repositories for new entities.

- [x] **Service:** `LeagueService` (Create League, Add Member, Get Rosters).

- [x] **Tests:** Unit tests for League creation and membership.

## Phase 2: Draft System

- [x] **Entities:** `Draft`, `DraftPick`.

- [x] **Service:** `DraftService` (Start, Pick, Next Turn, Auto-complete).

- [x] **Integration:** "Finish Draft" -> Populates `LeagueRoster`.

- [x] **UI:** `DraftView` (Real-time picking interface).

## Phase 3: Match Booking & Notification

- [x] **Booking Logic:** Update `ShowService`/`SegmentService`.

  - Detect if wrestlers in a segment belong to a `LeagueRoster`.

  - Create/Link `MatchFulfillment` records (tracking status of player results).

- [x] **Inbox:**

  - Create `MatchRequestNotification` type.

  - Trigger notifications on "Show Publish".

## Phase 4: Player Interaction (Match Fulfillment)

- [x] **Match Report Backend:** Service for players to submit results.

- [x] **Match Report UI:** View for players to enter results (Winner, Method, etc.).

- [x] **Service:** `MatchAdjudicationService` updates to accept "Pending" results from players.

## Phase 5: Show Finalization

- [x] **Booker UI:** Update `ShowDetailView` to show "Waiting for Results" or "Results In".

- [x] **Finalize Action:** Commissioner reviews and commits results to history.

## Phase 6: Public Showcases (Viewer)

- [x] **Public Views:** Read-only access to League Standings and Histories.

