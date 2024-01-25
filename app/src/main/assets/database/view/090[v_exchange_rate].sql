CREATE VIEW v_exchange_rate AS
SELECT
    from_currency_id,
    to_currency_id,
    rate_date,
    rate
FROM (
    SELECT from_currency_id, to_currency_id, rate_date, rate FROM currency_exchange_rate
    UNION
    SELECT to_currency_id, from_currency_id, rate_date, 1/rate FROM currency_exchange_rate
)
GROUP BY from_currency_id, to_currency_id, rate_date