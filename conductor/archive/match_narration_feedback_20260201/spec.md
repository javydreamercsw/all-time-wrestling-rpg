# Specification: Match Narration Feedback

## Goal

To allow players and bookers to provide specific instructions or "feedback" to the AI engine when generating match narrations from the `MatchView`.

## User Stories

- **As a Player/Booker**, I want to input specific details (e.g., "The match ends with a surprise roll-up", "Manager interferes") before generating the narration.
- **As a Player**, I want this "Generate with Feedback" option to be the primary way I generate narrations for my matches.

## Requirements

1. **UI Updates (`MatchView`):**
   - Add a `TextArea` for "Narration Feedback" or "Context".
   - Ensure this field is visible to users who have permission to generate narration.
   - Update the "Generate Narration" button (or add a new one) to send this feedback to the backend.
2. **Backend Updates:**
   - Ensure `SegmentNarrationContext` includes the user's instructions.
   - The `SegmentNarrationService` already supports instructions, so we just need to ensure the UI populates it correctly (which it partially does, based on recent E2E test fixes, but we need to verify it's exposed correctly for all users).
3. **Permissions:**
   - Visible to Bookers, Admins, and Players (for their own matches).

## Mockups

*(Mental Model)*
- **Match Narration Card**
- [Text Area: "Provide context..."]
- [Button: "Generate with Feedback"]
- [Text Area: Generated Narration]
- [Button: "Save"]
