# MinLauncher — Feature Spec

MinLauncher is a minimalist Android home screen replacement. It shows all your installed apps in a single scrollable list, ordered by how much you've used them today. No widgets, no folders, no distractions.

---

## Screen Layout

The screen has three sections stacked vertically:

1. **Header row** — time on the left, battery percentage on the right
2. **Search bar** — text field for filtering the app list
3. **App list** — scrollable list of every installed app

---

## Features

### App List

- Every launchable app installed on the device appears in the list
- Sorted by screen time today, most-used first; apps with equal usage sort alphabetically
- Each row shows:
  - App icon
  - App name
  - Time used today in `HH:MM:SS` format (e.g. `00h 12m 03s`)
- When you return to the launcher from an app, the list automatically scrolls back to the top

### Launching an App

- Tap any app row to open it

### Search

- Type in the search bar to filter the list in real time (case-insensitive)
- Pressing Back when something is typed clears the search and shows the full list
- Pressing Back when the search is empty scrolls the list back to the top

### Time Display

- The current time is shown in the top-left corner in 24-hour format: `HH:MM`
- Updates automatically at the start of each new minute

### Battery Display

- The current battery percentage is shown in the top-right corner (e.g. `85%`)
- Updates whenever the battery level changes

### Fullscreen Mode

- The system status bar is hidden — the launcher's own header shows time and battery instead
- The navigation bar is also hidden by default
- Swipe up from the bottom edge of the screen to temporarily reveal the navigation bar

### Daily Usage Stats

- Screen time shown in the app list reflects today only
- Stats reset automatically at midnight
- The sort order updates throughout the day as you use apps

---

## Permissions

### Usage Access (required)

MinLauncher needs the **Usage Access** permission to read how long you've spent in each app.

- If this permission hasn't been granted, the app list is replaced with a prompt: *"Usage access required"*
- Tap **Open Settings** to go to the Usage Access settings screen and grant permission
- Return to the launcher and it will automatically detect the change

---

## Visual Design

| Element | Value |
|---|---|
| Background | Black |
| Primary text | White |
| Stats text | Gray |
| Time & battery size | Large |
| App name size | Medium |
| Stats size | Small |
