-- Seed sample orders
INSERT INTO orders (id, user_id, restaurant_id, status, total_amount, created_at, updated_at) VALUES
(1, 2, '507f1f77bcf86cd799439011', 'PAID', 31.97, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(2, 3, '507f1f77bcf86cd799439012', 'PENDING_PAYMENT', 23.47, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
(3, 2, '507f1f77bcf86cd799439013', 'CANCELLED', 20.98, CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

-- Adjust the auto-increment sequence for orders table
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));

-- Seed sample order items
INSERT INTO order_items (order_id, name, price, quantity) VALUES
(1, 'Margherita Pizza', 12.99, 2),
(1, 'Garlic Bread', 5.99, 1),
(2, 'Classic Cheeseburger', 9.99, 2),
(2, 'French Fries', 3.49, 1),
(3, 'California Roll', 8.99, 1),
(3, 'Salmon Nigiri', 11.99, 1);
