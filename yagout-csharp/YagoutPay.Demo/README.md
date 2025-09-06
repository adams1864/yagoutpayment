YagoutPay.Demo (C#)
====================

This is the C# demo for Yagout hosted checkout and Direct API examples.

Purpose
-------
- Demonstrates the Hosted Checkout flow (auto-post to gateway) via `/start` and `/pay`.
- Demonstrates a minimal Direct API call via `/api/pay` (server-side JSON API).
- Provides callback endpoints `/payment/success` and `/payment/failure` which decrypt gateway responses and show a simple UI.

Required configuration
----------------------
The demo reads configuration from the `YagoutConfig` options (appsettings or environment variables). You must provide:

- `MeId` (merchant id)
- `MerchantKeyBase64` (merchant AES key, Base64 encoded)
- `GatewayUrl` (payment gateway auto-post URL)
- `ApiUrl` (payment gateway direct API URL)
- `BaseUrl` (public base URL where this app is reachable; defaults to `http://localhost:5198`)

You can set these in `appsettings.json` or as environment variables. Example environment variables (Windows CMD):

```powershell
setx MeId "your_me_id"
setx MerchantKeyBase64 "BASE64_KEY_HERE"
setx GatewayUrl "https://payments.example/gateway"
setx ApiUrl "https://payments.example/api/pay"
setx BaseUrl "https://yourserver.example"
```

Endpoints
---------
- `GET /` - demo form (creates a sample order_no)
- `GET /start?orderNo=...&amount=...&email=...&mobile=...` - storefront-friendly entrypoint that renders the auto-post form to the gateway
- `POST /pay` - form POST entrypoint (renders auto-post)
- `POST /api/pay` - server-side Direct API call (expects JSON body { orderNo, amount })
- `POST /payment/success` and `GET /payment/success` - callback handlers
- `POST /payment/failure` and `GET /payment/failure` - callback handlers

Notes
-----
- The demo uses AES-256-CBC with a static IV `0123456789abcdef` to match the gateway sample implementations. Keep keys secure and never commit them to source control.
- Hosted flow: the app builds a pipe-delimited merchant payload, encrypts it to Base64, computes a SHA-256 hex of `meId~order_no~amount~ETH~ETB`, encrypts that hex and sends both to the gateway.
- Direct API: `POST /api/pay` encrypts a JSON payload and posts to `ApiUrl`, then attempts to decrypt the `response` field.

How to run (dotnet)
--------------------
From the `YagoutPay.Demo` project folder:

```cmd
dotnet build
dotnet run --urls "http://localhost:5198"
```

Then visit `http://localhost:5198/` or call `/start` from your storefront.

Security
--------
- Remove any demo credentials before committing. Use environment variables or a secret store for production.

Troubleshooting
---------------
- If views do not render, ensure the project is run from the project folder so the Razor views are discovered.
- For API issues, enable logging and inspect the app logs for the encrypted/decrypted payloads.

License / Attribution
---------------------
This file documents the demo included in the repository and is provided for example purposes only.
