# Play Data Safety disclosure

Working notes for filling in Google Play Console's Data Safety form (T9.4). Every category
below is either "not collected" or "not applicable" — Fechtkarte has no network access, no
account system, and no third-party SDKs, so there is nothing else to disclose.

## Data collection: none

| Play Console category | Collected? | Notes |
|---|---|---|
| Location | No | Never requested, never accessed. |
| Personal info (name, email, address, etc.) | No | No account system exists. |
| Financial info | No | No purchases, no payment flow. |
| Health and fitness | No | |
| Messages | No | |
| Photos and videos | No | The app only *writes* images/PDFs it generates itself, via MediaStore/share — it never reads existing media. |
| Audio files | No | |
| Files and docs | No | Same as photos/videos: write-only, user-initiated (Save/Share), and only of the app's own generated output. |
| Calendar | No | |
| Contacts | No | |
| App activity (in-app actions, search history, etc.) | No | Nothing is logged or transmitted — see "App activity" below for the one nuance. |
| Web browsing | No | |
| App info and performance (crash logs, diagnostics) | No | No crash reporting or analytics SDK is bundled. |
| Device or other identifiers | No | No advertising ID, no device fingerprinting. |

## The one nuance: local-only preferences

Generation preferences (action count, thrust count, palette choices, enabled rules) are saved
locally via Android's DataStore, entirely on-device. This is **not** "app activity" in the Play
Console sense that form asks about — it never leaves the device, is never transmitted, and
Google's own guidance is that purely local storage with no transmission doesn't count as
"collection." Documented here for completeness, not because it changes any answer above.

## Data sharing: none

Fechtkarte shares no data with any third party, because it has no data to share and no
third-party integration to share it with.

## Data deletion

All app data (the local drill database, saved preferences) is removed automatically when the
app is uninstalled, via Android's standard app-data lifecycle — no separate deletion request
mechanism is needed because there is no server-side copy of anything to delete.

## Security

No sensitive data is collected, so there is no sensitive data in transit or at rest to secure
beyond what Android's own per-app sandboxing already provides.

See also: [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md), the user-facing version of the same facts.
