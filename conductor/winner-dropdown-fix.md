# Fix Winner Dropdown Issue

## Objective

To fix the issue where the "Winner" dropdown is empty when editing segments created via the AI show proposal.

## Key Files & Context

- `src/main/java/com/github/javydreamercsw/management/ui/view/show/ShowDetailView.java`
- `src/main/java/com/github/javydreamercsw/management/ui/view/show/EditSegmentDialog.java`
- `src/main/java/com/github/javydreamercsw/management/service/wrestler/WrestlerService.java`

## Background & Motivation

The user reported that when editing segments proposed by AI, the winner dropdown is empty.
Two primary issues have been identified:
1.  **Missing Initialization in `EditSegmentDialog`**: The `winnersCombo` is populated via the `participantsCombo` value change listener. However, during initialization, `participantsCombo.setValue` is called *before* the listener is added, and `winnersCombo.setItems()` is never explicitly called with the initial participants.
2.  **Filtering Bug in `WrestlerService`**: `findAllFiltered` iterates only over `findAll()` (which returns only active wrestlers from enabled expansions). If the `includedWrestlers` set contains wrestlers not returned by `findAll()` (e.g., if their status changed or they were forced in by AI), they are silently dropped. This causes `participantsCombo` to not receive those wrestlers as valid options, preventing them from being selected, which cascades to an empty winners list.

## Implementation Steps

1. **Fix `WrestlerService.findAllFiltered`**:
   - Modify the method to ensure that all wrestlers in `includedWrestlers` are always included in the final result, regardless of whether they appear in the `findAll()` stream.
   - Use a `Set` to collect the active wrestlers, then `addAll` from `includedWrestlers`, and finally sort and return the list.
2. **Fix `EditSegmentDialog.java`**:
   - Move the `participantsCombo.addValueChangeListener` to be added *before* `participantsCombo.setValue(existingParticipants)` so that setting the initial value triggers the population of the `winnersCombo`.
   - Alternatively, explicitly call `winnersCombo.setItems(existingParticipants.stream()...)` during initialization.
3. **Fix `ShowDetailView.java` (if applicable)**:
   - Verify that the order of initialization in `openEditSegmentDialog` correctly populates the `winnersCombo`. It currently calls `winnersCombo.setItems` directly during initialization, but it might suffer if `wrestlersCombo` fires a delayed event that clears it. Ensure the initial state is robust.

## Verification

- Review code logic.
- Run tests.

