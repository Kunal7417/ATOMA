-- ATOMA Pay Marketplace - Extended schema (JPA-aligned)

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    customer_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    variant_label VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS wishlist_items (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    customer_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL REFERENCES products(id),
    UNIQUE (customer_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES users(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    delivery_fee NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(14,2) NOT NULL,
    coupon_code VARCHAR(50),
    delivery_address VARCHAR(500),
    tracking_number VARCHAR(100),
    courier_name VARCHAR(100),
    payment_retry_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL,
    line_total NUMERIC(14,2) NOT NULL,
    variant_label VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id),
    transaction_ref VARCHAR(100) NOT NULL UNIQUE,
    wallet_transaction_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    escrow_held BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason VARCHAR(500),
    retry_attempts INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS settlements (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    gross_amount NUMERIC(14,2) NOT NULL,
    platform_commission NUMERIC(14,2) NOT NULL,
    merchant_net_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    wallet_settlement_ref VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    resolution_notes TEXT,
    sla_due_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    details TEXT
);

CREATE TABLE IF NOT EXISTS platform_policies (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    policy_key VARCHAR(100) NOT NULL UNIQUE,
    policy_value TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(40) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    "read" BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_json VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    merchant_id UUID REFERENCES merchants(id),
    product_rating INT NOT NULL,
    merchant_rating INT,
    delivery_rating INT,
    comment TEXT,
    moderated BOOLEAN NOT NULL DEFAULT FALSE,
    approved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_merchant ON orders(merchant_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_settlements_merchant ON settlements(merchant_id);
CREATE INDEX IF NOT EXISTS idx_settlements_order ON settlements(order_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
