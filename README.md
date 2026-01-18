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






MIT License

Copyright (c) 2026 Vitalii Reshetnyk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
