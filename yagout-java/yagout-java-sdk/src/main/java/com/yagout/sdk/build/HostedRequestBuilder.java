package com.yagout.sdk.build;

public final class HostedRequestBuilder {
    private HostedRequestBuilder() {}

    public static String buildMerchantPlainString(
            String ag_id, String me_id, String order_no, String amount,
            String country, String currency, String txn_type,
            String success_url, String failure_url, String channel,
            String cust_name, String email_id, String mobile_no, String unique_id, String is_logged_in,
            String bill_address, String bill_city, String bill_state, String bill_country, String bill_zip,
            String ship_address, String ship_city, String ship_state, String ship_country, String ship_zip, String ship_days, String address_count,
            String item_count, String item_value, String item_category,
            String upi_id, String upi_note, String upi_extra,
            String udf_1, String udf_2, String udf_3, String udf_4, String udf_5
    ) {
        String txn_details = String.join("|", ag_id, me_id, order_no, amount, country, currency, txn_type, success_url, failure_url, channel);
        String pg_details = String.join("|", "", "", "", "");
        String card_details = String.join("|", "", "", "", "", "");
        String cust_details = String.join("|", n(cust_name), n(email_id), n(mobile_no), n(unique_id), n(is_logged_in));
        String bill_details = String.join("|", n(bill_address), n(bill_city), n(bill_state), n(bill_country), n(bill_zip));
        String ship_details = String.join("|", n(ship_address), n(ship_city), n(ship_state), n(ship_country), n(ship_zip), n(ship_days), n(address_count));
        String item_details = String.join("|", n(item_count), n(item_value), n(item_category));
        String upi_details = String.join("|", n(upi_id), n(upi_note), n(upi_extra));
        String other_details = String.join("|", n(udf_1), n(udf_2), n(udf_3), n(udf_4), n(udf_5));
        return String.join("~", txn_details, pg_details, card_details, cust_details, bill_details, ship_details, item_details, upi_details, other_details);
    }

    private static String n(String s) { return s == null ? "" : s; }
}
