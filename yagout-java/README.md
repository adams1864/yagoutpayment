# YagoutPay Java SDK + Demo

Minimal Java SDK and Spring Boot demo for YagoutPay (Hosted + Direct API examples).

## Structure
- yagout-java-sdk — library: crypto + request builders
- yagout-java-demo — Spring Boot demo app (controllers, templates)

## Prerequisites
- Java 17+ (JDK)
- Maven 3.8+
- Public reachable URL or tunnel (ngrok) for gateway callbacks if testing hosted flow

Note: ensure JAVA_HOME points to your JDK installation. Temporary CMD example (current shell only):
```
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
set "PATH=C:\Tools\apache-maven-3.9.11\bin;%JAVA_HOME%\bin;%PATH%"
```
To persist, set via Windows Environment Variables UI or PowerShell (User/Machine) as appropriate.

## Configure
Edit `yagout-java-demo/src/main/resources/application.properties`:
```
me.id=YOUR_ME_ID
merchant.key.base64=YOUR_BASE64_KEY
aggregator.id=yagout
gateway.url=https://uatcheckout.yagoutpay.com/ms-transaction-core-1-0/paymentRedirection/checksumGatewayPage
api.url=https://uatcheckout.yagoutpay.com/ms-transaction-core-1-0/apiRedirection/apiIntegration
base.url=http://localhost:8080
```
Do not commit real credentials to source control.

## Build
From repository root:
```
mvn -q clean package
```

## Run demo
From `yagout-java/yagout-java-demo`:
```
mvn spring-boot:run
```
or run packaged jar:
```
java -jar target/yagout-java-demo-1.0.0.jar
```
Default app URL: http://localhost:8080

## Endpoints (demo)
- GET `/` — index page
- GET `/start` — hosted flow kickoff (builds merchant_request and auto-posts to gateway). Use from storefront.
  Example:
  ```
  /start?orderNo=ORD-12345&amount=125.50&email=buyer@example.com&mobile=+251900000000
  ```
- POST `/api/pay` — Direct API demo (server-to-server encrypted payload)
- POST `/payment/success` — gateway success callback
- POST `/payment/failure` — gateway failure callback

## Direct API example (curl)
```
curl -X POST http://localhost:8080/api/pay \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo":"ORD-1001",
    "amount":"1.00",
    "emailId":"user@example.com",
    "mobileNumber":"9000000000"
  }'
```

## Hosted flow (storefront)
Point BigCommerce / Zen Cart buttons to the demo `/start` URL with required query params (orderNo, amount, email, mobile). Server builds encrypted form and auto-submits to YagoutPay.

## Troubleshooting
- "The JAVA_HOME environment variable is not defined correctly" → ensure JAVA_HOME points to the JDK root that contains `bin\java.exe`.
- `mvn` not recognized → add Maven `bin` to PATH or run with full path to `mvn.cmd`.
- "Invalid Encryption" → verify merchant key is Base64 and decodes to 32 bytes; confirm IV = `0123456789abcdef` per provider guide.
- "Duplicate OrderID" → send a unique orderNo per transaction attempt.

## Notes
- Keep merchant key server-side only.
- Sample templates are in `yagout-java-demo/src/main/resources/templates`.
- The SDK code in `yagout-java-sdk` can be reused by other services.

Maintainer: local
