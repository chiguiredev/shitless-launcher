# Shitless Launcher — Spec

Minimalist Android home screen replacement. One scrollable list of all installed apps, ordered by how long you've used them today.

## Screen Layout

From top to bottom:
1. Header — current time (left) and battery % (right)
2. Search bar — filters the app list in real time
3. Total screen time — sum of all app usage today
4. App list — scrollable, one app per row

Each app row shows the app name and time used today (`00h 00m 00s`).

## Sorting

Apps are sorted by today's screen time, descending. Ties sort alphabetically (stable).

## Launching

Tap a row to open the app. On return to the launcher, the list scrolls back to the top.

## Search

- Filters the list in real time, case-insensitive
- Back with text typed → clears the search
- Back with empty search → scrolls list to top

## Header

- Time: 24-hour `HH:MM`, updates at the start of each new minute
- Battery: current percentage, updates on `ACTION_BATTERY_CHANGED`

## Fullscreen

- Status bar is hidden; the launcher header replaces it
- Navigation bar is hidden by default; swipe up from the bottom edge to reveal it transiently

## Usage Stats

- Screen time is read from `UsageStatsManager` using the system's aggregated stats
- Opens are counted from `UsageEvents` using reference counting (navigating between activities within the same app does not inflate the count)
- The currently active app (resumed but not yet paused) has its live session added on top of the aggregated total
- Stats cover from midnight to now; they reset automatically each day
- Stats are refreshed every time the launcher resumes

## Usage Access Permission

Requires the Usage Access special permission (`OPSTR_GET_USAGE_STATS`).

- Checked on init and every resume
- If not granted: the app list is replaced with a "Usage access required" message and an Open Settings button that deep-links to the Usage Access settings screen
- Permission is detected automatically when the user returns to the launcher
