# OSS Secure DNS Android Client

Android client for **OSS Secure DNS** — a privacy-focused, token-gated DNS-over-HTTPS (DoH) service operated by OSS Technologies in South Africa.

This app is a branded fork of [Intra](https://github.com/Jigsaw-Code/Intra) (Apache 2.0). It intercepts DNS queries via Android’s VpnService and sends them encrypted to the OSS Secure DNS resolver. Only DNS traffic is tunnelled; all other traffic remains normal.

## OSS Secure DNS

- **Local South African resolver** with privacy and filtering (malware, ads, and optional blocklists)
- **Token-gated** — one token per subscription instance
- **Plans**: Standard, Family, Business (fair-use limits on source IPs and query rate)
- **DoH endpoint pattern**: `https://dns1.oss.co.za/<bare-32-hex-path-id>`
- Customer portal: [https://portal.oss.co.za](https://portal.oss.co.za)

Log into the portal, open your Secure DNS subscription, and copy the **DoH path ID** (bare 32-character hex, no `token_` prefix). The app can also log into the portal for you and fetch the path ID automatically.

## Features of this client

- Hardened for OSS Secure DNS only (no public resolvers)
- Simple path-ID entry or one-tap portal login
- Uses the OSS Technologies logo and branding
- Built-in query history and performance visualisation (inherited from Intra)
- App exclusion list for compatibility

## Building

1. Clone this repo.
2. Open the `Android/` directory in Android Studio (3.2 or later recommended).
3. Place the OSS logo as `Android/app/src/main/res/drawable/oss_logo.png` (or vector).
4. Connect a device or start an emulator.
5. Build and run.

## Licence

This project is based on Intra by Jigsaw Operations LLC and is distributed under the Apache License 2.0. See the original [LICENSE](LICENSE) file.

OSS Technologies — https://oss.co.za
