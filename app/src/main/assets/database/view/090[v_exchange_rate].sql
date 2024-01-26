CREATE VIEW v_exchange_rate AS
SELECT
    from_currency_id,
    to_currency_id,
    rate_date,
    rate,
    is_flip
FROM (
    SELECT from_currency_id, to_currency_id, rate_date, rate, 0 AS is_flip FROM currency_exchange_rate
    UNION
    SELECT to_currency_id AS from_currency_id, from_currency_id AS to_currency_id, rate_date, 1/rate AS rate, 1 AS is_flip FROM currency_exchange_rate
)
GROUP BY from_currency_id, to_currency_id, rate_date