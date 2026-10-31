# CARTE debug APK signing — test only

GitHub hosted Android builds previously generated a different default debug key on every run. Those APKs cannot update one another on Android: identical package names with different signing certificates cause an installation failure.

The file `TEST_ONLY_debug_keystore.b64` contains an intentionally **public, disposable development key** (alias `androiddebugkey`, standard Android debug passwords). The CI job restores it as `app/ci-debug.keystore`, and Gradle explicitly signs debug builds with that file. CI also checks the SHA-256 certificate fingerprint inside the final APK, so a successful build cannot silently use a different key.

**Security:** this key is not secret. APKs signed with it are for local QA only and MUST NOT be distributed as trusted production software. Use a privately held production/release key and a different release signing configuration before publication.

**Migration:** the first APK using this key still cannot update any previously installed CARTE APK signed with the old random CI keys. Uninstall the old app once before installing the first stable-key debug build. This removes the previous local app preferences. Subsequent stable-key debug APKs can be installed as updates as long as their version codes do not decrease and package name stays the same.

Certificate identity is verified via the Android APK signing block. Never assume a successful build proves that two APKs can update each other.
