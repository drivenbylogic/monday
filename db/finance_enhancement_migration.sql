-- =============================================================================
-- Finance Enhancement Migration
-- Adds: account_id to transactions, investments table, monthly_allowance setting
-- =============================================================================

-- 1. Add account_id to transactions table (links transactions to bank accounts)
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS account_id UUID REFERENCES finance_accounts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);

-- 2. Add name/account_name to finance_accounts for cleaner labeling
ALTER TABLE finance_accounts
    ADD COLUMN IF NOT EXISTS account_name VARCHAR(255);

-- 3. Create investments table
CREATE TABLE IF NOT EXISTS investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    investment_type VARCHAR(50) NOT NULL DEFAULT 'ONE_TIME', -- 'ONE_TIME' or 'SIP'
    asset_class VARCHAR(50) NOT NULL DEFAULT 'EQUITY', -- 'EQUITY', 'DEBT', 'GOLD', 'REAL_ESTATE', 'CRYPTO', 'OTHER'
    instrument VARCHAR(255), -- e.g. 'NIFTY 50 Index Fund', 'HDFC Top 100'
    invested_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    current_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sip_amount NUMERIC(15, 2), -- monthly SIP amount if SIP type
    sip_day INTEGER, -- day of month for SIP deduction (1-31)
    start_date DATE,
    end_date DATE,
    units NUMERIC(15, 6),
    nav NUMERIC(15, 4), -- Net Asset Value
    folio_number VARCHAR(100),
    broker VARCHAR(100),
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'PAUSED', 'REDEEMED'
    account_id UUID REFERENCES finance_accounts(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_investments_updated_at ON investments;
CREATE TRIGGER trg_investments_updated_at
BEFORE UPDATE ON investments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_investments_type ON investments(investment_type);
CREATE INDEX IF NOT EXISTS idx_investments_status ON investments(status);

-- 4. Create app_settings table for monthly allowance and other global settings
CREATE TABLE IF NOT EXISTS app_settings (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default monthly expense allowance if not exists
INSERT INTO app_settings (key, value, description)
VALUES ('monthly_expense_allowance', '75000', 'Monthly expense budget cap in INR')
ON CONFLICT (key) DO NOTHING;

-- 5. Add credit card CRUD support: ensure credit_accounts has all needed columns
ALTER TABLE credit_accounts
    ADD COLUMN IF NOT EXISTS notes TEXT;

-- Ensure transactions has the right transaction_type casing support
-- (the existing check already accepts lowercase, but let's make it case-insensitive)
-- The existing constraint is: transaction_type VARCHAR(50) NOT NULL
-- No changes needed here as the service already handles case

-- 6. Create index for faster burn trajectory queries
CREATE INDEX IF NOT EXISTS idx_transactions_type_at ON transactions(transaction_type, transaction_at);

-- 7. Add billing_date and due_date to credit_accounts, and link finance_commitments and transactions to credit cards
ALTER TABLE credit_accounts ADD COLUMN IF NOT EXISTS billing_date DATE;
ALTER TABLE credit_accounts ADD COLUMN IF NOT EXISTS due_date DATE;
UPDATE credit_accounts SET billing_date = statement_closing_date WHERE billing_date IS NULL AND statement_closing_date IS NOT NULL;
UPDATE credit_accounts SET due_date = payment_due_date WHERE due_date IS NULL AND payment_due_date IS NOT NULL;
UPDATE credit_accounts SET statement_closing_date = billing_date WHERE statement_closing_date IS NULL AND billing_date IS NOT NULL;
UPDATE credit_accounts SET payment_due_date = due_date WHERE payment_due_date IS NULL AND due_date IS NOT NULL;

ALTER TABLE finance_commitments ADD COLUMN IF NOT EXISTS credit_card_id UUID REFERENCES credit_accounts(id) ON DELETE CASCADE;
ALTER TABLE finance_commitments ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES events(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_finance_commitments_credit_card_id ON finance_commitments(credit_card_id);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS credit_card_id UUID REFERENCES credit_accounts(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_credit_card_id ON transactions(credit_card_id);

-- 8. Cascading & Safe-Deletion Foreign Key Constraints
ALTER TABLE finance_commitments DROP CONSTRAINT IF EXISTS finance_commitments_account_id_fkey;
ALTER TABLE finance_commitments ADD CONSTRAINT finance_commitments_account_id_fkey FOREIGN KEY (account_id) REFERENCES finance_accounts(id) ON DELETE SET NULL;

ALTER TABLE budgets DROP CONSTRAINT IF EXISTS budgets_category_id_fkey;
ALTER TABLE budgets ADD CONSTRAINT budgets_category_id_fkey FOREIGN KEY (category_id) REFERENCES budget_categories(id) ON DELETE CASCADE;

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_category_id_fkey;
ALTER TABLE transactions ADD CONSTRAINT transactions_category_id_fkey FOREIGN KEY (category_id) REFERENCES budget_categories(id) ON DELETE SET NULL;

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_account_id_fkey;
ALTER TABLE transactions ADD CONSTRAINT transactions_account_id_fkey FOREIGN KEY (account_id) REFERENCES finance_accounts(id) ON DELETE SET NULL;

ALTER TABLE investments DROP CONSTRAINT IF EXISTS investments_account_id_fkey;
ALTER TABLE investments ADD CONSTRAINT investments_account_id_fkey FOREIGN KEY (account_id) REFERENCES finance_accounts(id) ON DELETE SET NULL;

-- 9. Budget period type support (MONTHLY / CUSTOM)
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS period_type VARCHAR(50) NOT NULL DEFAULT 'MONTHLY';

