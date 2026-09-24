-- Database is created externally (Render/TiDB/MySQL). JDBC URL selects it.
CREATE TABLE IF NOT EXISTS signup (
    formno VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    fname VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    marital VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pin VARCHAR(10) NOT NULL,
    religion VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    income VARCHAR(50) NOT NULL,
    education VARCHAR(50) NOT NULL,
    occupation VARCHAR(50) NOT NULL,
    pan VARCHAR(20) NOT NULL,
    aadhar VARCHAR(20) NOT NULL,
    senior VARCHAR(5) NOT NULL,
    existing VARCHAR(5) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    facilities VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_pan (pan),
    UNIQUE KEY unique_aadhar (aadhar)
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no VARCHAR(12) NOT NULL,
    formno VARCHAR(10),
    account_type VARCHAR(50) NOT NULL DEFAULT 'Saving',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    daily_limit DECIMAL(15,2) NOT NULL DEFAULT 50000,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    is_system TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_account_no (account_no),
    INDEX idx_accounts_formno (formno),
    INDEX idx_accounts_status (status)
);

CREATE TABLE IF NOT EXISTS login (
    formno VARCHAR(10),
    cardno VARCHAR(16) PRIMARY KEY,
    username VARCHAR(50),
    pin VARCHAR(100) NOT NULL,
    failed_attempts INT DEFAULT 0,
    is_locked INT DEFAULT 0,
    security_question VARCHAR(255),
    security_answer VARCHAR(255),
    account_id BIGINT,
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_login_account (account_id),
    UNIQUE KEY unique_login_username (username),
    FOREIGN KEY (formno) REFERENCES signup(formno) ON DELETE CASCADE
);

ALTER TABLE login ADD COLUMN account_id BIGINT NULL;
ALTER TABLE login ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';
ALTER TABLE login ADD INDEX idx_login_account (account_id);
ALTER TABLE login ADD COLUMN username VARCHAR(50) NULL;
ALTER TABLE login ADD UNIQUE KEY unique_login_username (username);

CREATE TABLE IF NOT EXISTS bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cardno VARCHAR(16) NOT NULL,
    date TIMESTAMP NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bank_cardno (cardno),
    INDEX idx_bank_date (date),
    FOREIGN KEY (cardno) REFERENCES login(cardno) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_ref VARCHAR(32) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    initiated_by VARCHAR(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_txn_ref (txn_ref),
    INDEX idx_txn_created (created_at),
    INDEX idx_txn_initiated_by (initiated_by)
);

CREATE TABLE IF NOT EXISTS txn_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    direction CHAR(1) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entries_account (account_id, id),
    INDEX idx_entries_txn (txn_id),
    FOREIGN KEY (txn_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE IF NOT EXISTS beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_account_id BIGINT NOT NULL,
    nickname VARCHAR(100),
    beneficiary_account_no VARCHAR(12) NOT NULL,
    beneficiary_name VARCHAR(100),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_beneficiary (owner_account_id, beneficiary_account_no),
    INDEX idx_benef_owner (owner_account_id),
    FOREIGN KEY (owner_account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cardno VARCHAR(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_token_hash (token_hash),
    INDEX idx_refresh_cardno (cardno),
    FOREIGN KEY (cardno) REFERENCES login(cardno) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cardno VARCHAR(16) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_cardno (cardno),
    FOREIGN KEY (cardno) REFERENCES login(cardno) ON DELETE CASCADE
);
