# KISS-eink

Opinionated fork of [KISS launcher](https://github.com/Neamar/KISS) for the **Mudita Kompakt** e-ink phone.

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## E-ink display rules

- **No animations.** Each layout change = ~0.4-0.5s e-ink refresh. Batch changes into one layout pass.
- **No `animateLayoutChanges`.** Instant visibility changes only.
- **Compact UI.** Screen is small and low-res. Minimize padding/margins. Every pixel counts.
- **High contrast.** Black on white. No shadows, no gradients, no transparency effects.
- **Fewer refreshes.** When hiding/showing UI (e.g. favorites bar + keyboard), do it during the system layout resize, not as a separate step.

## Upstream sync

- **Never rebase onto upstream.** Cherry-pick specific commits only.
- **In conflicts, favor our changes.** We are opinionated about e-ink UX.
- **Priority:** security fixes > performance > skip the rest.
- Upstream remote: `upstream` → `https://github.com/Neamar/KISS.git`
- Fork point: `cd6e54763642af4f96cc2246fbfdd42b94a43d03`

### How to sync

1. `git fetch upstream`
2. List new commits: `git log --oneline <last-synced-hash>..upstream/master`
3. Filter for fixes: `grep -iE 'fix|crash|npe|security|perf|async'` (exclude translations, lint, theme, gradle, drawable)
4. For each candidate: `git show --stat <hash>` — check if self-contained and how many of our files it touches
5. Cherry-pick: `git cherry-pick -X ours <hash>` — resolve conflicts favoring our code
6. Manual port anything that conflicts or partially applies
7. Update the sync log in memory: `memory/reference_upstream_sync_log.md`

## Key customizations

- E-ink theme: high contrast black/white, dotted separators, no shadows
- Icons hidden in result list (only text) for faster rendering
- Mudita Mindful Design: custom scrollbar, search bar, switch buttons
- Favorites bar hides when keyboard is visible (onGlobalLayout detection)
- Default icon pack: Arcticons Black (if installed)
- Adaptive icon shapes disabled by default
- Page-based scrolling instead of continuous scroll
- Phone + SMS as default favorites
