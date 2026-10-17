# SurroundSound — a native Android studio for found sound

**SurroundSound** is a native Android music studio built with **Jetpack Compose**, **Kotlin**, and a
custom real-time **DSP engine** (`AudioTrack` / `AudioRecord`). You capture or load a found sound —
a desk pound, a spoon clink, a knuckle tap, a glass bottle — and play it back as a six-voice
instrument with a 16-step sequencer, effects and a 4-bar WAV export.

The look comes from the SurroundSound theme design: one shell, three moods, drawn from tokens.

---

## 🎨 Themes

| Mood | Palette | Motif | Type |
| --- | --- | --- | --- |
| **Mossroom** (default) | Deep Hollow — forest greens, moss, amber | botanical | display serif · sans · mono numerals |
| **Pixel Hearth** | Meadow — daylight cream, loud primaries | whole-pixel, hard offset shadows | Silkscreen everywhere |
| **Sunday Analog** | Warm parlor — cream, dark wood, brass | paper and inked rules | display serif · sans · mono numerals |

- Every colour, corner radius, border width and font comes from one token set
  (`ui/theme/StudioTheme.kt`), so a theme swap is a single state change — no per-screen branching.
- The picker (**Tune icon**, top bar) shows a drawn mini studio per mood, its swatches, and
  Preview / Select / Active. Previewing borrows the mood until you **Apply**; the top bar becomes a
  `PREVIEWING / <mood> / CANCEL / APPLY` banner while it lasts.
- The choice persists in `SharedPreferences` (`surroundsound.studio`, key `theme`) and the system
  bars and window background follow it, so a light mood never shows dark bars.

## 🛠️ The studio

- **Timeline** — six rows (Kick, Snare, Hi-hat, Percussion, Melody, Bass). Steps are 16 to a bar in
  four beats, the playhead is drawn on a single canvas so it sweeps instead of jumping, and the
  selected cell is ringed while stopped. Melodic rows carry a MIDI note strip for the current scale.
  Rows have **M / S** (mute and solo) and tap-to-audition.
- **Sample** — record with the mic (12 s cap, peak and transient detection, quiet takes rejected
  with a reason) or load one of four built-in foley sources. Waveform with grab handles for the
  trim, Auto-detect to the first impact, and Play raw / Play trimmed auditions.
- **FX** — musical scale plus six dials: filter cutoff (log 200 Hz…16 kHz), distortion, delay time
  in sixteenths of the current tempo, delay mix, reverb mix and swing. Muted rows still export.
- **Pads** — a 2 × 3 board that fires on touch-down, one pad per voice, each in its theme colour.
- **Export** — four bars rendered offline through the same voices and effects you hear
  (44.1 kHz, 16-bit stereo WAV). *Export WAV* writes to `Android/data/com.example.surroundsound/files/renders`,
  *Share* hands the file to the system share sheet.

**Layout** — one column, capped at 640 dp and centred, on a phone in either orientation. In
landscape the FX rack splits into two racks; the header keeps the top bar and the tabs so the
transport and the four stations are always one tap away. Edge-to-edge insets, display cutouts and
font scaling are handled throughout.

## ✨ Engine notes

- Recording converts 16-bit PCM to normalised floats, auto-detects the transient and normalises the
  take, so a soft desk bang still drives the drums.
- The sequencer posts its step callbacks on the main thread; voices live in a
  `CopyOnWriteArrayList` and pattern edits deep-copy, so editing while playing is safe.
- Export reuses the same `renderWav` path, so a bounce matches the audition.

---

## 🚀 Open and run

1. Open **Android Studio**.
2. **File > Open…** and choose:
   ```
   /home/tt/Desktop/SurroundSound
   ```
3. Let Gradle sync, pick a device or emulator, and **Run (▶)**.

## 🛠️ Command line

```bash
cd /home/tt/Desktop/SurroundSound
./gradlew assembleDebug testDebugUnitTest
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### What still needs a device

`StudioFormatsTest` covers the read-outs (timecodes, delay divisions, cutoff labels) on the JVM.
These need a real phone before they can be signed off:

- a live mic take (desk bang) end to end, and a take that hits the 12 s cap,
- playhead smoothness while the sequencer runs,
- insets and touch targets in both orientations, at 1.5× font scale and with 3-button navigation,
- switching moods, and that the choice survives a restart.
