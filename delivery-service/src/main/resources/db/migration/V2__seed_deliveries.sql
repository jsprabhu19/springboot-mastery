INSERT INTO deliveries (id, order_id, partner_id, status, total_amount, created_at, updated_at) VALUES
(1, 1, 'bob_delivery', 'DELIVERED', 31.97, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(2, 2, 'alice_delivery', 'ASSIGNED', 23.47, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour');

SELECT setval('deliveries_id_seq', (SELECT MAX(id) FROM deliveries));
