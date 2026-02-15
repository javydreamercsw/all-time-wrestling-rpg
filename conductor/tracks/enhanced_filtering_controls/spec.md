# Specification: Enhanced Filtering Controls

## Overview

Currently, the segment selection UI and show templates lack granular controls for filtering wrestlers. This track aims to add alignment (Face/Heel) and gender filtering to improve the booking experience.

## Requirements

### 1. Segment UI Enhancements

- Add UI controls to the segment booking screen to filter available wrestlers by:
  - **Alignment:** Filter by Face, Heel, or Neutral.
  - **Gender:** Filter by Male, Female, or Non-Binary (if applicable).
- Filters should be reactive and update the list of selectable wrestlers immediately.

### 2. Show Template Enhancements

- Add a new setting to `ShowTemplate` to define gender constraints for the entire show.
- Options should include:
  - **All Male:** Only male wrestlers are selectable by default.
  - **All Female:** Only female wrestlers are selectable by default.
  - **Mixed (Default):** All genders are selectable.
- This template setting should serve as the default filter value for all segments within a show created from that template.

## User Experience

- Bookers should be able to quickly find the right talent for specific segments (e.g., "I need a Heel female for this promo").
- Show templates for specific divisions (e.g., a "Women's Evolution" PPV) should automatically restrict the talent pool to simplify booking.

