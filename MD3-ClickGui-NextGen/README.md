# MD3 Click GUI NextGen

Landscape-only Android Compose project targeting SDK 37. The Material 3 inspired workspace groups the implemented Scaffold, Theme, and Language modules into a three-column category, module, and settings layout.

## Open and run

1. Open this folder in Android Studio Ladybug or newer.
2. Allow Gradle to sync and select an API 37 emulator or landscape device.
3. Run the `app` configuration.

For a command-line build, use the committed wrapper (JDK 17 or newer, `JAVA_HOME` set):

```powershell
./gradlew.bat :app:assembleDebug
```

`gradle-here.ps1` is a convenience wrapper for machines that do not have `JAVA_HOME` or
`ANDROID_HOME` configured: it locates a JDK, the Android SDK, and a Gradle distribution, writes
`local.properties`, and forwards its arguments to Gradle. It is not required — `gradlew.bat` is the
supported entry point. Note that this project's Gradle up-to-date checks can report `UP-TO-DATE`
for freshly edited sources on some machines; pass `--rerun-tasks` (or `-Rerun` with the wrapper)
when a change does not appear in the build output.

The main surface is a Material 3 Expressive ClickGUI in `app/src/main/java/com/example/md3clickgui/ui/screens/ClickGuiScreen.kt`. The screen provides the floating container while the stateful window, category rail, and module cards are reusable components. Module metadata and settings live in independent definitions under `ui/modules`.

## Package structure

- `ui/screens/ClickGuiScreen.kt` owns only the outer floating ClickGUI container and theme callbacks.
- `ui/components/ClickGuiWindow.kt` arranges category navigation, a searchable module list, and the selected module's settings in three independent columns.
- `ui/components/CategoryRail.kt` and `ui/components/ModuleCard.kt` render reusable navigation and module controls.
- `ui/modules/` contains one definition per module (`ScaffoldModule`, `ThemeModule`, `LanguageModule`, plus the music modules) and the ordered `guiSections` catalog.
- `ui/model/GuiModels.kt` contains shared setting and section data structures, with no concrete module list.
- `ui/theme/` contains the Material 3 color scheme and shared tokens.
- `music/` contains the NetEase-backed player, its foreground playback service, and the session/recent stores.
- `MainActivity.kt` is only the Android entry point.

The three-column workspace is the main surface, with an optional ArrayList HUD overlay drawn above it. The former dynamic-island port (`com.mchack.island`) was removed on 2026-10-06.

## Compact workspace

- The category rail starts expanded on wider landscape screens and uses an icon rail below 720dp of workspace width. The menu button collapses it on wider screens; Config stays anchored at the bottom.
- Search shares the module heading row and matches both original and translated module names across categories. Selecting a category clears the search and opens its first available module after login.
- Selected modules use the theme accent. Navigation rows are 40dp, module rows have a 56dp minimum height, and settings use 48dp (56dp for sliders) with 4dp gaps and consistent label/control columns.
- Dropdown menus reveal from their anchor without shifting settings. Floating-button and language choices use sliding segments. Sliders show inline values and a drag tooltip; extension length snaps to whole blocks. The accent-color preview opens the existing picker.
- Navigation, modules, and settings scroll separately. Theme colors, login, configurations, floating buttons, and panel resizing remain available.
- Category labels and counts fade during rail expansion/collapse; one continuous selection background slides across row gaps, and the anchored Music/Config rows at the bottom of the rail are excluded from that slide. The module column keeps its width while the detail area gains the space released by the rail. Panel, section, and row transitions all use the Material 3 motion scheme (`MotionScheme.standard()`) — spring tokens rather than hand-tuned durations; see `ui/theme/NexusMotion.kt` for the token table and the per-call-site tier mapping. Click handlers commit state immediately, independently of these animations.
- Material ripple effects are disabled at the theme level. Compact switches keep a 48dp touch target around a 40×24dp track, sliders use an 8dp track and 24dp thumb, and dropdowns are capped at 180dp. Module enable switches appear only in the module list.

The [reference study](docs/reference-ui-study.md) records measured video timestamps, layout proportions, implementation choices, and validation status. The extracted reference frames and their annotated frame viewer are kept outside version control: the frames are large and can be regenerated from the sources listed in that study.
