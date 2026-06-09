# Admin

Welcome to the Admin guide. This documentation is automatically generated from the latest game features.

## Season Settings - Schedule Generation Confirmation

The Season Settings tab showing the 'Generate Season Confirmation' button, before execution.

![Season Settings - Schedule Generation Confirmation](/screenshots/admin-season-settings-confirmation.png)

---

## Account Management

Admins manage all user accounts from this view. Each row shows the account's username, email address, assigned roles, last login timestamp, and enabled/disabled status. Use Edit to change roles or reset credentials, and Delete to permanently remove an account.

![Account Management](/screenshots/account-list-overview.png)

---

## Universe Management

Universes are the top-level containers for all promotion data — shows, wrestlers, titles, factions, and storylines all belong to a universe. An installation can run multiple universes simultaneously, each with its own independent roster, calendar, and championship lineage.

![Universe Management](/screenshots/universe-list-overview.png)

---

## Export Custom Images

Download a ZIP of all custom images (wrestlers, factions, titles, arenas, and more) in one click. The archive preserves the original file paths so images can be restored on a new host without reconfiguration.

![Export Custom Images](/screenshots/export-universe-images.png)

---

## Managing Universe Invites

Admins can generate invite links to share with prospective members. Targeted links are single-use with a 7-day expiry; Community links are multi-use with no expiry. Active links are listed below with options to revoke.

![Managing Universe Invites](/screenshots/universe-invite-management.png)

---

## Reviewing Join Requests

Pending membership requests appear here. Approve to add the user as a member, Reject to decline while allowing them to re-request, or Block to permanently prevent future requests from that account.

![Reviewing Join Requests](/screenshots/universe-join-requests.png)

---

## Wrestler Relationships Management

Manage the social fabric of your promotion. Define marriages, family ties, and deep friendships that influence chemistry, AI narration, and random backstage events.

![Wrestler Relationships Management](/screenshots/admin-wrestler-relationships.png)

---

## Generate Template Art

Create unique branding for your show templates using AI. This art will be used in the booking interface and calendar.

![Generate Template Art](/screenshots/admin-show-template-art-generation.png)

---

## Season Settings - Before Schedule Generation

The Season Settings tab showing the 'Generate Season Schedule' button, before execution.

![Season Settings - Before Schedule Generation](/screenshots/admin-season-settings-before-generation.png)

---

## System Performance

Monitor AI response times and resource usage.

![System Performance](/screenshots/admin-observability-performance.png)

---

## Season Settings - After Schedule Generation

The Season Settings tab after successfully generating the season schedule.

![Season Settings - After Schedule Generation](/screenshots/admin-season-settings-after-generation.png)

---

## Cache Management

Monitor and manage application caches.

![Cache Management](/screenshots/admin-observability-cache.png)

---

## Database Management

Monitor database statistics and optimize performance.

![Database Management](/screenshots/admin-observability-database.png)

---

## System Pulse

Real-time health status of the application.

![System Pulse](/screenshots/admin-observability-pulse.png)

---

## Admin Tools

Perform critical maintenance tasks such as manual tier recalculation and account management.

![Admin Tools](/screenshots/admin-tools.png)

---

## Show Templates

Define reusable templates for your shows. Set up standard segments, match orders, and branding to quickly book consistent weekly episodes or pay-per-views.

![Show Templates](/screenshots/admin-show-templates.png)

---

## AI Configuration

Configure the Artificial Intelligence providers used for match narration, image generation, and creative assistance. You can switch between different LLM providers (OpenAI, Anthropic, Gemini) and configure their specific settings.

![AI Configuration](/screenshots/admin-ai-settings.png)

---

## Injury Management

Define different types of injuries, their severity, and recovery times. These are used to dynamically affect wrestler performance and availability in the campaign and booking modes.

![Injury Management](/screenshots/admin-injury-types.png)

---

## Wrestler Management

Manage the entire roster of wrestlers. Add new talent, edit existing stats, assign images, and manage contract details from this centralized view.

![Wrestler Management](/screenshots/admin-wrestler-list.png)

---

## Expansion Management

Group and toggle themed content sets. Enable or disable entire collections of wrestlers, teams, and factions to customize the available roster for matches and leagues.

![Expansion Management](/screenshots/admin-expansion-management.png)

---

## Video Walkthroughs

### MySQL Data Migration Wizard

Migration complete — all data is now in MySQL and verified. Restart the application with the MySQL Spring profile (spring.profiles.active=mysql) to run in production mode against the new database.

<video controls width="100%" style="border-radius:8px;margin-bottom:1rem">
  <source src="https://javydreamercsw.github.io/all-time-wrestling-rpg/videos/data-transfer-full-wizard.mp4" type="video/mp4">
</video>

---

### Account Management Walkthrough

New Account dialog — username and email are required; password is set on first login or by the admin. Roles can be combined: a Booker who also plays in leagues needs both the Booker and Player roles.

<video controls width="100%" style="border-radius:8px;margin-bottom:1rem">
  <source src="https://javydreamercsw.github.io/all-time-wrestling-rpg/videos/account-list-walkthrough.mp4" type="video/mp4">
</video>

---

### Universe Management Walkthrough

Create Universe dialog — name is required; type controls which game rules apply. Standard uses the default ATW ruleset; Fantasy enables league drafts; Historical locks the roster to a specific era.

<video controls width="100%" style="border-radius:8px;margin-bottom:1rem">
  <source src="https://javydreamercsw.github.io/all-time-wrestling-rpg/videos/universe-list-walkthrough.mp4" type="video/mp4">
</video>

---

### Inviting Members to a Universe

When someone follows the link and submits a request, click Requests to review it. Approve to add them as a member, Reject to decline, or Block to prevent future requests from that account.

<video controls width="100%" style="border-radius:8px;margin-bottom:1rem">
  <source src="https://javydreamercsw.github.io/all-time-wrestling-rpg/videos/universe-member-onboarding.mp4" type="video/mp4">
</video>

---
