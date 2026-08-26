# LiquidPE v1.0 (2026-08-26)

This tag contains the buildable LiquidPE Android/Xposed module source snapshot.

## Build

```powershell
.\gradlew.bat :app:test :app:assembleRelease --offline --no-daemon
```

The release signing keystore and `signing/keystore.properties` are intentionally excluded. Configure a local signing block before producing a distributable signed APK.

Verified local signed APK:

- Path: `app/build/outputs/apk/release/app-release.apk`
- SHA-256: `892B0F75E0D1402C18E74EBEDA28574658F361D2F6F9CB2BED6B79EB44AC1F5E`
- APK Signature Scheme: v2
