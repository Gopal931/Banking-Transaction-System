-- ============================================================================
-- Banking Transaction Processing System - Phase 2 Seed Data
-- ============================================================================

-- Clean existing data in reverse dependency order
DELETE FROM failed_transaction;
DELETE FROM transaction_audit;
DELETE FROM bank_transaction;
DELETE FROM account;
DELETE FROM customer;

-- ----------------------------------------------------------------------------
-- 1. Insert Customers (3 customers)
-- ----------------------------------------------------------------------------
INSERT INTO customer (id, first_name, last_name, email, phone, status, created_at, updated_at)
VALUES
(1, 'Alex', 'Morgan', 'alex.morgan@example.com', '+1-555-0101', 'ACTIVE', '2026-11-01 09:00:00+00', '2026-11-01 09:00:00+00'),
(2, 'Sarah', 'Connor', 'sarah.connor@example.com', '+1-555-0102', 'ACTIVE', '2026-11-02 10:15:00+00', '2026-11-02 10:15:00+00'),
(3, 'Wayne', 'Enterprises', 'finance@wayneenterprises.com', '+1-555-0103', 'ACTIVE', '2026-11-03 11:30:00+00', '2026-11-03 11:30:00+00');

-- Synchronize identity sequence for customer
SELECT setval(pg_get_serial_sequence('customer', 'id'), COALESCE(MAX(id), 1)) FROM customer;

-- ----------------------------------------------------------------------------
-- 2. Insert Accounts (4 accounts)
-- ----------------------------------------------------------------------------
INSERT INTO account (id, customer_id, account_number, account_type, balance, currency, status, created_at, updated_at)
VALUES
(1, 1, 'ACCT-CHK-8921', 'CHECKING', 12450.25, 'USD', 'ACTIVE', '2026-11-01 09:30:00+00', '2026-11-22 14:00:00+00'),
(2, 1, 'ACCT-SAV-3410', 'SAVINGS',  35800.50, 'USD', 'ACTIVE', '2026-11-01 09:45:00+00', '2026-11-22 14:00:00+00'),
(3, 2, 'ACCT-CHK-9811', 'CHECKING',  8920.00, 'USD', 'ACTIVE', '2026-11-02 10:30:00+00', '2026-11-23 16:30:00+00'),
(4, 3, 'ACCT-CORP-5520', 'CHECKING', 250000.00, 'USD', 'ACTIVE', '2026-11-03 12:00:00+00', '2026-11-23 16:30:00+00');

-- Synchronize identity sequence for account
SELECT setval(pg_get_serial_sequence('account', 'id'), COALESCE(MAX(id), 1)) FROM account;

-- ----------------------------------------------------------------------------
-- 3. Insert Successful Transactions (4 successful transactions)
-- ----------------------------------------------------------------------------
INSERT INTO bank_transaction (id, transaction_reference, source_account_id, target_account_id, transaction_type, amount, currency, status, description, created_at)
VALUES
(1, 'TXN-20261120-001', NULL, 1, 'DEPOSIT', 3200.00, 'USD', 'SUCCESS', 'Direct Deposit - Corporate Payroll', '2026-11-20 08:30:00+00'),
(2, 'TXN-20261121-002', 1, NULL, 'WITHDRAWAL', 142.50, 'USD', 'SUCCESS', 'Point of Sale - Metro Grocery Supermarket', '2026-11-21 17:45:00+00'),
(3, 'TXN-20261122-003', 1, 2, 'TRANSFER', 500.00, 'USD', 'SUCCESS', 'Internal Transfer - Automated Savings Buffer', '2026-11-22 14:00:00+00'),
(4, 'TXN-20261123-004', 4, 3, 'TRANSFER', 15000.00, 'USD', 'SUCCESS', 'Commercial Wire - Supplier Consulting Fee', '2026-11-23 16:30:00+00');

-- Synchronize identity sequence for bank_transaction
SELECT setval(pg_get_serial_sequence('bank_transaction', 'id'), COALESCE(MAX(id), 1)) FROM bank_transaction;

-- ----------------------------------------------------------------------------
-- 4. Insert Transaction Audit Records (4 audit records)
-- ----------------------------------------------------------------------------
INSERT INTO transaction_audit (id, transaction_id, account_id, action_type, amount, balance_before, balance_after, created_at)
VALUES
(1, 1, 1, 'CREDIT',  3200.00,  9250.25, 12450.25, '2026-11-20 08:30:00+00'),
(2, 2, 1, 'DEBIT',    142.50, 12592.75, 12450.25, '2026-11-21 17:45:00+00'),
(3, 3, 1, 'DEBIT',    500.00, 12950.25, 12450.25, '2026-11-22 14:00:00+00'),
(4, 3, 2, 'CREDIT',   500.00, 35300.50, 35800.50, '2026-11-22 14:00:00+00');

-- Synchronize identity sequence for transaction_audit
SELECT setval(pg_get_serial_sequence('transaction_audit', 'id'), COALESCE(MAX(id), 1)) FROM transaction_audit;

-- ----------------------------------------------------------------------------
-- 5. Insert Failed Transaction (1 failed transaction)
-- ----------------------------------------------------------------------------
INSERT INTO failed_transaction (id, transaction_reference, source_account_id, target_account_id, transaction_type, amount, error_code, error_message, failed_at)
VALUES
(1, 'TXN-20261124-901', 3, 4, 'TRANSFER', 50000.00, 'INSUFFICIENT_FUNDS', 'Transfer rejected: Account ACCT-CHK-9811 has insufficient available funds ($8,920.00 available, $50,000.00 requested)', '2026-11-24 10:15:00+00');

-- Synchronize identity sequence for failed_transaction
SELECT setval(pg_get_serial_sequence('failed_transaction', 'id'), COALESCE(MAX(id), 1)) FROM failed_transaction;
