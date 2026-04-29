# Specification: Export Show Card

## 1. Overview

The goal of this track is to implement a feature that allows users to export the "Show Card" (the list of matches and show details) to the clipboard in multiple pre-defined formats: Markdown, Facebook post, X (Twitter) post, and Bluesky post.

## 2. Functional Requirements

* **Trigger Locations:** The "Export Show Card" action must be accessible from two places:
  * The Show Details view (as a dedicated button or action).
  * The Show List view (as an action in the context menu for each show).
* **Export Modal Dialog:** Clicking the export action will open a modal dialog.
* **Format Selection:** The modal dialog must allow the user to select the desired export format (Markdown, Facebook, X, Bluesky).
* **Preview and Copy:** The modal dialog must display a live preview of the generated text based on the selected format. A prominent "Copy to Clipboard" button must be available to copy the previewed text.
* **Content Inclusion:** The exported text must include the following information for the selected show:
  * Show Details (Name, Date, Venue).
  * List of matches and participating wrestlers.
  * Match Types/Rules.
  * Indication of any Title Defenses.
* **Format Constraints:** The generated text must adhere to the character limits and formatting conventions of the selected platform (e.g., shorter text for X/Bluesky, potential use of hashtags).

## 3. Non-Functional Requirements

* The export logic should be extensible to easily add new formats in the future.
* The modal dialog should be responsive and fit within the application's existing Vaadin Lumo theme.

## 4. Acceptance Criteria

* [ ] User can click an "Export" action from the Show List.
* [ ] User can click an "Export" action from the Show Details view.
* [ ] Clicking "Export" opens a modal with a format selector and text preview.
* [ ] Changing the format selector dynamically updates the preview text.
* [ ] The preview text accurately reflects the show's details, matches, match types, and title defenses.
* [ ] Clicking "Copy to Clipboard" successfully copies the preview text to the user's system clipboard.
* [ ] The generated text for each format correctly utilizes platform-specific formatting (e.g., Markdown bolding, appropriate spacing for social media).

## 5. Out of Scope

* Direct API integration to post to social media platforms (this is strictly a "copy to clipboard" feature).

