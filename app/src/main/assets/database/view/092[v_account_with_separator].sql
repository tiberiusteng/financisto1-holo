CREATE VIEW v_account_with_separator AS
SELECT
    _id, icon, accent_color, title, creation_date, currency_id, total_amount,
    type, issuer, number, is_active, is_include_into_totals, last_category_id,
    last_account_id, total_limit, card_issuer, closing_day, payment_day, note,
    last_transaction_date, updated_on, remote_key, currency_name, sort_order,
    0 AS is_separator
FROM v_account
UNION
SELECT
    _id, '' AS icon, accent_color, title, 0 AS creation_date, 0 AS currency_id,
    0 AS total_amount, '' AS type, '' AS issuer, '' AS number, TRUE AS is_active,
    FALSE AS is_include_into_totals, 0 AS last_category_id, 0 AS last_account_id,
    0 AS total_limit, '' AS card_issuer, 0 AS closing_day, 0 AS payment_day,
    '' AS note, 0 AS last_transaction_date, 0 AS updated_on, '' AS remote_key,
    '' AS currency_name, sort_order, 1 AS is_separator
FROM account_separator