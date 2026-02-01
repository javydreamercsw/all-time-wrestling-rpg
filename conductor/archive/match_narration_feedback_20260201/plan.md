# Plan: Match Narration Feedback

## Phase 1: Analysis & Preparation

- [x] **Analyze `MatchView.java`:**
  - Confimed `feedbackArea` and "Generate with Feedback" button exist.
  - Found that visibility logic (`showGenerateButton`) currently supports Bookers, Admins, and *Campaign* matches.
  - **Gap Identified:** League matches (Multiplayer) are not covered. Players cannot generate narration for their league matches.
- [x] **Analyze `SegmentNarrationContext`:**
  - Verified it accepts instructions.

## Phase 2: Implementation

- [x] **Update `MatchView.java`:**
  - Inject `MatchFulfillmentRepository` (already injected).
  - Update `isCampaignMatch` logic (or rename it to `canGenerateNarration`) to include League Matches where the current user is a participant.
  - Ensure `MatchFulfillment` checks look for the `Segment` ID.

## Phase 3: Verification

- [x] **Create/Update E2E Test:**
  - Create `MatchViewNarrationE2ETest` (or similar) to verify a Player can see the feedback box for a League match.
  - Verify `BookerDocsE2ETest` continues to work.

