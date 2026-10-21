# Built APK Packages — Quest Rewards

This directory contains the ready-to-install Android Application Packages (APKs) for **Quest Rewards** (Version 1.0, VersionCode 1).

---

## 📦 Available Files

| File | Size | Type | Description |
| :--- | :--- | :--- | :--- |
| **`QuestRewards-v1.0-release.apk`** | ~24 MB | Release Build | Fully optimized, production-signed APK. Ideal for real-world device testing and direct distribution. |
| **`QuestRewards-v1.0-debug.apk`** | ~33 MB | Debug Build | Development APK signed with standard Android debug key. Includes logging and developer diagnostics. |
| **`SHA256SUMS.txt`** | Text | Checksums | Cryptographic SHA-256 integrity hashes for each APK file. |

---

## 📲 How to Install on Android

### Method 1: Direct File Download
1. In the AI Studio file explorer, right-click (or click options on) **`QuestRewards-v1.0-release.apk`** and download it to your computer or phone.
2. Transfer or open the APK on your Android device (Android 7.0 / Nougat API 24 or newer).
3. Tap the file to install (if prompted, enable *"Install unknown apps"* for your browser or file manager).

### Method 2: Via ADB (Android Debug Bridge)
If your phone is connected to your computer via USB debugging:
```bash
adb install -r QuestRewards-v1.0-release.apk
```

---

## 🛠️ How to Rebuild Locally

To rebuild either APK at any time from the project root:

- **Build Debug APK:**
  ```bash
  gradle :app:assembleDebug
  ```
  Output generated at: `app/build/outputs/apk/debug/app-debug.apk`

- **Build Release APK:**
  ```bash
  STORE_PASSWORD=android KEY_PASSWORD=android gradle :app:assembleRelease
  ```
  Output generated at: `app/build/outputs/apk/release/app-release.apk`

---

## 🚀 Automated CI/CD (GitHub Actions)

When pushed to GitHub, you can also build and create automated GitHub Releases by using the included workflow:
- File: `.github/workflows/release.yml`
- Triggers on version tags (e.g. `git tag v1.0 && git push origin v1.0`) or manually via GitHub Actions **Run workflow** button (`workflow_dispatch`).
- See `.github/RELEASE_GUIDE.md` for full instructions.
