namespace YagoutPay.Demo.Services;

public static class RequestBuilders
{
    public static string BuildHostedPlain(
        string ag_id, string me_id, string order_no, string amount,
        string country, string currency, string txn_type,
        string success_url, string failure_url, string channel,
        string cust_name = "", string email_id = "", string mobile_no = "", string unique_id = "", string is_logged_in = "Y",
        string bill_address = "", string bill_city = "", string bill_state = "", string bill_country = "", string bill_zip = "",
        string ship_address = "", string ship_city = "", string ship_state = "", string ship_country = "", string ship_zip = "", string ship_days = "", string address_count = "",
        string item_count = "", string item_value = "", string item_category = "",
        string upi_id = "", string upi_note = "", string upi_extra = "",
        string udf_1 = "", string udf_2 = "", string udf_3 = "", string udf_4 = "", string udf_5 = "")
    {
        string txn_details = string.Join('|', ag_id, me_id, order_no, amount, country, currency, txn_type, success_url, failure_url, channel);
        string pg_details = string.Join('|', "", "", "", "");
        string card_details = string.Join('|', "", "", "", "", "");
        string cust_details = string.Join('|', cust_name, email_id, mobile_no, unique_id, is_logged_in);
        string bill_details = string.Join('|', bill_address, bill_city, bill_state, bill_country, bill_zip);
        string ship_details = string.Join('|', ship_address, ship_city, ship_state, ship_country, ship_zip, ship_days, address_count);
        string item_details = string.Join('|', item_count, item_value, item_category);
        string upi_details = string.Join('|', upi_id, upi_note, upi_extra);
        string other_details = string.Join('|', udf_1, udf_2, udf_3, udf_4, udf_5);
        return string.Join('~', txn_details, pg_details, card_details, cust_details, bill_details, ship_details, item_details, upi_details, other_details);
    }
}
