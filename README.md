# HeartAudioCheck

Android application for offline heart sound analysis using microphone input.

⚠️ Not a medical device. For educational and exploratory purposes only.

## Features
- 30-second heart sound recording
- Envelope-based signal visualization
- Rhythm regularity analysis
- Approximate diagnostic pattern detection
- Offline processing (no cloud, no ML models)
- Multilingual UI (EN / UK / DE)

## Signal Processing Pipeline
- Band-pass filtering (20–200 Hz, 200–600 Hz)
- Envelope extraction
- Autocorrelation-based period estimation
- Peak detection
- HRV metrics (CV, RMSSD, pNN50)
- Murmur-like pattern scoring

## Technologies
- Kotlin
- Android SDK
- Custom DSP (FFT, autocorrelation)
- MVVM architecture

## Disclaimer
This application does not provide medical diagnosis.
