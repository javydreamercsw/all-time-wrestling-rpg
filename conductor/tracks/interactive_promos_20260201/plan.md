# Implementation Plan - Interactive Promos

- [x] **Design & Backend**
  - [x] Create `PromoService` to handle prompt construction and AI interaction for retorts.
  - [x] Update `SegmentNarrationService` to expose a method for single-turn responses if applicable.
- [x] **Frontend (UI)**
  - [x] Create `InteractivePromoView` (or modify `MatchView` to support "Promo" mode).
  - [x] Implement chat-like interface:
    - [x] Message history list.
    - [x] Input field for player.
    - [x] "Retort" trigger.
- [x] **Integration**
  - [x] Connect UI to `PromoService`.
  - [x] Save transcript to `Segment` entity.
- [x] **Testing** [2de3427]
  - [x] Unit tests for `PromoService`.
  - [x] E2E test for the interactive flow.

