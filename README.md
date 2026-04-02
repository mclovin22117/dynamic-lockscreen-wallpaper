# 🎵 DynamicLock

> **Your music. Your lock screen.**

DynamicLock is a free and open source Android app that automatically updates your lock screen wallpaper with high-quality album art of the song currently playing on Spotify. Every song, a new wallpaper — no effort needed.

---

## ✨ How It Works

1. Install the app
2. Grant Notification Access
3. Play any song on Spotify
4. The app fetches high-quality album art from Spotify via a secure backend
5. Your lock screen wallpaper automatically updates with a beautiful blurred background + centered album art
6. When you close Spotify, the wallpaper resets back to default

---

## 🏗️ Architecture

DynamicLock uses a **backend-first approach** for high-quality album art:

```
Phone App → Backend Server → Spotify Web API → Album Art (high-res)
```

- **Android App**: Detects playing song, requests album art from backend
- **Backend**: Holds your Spotify credentials securely, exchanges them for Spotify access token, fetches album metadata
- **Spotify Web API**: Returns the highest-quality album image available

This ensures:
- ✅ Your Spotify credentials are **never** embedded in the APK
- ✅ Album art is always the **highest quality** available
- ✅ No user login required
- ✅ Fast, cached responses

---

## 🔒 Privacy & Safety

**We take your privacy seriously. Here's our promise:**

| | |
|---|---|
| ✅ | No user data collected |
| ✅ | No tracking or analytics |
| ✅ | No ads |
| ✅ | Your Spotify credentials never leave your backend server |
| ✅ | Backend only fetches public album metadata |
| ✅ | Open source — audit it yourself |

**What gets sent to the backend?**
- Only the Spotify track ID (e.g., `3n3Ppam7vgaVa1iaRUc9Lp`)
- Your backend then exchanges your Spotify Client ID/Secret for an access token and fetches album metadata

**What about the host running the backend?**
- If you deploy the backend on your own server, only you see the traffic
- If you use a third-party host, follow their privacy policy
- Spotify album metadata is public; no private user data is fetched

---

## 📖 Open Source & Free

DynamicLock is and will always be:
- 🆓 **Free** — no paid version, no premium tier
- 🔓 **Open Source** — full source code available
- 🚫 **No Ads** — forever
- 🤝 **Community Driven** — contributions are welcome

---

## 📱 Requirements

- Android 7.0 (API 24) or higher
- Spotify app installed
- Notification Access permission

---

## 🛠️ Local Development Setup

Want to contribute or test DynamicLock locally? Follow these steps:

### Prerequisites
- **Backend**: Python 3.8+, pip
- **Android**: Android Studio (latest), JDK 17+, Android SDK (API 36)
- **Spotify**: Developer account (free) with Client ID and Secret
- **Device**: Android phone (7.0+) connected via USB, or Android emulator

### Step 1: Get Spotify Credentials

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Create a new app (accept terms, fill in details)
3. Copy your **Client ID** and **Client Secret**
4. Keep these secure — never commit them to git

### Step 2: Clone & Set Up Backend

```bash
git clone https://github.com/mclovin22117/dynamic-lockscreen-wallpaper.git
cd dynamic-lockscreen-wallpaper/backend

# Create virtual environment
python3 -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Copy example env file
cp .env.example .env

# Edit .env and paste your Spotify credentials
# SPOTIFY_CLIENT_ID=your_id_here
# SPOTIFY_CLIENT_SECRET=your_secret_here
nano .env
```

### Step 3: Start Backend

```bash
# From backend/ folder with .venv activated:
python -m uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000
```

You should see:
```
Uvicorn running on http://0.0.0.0:8000
```

Test it:
```bash
curl http://localhost:8000/health
# Should return: {"status":"ok"}
```

### Step 4: Configure Android App

```bash
cd ../  # Back to project root

# Edit local.properties and add:
# spotify.cover.api.baseUrl=http://127.0.0.1:8000
# spotify.cover.api.authToken=
nano local.properties
```

### Step 5: Build & Install Android App

```bash
# Connect phone via USB and enable USB debugging
# Then:
adb reverse tcp:8000 tcp:8000  # Forward localhost:8000 to phone

# Build and install debug APK
./gradlew installDebug
```

### Step 6: Test

1. Open app on phone
2. Grant **Notification Access** in settings
3. Play a song on Spotify
4. Lock screen wallpaper should update within seconds
5. Check logs:
```bash
adb logcat | grep DynamicLock
```

Look for:
```
Using backend cover art: 640x640
Lock screen wallpaper updated successfully
```

---

## 📦 Build Release APK (Local Testing Only)

To generate a signed APK for testing:

```bash
# Make sure your backend is deployed to a public URL first
# Then update local.properties:
# spotify.cover.api.baseUrl=https://your-api-domain.com
# spotify.cover.api.authToken=your_token_if_needed

# Build release APK
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk

# Or install directly
./gradlew installRelease
```

---

## 🚀 Production Deployment

For real-world use, you'll need:

1. **Deploy Backend** to a public server (Railway, Render, Fly.io, AWS, VPS, etc.)
   - Set environment variables on the server
   - Use HTTPS only
   - Add basic rate limiting and logging

2. **Update Android App** to use your backend URL:
   - Build release APK
   - Distribute via GitHub Releases or F-Droid

3. **Optional**: Add backend authentication token if deploying publicly

*Full production guide coming soon.*

---

## 🍴 Contributing

Found a bug? Have an idea? Contributions are welcome!

1. Fork this repo
2. Create a feature branch (`git checkout -b feature/your-idea`)
3. Make your changes
4. Test locally (follow dev setup above)
5. Commit and push
6. Open a Pull Request

**Before contributing:**
- Keep Android code in `app/`
- Keep backend code in `backend/`
- Never commit `.env` or `keystore.properties`
- Follow existing code style (Kotlin + Python conventions)
- Test on multiple Android versions if possible

---

## 📄 License

MIT License — feel free to use this code for anything.

---

## 🔗 Links

- **Spotify Developer**: https://developer.spotify.com
- **Android Studio**: https://developer.android.com/studio
- **Backend Framework**: FastAPI (https://fastapi.tiangolo.com)

---

## 💬 Support

Have questions? Issues? Suggestions?

- Create an [Issue](https://github.com/mclovin22117/dynamic-lockscreen-wallpaper/issues)
- Check existing discussions first

---

Made with ❤️ for music lovers everywhere.
3. Create a new branch
```bash
git checkout -b feature/your-feature-name
```
4. Make your changes and push
```bash
git add .
git commit -m "Add: your feature"
git push origin feature/your-feature-name
```
5. Open a **Pull Request** — contributions are always welcome!

---

## 🐛 Found a Bug?

Open an issue on GitHub:
👉 [github.com/mclovin22117/dynamic-lockscreen-wallpaper/issues](https://github.com/mclovin22117/dynamic-lockscreen-wallpaper/issues)

Please include:
- Your Android version
- Your device model
- Steps to reproduce the bug
- Relevant logs (`adb logcat -s "DynamicLock"`)

---
## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">Made with ❤️ by <a href="https://github.com/mclovin22117">mclovin22117</a></p>