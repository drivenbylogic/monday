-- =============================================================================
-- Monday Application: Master Consolidated Database Schema
-- File: master_schema.sql
-- Description: Unified PostgreSQL schema consolidating all modules and migrations (V1 - V12)
-- Target Database: PostgreSQL 17+ / 18+ (Supabase, AWS RDS, Neon, Cloud SQL compatible)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. EXTENSIONS & UTILITY FUNCTIONS
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Automated trigger function to update updated_at timestamp on row modification
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- 1. USERS & AUTHENTICATION
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- 2. PROJECT MANAGEMENT & DEEP WORK MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    tags JSONB,
    start_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_projects_updated_at ON projects;
CREATE TRIGGER trg_projects_updated_at
BEFORE UPDATE ON projects
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    start_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_milestones_updated_at ON milestones;
CREATE TRIGGER trg_milestones_updated_at
BEFORE UPDATE ON milestones
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS deep_work_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    duration_minutes INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    focus_score INTEGER DEFAULT 100,
    notes TEXT,
    tasks_completed JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_deep_work_sessions_updated_at ON deep_work_sessions;
CREATE TRIGGER trg_deep_work_sessions_updated_at
BEFORE UPDATE ON deep_work_sessions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS career_plans (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    target_date DATE,
    color VARCHAR(50),
    view_style VARCHAR(50),
    plan_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_career_plans_updated_at ON career_plans;
CREATE TRIGGER trg_career_plans_updated_at
BEFORE UPDATE ON career_plans
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_career_plans_created_at ON career_plans(created_at);

-- -----------------------------------------------------------------------------
-- 3. PERSONAL EVENTS & SCHEDULES MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) NOT NULL DEFAULT 'PERSONAL',
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_pattern VARCHAR(255),
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    requires_prep BOOLEAN NOT NULL DEFAULT FALSE,
    prep_title VARCHAR(255),
    prep_due_text VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_events_updated_at ON events;
CREATE TRIGGER trg_events_updated_at
BEFORE UPDATE ON events
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_events_start_at ON events(start_at);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);

CREATE TABLE IF NOT EXISTS event_prep_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    text VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    due DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_event_prep_items_updated_at ON event_prep_items;
CREATE TRIGGER trg_event_prep_items_updated_at
BEFORE UPDATE ON event_prep_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- 4. UNIFIED TASKS & REMINDERS MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    milestone_id UUID REFERENCES milestones(id) ON DELETE SET NULL,
    event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    parent_task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    due_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    estimated_minutes INTEGER CHECK (estimated_minutes IS NULL OR estimated_minutes >= 0),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_pattern VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_tasks_updated_at ON tasks;
CREATE TRIGGER trg_tasks_updated_at
BEFORE UPDATE ON tasks
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_due_at ON tasks(due_at);
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_milestone_id ON tasks(milestone_id);
CREATE INDEX IF NOT EXISTS idx_tasks_event_id ON tasks(event_id);
CREATE INDEX IF NOT EXISTS idx_tasks_parent_task_id ON tasks(parent_task_id);

CREATE TABLE IF NOT EXISTS reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    remind_at TIMESTAMPTZ NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reminders_exclusive_target CHECK (
        (task_id IS NOT NULL AND event_id IS NULL) OR
        (task_id IS NULL AND event_id IS NOT NULL)
    )
);

DROP TRIGGER IF EXISTS trg_reminders_updated_at ON reminders;
CREATE TRIGGER trg_reminders_updated_at
BEFORE UPDATE ON reminders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_reminders_remind_at_status ON reminders(remind_at, status);
CREATE INDEX IF NOT EXISTS idx_reminders_task_id ON reminders(task_id);
CREATE INDEX IF NOT EXISTS idx_reminders_event_id ON reminders(event_id);

-- -----------------------------------------------------------------------------
-- 5. FINANCE & WEALTH PORTFOLIO MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS budget_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_budget_categories_updated_at ON budget_categories;
CREATE TRIGGER trg_budget_categories_updated_at
BEFORE UPDATE ON budget_categories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL REFERENCES budget_categories(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    period_type VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    CONSTRAINT chk_budget_period CHECK (period_end >= period_start)
);

DROP TRIGGER IF EXISTS trg_budgets_updated_at ON budgets;
CREATE TRIGGER trg_budgets_updated_at
BEFORE UPDATE ON budgets
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_budgets_period ON budgets(period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_budgets_category_id ON budgets(category_id);

CREATE TABLE IF NOT EXISTS finance_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution VARCHAR(255) NOT NULL,
    account_type VARCHAR(255) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    last_reconciled TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_name VARCHAR(255)
);

DROP TRIGGER IF EXISTS trg_finance_accounts_updated_at ON finance_accounts;
CREATE TRIGGER trg_finance_accounts_updated_at
BEFORE UPDATE ON finance_accounts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS credit_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    card_id VARCHAR(50) NOT NULL UNIQUE,
    card_name VARCHAR(100) NOT NULL,
    last_four VARCHAR(10) NOT NULL,
    network VARCHAR(20) NOT NULL,
    credit_limit NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    current_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    statement_closing_date DATE,
    payment_due_date DATE,
    min_amount_due NUMERIC(15, 2) DEFAULT 0.00,
    auto_pay_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    billing_date DATE,
    due_date DATE
);

DROP TRIGGER IF EXISTS trg_credit_accounts_updated_at ON credit_accounts;
CREATE TRIGGER trg_credit_accounts_updated_at
BEFORE UPDATE ON credit_accounts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS finance_commitments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    account_id UUID REFERENCES finance_accounts(id) ON DELETE SET NULL,
    is_urgent BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    credit_card_id UUID REFERENCES credit_accounts(id) ON DELETE CASCADE,
    event_id UUID REFERENCES events(id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS trg_finance_commitments_updated_at ON finance_commitments;
CREATE TRIGGER trg_finance_commitments_updated_at
BEFORE UPDATE ON finance_commitments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_finance_commitments_credit_card_id ON finance_commitments(credit_card_id);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    transaction_at TIMESTAMPTZ NOT NULL,
    category_id UUID REFERENCES budget_categories(id) ON DELETE SET NULL,
    payment_method VARCHAR(50),
    description VARCHAR(255),
    notes TEXT,
    linked_module VARCHAR(50),
    linked_entity_id VARCHAR(100),
    linked_entity_type VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_id UUID REFERENCES finance_accounts(id) ON DELETE SET NULL,
    credit_card_id UUID REFERENCES credit_accounts(id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS trg_transactions_updated_at ON transactions;
CREATE TRIGGER trg_transactions_updated_at
BEFORE UPDATE ON transactions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_transactions_at ON transactions(transaction_at);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_credit_card_id ON transactions(credit_card_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type_at ON transactions(transaction_type, transaction_at);

CREATE TABLE IF NOT EXISTS portfolio_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    asset_class VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    current_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    unrealized_gain_percent NUMERIC(5, 2) DEFAULT 0.00,
    risk_rating VARCHAR(20) DEFAULT 'MODERATE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_portfolio_allocations_updated_at ON portfolio_allocations;
CREATE TRIGGER trg_portfolio_allocations_updated_at
BEFORE UPDATE ON portfolio_allocations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    investment_type VARCHAR(50) NOT NULL DEFAULT 'ONE_TIME',
    asset_class VARCHAR(50) NOT NULL DEFAULT 'EQUITY',
    instrument VARCHAR(255),
    invested_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    current_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sip_amount NUMERIC(15, 2),
    sip_day INTEGER,
    start_date DATE,
    end_date DATE,
    units NUMERIC(15, 6),
    nav NUMERIC(15, 4),
    folio_number VARCHAR(100),
    broker VARCHAR(100),
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
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

CREATE TABLE IF NOT EXISTS app_settings (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 6. VEHICLE FLEET MANAGEMENT MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50) NOT NULL UNIQUE,
    purchase_date DATE,
    purchase_amount NUMERIC(12, 2) CHECK (purchase_amount IS NULL OR purchase_amount >= 0),
    current_km INTEGER NOT NULL DEFAULT 0 CHECK (current_km >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    edition VARCHAR(255),
    variant VARCHAR(255),
    type VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    hero_image_url VARCHAR(1024),
    vin VARCHAR(255),
    emission_standard VARCHAR(255),
    engine_json JSONB,
    chassis_json JSONB,
    service_manual_url VARCHAR(1024),
    registered_owner VARCHAR(255),
    registering_authority VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_vehicles_updated_at ON vehicles;
CREATE TRIGGER trg_vehicles_updated_at
BEFORE UPDATE ON vehicles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS vehicle_service_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    service_date DATE NOT NULL,
    km_at_service INTEGER CHECK (km_at_service IS NULL OR km_at_service >= 0),
    service_type VARCHAR(100) NOT NULL,
    description TEXT,
    cost NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (cost >= 0),
    performed_by VARCHAR(255),
    notes TEXT,
    specifications_json JSONB,
    parts_cost NUMERIC(12, 2) DEFAULT 0.00,
    labor_cost NUMERIC(12, 2) DEFAULT 0.00,
    verified BOOLEAN DEFAULT FALSE,
    is_upcoming BOOLEAN DEFAULT FALSE,
    linked_transaction_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_vehicle_service_records_updated_at ON vehicle_service_records;
CREATE TRIGGER trg_vehicle_service_records_updated_at
BEFORE UPDATE ON vehicle_service_records
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_vehicle_service_records_veh_date ON vehicle_service_records(vehicle_id, service_date DESC);
CREATE INDEX IF NOT EXISTS idx_vehicle_service_vehicle_id ON vehicle_service_records(vehicle_id);

CREATE TABLE IF NOT EXISTS vehicle_service_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_record_id UUID NOT NULL REFERENCES vehicle_service_records(id) ON DELETE CASCADE,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    brand_spec VARCHAR(255),
    quantity NUMERIC(10, 2) DEFAULT 1,
    unit VARCHAR(50),
    item_cost NUMERIC(12, 2) DEFAULT 0.00 CHECK (item_cost >= 0),
    replacement_interval_months INTEGER CHECK (replacement_interval_months IS NULL OR replacement_interval_months > 0),
    replacement_interval_km INTEGER CHECK (replacement_interval_km IS NULL OR replacement_interval_km > 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehicle_service_items_record ON vehicle_service_items(service_record_id);

CREATE TABLE IF NOT EXISTS vehicle_maintenance_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    item_name VARCHAR(255) NOT NULL,
    interval_months INTEGER CHECK (interval_months IS NULL OR interval_months > 0),
    interval_km INTEGER CHECK (interval_km IS NULL OR interval_km > 0),
    last_performed_at DATE,
    last_performed_km INTEGER CHECK (last_performed_km IS NULL OR last_performed_km >= 0),
    next_due_at DATE,
    next_due_km INTEGER CHECK (next_due_km IS NULL OR next_due_km >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'OK',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_vehicle_maintenance_schedules_updated_at ON vehicle_maintenance_schedules;
CREATE TRIGGER trg_vehicle_maintenance_schedules_updated_at
BEFORE UPDATE ON vehicle_maintenance_schedules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_vehicle_maint_vehicle_id ON vehicle_maintenance_schedules(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_maintenance_schedules_veh ON vehicle_maintenance_schedules(vehicle_id);

CREATE TABLE IF NOT EXISTS vehicle_obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    obligation_type VARCHAR(50) NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(12, 2) CHECK (amount IS NULL OR amount >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_vehicle_obligations_updated_at ON vehicle_obligations;
CREATE TRIGGER trg_vehicle_obligations_updated_at
BEFORE UPDATE ON vehicle_obligations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_vehicle_obligations_due_date ON vehicle_obligations(due_date, status);
CREATE INDEX IF NOT EXISTS idx_vehicle_obligations_veh ON vehicle_obligations(vehicle_id, due_date);

CREATE TABLE IF NOT EXISTS vehicle_fuel_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    log_date DATE NOT NULL,
    brand VARCHAR(255),
    location VARCHAR(255),
    volume_liters NUMERIC(10, 2),
    total_cost NUMERIC(12, 2),
    price_per_liter NUMERIC(10, 2),
    odometer_km INTEGER,
    trip_efficiency NUMERIC(10, 2),
    fuel_brand VARCHAR(50),
    fuel_grade VARCHAR(50),
    station_location TEXT,
    full_tank BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehicle_fuel_logs_veh_date ON vehicle_fuel_logs(vehicle_id, log_date DESC);

CREATE TABLE IF NOT EXISTS vehicle_diagnostics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    scanned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    diagnostic_protocol VARCHAR(255),
    fault_codes_detected INTEGER,
    dtc_list JSONB,
    subsystem_status JSONB,
    summary TEXT
);

CREATE TABLE IF NOT EXISTS vehicle_telemetry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    odometer_json JSONB,
    next_scheduled_service_json JSONB,
    tires_json JSONB,
    electrical_json JSONB,
    drivetrain_json JSONB,
    fuel_json JSONB,
    operational_readiness_score INTEGER
);

CREATE TABLE IF NOT EXISTS vehicle_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    doc_type VARCHAR(50),
    title VARCHAR(255),
    identifier VARCHAR(255),
    issuer VARCHAR(255),
    valid_until VARCHAR(255),
    verification_source VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    status VARCHAR(50),
    covered_components JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    issue_date DATE,
    valid_from DATE,
    expiry_date DATE,
    notes TEXT,
    metadata JSONB,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_vehicle_documents_updated_at ON vehicle_documents;
CREATE TRIGGER trg_vehicle_documents_updated_at
BEFORE UPDATE ON vehicle_documents
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_vehicle_documents_veh_exp ON vehicle_documents(vehicle_id, expiry_date);
CREATE INDEX IF NOT EXISTS idx_vehicle_documents_veh_type ON vehicle_documents(vehicle_id, doc_type);

CREATE TABLE IF NOT EXISTS vehicle_insurance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    policy_name VARCHAR(255),
    underwriter VARCHAR(255),
    policy_number VARCHAR(255),
    coverage_scope TEXT,
    insured_declared_value NUMERIC(12, 2),
    premium_due NUMERIC(12, 2),
    due_date DATE,
    status VARCHAR(50),
    package_document_url VARCHAR(1024),
    is_renewed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    document_id UUID REFERENCES vehicle_documents(id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS trg_vehicle_insurance_updated_at ON vehicle_insurance;
CREATE TRIGGER trg_vehicle_insurance_updated_at
BEFORE UPDATE ON vehicle_insurance
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS vehicle_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    meta_text VARCHAR(255),
    completed BOOLEAN DEFAULT FALSE,
    priority VARCHAR(50),
    linked_module VARCHAR(100),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_vehicle_tasks_updated_at ON vehicle_tasks;
CREATE TRIGGER trg_vehicle_tasks_updated_at
BEFORE UPDATE ON vehicle_tasks
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_vehicle_tasks_veh ON vehicle_tasks(vehicle_id, completed);

CREATE TABLE IF NOT EXISTS vehicle_odometer_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    reading_date DATE NOT NULL,
    odometer_km INTEGER NOT NULL CHECK (odometer_km >= 0),
    source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    is_correction BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehicle_odometer_readings_veh ON vehicle_odometer_readings(vehicle_id, reading_date DESC, odometer_km DESC);

-- -----------------------------------------------------------------------------
-- 7. REVAMPED PERSONAL ADMINISTRATION MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS administration_obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    provider_name VARCHAR(255),
    category VARCHAR(100),
    obligation_type VARCHAR(50) NOT NULL DEFAULT 'BILL',
    expected_amount NUMERIC(12, 2),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    billing_frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    recurrence_rule VARCHAR(255),
    start_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    end_date DATE,
    trial_end_date DATE,
    cancellation_deadline DATE,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_obligations_updated_at ON administration_obligations;
CREATE TRIGGER trg_administration_obligations_updated_at
BEFORE UPDATE ON administration_obligations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_obl_next_due ON administration_obligations(next_due_date);
CREATE INDEX IF NOT EXISTS idx_admin_obl_status ON administration_obligations(status);
CREATE INDEX IF NOT EXISTS idx_admin_obl_category ON administration_obligations(category);
CREATE INDEX IF NOT EXISTS idx_admin_obl_archived ON administration_obligations(archived_at);

CREATE TABLE IF NOT EXISTS administration_people (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(255) NOT NULL,
    relationship_label VARCHAR(100),
    birthday DATE,
    contact_information TEXT,
    notes TEXT,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_people_updated_at ON administration_people;
CREATE TRIGGER trg_administration_people_updated_at
BEFORE UPDATE ON administration_people
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_people_display_name ON administration_people(display_name);
CREATE INDEX IF NOT EXISTS idx_admin_people_archived ON administration_people(archived_at);

CREATE TABLE IF NOT EXISTS administration_important_dates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    date DATE NOT NULL,
    date_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL',
    person_id UUID REFERENCES administration_people(id) ON DELETE SET NULL,
    recurrence_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL',
    recurrence_rule VARCHAR(255),
    reminder_lead_days INTEGER NOT NULL DEFAULT 1,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_dates_updated_at ON administration_important_dates;
CREATE TRIGGER trg_administration_dates_updated_at
BEFORE UPDATE ON administration_important_dates
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_dates_date ON administration_important_dates(date);
CREATE INDEX IF NOT EXISTS idx_admin_dates_person_id ON administration_important_dates(person_id);
CREATE INDEX IF NOT EXISTS idx_admin_dates_archived ON administration_important_dates(archived_at);

CREATE TABLE IF NOT EXISTS administration_gadgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    purchase_date DATE,
    purchase_price NUMERIC(12, 2),
    warranty_start_date DATE,
    warranty_end_date DATE,
    warranty_provider VARCHAR(255),
    notes TEXT,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_gadgets_updated_at ON administration_gadgets;
CREATE TRIGGER trg_administration_gadgets_updated_at
BEFORE UPDATE ON administration_gadgets
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_gadgets_category ON administration_gadgets(category);
CREATE INDEX IF NOT EXISTS idx_admin_gadgets_warranty_end ON administration_gadgets(warranty_end_date);
CREATE INDEX IF NOT EXISTS idx_admin_gadgets_archived ON administration_gadgets(archived_at);

CREATE TABLE IF NOT EXISTS administration_maintenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gadget_id UUID NOT NULL REFERENCES administration_gadgets(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    maintenance_type VARCHAR(100) NOT NULL DEFAULT 'SERVICING',
    due_date DATE NOT NULL,
    recurrence_rule VARCHAR(255),
    last_performed_at DATE,
    cost NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
    service_provider VARCHAR(255),
    service_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_maintenance_updated_at ON administration_maintenance;
CREATE TRIGGER trg_administration_maintenance_updated_at
BEFORE UPDATE ON administration_maintenance
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_maint_gadget_id ON administration_maintenance(gadget_id);
CREATE INDEX IF NOT EXISTS idx_admin_maint_due_date ON administration_maintenance(due_date);
CREATE INDEX IF NOT EXISTS idx_admin_maint_status ON administration_maintenance(status);

CREATE TABLE IF NOT EXISTS administration_checklists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    administration_record_type VARCHAR(50),
    administration_record_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_checklists_updated_at ON administration_checklists;
CREATE TRIGGER trg_administration_checklists_updated_at
BEFORE UPDATE ON administration_checklists
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_checklists_target ON administration_checklists(administration_record_type, administration_record_id);

CREATE TABLE IF NOT EXISTS administration_checklist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    checklist_id UUID NOT NULL REFERENCES administration_checklists(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    position INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_admin_chk_items_updated_at ON administration_checklist_items;
CREATE TRIGGER trg_admin_chk_items_updated_at
BEFORE UPDATE ON administration_checklist_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_chk_items_checklist_pos ON administration_checklist_items(checklist_id, position);

CREATE TABLE IF NOT EXISTS administration_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_administration_notes_updated_at ON administration_notes;
CREATE TRIGGER trg_administration_notes_updated_at
BEFORE UPDATE ON administration_notes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_admin_notes_updated_at ON administration_notes(updated_at DESC);

-- -----------------------------------------------------------------------------
-- 8. INDUSTRY INTELLIGENCE & DAILY BRIEFS MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS industry_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    feed_url VARCHAR(1024) NOT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'RSS',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_industry_sources_updated_at ON industry_sources;
CREATE TRIGGER trg_industry_sources_updated_at
BEFORE UPDATE ON industry_sources
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS industry_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    summary_date DATE NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    content_markdown TEXT NOT NULL,
    sources_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_industry_summaries_updated_at ON industry_summaries;
CREATE TRIGGER trg_industry_summaries_updated_at
BEFORE UPDATE ON industry_summaries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_industry_summaries_date ON industry_summaries(summary_date);

CREATE TABLE IF NOT EXISTS daily_briefs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brief_date DATE NOT NULL UNIQUE,
    structured_agenda_json JSONB NOT NULL,
    ai_summary_markdown TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'GENERATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_daily_briefs_updated_at ON daily_briefs;
CREATE TRIGGER trg_daily_briefs_updated_at
BEFORE UPDATE ON daily_briefs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_daily_briefs_date ON daily_briefs(brief_date);

-- -----------------------------------------------------------------------------
-- 9. INTELLIGENCE ENGINE & AI FEED PIPELINE MODULE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rss_feeds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    feed_url VARCHAR(255) NOT NULL UNIQUE,
    site_url VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    source_quality NUMERIC(3, 2) NOT NULL DEFAULT 0.5,
    last_fetched_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    last_failure_code VARCHAR(100),
    last_failure_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    area_of_interest VARCHAR(100) DEFAULT 'TECHNOLOGY',
    description TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    article_limit INTEGER NOT NULL DEFAULT 5,
    etag VARCHAR(255),
    last_modified_header VARCHAR(255),
    last_attempted_at TIMESTAMPTZ,
    last_fetch_status VARCHAR(50) DEFAULT 'IDLE'
);

DROP TRIGGER IF EXISTS trg_rss_feeds_updated_at ON rss_feeds;
CREATE TRIGGER trg_rss_feeds_updated_at
BEFORE UPDATE ON rss_feeds
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_rss_feeds_enabled ON rss_feeds(is_enabled);
CREATE INDEX IF NOT EXISTS idx_rss_feeds_area ON rss_feeds(area_of_interest);

CREATE TABLE IF NOT EXISTS articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_url VARCHAR(1000) NOT NULL,
    original_url VARCHAR(1000) NOT NULL,
    title VARCHAR(1000) NOT NULL,
    description TEXT,
    content_text TEXT,
    author VARCHAR(255),
    published_at TIMESTAMPTZ,
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    area_of_interest VARCHAR(100) DEFAULT 'TECHNOLOGY',
    processing_status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED',
    bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    bookmarked_at TIMESTAMPTZ,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    duplicate_of_id UUID REFERENCES articles(id) ON DELETE SET NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

DROP TRIGGER IF EXISTS trg_articles_updated_at ON articles;
CREATE TRIGGER trg_articles_updated_at
BEFORE UPDATE ON articles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_articles_canonical_url ON articles(canonical_url);
CREATE INDEX IF NOT EXISTS idx_articles_content_hash ON articles(content_hash);
CREATE INDEX IF NOT EXISTS idx_articles_created_at ON articles(created_at);
CREATE INDEX IF NOT EXISTS idx_articles_published_at ON articles(published_at);
CREATE INDEX IF NOT EXISTS idx_articles_processing_status ON articles(processing_status);
CREATE INDEX IF NOT EXISTS idx_articles_area_of_interest ON articles(area_of_interest);
CREATE INDEX IF NOT EXISTS idx_articles_bookmarked ON articles(bookmarked);
CREATE INDEX IF NOT EXISTS idx_articles_is_deleted ON articles(is_deleted);

CREATE TABLE IF NOT EXISTS article_sources (
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    feed_id UUID NOT NULL REFERENCES rss_feeds(id) ON DELETE CASCADE,
    external_guid VARCHAR(500),
    source_published_at TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (article_id, feed_id)
);

CREATE INDEX IF NOT EXISTS idx_article_sources_feed_id ON article_sources(feed_id);

CREATE TABLE IF NOT EXISTS interest_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    classification_config JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_interest_categories_updated_at ON interest_categories;
CREATE TRIGGER trg_interest_categories_updated_at
BEFORE UPDATE ON interest_categories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS user_interest_categories (
    user_id UUID NOT NULL,
    category_id UUID NOT NULL REFERENCES interest_categories(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, category_id)
);

CREATE TABLE IF NOT EXISTS article_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES interest_categories(id) ON DELETE CASCADE,
    score NUMERIC(5, 4) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    matched_signals JSONB,
    classified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (article_id, category_id)
);

CREATE TABLE IF NOT EXISTS daily_article_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_date DATE NOT NULL,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES interest_categories(id) ON DELETE CASCADE,
    rank INTEGER NOT NULL,
    ranking_score NUMERIC(5, 4) NOT NULL,
    selected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (selection_date, article_id, category_id)
);

CREATE TABLE IF NOT EXISTS article_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    summary TEXT,
    model VARCHAR(100),
    prompt_version VARCHAR(50),
    word_count INTEGER,
    generated_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tokens_used INTEGER DEFAULT 0,
    estimated_cost NUMERIC(8, 6) DEFAULT 0
);

DROP TRIGGER IF EXISTS trg_article_summaries_updated_at ON article_summaries;
CREATE TRIGGER trg_article_summaries_updated_at
BEFORE UPDATE ON article_summaries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_article_summaries_article_id ON article_summaries(article_id);
CREATE INDEX IF NOT EXISTS idx_article_summaries_generated_at ON article_summaries(generated_at);
CREATE INDEX IF NOT EXISTS idx_article_summaries_status ON article_summaries(status);

CREATE TABLE IF NOT EXISTS intelligence_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_date DATE NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL,
    feeds_attempted INTEGER NOT NULL DEFAULT 0,
    feeds_succeeded INTEGER NOT NULL DEFAULT 0,
    feeds_failed INTEGER NOT NULL DEFAULT 0,
    articles_seen INTEGER NOT NULL DEFAULT 0,
    articles_new INTEGER NOT NULL DEFAULT 0,
    articles_deduplicated INTEGER NOT NULL DEFAULT 0,
    articles_classified INTEGER NOT NULL DEFAULT 0,
    articles_selected INTEGER NOT NULL DEFAULT 0,
    summaries_generated INTEGER NOT NULL DEFAULT 0,
    summaries_failed INTEGER NOT NULL DEFAULT 0,
    failure_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trigger_type VARCHAR(50) DEFAULT 'SCHEDULED',
    tokens_used INTEGER NOT NULL DEFAULT 0,
    estimated_cost NUMERIC(8, 6) NOT NULL DEFAULT 0,
    summaries_skipped INTEGER NOT NULL DEFAULT 0,
    summaries_deferred INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_intelligence_runs_started_at ON intelligence_runs(started_at);
CREATE INDEX IF NOT EXISTS idx_intelligence_runs_status ON intelligence_runs(status);

CREATE TABLE IF NOT EXISTS intelligence_briefs (
    id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    topic_badge VARCHAR(100),
    impact_badge VARCHAR(100),
    impact_color VARCHAR(50),
    time_ago VARCHAR(50),
    summary TEXT,
    why_it_matters TEXT,
    why_it_matters_highlight TEXT,
    source VARCHAR(255),
    read_time VARCHAR(50),
    category VARCHAR(100),
    cross_link_label VARCHAR(100),
    cross_link_module VARCHAR(100),
    bookmarked BOOLEAN DEFAULT FALSE,
    bookmarked_at TIMESTAMPTZ,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    radar_x NUMERIC(5, 2),
    radar_y NUMERIC(5, 2),
    gravity VARCHAR(20),
    urgency_score INTEGER,
    color VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_intelligence_briefs_updated_at ON intelligence_briefs;
CREATE TRIGGER trg_intelligence_briefs_updated_at
BEFORE UPDATE ON intelligence_briefs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- 10. SYSTEM SEED / REFERENCE DATA
-- -----------------------------------------------------------------------------
INSERT INTO interest_categories (id, code, name, description, status) VALUES
('f0000001-0000-0000-0000-000000000001', 'ARTIFICIAL_INTELLIGENCE', 'Artificial Intelligence', 'AI, Machine Learning, LLMs', 'ACTIVE'),
('f0000002-0000-0000-0000-000000000002', 'SOFTWARE_DEVELOPMENT', 'Software Development', 'Programming, Architecture, Tools', 'ACTIVE'),
('f0000003-0000-0000-0000-000000000003', 'CLOUD_COMPUTING', 'Cloud Computing', 'AWS, Azure, GCP, Infrastructure', 'ACTIVE'),
('f0000004-0000-0000-0000-000000000004', 'CYBERSECURITY', 'Cybersecurity', 'Security, Privacy, Hacking', 'ACTIVE'),
('f0000005-0000-0000-0000-000000000005', 'TECHNOLOGY', 'Technology', 'General Tech News', 'ACTIVE'),
('f0000006-0000-0000-0000-000000000006', 'STARTUPS', 'Startups', 'Startups, Venture Capital', 'ACTIVE'),
('f0000007-0000-0000-0000-000000000007', 'BUSINESS', 'Business', 'Corporate News, Strategy', 'ACTIVE'),
('f0000008-0000-0000-0000-000000000008', 'FINANCE_MARKETS', 'Finance & Markets', 'Stocks, Economy, Crypto', 'ACTIVE'),
('f0000009-0000-0000-0000-000000000009', 'SCIENCE', 'Science', 'Scientific Discoveries', 'ACTIVE'),
('f000000a-0000-0000-0000-00000000000a', 'SPACE', 'Space', 'Space Exploration', 'ACTIVE'),
('f000000b-0000-0000-0000-00000000000b', 'AUTOMOTIVE', 'Automotive', 'Cars, EVs, Autonomous', 'ACTIVE'),
('f000000c-0000-0000-0000-00000000000c', 'GAMING', 'Gaming', 'Video Games, Industry', 'ACTIVE'),
('f000000d-0000-0000-0000-00000000000d', 'GEOPOLITICS', 'Geopolitics', 'Global Politics, Policy', 'ACTIVE'),
('f000000e-0000-0000-0000-00000000000e', 'SYSTEMS_ARCHITECTURE', 'Systems & Architecture', 'Operating systems, distributed systems, database internals, and infrastructure', 'ACTIVE'),
('f000000f-0000-0000-0000-00000000000f', 'VENTURE_CAPITAL', 'Venture & Macro', 'Venture capital, macro economics, funding, strategic investments', 'ACTIVE'),
('f0000010-0000-0000-0000-000000000010', 'SECURITY_SOVEREIGNTY', 'Security & Sovereignty', 'Cybersecurity, privacy, sovereignty, cryptographic systems', 'ACTIVE'),
('f0000011-0000-0000-0000-000000000011', 'DEV_TOOLS', 'Developer Tooling', 'Languages, frameworks, developer productivity, open source tools', 'ACTIVE'),
('f0000012-0000-0000-0000-000000000012', 'GENERAL', 'General Tech', 'Broad technology news and emerging trends', 'ACTIVE'),
('f0000013-0000-0000-0000-000000000013', 'QUANTUM_COMPUTING', 'Quantum Computing', 'Quantum hardware, quantum algorithms, quantum physics', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- Default Application Settings
INSERT INTO app_settings (key, value, description) VALUES
('monthly_expense_allowance', '75000', 'Monthly expense budget cap in INR')
ON CONFLICT (key) DO NOTHING;
