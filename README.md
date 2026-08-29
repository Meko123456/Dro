# Dro 🕰

[![CI](https://github.com/Meko123456/Dro/actions/workflows/ci.yml/badge.svg)](https://github.com/Meko123456/Dro/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**დრო** (*dro* — Georgian for "time") — a small Android app for people whose day is
spread across time zones: a live clock for each city you care about, a **working-hours
overlap** strip that shows when everyone is actually at their desk, and a home-screen
widget that keeps ticking without waking the phone.

No accounts, no network, no analytics. Time zones come from the platform's own tz database.

## Why this app exists

I live in Tbilisi and work with a team that is rarely in the same time zone as I am. The
question I ask several times a day is not *"what time is it in X"* but *"when today can I
reach X during their working hours, and what does that cost me in mine?"* Generic world
clocks answer the first question; Dro is built around the second.

(A small irony behind the name: the two cities I move between, Tbilisi and Dubai, are both
UTC+4 all year — the original "dual clock" idea was a no-op, so the app grew into the
overlap finder it should have been from the start.)

## Features (planned — see the [issues](https://github.com/Meko123456/Dro/issues))

- 🏠 **Home + cities** — pick a home zone and any number of others; each row shows the
  local time, the date when it differs from home ("tomorrow" / "yesterday") and the hour
  offset relative to home.
- ⏱ **Live** — clocks tick every minute while the app is open, aligned to the wall-clock
  minute boundary rather than a drifting timer.
- 🗓 **Working-hours overlap** — a 24-hour strip per city (default 09:00–18:00 local,
  editable) laid out on the home zone's axis, with the shared window highlighted and
  summarised as "Everyone's working 10:00–14:00 your time". Handles zones whose working
  day crosses midnight in home time, half-hour offsets, and DST on the given date.
- 🎚 **Time scrubber** — drag along the strip to answer "if it's 15:00 for me, what is it
  for them?" without doing the arithmetic.
- 📱 **Widget** — a classic `RemoteViews` app widget built on `TextClock`, which the system
  updates itself every minute with **no alarms and no wakeups** (Glance can't do this
  without scheduled updates, so this is one of the few places the old API is the right
  one).
- 🔒 **Private by design** — preferences only (DataStore); the app declares no permissions.
- ♿ **Accessible** — every clock row reads as one sentence to a screen reader; the overlap
  strip has a text equivalent.
- 🎨 **Material 3** — dynamic color, light/dark, edge-to-edge.

## Architecture

```
domain/   pure Kotlin, unit-tested — ZoneClock (local time / day shift / offset label),
          WorkingHours, OverlapFinder (shared window on the home axis), ZoneCatalog
data/     DataStore preferences: home zone, city list, working hours
ui/       Compose — clock list, add-city search, overlap strip + scrubber
widget/   RemoteViews + TextClock app widget
```

Pure logic never touches Android: everything under `domain/` is `java.time` and plain
Kotlin, tested with JUnit against fixed instants (including DST transition days).

## Building

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Kotlin 2.3, AGP 9, Compose BOM 2026.06, minSdk 26.

## License

[MIT](LICENSE) © 2026 Merab Kochlamazashvili
