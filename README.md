# imperium

Minimal Bluetooth HID mouse controller for Windows PCs from Android. Zero PC companion software required.

[Download the signed APK](https://github.com/xklob/imperium/releases/latest) · Android 9 or newer

## Features

- **Bluetooth HID Mouse (Zero-Companion):** Uses Android's native `BluetoothHidDevice` profile so Windows recognizes your phone as a standard hardware Bluetooth mouse.
- **Gestures:**
  - **1-finger slide:** Move pointer (adjustable speed + acceleration).
  - **1-finger tap:** Left click.
  - **1-finger press & hold:** Left click-and-hold (drag while holding, release on lift).
  - **1-finger double-tap:** Toggle **Drag Lock** (locks left click down so you can drag/ratchet across the screen; single taps are ignored while locked; double-tap again to release). Active Drag Lock highlights the screen border with a glowing neon red outline.
  - **2-finger tap:** Right click.
  - **2-finger double-tap:** Double click.
  - **Edge scroll bar:** Always-present vertical scroll bar on the right or left edge (configurable in Settings, with adjustable scroll speed and natural scroll toggle).
- **Standard Design:** Follows `design.klob.io` (Paper/Ink neutral surfaces, Paradigm green accents, bundled Space Grotesk with OFL license, System/Light/Dark appearance picker).

## Pairing With Windows

1. Open **imperium** and grant Bluetooth permission.
2. Tap **CONNECT PC** → **MAKE DISCOVERABLE**.
3. On Windows, open **Settings → Bluetooth & devices → Add device → Bluetooth** and pair your phone.
4. Back in **imperium**, tap your PC in **Bluetooth Hosts** to connect.

## Development

Kotlin, Jetpack Compose, Android SDK 36, Java 17, and Gradle 8.13.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

### Signed Releases

Set `IMPERIUM_SIGNING_PROPERTIES` to a private properties file containing `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`:

```sh
IMPERIUM_SIGNING_PROPERTIES=/home/caleb/.config/imperium/signing.properties ./gradlew assembleRelease lintRelease
```
