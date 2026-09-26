# Sotto UI/UX Audience Review

Phone-first audit against [`.cursor/skills/sotto-aac-audience`](../.cursor/skills/sotto-aac-audience/SKILL.md) and the plan in [plans/sotto-full-app-ux-audience-review.md](plans/sotto-full-app-ux-audience-review.md).

**Scope:** Full app (speak board, emergency/fullscreen, Two-Way caregiver, settings/theme/copy/privacy).  
**Status:** Review only — no app code changes in this PR.  
**Date:** 2026-09-26

## Verdict

Sotto already hits the hard parts of adult AAC well: dark-neutral speak surface, true one-tap TTS, Edit-gated admin, honest emergency copy, Flip split chrome, chime off by default, no analytics SDKs.

Biggest audience risks:

1. **Role confusion** — caregiver questions on the user’s speak board  
2. **Emergency access** — can disappear behind category filters; silent share is long-press only  
3. **Caregiver Flip** — chrome under 48dp; Yes/No dominate Wait/Repeat  
4. **Privacy copy** — absolute offline/no-upload claims vs backup, system STT, and chime behavior  

## Critical

| ID | Issue | Audience impact | Recommendation |
|---|---|---|---|
| BOARD-01 | Default Care cards are caregiver questions on the self-advocacy board | User may speak “Are you in pain?” as themselves | Keep Care prompts only in Two-Way; board uses first-person needs |
| EMER-01 | Silent bystander share is long-press only; no on-hero affordance | Shutdown + motor difficulty → can’t discover silent share | Primary **Show screen** on hero; Speak secondary |
| CARE-01 | Flip caregiver tools are ~36dp | Face-to-face path fails under stress | ≥48–56dp Flip chrome |
| SYS-01 | `allowBackup` / transfer may sync SharedPreferences despite “never leaves device” | AAC/medical phrases may hit cloud backup | Exclude sharedprefs from cloud backup (or `allowBackup=false`); document device transfer |

## High

| ID | Issue | Recommendation |
|---|---|---|
| BOARD-02 / EMER-08 | Top bar chrome competes with speak | Quieter chrome: Edit + overflow; de-emphasize Edit in speak mode |
| BOARD-03 / EMER-02 | Category filter can hide emergency hero (persisted) | Pin hero above filtered grid always |
| BOARD-04 | Quick-Speak `+` Save in communicate mode | Move save-to-card into Edit only |
| EMER-03 | Hero body text not stranger-scale on board | Larger hero type |
| EMER-04 | Fullscreen docks large Speak CTA | Default silent; Speak secondary |
| EMER-05 | No keep-screen-on in fullscreen | Keep awake while expanded |
| CARE-02 / CARE-12 | Yes/No visually dominate Wait/Repeat; emoji-led | Equal ~64dp text buttons |
| CARE-03 | Question picker mixes self-advocacy as “questions” | Care-only or tagged caregiver prompts |
| CARE-04 | Reply badge only on rotated user pane | Upright “Replied: …” on caregiver chrome |
| CARE-05 | Mic start clears prompt; no-match leaves blank | Clear only on real transcript; restore on no-match |
| CARE-06 | Outside-tap / back dismisses Two-Way | `dismissOnClickOutside=false`; explicit close |
| SYS-02 | Mic string claims audio never sent to any server | Align with Privacy Policy (system STT may network) |
| SYS-03 | “100% offline” in store/README/guide | Soften: private by design + honest model/STT caveats |
| SYS-04 | Chime bypasses silent mode + always vibrates | Respect ringer/DND; optional haptic; keep off-by-default |

## Medium / Low (summary)

Language switcher and Edit hit targets under 48dp; translation latency on first secondary speak; looping Listening pulse; caregiver tools inside prompt in non-Flip; chime on reply TTS; mandatory reply haptic; system STT sheet over HUD; dead purple template theme; dense settings dialog; unused Flip Back string; Quick-Speak always-on density; emoji emergency badge; TalkBack gaps.

## Strengths to keep

- Dark-neutral Material speak surface; mature Needs/Social copy  
- True one-tap card → TTS; Edit List gates FAB/reorder  
- Full-width amber hero + honest bystander phrase (when visible)  
- Flip: user content rotates, caregiver chrome stays upright  
- Dignified Wait/Repeat TTS (“Please give me a moment.”)  
- Chime and language switcher off by default; no analytics SDKs  
- Adult Play Store positioning vs pediatric AAC  

## Implementation backlog

### P0

- BOARD-01 — First-person Care on board / Care prompts only in Two-Way  
- EMER-01 — Primary Show screen on hero  
- BOARD-03 / EMER-02 — Sticky emergency hero  
- CARE-01 — Flip chrome ≥48dp  
- CARE-02 — Equal reply buttons  
- SYS-01 — Backup exclusions  
- SYS-02 / SYS-03 — Honest privacy/offline copy  

### P1

- BOARD-02 / BOARD-04 — Quieter speak chrome; Edit-only save-to-card  
- EMER-03 / EMER-04 / EMER-05 — Larger hero type; silent fullscreen default; keep screen on  
- CARE-03 / CARE-04 / CARE-05 / CARE-06 — Prompt picker role split; upright reply status; mic prompt restore; no outside dismiss  
- SYS-04 — Chime respects silent / optional haptic  

### P2

Remaining Medium/Low items (a11y content descriptions, theme cleanup, settings IA, haptic toggle, chip count, speaking indicator).

## Related

- Audience skill: [`.cursor/skills/sotto-aac-audience/SKILL.md`](../.cursor/skills/sotto-aac-audience/SKILL.md)  
- Plan: [plans/sotto-full-app-ux-audience-review.md](plans/sotto-full-app-ux-audience-review.md)  
- Mission: [CONTRIBUTING.md](../CONTRIBUTING.md)  
- Caregiver flows: [CAREGIVER_GUIDE.md](CAREGIVER_GUIDE.md)  
