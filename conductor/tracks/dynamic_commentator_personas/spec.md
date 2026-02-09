# Specification: Dynamic Commentator Personas (AI)

## Goal

Transform match narration from a single AI output into a dynamic back-and-forth dialogue between two distinct commentator personas (Face/Play-by-Play and Heel/Color).

## Requirements

1. **Commentator Profiles:**
   - Define a set of pre-defined commentator characters with unique traits (e.g., name, alignment, bias).
   - "The Voice": Professional, emphasizes wrestler skill, cheers for faces.
   - "The Instigator": Biased toward heels, justifies cheating, mocks the "underdog" mentality.
2. **Narration Refactoring:**
   - Update `SegmentNarrationService` to handle multi-persona prompts.
   - The AI must generate dialogue tags (e.g., "[VOICE]: ... [INSTIGATOR]: ...") that can be parsed by the UI.
3. **UI Display:**
   - Enhance `MatchView` to display narration as a chat-like transcript with commentator avatars or icons.
4. **Configuration:**
   - Allow Bookers to select the "Commentary Team" for a show or let it be randomized.

## Success Criteria

- Narration outputs contain distinct voices for Face and Heel personas.
- `MatchView` displays dialogue in a visually distinct format (different styles for each speaker).
- At least 2 commentary teams are available for selection.

