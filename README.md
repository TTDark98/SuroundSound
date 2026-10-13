# SurroundSound - Professional Landscape DAW Studio for Android

**SurroundSound** is a high-performance native Android music studio and digital audio workstation (DAW) designed specifically for **landscape mode**, built using **Jetpack Compose**, **Kotlin**, and a custom real-time **DSP Audio Engine** with `AudioTrack` and `AudioRecord`.

---

## 🎛️ What's New in This Version

1. **Fixed Microphone Recording**:
   - Live microphone recording now properly captures audio into memory, converts 16-bit PCM to normalized floating-point samples, performs transient onset auto-detection, and immediately loads the audio so all 6 voices (Kick, Snare, Hi-Hat, Perc, Melody, Bass) play your recorded sound!

2. **Native Landscape Mode**:
   - Screen orientation locked to `sensorLandscape` in `AndroidManifest.xml`.
   - Layout optimized for wide touchscreens and tablets, mimicking professional DAWs (Ableton Live, FL Studio Mobile, Roland TR-8S, Koala Sampler).

3. **Complete UI Revamp (Professional DAW Aesthetic)**:
   - **Obsidian / Carbon Studio Theme**: High-contrast matte panels with glowing neon LEDs (Neon Emerald, Electric Amber, Electric Cyan, Neon Violet, Synth Pink).
   - **Pro Master Transport Bar**:
     - Brand title: `SURROUND SOUND PRO`
     - Style dropdown: Instant switching between Lo-Fi, Trap, House, Afrobeat, and Synthwave
     - Prominent illuminated Play/Pause button (green glow)
     - Dedicated Stop and Record (red pulse) buttons
     - Digital BPM box with increment / decrement steppers
     - View switchers: `[ 🎛 TIMELINE ]`, `[ 🌊 SAMPLE ]`, `[ 🎚 FX RACK ]`, `[ 🎹 MPC PADS ]`, `[ ⚡ FULL STUDIO ]`
     - Export Master button
   - **Timeline Channel Strips**:
     - Dedicated channel header for each instrument (Kick, Snare, Hi-Hat, Perc, Melody, Bass) with colored LED tag, `[ M ]` Mute button, `[ S ]` Solo button, and instant audition tap.
     - 16-step grid arranged into 4 musical beats with alternating beat backgrounds.
     - Running playhead sweeps smoothly across the entire grid during playback.
     - Melody channel shows note intervals (`Root`, `+3`, `+7`, `+12`, etc.) directly inside the step button with tap-to-cycle.
     - Clear Pattern and Randomize Pattern action buttons with clean icons and clear text.
   - **Waveform & Slicer Station**:
     - High-definition Canvas waveform with illuminated active slice range.
     - In/Out trim sliders and transient markers.
     - "Auto Transient", "Play Raw", and "Play Trimmed" audition buttons.
   - **Acoustic Foley Board**:
     - Tactile cards for *Hardwood Desk Pound*, *Steel Spoon Clink*, *Finger Snap & Knuckle Tap*, and *Resonant Glass Bottle*.
   - **Modular FX Rack**:
     - Analog Lowpass Filter, Tube Saturation/Drive, Stereo Tape Delay, Studio Reverb, and Harmonic Scale Quantizer.
   - **MPC Finger-Drumming Pads**:
     - 6 large responsive touch pads with glowing neon rims and haptic touch feedback.
   - **WAV Master Audio Export**:
     - 4-bar studio master offline render (44.1kHz 16-bit Stereo) with Android Share Sheet integration.

---

## 🚀 How to Open and Run in Android Studio

1. Open **Android Studio**.
2. Select **File > Open...**.
3. Choose the folder:
   ```
   /home/tt/Desktop/bangtobeat/SurroundSound
   ```
4. Click **OK**.
5. Once Gradle Sync finishes, select your connected Android device or emulator (in landscape orientation) and click **Run (▶)**.

---

## 🛠️ Command-Line Build

```bash
cd /home/tt/Desktop/bangtobeat/SurroundSound
./gradlew assembleDebug
```
The compiled APK is available at:
```
app/build/outputs/apk/debug/app-debug.apk
```
