Zen Cart integration (quick path)

Why this approach
- Secure: secrets stay on your backend.
- Minimal PHP: redirect shoppers to your backend /start.
- Fast for POC and competitions.

Option A — One-line button/link (easiest)
Add to your checkout page template (e.g., includes/templates/YOUR_TEMPLATE/templates/tpl_checkout_payment_default.php):

<a class="button" href="https://YOUR-BACKEND.example.com/start?orderNo=ORD-<?= time() ?>&amount=<?= number_format($cart->total, 2) ?>&email=<?= urlencode($_SESSION['customer_email']) ?>&mobile=<?= urlencode($_SESSION['customer_telephone']) ?>">Pay with Yagout</a>

Option B — Lightweight payment module (relay)
Create two files in your Zen Cart install:
1) includes/modules/payment/yagoutpay.php
2) includes/languages/english/modules/payment/yagoutpay.php

Module outline (yagoutpay.php):
<?php
class yagoutpay {
  var $code, $title, $description, $enabled;
  function __construct() {
    $this->code = 'yagoutpay';
    $this->title = 'YagoutPay';
    $this->description = 'Pay via YagoutPay (hosted)';
    $this->enabled = true;
  }
  function javascript_validation() { return false; }
  function selection() { return ['id' => $this->code, 'module' => $this->title]; }
  function pre_confirmation_check() { return false; }
  function confirmation() { return []; }
  function process_button() {
    global $order, $cart;
    $orderNo = 'ORD-' . time();
    $amount = number_format($order->info['total'], 2, '.', '');
    $email = $_SESSION['customer_email'] ?? '';
    $mobile = $_SESSION['customer_telephone'] ?? '';
    $url = 'https://YOUR-BACKEND.example.com/start' .
           '?orderNo=' . urlencode($orderNo) .
           '&amount=' . urlencode($amount) .
           '&email=' . urlencode($email) .
           '&mobile=' . urlencode($mobile);
    // Redirect via auto-submit form
    return '<form id="yagoutForm" method="GET" action="' . htmlspecialchars($url) . '"></form>' .
           '<script>document.getElementById("yagoutForm").submit();</script>';
  }
  function before_process() { return false; }
  function after_process() { return false; }
  function check() { return 1; }
  function install() { }
  function remove() { }
  function keys() { return []; }
}
?>

Language file (yagoutpay.php):
<?php
define('MODULE_PAYMENT_YAGOUTPAY_TEXT_TITLE', 'YagoutPay');
define('MODULE_PAYMENT_YAGOUTPAY_TEXT_DESCRIPTION', 'Pay via YagoutPay');
?>

Callbacks
- Configure your backend BaseUrl so YagoutPay calls /payment/success and /payment/failure.
- After handling, your backend can 302 to Zen Cart’s checkout_success page.

Security
- Do not place merchant keys in PHP. Keep encryption/decryption on your backend.
