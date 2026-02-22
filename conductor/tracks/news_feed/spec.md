# AI-Generated "Wrestling World" News Feed - Specification

## Overview

Add a real-time news ticker and a "Social Media" dashboard that synthesizes recent match results, title changes, and backstage rumors into a living feed. A major monthly synthesis occurs after Premium Live Events (PLEs) to wrap up the month's narrative.

## Features

- **News Ticker:** A lightweight, scrolling feed of headlines visible on main dashboards.
- **Social Media Dashboard:** A dedicated view showing mock social posts from wrestlers, commentators, and fans, reacting to recent events.
- **AI Synthesis:**
  - **Real-time:** Individual segment/show reactions.
  - **Monthly Wrap-Up:** A comprehensive synthesis of the entire month's events, triggered after the monthly PLE, providing a "State of the Wrestling World" report.
- **Rumor Mill:** Procedural generation of trade rumors, contract status updates, or backstage drama.
- **Export News:** Option to download the monthly wrap-up as a "Newsletter" PDF or JSON.

## Technical Goals

- Implement a aggregation logic to gather events over a 30-day window.
- Create a dedicated `SocialMediaView` with Vaadin.
- Enhance `NewsGenerationService` with a monthly synthesis prompt.

