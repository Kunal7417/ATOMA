-- ATOMA Pay Marketplace - Initial Schema
-- PostgreSQL 15+

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    profile_image_url VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    owner_user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    business_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    tin_number VARCHAR(100) NOT NULL,
    business_license_number VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    kyc_status VARCHAR(30) NOT NULL,
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    address VARCHAR(500),
    city VARCHAR(100),
    risk_score VARCHAR(20),
    commission_rate NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    provisional_active BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS kyc_documents (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    document_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    review_notes VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(150) NOT NULL UNIQUE,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_id UUID REFERENCES categories(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    commission_rate NUMERIC(5,2)
);

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    title VARCHAR(250) NOT NULL,
    slug VARCHAR(280) NOT NULL UNIQUE,
    description TEXT,
    sku VARCHAR(100) NOT NULL UNIQUE,
    price NUMERIC(14,2) NOT NULL,
    compare_at_price NUMERIC(14,2),
    stock_quantity INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    brand VARCHAR(100),
    search_vector VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS product_images (
    product_id UUID NOT NULL REFERENCES products(id),
    image_url VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_merchant ON products(merchant_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
