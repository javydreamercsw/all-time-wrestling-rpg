# Specification: AI-Driven News & Rumor Feed

## Goal

Create a procedurally generated news feed that reacts to in-game events (match results, alignment changes, injuries, promos) to make the game world feel "alive."

## Requirements

1. **News Generator Service:**
   - A service that listens for game events or runs at the start of a "New Day."
   - Uses AI to synthesize recent events into catchy headlines and short blurbs.
   - **Selective Triggering:** AI calls are restricted to significant events (Title changes/defenses, new injuries, rivalry finales, or main events) to optimize token usage.
   - **Show Synthesis Mode:** Option to generate a single "Show Roundup" instead of per-segment items.
2. **Rumor Engine:**
   - Procedurally generates non-factual or "potential" events (e.g., "[Wrestler] seen talking to [Faction Manager]").
   - **Probability-Based:** Triggers based on a configurable daily "roll" chance.
   - Influences "Feud Heat" if players interact with the rumors.
3. **Dashboard Integration:**
   - A new component on the main Dashboard to display the "Latest Headlines."
4. **Persistence & Settings:**
   - News items should be stored in the database so players can view a "News Archive."
   - **Configurability:** Admins can toggle the feature, set rumor probability, and choose the generation strategy (Segment vs. Show) via Game Settings.

## Success Criteria

- The Dashboard displays news items relevant to the most recent show results.
- Rumors include plausible combinations of wrestlers and factions.
- News archive is accessible via the UI.

