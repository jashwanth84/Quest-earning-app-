# GitHub Actions Release Workflow Guide

This repository includes a production-ready GitHub Actions workflow (`.github/workflows/release.yml`) to automatically build and release the **Quest Rewards** Android app.

---

## 🚀 Features

- **Automated Triggers**:
  - Automatically runs on git tags (e.g. `git tag v1.0.0 && git push origin v1.0.0`).
  - Automatically runs on pushes to `main` or `master`.
  - Manual trigger via **Actions > Build & Release APK > Run workflow** (`workflow_dispatch`).
- **Build Outputs**:
  - **Release APK**: `QuestRewards-v{version}-release.apk` (ready for direct sideloading or distribution).
  - **Release AAB**: `QuestRewards-v{version}-release.aab` (Android App Bundle for Google Play Console).
  - **Debug APK**: Optional via manual workflow selection.
  - **SHA256 Checksums**: `SHA256SUMS.txt` for integrity verification.
- **Dual Distribution**:
  - **Workflow Run Artifacts**: Uploaded directly to the Actions run summary (kept for 90 days).
  - **GitHub Releases**: Automatically publishes a GitHub Release with attached APK, AAB, and release notes.
- **Zero Configuration Setup**:
  - If no custom keystore secret is configured, the workflow automatically generates a release signing keystore on the fly with the required alias `upload`.
  - Also seamlessly supports production keystores stored in GitHub Secrets.

---

## 🔑 Optional: Configuring Custom Release Keystore in GitHub Secrets

To sign releases with your own production keystore:

1. Convert your keystore file to base64:
   ```bash
   base64 -w 0 my-upload-key.jks > keystore_base64.txt
   ```
2. In your GitHub repository, go to **Settings > Secrets and variables > Actions > New repository secret**.
3. Add the following repository secrets:
   - `KEYSTORE_BASE64`: Paste the content of `keystore_base64.txt`.
   - `STORE_PASSWORD`: Keystore password.
   - `KEY_PASSWORD`: Key password for alias `upload`.
   - `GEMINI_API_KEY`: *(Optional)* Your Gemini API key for AI features.
   - `GOOGLE_SERVICES_JSON`: *(Optional)* Content of `google-services.json` if not tracked in git.

---

## 📦 How to Trigger a Release

### Option 1: Git Tag (Recommended for official versions)
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Option 2: Manual Trigger in GitHub UI
1. Go to the **Actions** tab in your GitHub repository.
2. Select **Build and Release Android APK** from the left sidebar.
3. Click **Run workflow**.
4. Choose the build variant (`release`, `debug`, or `both`) and click **Run workflow**.
