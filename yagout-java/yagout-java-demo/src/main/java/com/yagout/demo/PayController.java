package com.yagout.demo;

import com.yagout.sdk.build.ApiRequestBuilder;
import com.yagout.sdk.client.YagoutClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PayController {

    @Value("${api.url}")
    private String apiUrl;

    @Value("${me.id}")
    private String meId;

    @Value("${merchant.key.base64}")
    private String merchantKeyBase64;

    @Value("${aggregator.id:yagout}")
    private String agId;

    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping(value = "/api/pay", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> apiPay(@RequestBody Map<String, Object> body) {
        try {
            String orderNo = (String) body.get("orderNo");
            String amount = String.valueOf(body.get("amount"));

            Map<String, Object> p = new LinkedHashMap<>();
            p.put("agId", agId);
            p.put("meId", meId);
            p.put("orderNo", orderNo);
            p.put("amount", amount); // keep as provided; gateway accepts string amounts
            p.put("country", "ETH");
            p.put("currency", "ETB");
            p.put("transactionType", "SALE");
            p.put("sucessUrl", baseUrl + "/payment/success");
            p.put("failureUrl", baseUrl + "/payment/failure");
            p.put("channel", "API");
            if (body.containsKey("emailId")) p.put("emailId", body.get("emailId"));
            if (body.containsKey("mobileNumber")) p.put("mobileNumber", body.get("mobileNumber"));
            if (body.containsKey("pg_details")) p.put("pg_details", body.get("pg_details"));

            Map<String, Object> plainObj = ApiRequestBuilder.buildApiPlainObject(p);
            YagoutClient client = new YagoutClient(apiUrl, meId, merchantKeyBase64);
            return client.callDirectApi(plainObj);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            err.put("exception", e.getClass().getName());
            return err;
        }
    }
}
