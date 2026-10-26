# Soultemis

**A community fork of [Artemis](https://github.com/MobinYengejehi/Artemis), itself based on [Moonlight Android](https://github.com/moonlight-stream/moonlight-android).**

Soultemis is an independent experimental Android streaming client focused on providing a **virtual DualShock 4 touchpad** alongside a physical controller, particularly devices such as the GameSir X5 Lite. It is not an official Artemis, Moonlight, or Apollo release.

## Download

[**Download the latest Soultemis preview APK**](https://github.com/CapolinerSoul/Soultemis/releases)

The first public APK is **debug-signed** and provided for testing, not as a production-signed release. It uses a separate application ID (`com.limelight.soultemis`) so it can coexist with Artemis. Future production signing may require uninstalling a debug-signed build.

## Soultemis additions

- On-screen **DS4 touch surface**, including multi-finger movement and touchpad click, routed through Moonlight controller-touch messages to a compatible host (tested with Apollo).
- Visible finger-tracking feedback and touchpad-click state.
- A touchpad panel designed to support repositioning, resizing, and the HUD opacity setting.
- Soultemis touchpad settings for sensitivity, finger indicators, optional tap/long-press click, and overlay opacity.
- A compact grey floating touchpad toggle that can be repositioned and resized; it is hidden when Soultemis's Xbox mode is selected.
- Optional touch diagnostics for controller selection and touch-event status.
- When the touchpad interface is hidden, Artemis's normal screen-as-mouse behavior remains available.
- Independent app identity, label, and mascot-based launcher icon.

**Controller mapping:** Soultemis retains Artemis's multi-controller and merge-input options; it does not automatically decide which physical controller gets the virtual touch surface. Controller slot behavior should be verified for each setup.

**Xbox compatibility:** DS4 touch gestures require a virtual PlayStation-compatible controller. When Apollo emulates Xbox 360, DS4 touch events do not produce a DS4 touchpad. Soultemis's Xbox UI mode is a local choice, not a way to read Apollo's host setting remotely.

## Using Soultemis

1. Install and configure [Apollo](https://github.com/ClassicOldSong/Apollo) on the host PC.
2. Install the APK from [Releases](https://github.com/CapolinerSoul/Soultemis/releases) on Android.
3. Pair the client with Apollo, connect a GameSir or another compatible controller, and choose DS4 emulation on Apollo for touchpad testing.
4. Enable Soultemis's controller overlay to access the DS4 touchpad and its floating toggle. Hide it to use the screen as a mouse.
5. Configure touchpad behavior from Soultemis settings and customize positioning via the overlay editor.

## Source and build

The preview source lives on the `soultemis-preview` branch. Follow the original Artemis build prerequisites (Android SDK, NDK, Java and Gradle); this fork uses Artemis's Android project structure.

On Windows, build the non-root debug flavor:

```powershell
.\gradlew.bat :app:assembleNonRoot_gameDebug
```

## Credits and license

- Upstream Android streaming client: [MobinYengejehi/Artemis](https://github.com/MobinYengejehi/Artemis)
- Moonlight Android origins: [moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android)
- Host used for testing: [ClassicOldSong/Apollo](https://github.com/ClassicOldSong/Apollo)

This project is a modified fork, not affiliated with or endorsed by the original teams. Original copyright notices and license files must be retained. See [LICENSE.txt](LICENSE.txt) and included third-party licensing notices. Artemis distributes its code under the GNU General Public License version 3. Changes in Soultemis are distributed subject to the applicable upstream and third-party licenses.

For original Artemis documentation see [README.upstream.md](README.upstream.md).
