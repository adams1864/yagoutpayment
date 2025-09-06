package com.yagout.sdk.build;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiRequestBuilder {
    private ApiRequestBuilder() {}

    public static Map<String, Object> buildApiPlainObject(Map<String, Object> p) {
        Map<String, Object> txn = new LinkedHashMap<>();
        txn.put("agId", p.getOrDefault("agId", "yagout"));
        txn.put("meId", p.get("meId"));
        txn.put("orderNo", p.get("orderNo"));
        txn.put("amount", p.get("amount"));
        txn.put("country", p.getOrDefault("country", "ETH"));
        txn.put("currency", p.getOrDefault("currency", "ETB"));
        txn.put("transactionType", p.getOrDefault("transactionType", "SALE"));
        txn.put("sucessUrl", p.getOrDefault("sucessUrl", "")); // keep guide spelling
        txn.put("failureUrl", p.getOrDefault("failureUrl", ""));
        txn.put("channel", p.getOrDefault("channel", "API"));

        Map<String, Object> pg = new LinkedHashMap<>();
        Map<String, Object> pgIn = asMap(p.get("pg_details"));
        if (pgIn != null) pg.putAll(pgIn);

        Map<String, Object> cust = new LinkedHashMap<>();
        cust.put("customerName", p.getOrDefault("customerName", ""));
        cust.put("emailId", p.getOrDefault("emailId", ""));
        cust.put("mobileNumber", p.getOrDefault("mobileNumber", ""));
        cust.put("uniqueId", p.getOrDefault("uniqueId", ""));
        cust.put("isLoggedIn", p.getOrDefault("isLoggedIn", "Y"));

        Map<String, Object> bill = obj("billAddress", p, "", "billCity", "", "billState", "", "billCountry", "", "billZip", "");
        Map<String, Object> ship = obj("shipAddress", p, "", "shipCity", "", "shipState", "", "shipCountry", "", "shipZip", "", "shipDays", "", "addressCount", "");
        Map<String, Object> item = obj("itemCount", p, "", "itemValue", "", "itemCategory", "");
        Map<String, Object> card = obj("cardNumber", p, "", "expiryMonth", "", "expiryYear", "", "cvv", "", "cardName", "");

        Map<String, Object> other = new LinkedHashMap<>();
        for (int i = 1; i <= 7; i++) {
            other.put("udf" + i, p.getOrDefault("udf" + i, ""));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("card_details", card);
        root.put("other_details", other);
        root.put("ship_details", ship);
        root.put("txn_details", txn);
        root.put("item_details", item);
        root.put("cust_details", cust);
        root.put("pg_details", pg);
        root.put("bill_details", bill);
        return root;
    }

    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) m;
            return cast;
        }
        return null;
    }

    private static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            String k = (String) kv[i];
            String v = (String) kv[i + 1];
            m.put(k, v);
        }
        return m;
    }
}
