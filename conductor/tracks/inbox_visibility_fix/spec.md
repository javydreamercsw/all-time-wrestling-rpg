# Specification: Inbox Visibility Fix

## Problem Description

There's a bug where people see everyone's messages in the inbox but only can action their messages. They should only see their messages.

## Current Behavior

The `InboxService.search` method does not enforce target filtering if no filters (targets or accountId) are provided. If both are null/empty, it returns all items.

## Desired Behavior

If a user is not an Admin or Booker, they should only see items targeted at them (either via their Account ID or their Wrestler ID).
If no targets are provided in the search, the system should default to the current user's targets unless the user has elevated privileges.

## Reproduction

- Create an inbox item for User A.
- Log in as User B.
- Observe that the item for User A is visible in the inbox list if no filters are applied (or if filters are cleared).

## Success Criteria

- User A only sees messages targeted at User A or their Wrestler.
- User B only sees messages targeted at User B or their Wrestler.
- Admin and Booker can still see all messages (or filter as they wish).
- Regression tests pass.

