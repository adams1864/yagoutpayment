using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using YagoutPay.Demo.Services;
using System.Linq;

namespace YagoutPay.Demo.Controllers;

public class HomeController : Controller
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<HomeController> _logger;
    private readonly YagoutConfig _cfg;

    public HomeController(IHttpClientFactory httpClientFactory, ILogger<HomeController> logger, IOptions<YagoutConfig> options)
    {
        _httpClientFactory = httpClientFactory;
        _logger = logger;
        _cfg = options.Value;
    }

    public IActionResult Index()
    {
        ViewBag.OrderNo = $"ORD-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";
        return View();
    }

    [HttpGet("/pay")]
    public IActionResult PayGetRedirect() => RedirectToAction(nameof(Index));

    [HttpPost("/pay")]
    public IActionResult Pay([FromForm] string order_no, [FromForm] string amount, [FromForm] string? email_id, [FromForm] string? mobile_no)
    {
        var amountNorm = NormalizeAmount(amount);
        var plain = RequestBuilders.BuildHostedPlain(
            _cfg.AggregatorId, _cfg.MeId, order_no, amountNorm,
            "ETH", "ETB", "SALE",
            _cfg.BaseUrl + "/payment/success", _cfg.BaseUrl + "/payment/failure", "WEB",
            "", email_id ?? "", mobile_no ?? "", "", "Y");

        var merchantRequest = CryptoUtil.EncryptBase64(plain, _cfg.MerchantKeyBase64);
        var hashInput = $"{_cfg.MeId}~{order_no}~{amountNorm}~ETH~ETB";
        var sha256Hex = CryptoUtil.Sha256Hex(hashInput);
        var encryptedHash = CryptoUtil.EncryptBase64(sha256Hex, _cfg.MerchantKeyBase64);

        ViewBag.Action = _cfg.GatewayUrl;
        ViewBag.Fields = new Dictionary<string, string>
        {
            ["me_id"] = _cfg.MeId,
            ["merchant_request"] = merchantRequest,
            ["hash"] = encryptedHash
        };
        return View("AutoPost");
    }

    // Simple GET entrypoint for storefronts (no CORS needed): /start?orderNo=...&amount=...&email=...&mobile=...
    [HttpGet("/start")]
    public IActionResult Start([FromQuery] string orderNo, [FromQuery] string amount, [FromQuery] string? email, [FromQuery] string? mobile)
    {
        if (string.IsNullOrWhiteSpace(orderNo) || string.IsNullOrWhiteSpace(amount))
            return BadRequest("orderNo and amount are required");

        var amountNorm = NormalizeAmount(amount);
        var plain = RequestBuilders.BuildHostedPlain(
            _cfg.AggregatorId, _cfg.MeId, orderNo, amountNorm,
            "ETH", "ETB", "SALE",
            _cfg.BaseUrl + "/payment/success", _cfg.BaseUrl + "/payment/failure", "WEB",
            "", email ?? "", mobile ?? "", "", "Y");

        var merchantRequest = CryptoUtil.EncryptBase64(plain, _cfg.MerchantKeyBase64);
        var hashInput = $"{_cfg.MeId}~{orderNo}~{amountNorm}~ETH~ETB";
        var sha256Hex = CryptoUtil.Sha256Hex(hashInput);
        var encryptedHash = CryptoUtil.EncryptBase64(sha256Hex, _cfg.MerchantKeyBase64);

        ViewBag.Action = _cfg.GatewayUrl;
        ViewBag.Fields = new Dictionary<string, string>
        {
            ["me_id"] = _cfg.MeId,
            ["merchant_request"] = merchantRequest,
            ["hash"] = encryptedHash
        };
        return View("AutoPost");
    }

    [HttpPost("/payment/success")]
    public IActionResult Success()
    {
        var body = Request.HasFormContentType ? Request.Form.ToDictionary(k => k.Key, v => (string)v.Value!) : new Dictionary<string, string>();
        var picked = PickEncryptedField(body);
        var decrypted = TryDecrypt(picked.value);
        var details = ParseDecrypted(decrypted);
        _logger.LogInformation("SUCCESS payload:\n{payload}", FormatOrdered(body));
        _logger.LogInformation("SUCCESS picked={picked} details={details} decrypted={decrypted}", picked.field, details, decrypted);

        ViewBag.Details = details;
        ViewBag.UserMessage = BuildUserMessageSuccess(details);
        return View();
    }

    [HttpGet("/payment/success")]
    public IActionResult SuccessGet()
    {
        var q = Request.Query.ToDictionary(k => k.Key, v => v.Value.ToString());
        var picked = PickEncryptedField(q);
        var decrypted = TryDecrypt(picked.value);
        var details = ParseDecrypted(decrypted);
        _logger.LogInformation("SUCCESS (GET) payload:\n{payload}", FormatOrdered(q));
        _logger.LogInformation("SUCCESS (GET) picked={picked} details={details} decrypted={decrypted}", picked.field, details, decrypted);
        ViewBag.Details = details;
        ViewBag.UserMessage = BuildUserMessageSuccess(details);
        return View("Success");
    }

    [HttpPost("/payment/failure")]
    public IActionResult Failure()
    {
        var body = Request.HasFormContentType ? Request.Form.ToDictionary(k => k.Key, v => (string)v.Value!) : new Dictionary<string, string>();
        var picked = PickEncryptedField(body);
        var decrypted = TryDecrypt(picked.value);
        var details = ParseDecrypted(decrypted);
        _logger.LogInformation("FAILURE payload:\n{payload}", FormatOrdered(body));
        _logger.LogInformation("FAILURE picked={picked} details={details} decrypted={decrypted}", picked.field, details, decrypted);

        ViewBag.Details = details;
        ViewBag.UserMessage = BuildUserMessageFailure(details);
        return View();
    }

    [HttpGet("/payment/failure")]
    public IActionResult FailureGet()
    {
        var q = Request.Query.ToDictionary(k => k.Key, v => v.Value.ToString());
        var picked = PickEncryptedField(q);
        var decrypted = TryDecrypt(picked.value);
        var details = ParseDecrypted(decrypted);
        _logger.LogInformation("FAILURE (GET) payload:\n{payload}", FormatOrdered(q));
        _logger.LogInformation("FAILURE (GET) picked={picked} details={details} decrypted={decrypted}", picked.field, details, decrypted);
        ViewBag.Details = details;
        ViewBag.UserMessage = BuildUserMessageFailure(details);
        return View("Failure");
    }

    [HttpPost("/api/pay")]
    public async Task<IActionResult> ApiPay([FromBody] JsonElement body)
    {
        if (!body.TryGetProperty("orderNo", out var orderNoEl) || !body.TryGetProperty("amount", out var amountEl))
            return BadRequest(new { error = "orderNo and amount are required" });
        var orderNo = orderNoEl.GetString()!;
        var amount = amountEl.GetString() ?? amountEl.GetRawText();
        var amountNorm = NormalizeAmount(amount);

        var plainObj = new
        {
            txn_details = new { agId = _cfg.AggregatorId, meId = _cfg.MeId, orderNo, amount = amountNorm, country = "ETH", currency = "ETB", transactionType = "SALE", sucessUrl = _cfg.BaseUrl + "/payment/success", failureUrl = _cfg.BaseUrl + "/payment/failure", channel = "API" },
            pg_details = new { },
            card_details = new { },
            cust_details = new { },
            bill_details = new { },
            ship_details = new { },
            item_details = new { },
            other_details = new { }
        };

        var plainJson = JsonSerializer.Serialize(plainObj);
        var merchantRequest = CryptoUtil.EncryptBase64(plainJson, _cfg.MerchantKeyBase64);
        var payload = new { merchantId = _cfg.MeId, merchantRequest };

        var http = _httpClientFactory.CreateClient();
        var resp = await http.PostAsync(_cfg.ApiUrl, new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json"));
        var text = await resp.Content.ReadAsStringAsync();
        try
        {
            using var doc = JsonDocument.Parse(text);
            if (doc.RootElement.TryGetProperty("response", out var respEnc) && respEnc.ValueKind == JsonValueKind.String)
            {
                var decrypted = CryptoUtil.DecryptBase64(respEnc.GetString()!, _cfg.MerchantKeyBase64);
                return Ok(new { request = payload, response = JsonSerializer.Deserialize<object>(text), decrypted });
            }
            return Ok(new { request = payload, raw = text });
        }
        catch
        {
            return Ok(new { request = payload, raw = text });
        }
    }

    private static (string field, string value) PickEncryptedField(Dictionary<string, string> body)
    {
        if (body.TryGetValue("txn_response", out var t) && !string.IsNullOrEmpty(t)) return ("txn_response", t);
        if (body.TryGetValue("merchant_response", out var m) && !string.IsNullOrEmpty(m)) return ("merchant_response", m);
        if (body.TryGetValue("response", out var r) && !string.IsNullOrEmpty(r)) return ("response", r);
        if (body.TryGetValue("data", out var d) && !string.IsNullOrEmpty(d)) return ("data", d);
        foreach (var kv in body) if (!string.IsNullOrEmpty(kv.Value)) return (kv.Key, kv.Value);
        return ("", "");
    }

    private string TryDecrypt(string cipher)
    {
        try { return string.IsNullOrEmpty(cipher) ? "" : CryptoUtil.DecryptBase64(cipher, _cfg.MerchantKeyBase64); }
        catch (Exception e) { return $"Decryption error: {e.Message}"; }
    }

    private static Dictionary<string, string> ParseDecrypted(string str)
    {
        var details = new Dictionary<string, string>();
        if (string.IsNullOrEmpty(str)) return details;
        var parts = str.Split('|');
        if (parts.Length >= 13)
        {
            details["order_no"] = parts[2];
            details["amount"] = parts[3];
            details["date_time"] = (parts[6] + " " + parts[7]).Trim();
            details["transaction_id"] = parts[8];
            details["status"] = parts[10];
        }
        return details;
    }

    private static string BuildUserMessageSuccess(Dictionary<string, string> d)
    {
        var msg = new StringBuilder("Thank you! Your payment was successful.");
        if (d.TryGetValue("amount", out var amt) && !string.IsNullOrEmpty(amt)) msg.Append(" Amount paid: ").Append(amt).Append('.');
        if (d.TryGetValue("order_no", out var ord) && !string.IsNullOrEmpty(ord)) msg.Append(" Order No: ").Append(ord).Append('.');
        if (d.TryGetValue("date_time", out var dt) && !string.IsNullOrEmpty(dt)) msg.Append(" Date: ").Append(dt).Append('.');
        if (d.TryGetValue("status", out var st) && !string.Equals(st, "Successful", StringComparison.OrdinalIgnoreCase)) msg.Append(" Status: ").Append(st).Append('.');
        return msg.ToString();
    }

    private static string BuildUserMessageFailure(Dictionary<string, string> d)
    {
        var msg = new StringBuilder("Sorry, your payment could not be processed.");
        if (d.TryGetValue("amount", out var amt) && !string.IsNullOrEmpty(amt)) msg.Append(" Amount: ").Append(amt).Append('.');
        if (d.TryGetValue("order_no", out var ord) && !string.IsNullOrEmpty(ord)) msg.Append(" Order No: ").Append(ord).Append('.');
        if (d.TryGetValue("date_time", out var dt) && !string.IsNullOrEmpty(dt)) msg.Append(" Date: ").Append(dt).Append('.');
        if (d.TryGetValue("status", out var st)) msg.Append(" Status: ").Append(st).Append('.');
        return msg.ToString();
    }

    private static string FormatOrdered(Dictionary<string, string> body)
    {
        if (body.Count == 0) return "{}";
        var order = new[] { "txn_response", "pg_details", "txn_details", "other_details", "fraud_details", "card_details", "cust_details", "bill_details", "ship_details" };
        var keys = new List<string>();
        foreach (var k in order) if (body.ContainsKey(k)) keys.Add(k);
        foreach (var k in body.Keys) if (!keys.Contains(k)) keys.Add(k);
        var sb = new StringBuilder();
        sb.AppendLine("{");
        for (int i = 0; i < keys.Count; i++)
        {
            var k = keys[i];
            var v = body[k];
            sb.Append("  ").Append(k).Append(": '").Append(v).Append("'");
            if (i < keys.Count - 1) sb.Append(',');
            sb.AppendLine();
        }
        sb.Append('}');
        return sb.ToString();
    }

    private static string NormalizeAmount(string amount)
    {
        try { return decimal.Parse(amount, System.Globalization.CultureInfo.InvariantCulture).ToString(System.Globalization.CultureInfo.InvariantCulture).TrimEnd('0').TrimEnd('.'); }
        catch { return amount; }
    }
}

public class YagoutConfig
{
    public string MeId { get; set; } = "";
    public string MerchantKeyBase64 { get; set; } = "";
    public string AggregatorId { get; set; } = "yagout";
    public string GatewayUrl { get; set; } = "";
    public string ApiUrl { get; set; } = "";
    public string BaseUrl { get; set; } = "http://localhost:5198";
}
