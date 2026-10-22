# Profile UI Contract

## Scope and references

This is the native Android user-interface contract for
[Phase 1](../spec.md), using the [mock screens](../ui-mockups.html) as a visual reference.
Recommend Material 3 for Jetpack Compose; do not use React MUI or embed the HTML in a
WebView. No network API or new persisted entity is introduced by this contract.

## States and transitions

| State | Visible behavior | Transition |
|---|---|---|
| Loading | Announce that the local profile is loading; do not show setup prematurely | Existing profile opens Saved; absence opens Setup; failure opens Load error |
| Load error | Explain that the profile could not be read and offer retry; do not treat failure as absence | Retry returns to Loading |
| Setup (mock 01) | Name and currency fields, optional appearance action, create button | Invalid submission stays in Setup with field errors; valid submission enters Saving |
| Personalization (mock 02) | Optional Material bottom sheet with named avatar and color choices | Apply updates draft appearance; skip uses default appearance; dismiss keeps prior selection; all return to Setup |
| Saving | Progress state with repeated submission blocked; preserve the draft | Confirmed local save opens Saved; failure opens Save error |
| Save error (mock 03) | Retain draft values and selections; explicitly state profile was not created | Retry attempts the same draft without requiring re-entry |
| Saved (mock 04) | Show persisted name, currency, and optional appearance | Reopening loads the same identity and does not create a second profile |

The failure mock is a state of the same setup flow, not a separate profile. Its sample
storage advice is illustrative: implementation copy must match the actual failure and
must not claim that low storage caused an unknown error. Saved status requires a committed
local record, not a button click or completion of an animation.

## Components and presentation

- Use Material 3 `Scaffold`/`TopAppBar`, `OutlinedTextField`, a locally populated currency
  picker, primary `Button`, and secondary `TextButton` where appropriate.
- Present optional appearance as `ModalBottomSheet`; provide named, selectable controls
  for avatars and colors with a non-color selection indicator.
- Use a flat error `Surface` for persistent retry guidance and `OutlinedCard` for the
  saved profile. Do not rely on a transient snackbar as the only save-failure feedback.
- Drive rendering through the existing MVI ViewModel state and intents. Composables must
  not directly create profiles or maintain a competing persisted source of truth.
- Render display names as text. Built-in appearance options require no network or
  camera/photo permission. Profile identity remains stored, not exposed as a setup field.

## Accessibility, localization, and adaptation

- Localize labels, validation, loading, status, and error copy. Store currency identity
  separately from localized display labels.
- Provide at least 48 dp touch targets, meaningful TalkBack labels, selected state,
  logical focus order, and announced errors/progress.
- Use Material theme roles with verified text contrast: at least 4.5:1 for normal text
  and 3:1 for large text. Do not communicate status or selection through color alone.
- Adapt to available width, right-to-left layouts, large system text, and keyboard
  insets. Scroll content as necessary; do not enforce the mockup's 390 x 844 dimensions.
- Keep the flat monochrome/sage visual direction; system bars are managed by Android,
  not reproduced from the HTML mockup.

## Validation

Compose tests cover required-field errors, apply/skip/dismiss behavior, loading, read
failure, duplicate-submit prevention, save retry retaining values, and existing-profile
rendering. Validate localization and accessibility as described in
[quickstart.md](../quickstart.md). The HTML demo's in-memory behavior proves neither Room
persistence nor secure storage.
