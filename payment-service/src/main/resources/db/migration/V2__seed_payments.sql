INSERT INTO payments (id, order_id, amount, status, razorpay_order_id, razorpay_payment_id, idempotency_key, created_at, updated_at) VALUES
(1, '1', 31.97, 'SUCCESS', 'order_1_rp', 'pay_1_rp', 'idem_key_1', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(2, '2', 23.47, 'PENDING', NULL, NULL, 'idem_key_2', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
(3, '3', 20.98, 'FAILED', NULL, NULL, 'idem_key_3', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

SELECT setval('payments_id_seq', (SELECT MAX(id) FROM payments));
