# Plan: Port shitless-launcher to Zig (Android, Zero Java/C)

## Context
Port the existing Kotlin/Jetpack Compose Android launcher to pure Zig as a learning exercise.
- Zero Java/Kotlin source files authored by us
- Zero third-party C library dependencies (bionic libc/libandroid/liblog are OS-provided, not shipped)
- No icons — display only app labels
- Use `android.app.NativeActivity` (built into Android, referenced in manifest but not written by us)
- Use `pm`/`am` shell commands + APK parsing instead of Java PackageManager APIs

## New Project Location
`shitless-launcher-zig/` (sibling directory to current project)

## File Structure
```
shitless-launcher-zig/
  build.zig          — cross-compile libmain.so for aarch64-linux-android
  build.zig.zon      — Zig package manifest
  Makefile           — aapt2 compile/link, zipalign, apksigner, adb deploy
  AndroidManifest.xml
  res/values/strings.xml
  src/
    main.zig         — ANativeActivity_onCreate, event loop, app thread
    render.zig       — ANativeWindow raw pixel buffer renderer
    input.zig        — touch + hard key event handling
    ui.zig           — screen state machine (pinned / search), layout logic
    font.zig         — bitmap font glyph blitter
    apps.zig         — app list loading, filtering, scoring
    axml.zig         — binary AXML parser (Android binary XML)
    arsc.zig         — resources.arsc parser (resource ID → label string)
    usage.zig        — parse `dumpsys usagestats` for foreground time
    persist.zig      — file-based storage for pinned list + label cache
    shell.zig        — std.process.Child wrapper for pm/am/dumpsys
    time.zig         — clock_gettime, format HH:MM
    battery.zig      — read /sys/class/power_supply/battery/capacity
    log.zig          — __android_log_write wrapper for logcat
  tools/
    gen_font.zig     — offline: converts Terminus BDF → src/font_data.zig
```

## Key Technical Decisions

### APK + Manifest (no Java)
- `android:hasCode="false"` in manifest — no Dalvik bytecode
- `android.app.lib_name` meta-data points to `libmain` (.so)
- `CATEGORY_HOME` + `CATEGORY_DEFAULT` register as launcher
- APK assembled manually: `aapt2 compile → aapt2 link → copy .so → zipalign → apksigner`

### App Discovery Pipeline
1. `pm list packages -f -e` → package names + APK file paths
2. For each APK (ZIP): `std.zip` → extract `AndroidManifest.xml` + `resources.arsc`
3. `axml.zig`: parse binary AXML → find `android:label` resource ID + main activity class name
4. `arsc.zig`: parse resources.arsc → resolve resource ID → label string
5. Cache results in `/data/data/com.shitless.launcher/files/label_cache.tsv` (keyed by APK mtime)

### Launching Apps
```
am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n <pkg>/<activity>
```

### UI Rendering
- Raw ANativeWindow pixel buffer (no OpenGL — overkill for text lists)
- Terminus bitmap font: `tools/gen_font.zig` generates `src/font_data.zig` at build time
- Pixel format: query `ANativeWindow_getFormat()`, support RGBA_8888 / RGBX_8888 / RGB_565

### Zig Libraries Used (all stdlib or pure Zig)
| Need | Solution |
|---|---|
| ZIP | `std.zip` (Zig stdlib 0.13+) |
| PNG/icons | Not needed |
| Subprocess | `std.process.Child` |
| Threading | `std.Thread` + `std.Thread.Mutex` |
| Atomic flags | `std.atomic.Value` |

### Known Limitation: Soft Keyboard
NativeActivity has poor IME support — `InputConnection` requires Java. Search bar will support hard key events only (physical keyboards, some virtual keyboards). Soft keyboard support would require a thin JNI call; deferred.

## Features to Port
- Two screens: Pinned (default) and Search
- Search/filter by label substring
- Pin/unpin on long press
- Usage time display (from `dumpsys usagestats`)
- Sort by usage score
- Bottom action bar: phone, search, camera (text labels, no icons)
- Time + battery in header
- Cursor blink in search bar
- Momentum scroll

## Phased Delivery

### Phase 1 — Toolchain + Black Screen
- `build.zig` produces `libmain.so` for ARM64
- Makefile assembles + installs APK
- `main.zig` exports `ANativeActivity_onCreate`, spawns app thread, renders black
- *Goal: phone shows black screen, no crash*

### Phase 2 — Text Rendering
- `tools/gen_font.zig` + `font.zig` + render "Hello World" + timestamp
- *Goal: text visible on screen*

### Phase 3 — Input + Scrolling
- `input.zig`: tap, scroll, back key
- Scroll a hardcoded app list
- *Goal: scrollable list responds to touch*

### Phase 4 — Real App List
- `shell.zig`, `axml.zig`, `arsc.zig`, `apps.zig`
- Background thread loads apps, atomic flag triggers re-render
- Label cache in `persist.zig`
- *Goal: real app labels from all installed apps*

### Phase 5 — Usage + Scoring
- `usage.zig` parses `dumpsys usagestats`
- Sort by today's foreground time
- Duration formatted as HH:MM:SS

### Phase 6 — Full Feature Parity
- Two screens, pin/unpin, search filter, action bar, clock, battery

## Critical Reference Files (do not modify)
- `app/src/main/kotlin/com/shitless/launcher/LauncherViewModel.kt` — business logic source of truth
- `app/src/main/kotlin/com/shitless/launcher/LauncherApp.kt` — screen state machine + lifecycle
- `app/src/main/kotlin/com/shitless/launcher/screens/PinnedScreen.kt` — pinned screen layout
- `app/src/main/kotlin/com/shitless/launcher/DesignTokens.kt` — color/spacing constants to replicate
- `app/src/main/AndroidManifest.xml` — reference for permissions + intent filters

## Verification
1. `zig build` → produces `zig-out/lib/libmain.so` for aarch64
2. `make package` → produces signed APK
3. `make deploy` → installs + launches on connected device
4. Device shows launcher; pressing Home returns to it
5. All installed apps appear with correct labels
6. Search filters correctly; long press pins/unpins
7. `adb logcat -s shitless-launcher` shows no errors
