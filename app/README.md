# 🎵 DynamicLock

> **Your music. Your lock screen.**

DynamicLock is a free and open source Android app that automatically updates your lock screen wallpaper with the album art of the song currently playing on Spotify. Every song, a new wallpaper — no effort needed.

---

## ✨ How It Works

1. Install the app
2. Grant Notification Access
3. Play any song on Spotify
4. Your lock screen wallpaper automatically changes to the album art
5. When you close Spotify, the wallpaper resets back to default

That's it. No setup. No fuss.

---

## 🔒 Privacy & Safety

**We take your privacy seriously. Here's our promise:**

| | |
|---|---|
| ✅ | No data is collected |
| ✅ | No internet permission required |
| ✅ | Nothing is sent to any server |
| ✅ | Works 100% offline on your device |
| ✅ | No ads, no tracking, no analytics |
| ✅ | No account required |

DynamicLock only uses **Notification Access** to read Spotify's album art from your local media session. It does not read, store or transmit any of your notifications or personal data.

**Don't trust us? Good. Read the code yourself. Every single line is open source.**

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

## 🛠️ Build It Yourself

Want to build DynamicLock from source? Here's how:

### Prerequisites
- Android Studio (latest version)
- JDK 17 or higher
- Android SDK (API 36)
- Git

### Clone the Repo
```bash
git clone https://github.com/mclovin22117/dynamic-lockscreen-wallpaper.git
cd dynamic-lockscreen-wallpaper
```

### Build & Install
```bash
# Debug build
./gradlew installDebug

# Or generate an APK
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Or Open in Android Studio
1. Open **Android Studio**
2. Click **"Open"** and select the cloned folder
3. Let Gradle sync
4. Click **▶ Run**

---

## 🍴 Fork It

Want to make your own version?

1. Click **Fork** on the top right of this page
2. Clone your fork
```bash
git clone https://github.com/YOUR_USERNAME/dynamic-lockscreen-wallpaper.git
```
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

```
MIT License

Copyright (c) 2026 mclovin22117

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software to deal in the Software without restriction, including without
limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software.
```

---

<p align="center">Made with ❤️ by <a href="https://github.com/mclovin22117">mclovin22117</a></p>