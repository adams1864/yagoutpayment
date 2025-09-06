BigCommerce integration (quick and secure)

Why this approach
- No secrets in the storefront. All crypto stays on your backend.
- No custom app needed. Just add a storefront script/button.
- Reuses your backend hosted flow and callbacks.

Prerequisites
- Backend (Node or C#) running with:
  - GET /start?orderNo=...&amount=...&email=...&mobile=...
  - POST/GET /payment/success and /payment/failure
- Public URL for your backend (use ngrok/Cloudflare Tunnel in dev)

Step-by-step (Script Manager)
1) Ensure your backend BaseUrl points to your public URL (so callbacks work).
2) In BigCommerce admin: Storefront > Script Manager > Create a Script.
   - Name: YagoutPay Button
   - Description: Start payment via backend
   - Location: Footer
   - Select pages: All pages (or Checkout only)
   - Script type: Script
   - Script contents: paste the snippet below (edit BACKEND_BASE)
3) Add a button in your theme (e.g., cart or checkout template) that calls payWithYagout(). You can also inject the button HTML directly in the same Script Manager entry.

Snippet (minimal)
<script>
  // Set to your backend public URL (ngrok/Cloudflare or production domain)
  const BACKEND_BASE = 'https://YOUR-BACKEND.example.com';
  const MAKE_ORDER_NO = () => 'ORD-' + Date.now();
  const GET_AMOUNT = () => {
    // For a demo, hardcode or read from the page
    const el = document.querySelector('[data-cart-total]');
    return el ? el.textContent.trim().replace(/[^0-9.]/g, '') : '1.00';
  };
  function payWithYagout(email, mobile) {
    const orderNo = MAKE_ORDER_NO();
    const amount = GET_AMOUNT();
    const url = new URL(BACKEND_BASE + '/start');
    url.searchParams.set('orderNo', orderNo);
    url.searchParams.set('amount', amount);
    if (email) url.searchParams.set('email', email);
    if (mobile) url.searchParams.set('mobile', mobile);
    window.location.href = url.toString();
  }
</script>
<button type="button" onclick="payWithYagout('buyer@example.com','+251900000000')">Pay with Yagout</button>

Callbacks and order confirmation
- YagoutPay calls your backend success/failure URLs.
- Your backend logs/decrypts safely and can 302 back to your BigCommerce confirmation page.
- Optional: from your backend, call BigCommerce Orders API post-success to create/mark orders.
