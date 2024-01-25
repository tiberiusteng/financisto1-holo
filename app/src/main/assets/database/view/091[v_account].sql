CREATE VIEW v_account AS
SELECT
    a.*,
    c.name AS currency_name
FROM account a
LEFT JOIN currency c ON a.currency_id=c._id