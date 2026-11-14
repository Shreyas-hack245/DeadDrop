# DeadDrop

DeadDrop is a native Android treasure-hunt prototype built with Kotlin, Jetpack Compose, and Material 3. The project is organized into feature, data, and theme packages. It opens directly in an offline demo so an API key or wallet is not needed to explore the experience.

## Run the demo

1. Install Android Studio with Android SDK Platform 36 and JDK 17 or newer.
2. Open this folder as an existing Gradle project and let Gradle sync.
3. Run the `app` configuration on an Android 8.0+ device or emulator.
4. Choose **Start exploring**. Tap a signal on the map, review its clue, grant location permission or explicitly simulate demo arrival, use the seeded QR token or sample QR button, and solve the sample answer `MOON`.
5. The reward is saved to the in-memory demo collection for the current app session.

The seeded San Francisco coordinates and distances are illustrative. The map in demo mode is a custom offline illustration, not Google Maps data. The GPS screen requests permission, reads the last fused location when available, and calculates distance to the seeded coordinates. You may explicitly simulate an arrival so the demo remains usable away from San Francisco. The sample QR token and puzzle are local demo steps; neither is a server-verified challenge. Demo artifacts are off chain.

## Current integrations and setup

The repository contains a seeded `DemoTreasureRepository`, on-device Fused Location Provider distance display, and a CameraX preview with ML Kit QR decoding. The hand-drawn map is offline; Google Maps tiles, Firebase Auth/Firestore, geofencing, IPFS upload, and Polygon contract calls are not wired into the runnable demo. The QR detector accepts a demo token only; discovery still needs server-side validation before any production reward. No real sign-in, map tile, geofence decision, upload, NFT mint, or transaction is performed. The wallet and activity screens say so explicitly.

To add production services:

- **Firebase Auth and Firestore:** create a Firebase Android app for `com.deaddrop`, add the Google Services Gradle plugin and `google-services.json`, enable the desired auth providers, and define Firestore rules that prevent clients from awarding artifacts directly. Add authenticated repository implementations behind `TreasureRepository` and persist profile, hunt, and artifact records. Keep secrets and service account credentials out of the Android app.
- **Maps SDK:** enable Maps SDK for Android in Google Cloud, restrict an Android API key to the release package and signing certificate, and provide it through a local, untracked properties file or CI secret. Replace the demo map illustration with a map composable and keep the seeded mode available without the key.
- **Location and geofences:** Fused Location Provider currently shows device distance to the sample coordinates. Add a server-backed geofence service, request foreground location only when a hunt starts, handle denied and permanently denied states, and register geofences only with informed consent. Send location evidence to a trusted backend that checks freshness, radius, replay, and abuse limits. A client-only check is not proof of discovery.
- **Camera / QR:** the demo already requests camera access, binds CameraX to the screen lifecycle, and decodes QR codes with ML Kit. Replace the demo token with signed, expiring challenge tokens validated by the backend. Do not accept a hardcoded QR value as a production claim.
- **IPFS:** configure a pinning provider endpoint and credentials on a trusted backend. Upload metadata and media from that service, then store the returned CID. Do not ship provider secrets in the APK.
- **Polygon testnet:** select Polygon Amoy, deploy and audit an ERC-721 contract, and configure the public RPC, chain ID, contract address, and explorer URL. Use a wallet connector for explicit user signing. Mint only after backend discovery validation. Treat pending, rejected, failed, and confirmed receipts separately; never label a mock result as a transaction.

A starter contract is in `contracts/DeadDropArtifact.sol`; it restricts minting to the trusted operator and prevents reusing a discovery ID. Compile it with Solidity 0.8.24 and OpenZeppelin Contracts 5.x, audit it, and deploy to Amoy before wiring a backend signer. The Android app does not currently call this contract.

Real blockchain activity remains disabled until the wallet, contract, RPC, and secure minting service are configured. Never place private keys, seed phrases, Firebase service-account keys, or IPFS pinning secrets in the client.

## Project layout

- `core/theme`: dark palette and reusable design tokens
- `data/model`: treasure model
- `data/repository`: seeded offline repository
- `feature/home`: map shell, Navigation Compose host, ViewModel, and shared UI
- `feature/auth`: first-run onboarding and demo sign-in entry point
- `feature/hunt`: clue, fused location, CameraX/ML Kit QR, puzzle, and reveal screens
- `feature/collection`: artifact collection and sample market
- `feature/profile`: profile, wallet, history, and local sign-in screens

## Build

Use `gradlew.bat :app:assembleDebug` from Windows or `./gradlew :app:assembleDebug` from macOS/Linux. The project targets SDK 36, minimum SDK 26, and Java 17 bytecode.
