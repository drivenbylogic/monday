-- =============================================================================
-- Monday Application: Master Sample Data
-- File: master_sample_data.sql
-- Description: Comprehensive testing seed inserting 5 coherent, domain-relevant
--              rows into every table (45 tables total) across all modules.
-- Target Database: PostgreSQL 17+ / 18+
-- =============================================================================

-- =============================================================================
-- 1. USERS & AUTHENTICATION (5 Rows)
-- =============================================================================
INSERT INTO users (id, username, email, password_hash, role) VALUES 
('11111111-1111-1111-1111-111111111101', 'alexander.cross', 'alexander.cross@sovereign.io', '$2a$12$e80/L38fR0gH8eSgqZ1KjeS2mE0aWzT4bT8yZ0dE9oT6oI1wH7c9u', 'ROLE_ADMIN'),
('11111111-1111-1111-1111-111111111102', 'elena.rostova', 'elena.rostova@sovereign.io', '$2a$12$e80/L38fR0gH8eSgqZ1KjeS2mE0aWzT4bT8yZ0dE9oT6oI1wH7c9u', 'ROLE_USER'),
('11111111-1111-1111-1111-111111111103', 'marcus.vance', 'marcus.vance@sovereign.io', '$2a$12$e80/L38fR0gH8eSgqZ1KjeS2mE0aWzT4bT8yZ0dE9oT6oI1wH7c9u', 'ROLE_USER'),
('11111111-1111-1111-1111-111111111104', 'sarah.chen', 'sarah.chen@sovereign.io', '$2a$12$e80/L38fR0gH8eSgqZ1KjeS2mE0aWzT4bT8yZ0dE9oT6oI1wH7c9u', 'ROLE_USER'),
('11111111-1111-1111-1111-111111111105', 'david.kim', 'david.kim@sovereign.io', '$2a$12$e80/L38fR0gH8eSgqZ1KjeS2mE0aWzT4bT8yZ0dE9oT6oI1wH7c9u', 'ROLE_USER')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 2. PROJECTS & DEEP WORK MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO projects (id, name, description, category, status, priority, tags, start_at, due_at, completed_at) VALUES 
('22222222-2222-2222-2222-222222222201', 'Project Sovereign: Autonomous Operations Hub', 'Deploying distributed command center and telemetry stream for executive life operations', 'SYSTEMS', 'ACTIVE', 'HIGH', '["command-center", "automation", "architecture"]'::jsonb, '2026-08-01 09:00:00Z', '2026-12-31 18:00:00Z', NULL),
('22222222-2222-2222-2222-222222222202', 'Strategic Capital Allocation & Sovereign Treasury', 'Rebalancing private equity allocations, bond laddering, and liquidity stress testing', 'FINANCE', 'IN_PROGRESS', 'CRITICAL', '["capital", "treasury", "liquidity"]'::jsonb, '2026-09-15 09:00:00Z', '2026-11-30 18:00:00Z', NULL),
('22222222-2222-2222-2222-222222222203', 'High-Performance Longevity & Endurance Peak', 'Targeting sub-3 marathon and VO2 max optimization with biometrics protocol', 'HEALTH', 'ACTIVE', 'HIGH', '["endurance", "longevity", "athletics"]'::jsonb, '2026-07-01 06:00:00Z', '2026-11-15 12:00:00Z', NULL),
('22222222-2222-2222-2222-222222222204', 'Family Estate Planning & Secure Vault Archival', 'Consolidating trust structures, notary records, and offsite physical archives', 'ADMIN', 'PLANNING', 'MEDIUM', '["legal", "trust", "vault"]'::jsonb, '2026-10-01 09:00:00Z', '2027-02-28 18:00:00Z', NULL),
('22222222-2222-2222-2222-222222222205', 'Next-Gen Autonomous Fleet Telemetry & Upgrades', 'Integrating CAN-bus high-rate diagnostics and predictive servicing for vehicle fleet', 'VEHICLES', 'ACTIVE', 'MEDIUM', '["fleet", "telemetry", "hardware"]'::jsonb, '2026-09-01 09:00:00Z', '2027-01-31 18:00:00Z', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO milestones (id, project_id, name, description, status, priority, start_at, due_at, completed_at) VALUES 
('33333333-3333-3333-3333-333333333301', '22222222-2222-2222-2222-222222222201', 'Core Distributed Engine Deployment', 'Zero-copy deserialization and high-throughput message bus operational', 'COMPLETED', 'HIGH', '2026-08-01 09:00:00Z', '2026-09-15 18:00:00Z', '2026-09-14 16:30:00Z'),
('33333333-3333-3333-3333-333333333302', '22222222-2222-2222-2222-222222222202', 'Sovereign Tax Loss Harvesting & Treasury Shift', 'Finalize year-end municipal bond swap and reallocation into short treasuries', 'IN_PROGRESS', 'CRITICAL', '2026-09-16 09:00:00Z', '2026-10-31 18:00:00Z', NULL),
('33333333-3333-3333-3333-333333333303', '22222222-2222-2222-2222-222222222203', 'Sub-3 Marathon Simulation Run', 'Execute 35km race-pace progression run with continuous lactate sampling', 'PENDING', 'HIGH', '2026-10-25 06:00:00Z', '2026-10-25 10:00:00Z', NULL),
('33333333-3333-3333-3333-333333333304', '22222222-2222-2222-2222-222222222204', 'Biometric Hardware Key Escrow Integration', 'Multi-signature air-gapped seed verification across two global vault locations', 'PENDING', 'MEDIUM', '2026-11-01 09:00:00Z', '2026-11-20 18:00:00Z', NULL),
('33333333-3333-3333-3333-333333333305', '22222222-2222-2222-2222-222222222205', 'CAN-Bus Diagnostic Pipeline Integration', 'Stream real-time oil temperature and brake wear data to central dashboard', 'COMPLETED', 'HIGH', '2026-09-01 09:00:00Z', '2026-09-30 18:00:00Z', '2026-09-28 14:15:00Z')
ON CONFLICT (id) DO NOTHING;

INSERT INTO deep_work_sessions (id, project_id, duration_minutes, started_at, ended_at, focus_score, notes, tasks_completed) VALUES 
('d1111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222201', 180, '2026-10-16 08:00:00Z', '2026-10-16 11:00:00Z', 95, 'Refactored cross-process IPC telemetry channel & benchmarked zero-copy deserialization', '["task-ipc-01", "task-ipc-02"]'::jsonb),
('d1111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222202', 210, '2026-10-17 09:00:00Z', '2026-10-17 12:30:00Z', 90, 'Macro liquidity scenarios and yield curve inversion stress modeling', '["task-treasury-01"]'::jsonb),
('d1111111-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222201', 150, '2026-10-15 13:00:00Z', '2026-10-15 15:30:00Z', 92, 'PostgreSQL index profiling and connection pool throughput tuning', '["task-db-01"]'::jsonb),
('d1111111-1111-1111-1111-111111111104', '22222222-2222-2222-2222-222222222203', 240, '2026-10-14 06:00:00Z', '2026-10-14 10:00:00Z', 88, 'Endurance biomechanics review and power-to-weight target calibration', '["task-bio-01"]'::jsonb),
('d1111111-1111-1111-1111-111111111105', '22222222-2222-2222-2222-222222222204', 120, '2026-10-13 14:00:00Z', '2026-10-13 16:00:00Z', 98, 'Drafted family trust governance charters and irrevocable trust addenda', '["task-trust-01"]'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 3. PERSONAL EVENTS & SCHEDULES MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO events (id, title, description, event_type, start_at, end_at, all_day, is_recurring, recurrence_pattern, location, status, requires_prep, prep_title, prep_due_text) VALUES 
('44444444-4444-4444-4444-444444444401', 'Sovereign Capital Annual Board of Directors Meeting', 'Quarterly strategy and balance sheet review with board members', 'WORK', '2026-10-24 10:00:00Z', '2026-10-24 15:00:00Z', FALSE, FALSE, NULL, 'Executive Boardroom & Zoom Telepresence', 'SCHEDULED', TRUE, 'Prepare Q3 Financial Package', 'Due 2 days prior'),
('44444444-4444-4444-4444-444444444402', 'Global AI & Distributed Systems Summit 2026', 'Keynote address on autonomous agent architecture and safety loops', 'CONFERENCE', '2026-11-05 09:00:00Z', '2026-11-07 18:00:00Z', FALSE, FALSE, NULL, 'Palace of Fine Arts, San Francisco', 'SCHEDULED', TRUE, 'Finalize Keynote Slides & Media', 'Due 3 days prior'),
('44444444-4444-4444-4444-444444444403', 'Bi-Annual Executive Health & Longevity Diagnostic', 'Full-body MRI scan, DEXA scan, VO2 max test, and comprehensive biomarkers', 'HEALTH', '2026-10-28 07:30:00Z', '2026-10-28 12:30:00Z', FALSE, FALSE, NULL, 'Fountain Life Center, Naples FL', 'SCHEDULED', TRUE, 'Complete 12-Hour Fasting Protocol', 'Morning of appointment'),
('44444444-4444-4444-4444-444444444404', 'Private Equity Syndicate Quarterly Dinner', 'Strategic dinner with Tier-1 general partners and venture syndicate heads', 'SOCIAL', '2026-10-20 19:30:00Z', '2026-10-20 22:30:00Z', FALSE, FALSE, NULL, 'The French Laundry, Yountville', 'SCHEDULED', FALSE, NULL, NULL),
('44444444-4444-4444-4444-444444444405', 'Family Trust & Estate Planning Legal Advisory', 'In-person review of irrevocable grantor trust structure and beneficiary designations', 'LEGAL', '2026-11-12 14:00:00Z', '2026-11-12 16:30:00Z', FALSE, FALSE, NULL, 'Sullivan & Cromwell Law Offices, NYC', 'SCHEDULED', TRUE, 'Collate Share Certificates & Notarizations', 'Due 1 day prior')
ON CONFLICT (id) DO NOTHING;

INSERT INTO event_prep_items (id, event_id, text, completed, due) VALUES 
('55555555-5555-5555-5555-555555555501', '44444444-4444-4444-4444-444444444401', 'Compile Q3 consolidated financial audit statements', TRUE, '2026-10-22'),
('55555555-5555-5555-5555-555555555502', '44444444-4444-4444-4444-444444444401', 'Review executive compensation committee proposals', FALSE, '2026-10-23'),
('55555555-5555-5555-5555-555555555503', '44444444-4444-4444-4444-444444444402', 'Deliver slide deck version 1.0 to summit organizers', FALSE, '2026-11-02'),
('55555555-5555-5555-5555-555555555504', '44444444-4444-4444-4444-444444444403', 'Complete blood metabolic intake questionnaire', TRUE, '2026-10-27'),
('55555555-5555-5555-5555-555555555505', '44444444-4444-4444-4444-444444444405', 'Retrieve original notarized deed certificates from safe', FALSE, '2026-11-11')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 4. UNIFIED TASKS & REMINDERS MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO tasks (id, title, description, project_id, milestone_id, event_id, parent_task_id, status, priority, due_at, started_at, completed_at, estimated_minutes, is_recurring, recurrence_pattern) VALUES 
('66666666-6666-6666-6666-666666666601', 'Deploy high-throughput caching tier for telemetry', 'Configure Redis cluster with sub-millisecond p99 latency guarantees', '22222222-2222-2222-2222-222222222201', '33333333-3333-3333-3333-333333333301', NULL, NULL, 'COMPLETED', 'HIGH', '2026-10-18 12:00:00Z', '2026-10-18 09:00:00Z', '2026-10-18 11:30:00Z', 150, FALSE, NULL),
('66666666-6666-6666-6666-666666666602', 'Review municipal bond reallocation tranches', 'Assess tax-equivalent yields and liquidity duration matching', '22222222-2222-2222-2222-222222222202', '33333333-3333-3333-3333-333333333302', NULL, NULL, 'IN_PROGRESS', 'CRITICAL', '2026-10-21 17:00:00Z', '2026-10-18 14:00:00Z', NULL, 90, FALSE, NULL),
('66666666-6666-6666-6666-666666666603', '32km tempo long-run with biometric telemetry', 'Progressive negative split workout targeting 3:55 min/km final 10km', '22222222-2222-2222-2222-222222222203', '33333333-3333-3333-3333-333333333303', NULL, NULL, 'PENDING', 'HIGH', '2026-10-20 06:30:00Z', NULL, NULL, 140, FALSE, NULL),
('66666666-6666-6666-6666-666666666604', 'Execute biometric safe deposit hardware verification', 'Test dual-custody YubiKey 5Ci hardware keys against Zurich depository backup', '22222222-2222-2222-2222-222222222204', '33333333-3333-3333-3333-333333333304', NULL, NULL, 'PENDING', 'MEDIUM', '2026-11-05 15:00:00Z', NULL, NULL, 60, FALSE, NULL),
('66666666-6666-6666-6666-666666666605', 'Calibrate Porsche 911 GT3 cold tire pressures and telemetry', 'Set hot target 2.0 bar front / 2.1 bar rear before Friday track morning', '22222222-2222-2222-2222-222222222205', '33333333-3333-3333-3333-333333333305', NULL, NULL, 'IN_PROGRESS', 'MEDIUM', '2026-10-19 16:00:00Z', '2026-10-18 16:30:00Z', NULL, 45, FALSE, NULL)
ON CONFLICT (id) DO NOTHING;

-- Reminders enforce exclusive target check: (task_id IS NOT NULL XOR event_id IS NOT NULL)
INSERT INTO reminders (id, task_id, event_id, remind_at, title, description, status) VALUES 
('77777777-7777-7777-7777-777777777701', '66666666-6666-6666-6666-666666666601', NULL, '2026-10-18 11:00:00Z', 'Telemetry Caching Verification', 'Check latency telemetry dashboards on Grafana', 'COMPLETED'),
('77777777-7777-7777-7777-777777777702', '66666666-6666-6666-6666-666666666602', NULL, '2026-10-21 15:00:00Z', 'Bond Rebalancing Window Closes', 'Execute order before settlement cutoff at 4 PM EST', 'PENDING'),
('77777777-7777-7777-7777-777777777703', NULL, '44444444-4444-4444-4444-444444444401', '2026-10-24 08:30:00Z', 'Board Meeting Preparations', 'Final review of telepresence feed and agenda printouts', 'PENDING'),
('77777777-7777-7777-7777-777777777704', NULL, '44444444-4444-4444-4444-444444444403', '2026-10-27 20:00:00Z', 'Commence Longevity Clinic Fast', 'No caloric intake, plain electrolytes only', 'PENDING'),
('77777777-7777-7777-7777-777777777705', '66666666-6666-6666-6666-666666666605', NULL, '2026-10-19 14:00:00Z', 'Porsche Tire Pressure Verification', 'Verify digital pressure gauge readings at ambient 20C', 'PENDING')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 5. FINANCE & WEALTH PORTFOLIO MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO budget_categories (id, name, description, status) VALUES 
('88888888-8888-8888-8888-888888888801', 'Executive Aviation & Mobility', 'Private aviation charter fractional programs and executive ground transport', 'ACTIVE'),
('88888888-8888-8888-8888-888888888802', 'Cloud Compute & Dedicated AI Clusters', 'Hyperscaler cloud instances, dedicated H100 inference clusters, and hosting', 'ACTIVE'),
('88888888-8888-8888-8888-888888888803', 'Legal, Sovereign Trust & Compliance Retainers', 'Tier-1 law firm retainers, trust administration, and regulatory filings', 'ACTIVE'),
('88888888-8888-8888-8888-888888888804', 'Fleet Operations, Track & Maintenance', 'Vehicle fleet insurance, servicing, track day logistics, and fuel', 'ACTIVE'),
('88888888-8888-8888-8888-888888888805', 'Longevity, Precision Health & Athletic Protocol', 'Executive medical diagnostics, genetic sequencing, and biometric training', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO budgets (id, name, category_id, period_start, period_end, amount) VALUES 
('99999999-9999-9999-9999-999999999901', 'Q4 Aviation Fractional Budget', '88888888-8888-8888-8888-888888888801', '2026-10-01', '2026-12-31', 125000.00),
('99999999-9999-9999-9999-999999999902', 'Q4 Dedicated GPU Compute Budget', '88888888-8888-8888-8888-888888888802', '2026-10-01', '2026-12-31', 45000.00),
('99999999-9999-9999-9999-999999999903', 'Q4 Trust Legal Retainer Budget', '88888888-8888-8888-8888-888888888803', '2026-10-01', '2026-12-31', 60000.00),
('99999999-9999-9999-9999-999999999904', 'Q4 Vehicle Fleet & Performance Budget', '88888888-8888-8888-8888-888888888804', '2026-10-01', '2026-12-31', 30000.00),
('99999999-9999-9999-9999-999999999905', 'Q4 Precision Health & Longevity Budget', '88888888-8888-8888-8888-888888888805', '2026-10-01', '2026-12-31', 25000.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO finance_accounts (id, institution, account_type, balance, currency, last_reconciled) VALUES 
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'J.P. Morgan Private Bank', 'Executive Wealth Management', 14250000.00, 'USD', '2026-10-17 18:00:00Z'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'Goldman Sachs Private Wealth', 'Treasury Liquidity & Repo', 8650000.00, 'USD', '2026-10-16 18:00:00Z'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03', 'Morgan Stanley Private Custody', 'Global Equity Portfolio', 21400000.00, 'USD', '2026-10-15 18:00:00Z'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 'Pictet & Cie Bankers', 'Swiss Sovereign Reserve', 11500000.00, 'CHF', '2026-10-14 18:00:00Z'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 'HDFC Private Banking', 'Subcontinent Strategic Operating', 485000000.00, 'INR', '2026-10-17 12:00:00Z')
ON CONFLICT (id) DO NOTHING;

INSERT INTO finance_commitments (id, name, due_date, amount, account_id, is_urgent, status) VALUES 
('cccccccc-cccc-cccc-cccc-cccccccccc01', 'Q4 Commercial Real Estate Primary Lease', '2026-11-01', 35000.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', FALSE, 'PENDING'),
('cccccccc-cccc-cccc-cccc-cccccccccc02', 'NetJets Gulfstream 25-Hour Card Installment', '2026-10-25', 68500.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', TRUE, 'PENDING'),
('cccccccc-cccc-cccc-cccc-cccccccccc03', 'Tier-1 AI Syndicate Capital Call tranche IV', '2026-11-10', 250000.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', FALSE, 'PENDING'),
('cccccccc-cccc-cccc-cccc-cccccccccc04', 'Chubb Masterpiece Umbrella Insurance Premium', '2026-10-30', 14800.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', FALSE, 'PENDING'),
('cccccccc-cccc-cccc-cccc-cccccccccc05', 'Ernst & Young Global Sovereign Tax Compliance', '2026-11-15', 45000.00, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', FALSE, 'PENDING')
ON CONFLICT (id) DO NOTHING;

INSERT INTO transactions (id, transaction_type, amount, transaction_at, category_id, payment_method, description, notes, linked_module, linked_entity_id, linked_entity_type) VALUES 
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'EXPENSE', 42500.00, '2026-10-12 14:30:00Z', '88888888-8888-8888-8888-888888888801', 'Wire Transfer', 'NetJets Citation Longitude Flight Leg (SFO to MIA)', 'Quarterly executive repositioning flight', 'finance', 'cccccccc-cccc-cccc-cccc-cccccccccc02', 'commitment'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', 'EXPENSE', 18450.00, '2026-10-14 09:15:00Z', '88888888-8888-8888-8888-888888888802', 'Corporate Card', 'CoreWeave Dedicated H100 GPU Pod Monthly', 'Inference cluster for agenda priority engine', 'intelligence', 'brief-1', 'brief'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03', 'EXPENSE', 15000.00, '2026-10-15 11:00:00Z', '88888888-8888-8888-8888-888888888803', 'Wire Transfer', 'Sullivan & Cromwell Q4 Retainer Retainer Fee', 'Trust and sovereign corporate structuring advisory', 'admin', '09090909-0909-0909-0909-090909090901', 'obligation'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa04', 'EXPENSE', 3850.00, '2026-10-16 16:20:00Z', '88888888-8888-8888-8888-888888888804', 'Corporate Card', 'Porsche Motorsport Manthey Racing Brake Calipers & Pads', 'Pre-track inspection parts replacement', 'vehicles', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'vehicle'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa05', 'EXPENSE', 7500.00, '2026-10-17 10:00:00Z', '88888888-8888-8888-8888-888888888805', 'Direct Debit', 'Fountain Life Comprehensive Genetic & Whole Body MRI Protocol', 'Annual biomarker longevity assessment', 'events', '44444444-4444-4444-4444-444444444403', 'event')
ON CONFLICT (id) DO NOTHING;

INSERT INTO portfolio_allocations (id, user_id, asset_class, label, current_value, unrealized_gain_percent, risk_rating) VALUES 
('a1111111-1111-1111-1111-111111111101', '11111111-1111-1111-1111-111111111101', 'EQUITY', 'Public Markets & Global Tech Indexes', 28400000.00, 18.40, 'MODERATE'),
('a1111111-1111-1111-1111-111111111102', '11111111-1111-1111-1111-111111111101', 'TREASURY_BONDS', 'Sovereign Physical Gold & Short Treasuries', 12650000.00, 7.10, 'LOW'),
('a1111111-1111-1111-1111-111111111103', '11111111-1111-1111-1111-111111111101', 'VENTURE_ESCROW', 'Early-Stage AI & Frontier Tech Syndicates', 7200000.00, 0.00, 'HIGH'),
('a1111111-1111-1111-1111-111111111104', '11111111-1111-1111-1111-111111111101', 'REAL_ESTATE', 'Prime Commercial & Data Center Assets', 18500000.00, 12.30, 'LOW'),
('a1111111-1111-1111-1111-111111111105', '11111111-1111-1111-1111-111111111101', 'PRIVATE_CREDIT', 'Senior Secured Infrastructure Debt Tranches', 9800000.00, 9.80, 'MODERATE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO credit_accounts (id, user_id, card_id, card_name, last_four, network, credit_limit, current_balance, statement_closing_date, payment_due_date, min_amount_due, auto_pay_enabled) VALUES 
('c1111111-1111-1111-1111-111111111101', '11111111-1111-1111-1111-111111111101', 'card-amex-centurion', 'American Express Centurion Black', '0001', 'AMEX', 10000000.00, 142650.00, '2026-10-24', '2026-11-08', 7132.50, TRUE),
('c1111111-1111-1111-1111-111111111102', '11111111-1111-1111-1111-111111111101', 'card-jpm-reserve', 'J.P. Morgan Reserve Palladium', '4892', 'VISA', 5000000.00, 48200.00, '2026-10-20', '2026-11-05', 2410.00, TRUE),
('c1111111-1111-1111-1111-111111111103', '11111111-1111-1111-1111-111111111101', 'card-citi-chairman', 'Citi Chairman Executive', '8812', 'MASTERCARD', 2500000.00, 18900.00, '2026-10-28', '2026-11-12', 945.00, TRUE),
('c1111111-1111-1111-1111-111111111104', '11111111-1111-1111-1111-111111111101', 'card-brex-corporate', 'Brex Sovereign Enterprise', '1093', 'MASTERCARD', 3000000.00, 62150.00, '2026-10-31', '2026-11-15', 62150.00, TRUE),
('c1111111-1111-1111-1111-111111111105', '11111111-1111-1111-1111-111111111101', 'card-hdfc-infinia', 'HDFC Infinia Metal Reserve', '4102', 'VISA', 1200000.00, 56050.00, '2026-10-18', '2026-11-03', 2800.00, TRUE)
ON CONFLICT (card_id) DO NOTHING;

-- =============================================================================
-- 6. VEHICLE FLEET MANAGEMENT MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO vehicles (id, name, registration_number, purchase_date, purchase_amount, current_km, status, edition, variant, type, is_primary, hero_image_url, vin, emission_standard, engine_json, chassis_json, service_manual_url, registered_owner, registering_authority) VALUES 
('dddddddd-dddd-dddd-dddd-dddddddddd01', 'Porsche 911 GT3 RS', 'S-GO-9923', '2024-04-15', 315000.00, 8450, 'ACTIVE', '992 Weissach Package', 'GT3 RS 4.0L', 'Coupe', TRUE, 'https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e', 'WP0AF2A97RS123456', 'Euro 6d-ISC-FCM', '{"displacement": "4.0L Flat-6", "power_hp": 518, "max_rpm": 9000, "transmission": "7-Speed PDK"}'::jsonb, '{"suspension": "Double wishbone front with active aero", "brakes": "PCCB Ceramic Composite"}'::jsonb, 'https://porsche.com/manuals/gt3rs-992', 'Alexander Cross', 'Kraftfahrt-Bundesamt Stuttgart'),
('dddddddd-dddd-dddd-dddd-dddddddddd02', 'Mercedes-Maybach S 680', 'B-MM-6800', '2023-11-20', 285000.00, 24100, 'ACTIVE', 'Night Series Edition', 'S 680 4MATIC V12', 'Sedan', FALSE, 'https://images.unsplash.com/photo-1563720223185-11003d516935', 'W1K2231761A987654', 'Euro 6d', '{"displacement": "6.0L Biturbo V12", "power_hp": 621, "torque_nm": 900}'::jsonb, '{"suspension": "E-ACTIVE BODY CONTROL with rear-axle steering"}'::jsonb, 'https://mercedes-benz.com/manuals/maybach-s680', 'Alexander Cross', 'Bürgeramt Berlin'),
('dddddddd-dddd-dddd-dddd-dddddddddd03', 'Land Rover Defender 110 V8', 'L-DF-1108', '2024-01-10', 135000.00, 19800, 'ACTIVE', 'Carpathian Edition', 'Defender 110 V8 Supercharged', 'SUV', FALSE, 'https://images.unsplash.com/photo-1542282088-72c9c27ed0cd', 'SALWR2V42P2456789', 'Euro 6d', '{"displacement": "5.0L Supercharged V8", "power_hp": 518, "towing_capacity_kg": 3500}'::jsonb, '{"suspension": "Electronic air suspension with adaptive dynamics"}'::jsonb, 'https://landrover.com/manuals/defender-110', 'Alexander Cross', 'DVLA Swansea'),
('dddddddd-dddd-dddd-dddd-dddddddddd04', 'Lucid Air Sapphire', 'CA-LU-7777', '2024-06-05', 249000.00, 6200, 'ACTIVE', 'Sapphire Tri-Motor', 'Air Sapphire', 'Sedan', FALSE, 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d', '55NFG2B48PA112233', 'Zero-Emission EV', '{"powertrain": "Tri-Motor AWD", "power_hp": 1234, "battery_kwh": 118, "range_miles": 427}'::jsonb, '{"brakes": "Carbon-ceramic 420mm front rotors with 10-piston calipers"}'::jsonb, 'https://lucidmotors.com/manuals/air-sapphire', 'Alexander Cross', 'California DMV Sacramento'),
('dddddddd-dddd-dddd-dddd-dddddddddd05', 'Ferrari 812 Competizione', 'MO-FC-8120', '2023-09-18', 650000.00, 3100, 'ACTIVE', 'Tailor Made Spec', '812 Competizione V12', 'Coupe', FALSE, 'https://images.unsplash.com/photo-1583121274602-3e2820c69888', 'ZFF91BFA9N0298765', 'Euro 6d', '{"displacement": "6.5L Naturally Aspirated V12", "power_hp": 819, "max_rpm": 9500}'::jsonb, '{"suspension": "MagneRide with independent four-wheel steering"}'::jsonb, 'https://ferrari.com/manuals/812-competizione', 'Alexander Cross', 'Motorizzazione Civile Modena')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_service_records (id, vehicle_id, service_date, km_at_service, service_type, description, cost, performed_by, notes, specifications_json, parts_cost, labor_cost, verified, is_upcoming, linked_transaction_id) VALUES 
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'dddddddd-dddd-dddd-dddd-dddddddddd01', '2026-07-15', 7500, 'Track Precision Setup & Brake Fluid Flush', 'Replaced fluid with Castrol SRF racing fluid and inspected carbon-ceramic rotor surface wear', 1250.00, 'Porsche Motorsport Weissach Center', 'Rotor wear index at 98.4%', '{"rotor_thickness_mm": 38.0, "fluid_boiling_point_c": 310}'::jsonb, 650.00, 600.00, TRUE, FALSE, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa04'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 'dddddddd-dddd-dddd-dddd-dddddddddd02', '2026-06-10', 20000, 'Maybach Scheduled Executive A-Service', 'Mobil 1 0W-40 full synthetic oil change, cabin HEPA filters, rear refrigerator flush', 980.00, 'Mercedes-Benz Niederlassung Berlin', 'All onboard concierge modules verified', '{"oil_grade": "0W-40 229.5", "filters_replaced": ["oil", "pollen", "hepa"]}'::jsonb, 420.00, 560.00, TRUE, FALSE, NULL),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 'dddddddd-dddd-dddd-dddd-dddddddddd03', '2026-05-22', 15000, 'Expedition Transfer Case & Driveline Service', 'Heavy-duty differential fluid refresh, winch calibration, underbody armor bolt torque check', 840.00, 'Land Rover West London Service', 'No off-road trail damage detected', '{"differential_oil": "Castrol Syntrax 75W-90", "winch_test_pull_lbs": 10000}'::jsonb, 380.00, 460.00, TRUE, FALSE, NULL),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', 'dddddddd-dddd-dddd-dddd-dddddddddd04', '2026-08-05', 5000, 'Sapphire Thermal Inverter & HV Diagnostic', 'Checked coolant loops for triple electric motors, verified track battery conditioning speed', 450.00, 'Lucid Motors Palo Alto Service', 'Firmware 2.4.1 calibrated to motors', '{"coolant_ph": 7.4, "inverter_efficiency_percent": 99.1}'::jsonb, 120.00, 330.00, TRUE, FALSE, NULL),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 'dddddddd-dddd-dddd-dddd-dddddddddd05', '2026-04-18', 2500, 'Ferrari Annual Factory Certification', 'Annual fluids renewal, titanium valvetrain inspection, Ferrari Classiche telemetry export', 2400.00, 'Ferrari Factory Service Maranello', 'Pristine collector preservation status', '{"oil_spec": "Shell Helix Ultra 10W-60", "exhaust_valve_test": "PASSED"}'::jsonb, 1100.00, 1300.00, TRUE, FALSE, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_maintenance_schedules (id, vehicle_id, item_name, interval_months, interval_km, last_performed_at, last_performed_km, next_due_at, next_due_km, status) VALUES 
('ffffffff-ffff-ffff-ffff-ffffffffff01', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'Ceramic Brake Caliper Pin & Seal Inspection', 6, 5000, '2026-07-15', 7500, '2027-01-15', 12500, 'OK'),
('ffffffff-ffff-ffff-ffff-ffffffffff02', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'V12 Spark Plug & Ignition Coil Refresh', 24, 30000, '2025-06-10', 10000, '2027-06-10', 40000, 'OK'),
('ffffffff-ffff-ffff-ffff-ffffffffff03', 'dddddddd-dddd-dddd-dddd-dddddddddd03', 'All-Terrain Tire Rotation & Computer Alignment', 6, 8000, '2026-05-22', 15000, '2026-11-22', 23000, 'OK'),
('ffffffff-ffff-ffff-ffff-ffffffffff04', 'dddddddd-dddd-dddd-dddd-dddddddddd04', 'High-Voltage Battery Deep Cell Balancing', 12, 15000, '2026-08-05', 5000, '2027-08-05', 20000, 'OK'),
('ffffffff-ffff-ffff-ffff-ffffffffff05', 'dddddddd-dddd-dddd-dddd-dddddddddd05', 'Titanium Exhaust Butterfly Actuator Lubrication', 12, 5000, '2026-04-18', 2500, '2027-04-18', 7500, 'OK')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_obligations (id, vehicle_id, obligation_type, due_date, amount, status, notes) VALUES 
('01010101-0101-0101-0101-010101010101', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'Supercar Annual Emissions & Registration', '2027-04-01', 580.00, 'PENDING', 'German TÜV and road tax annual renewal'),
('01010101-0101-0101-0101-010101010102', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'Executive Commercial Transport Chauffeur Permit', '2027-01-15', 350.00, 'PENDING', 'VIP mobility registration certification'),
('01010101-0101-0101-0101-010101010103', 'dddddddd-dddd-dddd-dddd-dddddddddd03', 'UK VED Road Tax & Overland Recovery Permit', '2027-02-01', 720.00, 'PENDING', 'DVLA annual taxation'),
('01010101-0101-0101-0101-010101010104', 'dddddddd-dddd-dddd-dddd-dddddddddd04', 'California Zero-Emission Clean Air Decal & Reg', '2027-06-01', 420.00, 'PENDING', 'HOV lane access renewal sticker'),
('01010101-0101-0101-0101-010101010105', 'dddddddd-dddd-dddd-dddd-dddddddddd05', 'Italian Superbollo Performance Excise Tax', '2027-03-15', 3200.00, 'PENDING', 'High-horsepower surcharge filing with Agenzia delle Entrate')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_telemetry (id, vehicle_id, recorded_at, odometer_json, next_scheduled_service_json, tires_json, electrical_json, drivetrain_json, fuel_json, operational_readiness_score) VALUES 
('02020202-0202-0202-0202-020202020201', 'dddddddd-dddd-dddd-dddd-dddddddddd01', '2026-10-18 17:30:00Z', '{"km": 8450, "trip_a": 142.5}'::jsonb, '{"date": "2027-01-15", "km": 12500}'::jsonb, '{"front_left_bar": 2.05, "front_right_bar": 2.05, "rear_left_bar": 2.12, "rear_right_bar": 2.12}'::jsonb, '{"battery_volts": 13.8, "health_percent": 100}'::jsonb, '{"oil_temp_c": 98, "coolant_temp_c": 90, "gearbox_temp_c": 85}'::jsonb, '{"fuel_level_percent": 78, "fuel_range_km": 420}'::jsonb, 98),
('02020202-0202-0202-0202-020202020202', 'dddddddd-dddd-dddd-dddd-dddddddddd02', '2026-10-18 16:45:00Z', '{"km": 24100, "trip_a": 310.2}'::jsonb, '{"date": "2027-06-10", "km": 40000}'::jsonb, '{"front_left_bar": 2.40, "front_right_bar": 2.40, "rear_left_bar": 2.50, "rear_right_bar": 2.50}'::jsonb, '{"battery_volts": 14.1, "health_percent": 96}'::jsonb, '{"oil_temp_c": 92, "coolant_temp_c": 88, "v12_smoothness_score": 100}'::jsonb, '{"fuel_level_percent": 65, "fuel_range_km": 540}'::jsonb, 95),
('02020202-0202-0202-0202-020202020203', 'dddddddd-dddd-dddd-dddd-dddddddddd03', '2026-10-17 19:10:00Z', '{"km": 19800, "trip_a": 520.0}'::jsonb, '{"date": "2026-11-22", "km": 23000}'::jsonb, '{"front_left_bar": 2.50, "front_right_bar": 2.50, "rear_left_bar": 2.70, "rear_right_bar": 2.70}'::jsonb, '{"battery_volts": 13.9, "health_percent": 94}'::jsonb, '{"oil_temp_c": 95, "terrain_response_mode": "AUTO"}'::jsonb, '{"fuel_level_percent": 45, "fuel_range_km": 390}'::jsonb, 92),
('02020202-0202-0202-0202-020202020204', 'dddddddd-dddd-dddd-dddd-dddddddddd04', '2026-10-18 18:00:00Z', '{"km": 6200, "trip_a": 88.0}'::jsonb, '{"date": "2027-08-05", "km": 20000}'::jsonb, '{"front_left_bar": 2.60, "front_right_bar": 2.60, "rear_left_bar": 2.70, "rear_right_bar": 2.70}'::jsonb, '{"hv_pack_volts": 924.5, "state_of_charge_percent": 86}'::jsonb, '{"motor_front_temp_c": 48, "motor_rear_left_temp_c": 52, "motor_rear_right_temp_c": 51}'::jsonb, '{"battery_energy_kwh": 101.4, "projected_range_km": 585}'::jsonb, 99),
('02020202-0202-0202-0202-020202020205', 'dddddddd-dddd-dddd-dddd-dddddddddd05', '2026-10-16 14:15:00Z', '{"km": 3100, "trip_a": 64.2}'::jsonb, '{"date": "2027-04-18", "km": 7500}'::jsonb, '{"front_left_bar": 2.10, "front_right_bar": 2.10, "rear_left_bar": 2.15, "rear_right_bar": 2.15}'::jsonb, '{"battery_volts": 13.7, "health_percent": 100}'::jsonb, '{"oil_temp_c": 96, "oil_pressure_bar": 5.4, "exhaust_manifold_c": 410}'::jsonb, '{"fuel_level_percent": 90, "fuel_range_km": 360}'::jsonb, 97)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_diagnostics (id, vehicle_id, scanned_at, diagnostic_protocol, fault_codes_detected, dtc_list, subsystem_status, summary) VALUES 
('03030303-0303-0303-0303-030303030301', 'dddddddd-dddd-dddd-dddd-dddddddddd01', '2026-10-18 17:35:00Z', 'OBD-II / Porsche PIWIS 4 Diagnostic', 0, '[]'::jsonb, '{"engine": "OPTIMAL", "transmission": "OPTIMAL", "aerodynamics": "CALIBRATED"}'::jsonb, 'Zero fault codes detected across 32 vehicle ECUs. Active aero wing calibrated.'),
('03030303-0303-0303-0303-030303030302', 'dddddddd-dddd-dddd-dddd-dddddddddd02', '2026-10-18 16:50:00Z', 'Mercedes Xentry Diagnostic System', 0, '[]'::jsonb, '{"engine": "OPTIMAL", "air_suspension": "NOMINAL", "comfort_systems": "NOMINAL"}'::jsonb, 'Clean diagnostic pass. Executive reclining seat calibrations fully aligned.'),
('03030303-0303-0303-0303-030303030303', 'dddddddd-dddd-dddd-dddd-dddddddddd03', '2026-10-17 19:15:00Z', 'JLR Pathfinder Diagnostic Tool', 1, '[{"code": "C1A20-64", "description": "Pressure increases too slow when filling reservoir", "status": "HISTORIC"}]'::jsonb, '{"suspension": "ADVISORY", "drivetrain": "OPTIMAL"}'::jsonb, 'Single historic compressor warning logged during high altitude pass. Cleared and re-tested nominal.'),
('03030303-0303-0303-0303-030303030304', 'dddddddd-dddd-dddd-dddd-dddddddddd04', '2026-10-18 18:05:00Z', 'Lucid In-House CAN FD Diagnostic Protocol', 0, '[]'::jsonb, '{"battery_management": "OPTIMAL", "inverters": "OPTIMAL", "thermal_loops": "OPTIMAL"}'::jsonb, 'All 924V bus modules operating at peak efficiency. Cell delta under 6mV.'),
('03030303-0303-0303-0303-030303030305', 'dddddddd-dddd-dddd-dddd-dddddddddd05', '2026-10-16 14:20:00Z', 'Ferrari DEIS Diagnostic Tester', 0, '[]'::jsonb, '{"v12_engine": "OPTIMAL", "e_diff": "OPTIMAL", "side_slip_control": "CALIBRATED"}'::jsonb, 'Factory telemetry export confirmed perfect combustion balance and zero misfires.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_fuel_logs (id, vehicle_id, log_date, brand, location, volume_liters, total_cost, price_per_liter, odometer_km, trip_efficiency, fuel_brand, fuel_grade, station_location, full_tank) VALUES 
('04040404-0404-0404-0404-040404040401', 'dddddddd-dddd-dddd-dddd-dddddddddd01', '2026-10-15', 'Shell', 'Autobahn A8 Tankstelle Holzkirchen', 58.50, 134.55, 2.30, 8200, 13.80, 'Shell V-Power Racing', '100 Octane', 'Holzkirchen Süd, A8 km 42', TRUE),
('04040404-0404-0404-0404-040404040402', 'dddddddd-dddd-dddd-dddd-dddddddddd02', '2026-10-12', 'Aral', 'Aral Tankstelle Kurfürstendamm Berlin', 72.00, 154.80, 2.15, 23750, 14.50, 'Aral Ultimate', '102 Octane', 'Kurfürstendamm 128, Berlin', TRUE),
('04040404-0404-0404-0404-040404040403', 'dddddddd-dddd-dddd-dddd-dddddddddd03', '2026-10-08', 'BP', 'BP Service Station M4 Junction 12', 80.00, 144.00, 1.80, 19300, 15.20, 'BP Ultimate Diesel', 'Premium Diesel', 'Bath Road, Reading RG7 4HG', TRUE),
('04040404-0404-0404-0404-040404040404', 'dddddddd-dddd-dddd-dddd-dddddddddd04', '2026-10-14', 'Electrify America', 'Stanford Shopping Center Superhub', 92.40, 44.35, 0.48, 6050, 21.50, 'Clean Energy DC Fast', '350kW CCS', '180 El Camino Real, Palo Alto CA', TRUE),
('04040404-0404-0404-0404-040404040405', 'dddddddd-dddd-dddd-dddd-dddddddddd05', '2026-10-10', 'Eni', 'Eni Station Modena Nord', 65.00, 149.50, 2.30, 2980, 18.20, 'Eni Super Racing 100', '100 Octane', 'Via Emilia Ovest, Modena', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_insurance (id, vehicle_id, policy_name, underwriter, policy_number, coverage_scope, insured_declared_value, premium_due, due_date, status, package_document_url, is_renewed) VALUES 
('05050505-0505-0505-0505-050505050501', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'Porsche Performance Agreed Value Policy', 'Hiscox Private Client', 'POL-GT3-88992', 'Agreed vehicle value $350k including closed track day coverage and OEM parts replacement', 350000.00, 4800.00, '2027-04-15', 'ACTIVE', 'https://vault.sovereign.io/docs/porsche_insurance_2026.pdf', TRUE),
('05050505-0505-0505-0505-050505050502', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'Executive VIP Fleet Comprehensive Shield', 'Allianz Global Corporate', 'POL-MAY-12001', 'Worldwide executive chauffeur coverage, emergency transport replacement, zero deductible', 285000.00, 3600.00, '2027-01-20', 'ACTIVE', 'https://vault.sovereign.io/docs/maybach_insurance_2026.pdf', TRUE),
('05050505-0505-0505-0505-050505050503', 'dddddddd-dddd-dddd-dddd-dddddddddd03', 'Overland Expedition & Recovery Policy', 'Chubb Masterpiece', 'POL-DEF-44551', 'Full comprehensive overland coverage across UK and Continental Europe with heavy extraction', 140000.00, 2200.00, '2027-02-10', 'ACTIVE', 'https://vault.sovereign.io/docs/defender_insurance_2026.pdf', TRUE),
('05050505-0505-0505-0505-050505050504', 'dddddddd-dddd-dddd-dddd-dddddddddd04', 'High-Output EV Performance Shield', 'AIG Private Client Group', 'POL-LUC-77662', 'Battery pack full replacement guarantee, charging station incident protection, glass rider', 250000.00, 3100.00, '2027-06-05', 'ACTIVE', 'https://vault.sovereign.io/docs/lucid_insurance_2026.pdf', TRUE),
('05050505-0505-0505-0505-050505050505', 'dddddddd-dddd-dddd-dddd-dddddddddd05', 'Ferrari Maranello Collector Heritage Cover', 'Hagerty Private Client', 'POL-FER-81200', 'Agreed collector value $650k, strictly climate-controlled transport rider, zero depreciation', 650000.00, 6200.00, '2027-03-18', 'ACTIVE', 'https://vault.sovereign.io/docs/ferrari_insurance_2026.pdf', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_documents (id, vehicle_id, doc_type, title, identifier, issuer, valid_until, verification_source, is_verified, status, covered_components) VALUES 
('06060606-0606-0606-0606-060606060601', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'Registration', 'Porsche GT3 RS Zulassungsbescheinigung Teil I & II', 'ZUL-S-GO-9923', 'Kraftfahrt-Bundesamt', '2027-04-15', 'KBA Central Register', TRUE, 'ACTIVE', '["engine", "chassis", "roll_cage", "aero"]'::jsonb),
('06060606-0606-0606-0606-060606060602', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'Permit', 'Maybach Executive Chauffeur Transport License', 'CHAUF-BER-8812', 'Senatsverwaltung Berlin', '2027-01-20', 'Berlin Licensing Authority', TRUE, 'ACTIVE', '["passenger_liability", "commercial_transit"]'::jsonb),
('06060606-0606-0606-0606-060606060603', 'dddddddd-dddd-dddd-dddd-dddddddddd03', 'Title', 'Defender 110 V8 UK V5C Logbook & Registration', 'V5C-GB-9912004', 'DVLA Swansea', 'Lifetime', 'DVLA Vehicle Enquiry Service', TRUE, 'ACTIVE', '["towing_rig", "winch_homologation"]'::jsonb),
('06060606-0606-0606-0606-060606060604', 'dddddddd-dddd-dddd-dddd-dddddddddd04', 'Warranty', 'Lucid 8-Year HV Powertrain & Battery Warranty Certificate', 'WAR-LUC-8Y-100K', 'Lucid Motors Corp', '2032-06-05', 'Lucid Corporate Warranty Registry', TRUE, 'ACTIVE', '["battery_pack", "tri_motor_powertrain", "inverters"]'::jsonb),
('06060606-0606-0606-0606-060606060605', 'dddddddd-dddd-dddd-dddd-dddddddddd05', 'Certificate', 'Ferrari Classiche Certificate of Authenticity', 'CLASSICHE-812C-49', 'Ferrari SpA Maranello', 'Permanent', 'Ferrari Classiche Department', TRUE, 'ACTIVE', '["matching_numbers_engine", "gearbox", "chassis"]'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_tasks (id, vehicle_id, title, meta_text, completed, priority, linked_module, completed_at) VALUES 
('07070707-0707-0707-0707-070707070701', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'Mount fresh set of Michelin Pilot Sport Cup 2 R tires', 'Track day preparation at Hockenheimring', FALSE, 'HIGH', 'vehicles', NULL),
('07070707-0707-0707-0707-070707070702', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'Perform full executive leather conditioning and ceramic detail', 'Interior sanitization and coating before board arrival', TRUE, 'MEDIUM', 'vehicles', '2026-10-16 16:00:00Z'),
('07070707-0707-0707-0707-070707070703', 'dddddddd-dddd-dddd-dddd-dddddddddd03', 'Inspect synthetic winch line and kinetic recovery rope', 'Check for abrasion before alpine winter tour', FALSE, 'LOW', 'vehicles', NULL),
('07070707-0707-0707-0707-070707070704', 'dddddddd-dddd-dddd-dddd-dddddddddd04', 'Verify Lucid OTA firmware update 2.4.1 installation status', 'Ensures high-speed charging thermal pre-conditioning optimization', TRUE, 'HIGH', 'vehicles', '2026-10-17 11:30:00Z'),
('07070707-0707-0707-0707-070707070705', 'dddddddd-dddd-dddd-dddd-dddddddddd05', 'Connect CTEK battery tender and verify 100% trickle state', 'Maintain optimal battery chemistry during seasonal storage', FALSE, 'MEDIUM', 'vehicles', NULL)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 7. PERSONAL ADMINISTRATION & GOVERNANCE MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO admin_items (id, title, description, item_type, category, amount, due_date, expiry_date, renewal_date, is_recurring, recurrence_pattern, status) VALUES 
('08080808-0808-0808-0808-080808080801', 'Global Sovereign Diplomatic Passports & Apostille File', 'Consolidated passport renewals and multi-entry consular visas', 'DOCUMENT', 'IDENTITY', 1850.00, '2028-05-15', '2028-05-15', '2028-02-15', FALSE, NULL, 'ACTIVE'),
('08080808-0808-0808-0808-080808080802', 'Delaware Holding LLC Annual Franchise Tax Filing', 'State of Delaware entity franchise tax and registered agent fee', 'BILL', 'COMPLIANCE', 450.00, '2026-11-01', '2026-11-01', '2027-11-01', TRUE, 'ANNUAL', 'ACTIVE'),
('08080808-0808-0808-0808-080808080803', 'Air-Gapped Cold Storage Seed Escrow Audit', 'Physical inspection and key ceremony across dual private vault vaults', 'AUDIT', 'SECURITY', 2500.00, '2026-12-15', '2026-12-15', '2027-12-15', TRUE, 'ANNUAL', 'ACTIVE'),
('08080808-0808-0808-0808-080808080804', 'Executive Residence Fireproof Archive Nitrogen Purge Service', 'Annual atmospheric recharge for hermetic document storage vault', 'SERVICE', 'FACILITY', 1200.00, '2026-11-20', '2026-11-20', '2027-11-20', TRUE, 'ANNUAL', 'ACTIVE'),
('08080808-0808-0808-0808-080808080805', 'Private Aviation Hangar Facility Lease Renewal', 'FBO private hangar bay lease and security credentialing', 'CONTRACT', 'PROPERTY', 48000.00, '2027-01-01', '2027-01-01', '2027-01-01', TRUE, 'ANNUAL', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_obligations (id, title, detail, due_date, cost_amount, paid, linked_module, category, action_type, badge_type, dossier_data, lease_data) VALUES 
('09090909-0909-0909-0909-090909090901', 'Q4 Estimated Federal Income Tax Filing', 'Form 1040-ES quarterly estimated tax payment to Internal Revenue Service', '2027-01-15', 385000.00, FALSE, 'finance', 'TAX', 'PAYMENT', 'CRITICAL', '{"irs_form": "1040-ES", "preparer": "Ernst & Young LLP"}'::jsonb, NULL),
('09090909-0909-0909-0909-090909090902', 'Commercial Penthouse Real Estate Property Tax', 'County tax collector semiannual property assessment tranche I', '2026-12-10', 42500.00, FALSE, 'admin', 'PROPERTY', 'ESCROW', 'URGENT', '{"parcel_id": "042-881-992", "county": "Miami-Dade"}'::jsonb, '{"lease_type": "OWNED", "square_feet": 8400}'::jsonb),
('09090909-0909-0909-0909-090909090903', 'Foreign Bank & Financial Accounts (FBAR) Report FinCEN 114', 'Annual mandatory reporting of foreign bank accounts exceeding statutory threshold', '2027-04-15', NULL, FALSE, 'admin', 'COMPLIANCE', 'FILING', 'STANDARD', '{"form": "FinCEN 114", "reporting_year": 2026}'::jsonb, NULL),
('09090909-0909-0909-0909-090909090904', 'Sovereign Family Trust Trustee Resolution & Audit Sign-Off', 'Annual trustee fiduciary sign-off and balance sheet attestation for trust beneficiaries', '2026-11-30', 15000.00, FALSE, 'admin', 'LEGAL', 'SIGNATURE', 'URGENT', '{"trust_name": "Sovereign Irrevocable Dynastic Trust", "counsel": "Sullivan & Cromwell"}'::jsonb, NULL),
('09090909-0909-0909-0909-090909090905', 'Executive Aviation Fractional Tail Share Operating Assessment', 'Quarterly management fee and maintenance reserve for tail N812SC', '2026-10-31', 28500.00, TRUE, 'finance', 'AVIATION', 'RESERVE', 'RESOLVED', '{"tail_number": "N812SC", "aircraft": "Bombardier Global 7500"}'::jsonb, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_subscriptions (id, name, plan, schedule_type, schedule_note, payment_source, monthly_commitment, annual_equivalent, status, category) VALUES 
('10101010-1010-1010-1010-101010101001', 'Bloomberg Professional Service', 'Dual-screen market terminal, real-time B-PIPE feed, and AIM compliance', 'MONTHLY', 'Auto-debited on 1st of month', 'J.P. Morgan Card', 2500.00, 30000.00, 'ACTIVE', 'INTELLIGENCE'),
('10101010-1010-1010-1010-101010101002', 'OpenAI Enterprise Dedicated Inference Cluster', 'Dedicated throughput tier with custom safety filters and 128k context', 'MONTHLY', 'Billed on usage threshold', 'Brex Corporate Card', 4500.00, 54000.00, 'ACTIVE', 'TECHNOLOGY'),
('10101010-1010-1010-1010-101010101003', 'Starlink Global Maritime Priority Tier', 'High-speed low-latency satellite internet for transit and remote sites', 'MONTHLY', 'Automatic renewal', 'Amex Centurion', 1000.00, 12000.00, 'ACTIVE', 'COMMUNICATIONS'),
('10101010-1010-1010-1010-101010101004', 'PitchBook Private Equity & Venture Terminal', 'Complete global institutional fund and deal tracking database', 'ANNUAL', 'Annual invoice paid upfront in January', 'J.P. Morgan Wire', 1850.00, 22200.00, 'ACTIVE', 'RESEARCH'),
('10101010-1010-1010-1010-101010101005', 'MedAire Global Executive Medical Concierge', '24/7 aviation and maritime emergency medical response and physician routing', 'ANNUAL', 'Annual policy renewal', 'Amex Centurion', 1250.00, 15000.00, 'ACTIVE', 'HEALTH')
ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_vault (id, name, verification_badge, type_badge, subtext, meta1, meta2, icon, issuer, identifier, valid_until, notes, hash_sha256, physical_location, last_synced_at) VALUES 
('12121212-1212-1212-1212-121212121201', 'Sovereign Irrevocable Dynastic Trust Indenture', 'CERTIFIED_LEGAL', 'LEGAL_CHARTER', 'Master dynastic family trust agreement executed under Delaware jurisdiction', 'Delaware Law', 'Apostilled', 'shield-check', 'Delaware Court of Chancery', 'TRUST-DE-2024-001', 'Permanent', 'Original physical parchment stored in nitrogen-sealed archival case', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'Zurich Freezone Vault Room A', '2026-10-10 14:00:00Z'),
('12121212-1212-1212-1212-121212121202', 'Cryptographic Master Root Seed Key Escrow', 'AIR_GAPPED', 'SECURITY_SECRET', 'Shamir secret shares (3-of-5) of sovereign cold storage recovery keys', 'BIP-39 Standard', 'Titanium Plate', 'key', 'Hardware Security Escrow', 'ESCROW-SEED-TITAN-1', 'Perpetual', 'Laser-etched into grade 5 titanium plate sealed in tamper-evident pouch', 'a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e', 'Executive Residence Vault Safe', '2026-10-15 09:30:00Z'),
('12121212-1212-1212-1212-121212121203', 'Miami Island Penthouse Warranty Deed', 'VERIFIED_TITLE', 'REAL_ESTATE', 'Recorded original title deed and title insurance policy for primary residence', 'County Clerk Recorded', 'Free & Clear', 'home', 'Miami-Dade County Property Appraiser', 'DEED-FL-2023-88910', 'Indefinite', 'Clean title confirmed with First American Title Insurance', 'b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9', 'Headquarters Secure Suite Safe', '2026-09-01 11:00:00Z'),
('12121212-1212-1212-1212-121212121204', 'Executive Power of Attorney & Living Directive', 'NOTARIZED', 'DIRECTIVE', 'Springing power of attorney and comprehensive medical directives', 'Dual Notarized', 'Healthcare Proxies', 'file-text', 'State of California Notary Commission', 'POA-CA-2025-441', 'Until Revocation', 'Authorizes Elena Rostova and David Kim as co-attorneys in fact', 'c4ca4238a0b923820dcc509a6f75849b', 'Zurich Freezone Vault Room A', '2026-10-05 16:20:00Z'),
('12121212-1212-1212-1212-121212121205', 'Series Seed Preferred Stock Share Certificates', 'ORIGINAL_EQUITY', 'EQUITY_CERTIFICATE', 'Physical original share certificates for 15,000,000 shares in Sovereign Corp', 'Board Certified', 'Fully Paid', 'award', 'Sovereign Systems Corp Board of Directors', 'CERT-SEED-001', 'Permanent', 'Countersigned by corporate secretary and legal counsel', '1679091c5a880faf6fb5e6087eb1b2dc', 'Cayman Corporate Depository', '2026-08-20 10:15:00Z')
ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_vault_audit_history (id, vault_id, action, timestamp) VALUES 
('13131313-1313-1313-1313-131313131301', '12121212-1212-1212-1212-121212121201', 'DOCUMENT_UPLOADED: Master deed digital scan notarized with SHA-256 integrity seal', '2026-10-10 14:05:00Z'),
('13131313-1313-1313-1313-131313131302', '12121212-1212-1212-1212-121212121202', 'KEY_CEREMONY_AUDIT: Physical inspection and dual-custody tamper seal verification pass', '2026-10-15 09:35:00Z'),
('13131313-1313-1313-1313-131313131303', '12121212-1212-1212-1212-121212121203', 'ANNUAL_TITLE_SYNC: County clerk land records cross-referenced with municipal registry', '2026-09-01 11:05:00Z'),
('13131313-1313-1313-1313-131313131304', '12121212-1212-1212-1212-121212121204', 'LEGAL_COUNSEL_REVIEW: Partner at Sullivan & Cromwell reviewed healthcare proxy mandates', '2026-10-05 16:25:00Z'),
('13131313-1313-1313-1313-131313131305', '12121212-1212-1212-1212-121212121205', 'AUDITOR_ATTESTATION: Ernst & Young external auditor inspected capitalization table records', '2026-08-20 10:20:00Z')
ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_tasks (id, title, meta_text, completed, priority, linked_module, completed_at) VALUES 
('14141414-1414-1414-1414-141414141401', 'Audit physical tamper seals on Zurich vault deposit box', 'Verify numbered seal sequence matches legal escrow log', FALSE, 'HIGH', 'admin', NULL),
('14141414-1414-1414-1414-141414141402', 'Sign annual corporate board resolutions for Delaware entities', 'Execute electronic signatures through secure DocuSign HSM appliance', TRUE, 'HIGH', 'admin', '2026-10-17 15:30:00Z'),
('14141414-1414-1414-1414-141414141403', 'Coordinate international medical evacuation insurance policy update', 'Add new Gulfstream tail number to MedAire primary manifest', TRUE, 'MEDIUM', 'admin', '2026-10-14 11:00:00Z'),
('14141414-1414-1414-1414-141414141404', 'Review Q3 consolidated family office management fee distribution', 'Verify 1.2% annualized hurdle calculation for advisory committee', FALSE, 'MEDIUM', 'finance', NULL),
('14141414-1414-1414-1414-141414141405', 'Dispatch apostilled trust certificates to Swiss banking counsel', 'Send via secure diplomatic courier with live GPS tracking', FALSE, 'URGENT', 'admin', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_physical_archives (id, name, type, icon, inventory_summary, security_standard, items_stored_json) VALUES 
('15151515-1515-1515-1515-151515151501', 'Executive Residence Vault Room', 'REINFORCED_CLASS_5', 'shield', 'Primary active document archive, biometric access, dry fire suppression, Faraday shield', 'UL Class 350 4-Hour Fire & Impact', '{"items": ["passports", "hardware_keys", "current_deeds", "emergency_cash"]}'::jsonb),
('15151515-1515-1515-1515-151515151502', 'Zurich Freeport Sovereign Safe Deposit', 'HIGH_SECURITY_FREEZONE', 'lock', 'Offshore long-term legal charter repository and physical gold bullion escrow', 'Swiss Banking Security Level 10', '{"items": ["trust_charters", "gold_bars_9999", "seed_share_alpha", "share_certificates"]}'::jsonb),
('15151515-1515-1515-1515-151515151503', 'Headquarters Secure Document Depository', 'CLIMATE_CONTROLLED_ROOM', 'archive', '10-year historical accounting records, tax audit returns, employee agreements', 'NFPA 232 Standard for Record Protection', '{"items": ["tax_returns_2015_2025", "vendor_contracts", "patents_portfolio"]}'::jsonb),
('15151515-1515-1515-1515-151515151504', 'Island Estate Storm-Proof Vault Safe', 'SUBMERSIBLE_FIREPROOF', 'umbrella', 'Waterproof IP68 titanium safe for maritime property title, yacht registration, storm plans', 'IP68 & 2-Hour 1850F Fire Rating', '{"items": ["vessel_certificates", "emergency_satellite_transceivers", "backup_credentials"]}'::jsonb),
('15151515-1515-1515-1515-151515151505', 'Cayman Islands Corporate Legal Registry', 'OFFSHORE_CUSTODIAL_DEPOT', 'briefcase', 'Custodial archive for offshore international business company filings and share registers', 'Cayman Monetary Authority Custody Standard', '{"items": ["holding_co_articles", "apostilles", "custody_ledgers"]}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 8. INDUSTRY INTELLIGENCE & DAILY BRIEFS MODULE (5 Rows per table)
-- =============================================================================
INSERT INTO industry_sources (id, name, feed_url, source_type, is_active) VALUES 
('22222222-2222-2222-2222-222222222201', 'Hacker News Frontpage High Signal', 'https://hnrss.org/frontpage?points=100', 'RSS', TRUE),
('22222222-2222-2222-2222-222222222202', 'arXiv Computer Science Artificial Intelligence', 'https://rss.arxiv.org/rss/cs.AI', 'RSS', TRUE),
('22222222-2222-2222-2222-222222222203', 'Financial Times Global Economy & Markets', 'https://www.ft.com/global-economy?format=rss', 'RSS', TRUE),
('22222222-2222-2222-2222-222222222204', 'IEEE Spectrum Computing & Robotics', 'https://spectrum.ieee.org/feeds/topic/computing.rss', 'RSS', TRUE),
('22222222-2222-2222-2222-222222222205', 'Stratechery by Ben Thompson', 'https://stratechery.com/feed/', 'RSS', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO industry_summaries (id, summary_date, title, content_markdown, sources_json) VALUES 
('23232323-2323-2323-2323-232323232301', CURRENT_DATE, 'Executive Intelligence Briefing: Inference Compute Economics & Autonomous Tool Loops', '# Executive Briefing\n\nKey trends: Hybrid compute allocation drops failure modes by 42%. Hyperscalers revise egress pricing.', '["https://hnrss.org", "https://arxiv.org/abs/2610.001"]'::jsonb),
('23232323-2323-2323-2323-232323232302', CURRENT_DATE - 1, 'Systems Architecture Briefing: SQLite 3.47 JSONB Engines & Local-First Ledgers', '# Systems Briefing\n\nDetailed breakdown of lock contention reductions in local-first embedded databases.', '["https://sqlite.org/release_notes", "https://news.ycombinator.com"]'::jsonb),
('23232323-2323-2323-2323-232323232303', CURRENT_DATE - 2, 'Global Capital & Treasury Dispatch: Inverted Yield Curve Opportunities in Senior Debt', '# Capital Dispatch\n\nPrivate credit yields reach generational spread over sovereign treasuries.', '["https://ft.com", "https://bloomberg.com"]'::jsonb),
('23232323-2323-2323-2323-232323232304', CURRENT_DATE - 3, 'Frontier Hardware Review: Silicon Photonics and 800G Transceivers in AI Clusters', '# Hardware Review\n\nOptical interconnects demonstrate 3.8x power reduction in GPU training fabrics.', '["https://spectrum.ieee.org"]'::jsonb),
('23232323-2323-2323-2323-232323232305', CURRENT_DATE - 4, 'Executive Health & Longevity Review: Glymphatic Clearance and Sleep Biomarker Metrics', '# Health Review\n\nPhase-3 clinical data confirms specific deep sleep acoustic waveforms enhance brain metabolite flushing.', '["https://nature.com", "https://fountainlife.com"]'::jsonb)
ON CONFLICT (summary_date) DO NOTHING;

INSERT INTO daily_briefs (id, brief_date, structured_agenda_json, ai_summary_markdown, status) VALUES 
('24242424-2424-2424-2424-242424242401', CURRENT_DATE, '{"focus": "Q4 Capital & Systems Review", "agenda": [{"time": "10:00", "title": "Board of Directors Prep"}, {"time": "14:00", "title": "Telemetry Engine Benchmarks"}]}'::jsonb, 'High focus day required. Two critical milestones pending execution. 3 Deep work blocks scheduled.', 'GENERATED'),
('24242424-2424-2424-2424-242424242402', CURRENT_DATE - 1, '{"focus": "Treasury Bond Ladder Execution", "agenda": [{"time": "09:00", "title": "Goldman Sachs Syndicate Call"}, {"time": "16:00", "title": "Long Run Endurance"}]}'::jsonb, 'Treasury shifts completed smoothly. Bond laddering locked in 4.85% tax-free equivalent yield.', 'COMPLETED'),
('24242424-2424-2424-2424-242424242403', CURRENT_DATE - 2, '{"focus": "Architecture & IPC Zero-Copy Deserialization", "agenda": [{"time": "11:00", "title": "Engineering Review"}, {"time": "15:00", "title": "Safe Deposit Audit"}]}'::jsonb, 'Productive deep work day. 3.5 hours of uninterrupted coding completed.', 'COMPLETED'),
('24242424-2424-2424-2424-242424242404', CURRENT_DATE - 3, '{"focus": "Fleet Telemetry & Diagnostics Review", "agenda": [{"time": "14:00", "title": "Porsche Track Day Shakedown"}]}'::jsonb, 'Vehicle diagnostics green across all ECUs. Track pressures calibrated to ambient temperature.', 'COMPLETED'),
('24242424-2424-2424-2424-242424242405', CURRENT_DATE - 4, '{"focus": "Weekly Strategy Synthesis & Health Recovery", "agenda": [{"time": "08:00", "title": "Longevity Biomarker Panel"}]}'::jsonb, 'Restorative day. Full biomarker panel completed at clinic. HRV trending +14% above baseline.', 'COMPLETED')
ON CONFLICT (brief_date) DO NOTHING;

-- =============================================================================
-- 9. INTELLIGENCE ENGINE & AI FEED PIPELINE (5 Rows per table)
-- =============================================================================
INSERT INTO rss_feeds (id, name, feed_url, site_url, status, source_quality, last_fetched_at, last_success_at, last_failure_at, last_failure_code, last_failure_message) VALUES 
('16161616-1616-1616-1616-161616161601', 'MIT Technology Review', 'https://www.technologyreview.com/feed/', 'https://www.technologyreview.com', 'ACTIVE', 0.95, '2026-10-18 12:00:00Z', '2026-10-18 12:00:00Z', NULL, NULL, NULL),
('16161616-1616-1616-1616-161616161602', 'Hacker News Curated Feed', 'https://hnrss.org/best', 'https://news.ycombinator.com', 'ACTIVE', 0.90, '2026-10-18 13:00:00Z', '2026-10-18 13:00:00Z', NULL, NULL, NULL),
('16161616-1616-1616-1616-161616161603', 'Stratechery Technology Strategy', 'https://stratechery.com/feed/rss/', 'https://stratechery.com', 'ACTIVE', 0.92, '2026-10-18 11:30:00Z', '2026-10-18 11:30:00Z', NULL, NULL, NULL),
('16161616-1616-1616-1616-161616161604', 'Financial Times Tech Wire', 'https://www.ft.com/technology?format=rss', 'https://www.ft.com', 'ACTIVE', 0.94, '2026-10-18 14:15:00Z', '2026-10-18 14:15:00Z', NULL, NULL, NULL),
('16161616-1616-1616-1616-161616161605', 'Ars Technica Systems & Hardware', 'https://feeds.arstechnica.com/arstechnica/index', 'https://arstechnica.com', 'ACTIVE', 0.88, '2026-10-18 10:45:00Z', '2026-10-18 10:45:00Z', NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO articles (id, canonical_url, original_url, title, description, content_text, author, published_at, content_hash) VALUES 
('17171717-1717-1717-1717-171717171701', 'https://technologyreview.com/2026/10/18/autonomous-context-distillation', 'https://technologyreview.com/2026/10/18/autonomous-context-distillation', 'Autonomous Context Distillation & Hybrid Planning Loops', 'Technical analysis of inference-time scaling and verification loops', 'Full technical article text detailing benchmark results on complex agent planning loops...', 'Dr. Julian Sterling', '2026-10-18 08:30:00Z', 'a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef'),
('17171717-1717-1717-1717-171717171702', 'https://news.ycombinator.com/item?id=41882901', 'https://sqlite.org/draft/release347.html', 'SQLite 3.47 Introduces Enhanced JSONB Storage Engine', 'Low-lock concurrency and 3.2x throughput improvements in embedded database engine', 'Comprehensive release notes covering memory-mapped I/O enhancements and binary JSON indexing...', 'D. Richard Hipp', '2026-10-17 14:00:00Z', 'b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef12'),
('17171717-1717-1717-1717-171717171703', 'https://stratechery.com/2026/hyperscaler-cloud-egress-pricing-shift', 'https://stratechery.com/2026/hyperscaler-cloud-egress-pricing-shift', 'Major Cloud Providers Revise Egress Pricing and Tier-1 GPU Allocation', 'Analysis of regulatory pressure opening data portability across cloud hyperscalers', 'Strategic analysis evaluating AWS, Azure and GCP pricing movements and dedicated cluster availability...', 'Ben Thompson', '2026-10-18 06:15:00Z', 'c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef1234'),
('17171717-1717-1717-1717-171717171704', 'https://ft.com/content/private-credit-liquidity-stress-test', 'https://ft.com/content/private-credit-liquidity-stress-test', 'Private Credit Assets Approach $2 Trillion: Sovereign Stress Testing', 'Global financial markets monitor credit spreads and liquidity duration mismatches', 'Financial Times in-depth investigation into direct lending fund covenants and sovereign wealth allocations...', 'Gillian Tett', '2026-10-16 11:45:00Z', 'd4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef123456'),
('17171717-1717-1717-1717-171717171705', 'https://arstechnica.com/cars/2026/10/solid-state-battery-pack-breakthrough', 'https://arstechnica.com/cars/2026/10/solid-state-battery-pack-breakthrough', 'Solid-State Battery Breakthrough in Next-Gen High-Output Powertrains', 'Automotive engineers validate 450 Wh/kg cell energy density in track shakedowns', 'Lab tests reveal solid electrolyte ceramic separators withstand over 1,500 fast charging cycles...', 'Jonathan M. Gitlin', '2026-10-17 16:30:00Z', 'e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef12345678')
ON CONFLICT (id) DO NOTHING;

INSERT INTO article_sources (article_id, feed_id, external_guid, source_published_at) VALUES 
('17171717-1717-1717-1717-171717171701', '16161616-1616-1616-1616-161616161601', 'mit-tr-guid-88912', '2026-10-18 08:30:00Z'),
('17171717-1717-1717-1717-171717171702', '16161616-1616-1616-1616-161616161602', 'hn-post-guid-41882901', '2026-10-17 14:00:00Z'),
('17171717-1717-1717-1717-171717171703', '16161616-1616-1616-1616-161616161603', 'stratechery-guid-2026-egress', '2026-10-18 06:15:00Z'),
('17171717-1717-1717-1717-171717171704', '16161616-1616-1616-1616-161616161604', 'ft-guid-credit-2026-88', '2026-10-16 11:45:00Z'),
('17171717-1717-1717-1717-171717171705', '16161616-1616-1616-1616-161616161605', 'ars-guid-cars-battery-450', '2026-10-17 16:30:00Z')
ON CONFLICT (article_id, feed_id) DO NOTHING;

-- User interest categories (5 rows)
INSERT INTO user_interest_categories (user_id, category_id) VALUES 
('11111111-1111-1111-1111-111111111101', 'f0000001-0000-0000-0000-000000000001'),
('11111111-1111-1111-1111-111111111101', 'f0000008-0000-0000-0000-000000000008'),
('11111111-1111-1111-1111-111111111101', 'f000000b-0000-0000-0000-00000000000b'),
('11111111-1111-1111-1111-111111111102', 'f0000002-0000-0000-0000-000000000002'),
('11111111-1111-1111-1111-111111111105', 'f0000003-0000-0000-0000-000000000003')
ON CONFLICT (user_id, category_id) DO NOTHING;

INSERT INTO article_classifications (id, article_id, category_id, score, is_primary, matched_signals) VALUES 
('18181818-1818-1818-1818-181818181801', '17171717-1717-1717-1717-171717171701', 'f0000001-0000-0000-0000-000000000001', 0.9850, TRUE, '{"signals": ["inference_scaling", "planning_loops", "multi_step_agents"]}'::jsonb),
('18181818-1818-1818-1818-181818181802', '17171717-1717-1717-1717-171717171702', 'f0000002-0000-0000-0000-000000000002', 0.9620, TRUE, '{"signals": ["jsonb", "sqlite", "embedded_databases", "concurrency"]}'::jsonb),
('18181818-1818-1818-1818-181818181803', '17171717-1717-1717-1717-171717171703', 'f0000003-0000-0000-0000-000000000003', 0.9410, TRUE, '{"signals": ["cloud_egress", "gpu_clusters", "hyperscalers"]}'::jsonb),
('18181818-1818-1818-1818-181818181804', '17171717-1717-1717-1717-171717171704', 'f0000008-0000-0000-0000-000000000008', 0.9780, TRUE, '{"signals": ["private_credit", "senior_debt", "sovereign_liquidity"]}'::jsonb),
('18181818-1818-1818-1818-181818181805', '17171717-1717-1717-1717-171717171705', 'f000000b-0000-0000-0000-00000000000b', 0.9530, TRUE, '{"signals": ["solid_state_battery", "ev_powertrains", "thermal_management"]}'::jsonb)
ON CONFLICT (article_id, category_id) DO NOTHING;

INSERT INTO daily_article_selections (id, selection_date, article_id, category_id, rank, ranking_score) VALUES 
('19191919-1919-1919-1919-191919191901', CURRENT_DATE, '17171717-1717-1717-1717-171717171701', 'f0000001-0000-0000-0000-000000000001', 1, 0.9850),
('19191919-1919-1919-1919-191919191902', CURRENT_DATE, '17171717-1717-1717-1717-171717171702', 'f0000002-0000-0000-0000-000000000002', 1, 0.9620),
('19191919-1919-1919-1919-191919191903', CURRENT_DATE, '17171717-1717-1717-1717-171717171703', 'f0000003-0000-0000-0000-000000000003', 1, 0.9410),
('19191919-1919-1919-1919-191919191904', CURRENT_DATE, '17171717-1717-1717-1717-171717171704', 'f0000008-0000-0000-0000-000000000008', 1, 0.9780),
('19191919-1919-1919-1919-191919191905', CURRENT_DATE, '17171717-1717-1717-1717-171717171705', 'f000000b-0000-0000-0000-00000000000b', 1, 0.9530)
ON CONFLICT (selection_date, article_id, category_id) DO NOTHING;

INSERT INTO article_summaries (id, article_id, summary, model, prompt_version, word_count, generated_at, status, failure_reason) VALUES 
('20202020-2020-2020-2020-202020202001', '17171717-1717-1717-1717-171717171701', 'Key insight: Hybrid test-time search algorithm reduces error rates in multi-hop reasoning by over 40% when paired with validator feedback.', 'gpt-4o', 'v2.1-executive-brief', 145, '2026-10-18 09:00:00Z', 'COMPLETED', NULL),
('20202020-2020-2020-2020-202020202002', '17171717-1717-1717-1717-171717171702', 'Key insight: SQLite 3.47 native JSONB binary representation allows direct pointer queries without parsing string JSON, providing immediate wins for embedded telemetry.', 'gpt-4o', 'v2.1-executive-brief', 120, '2026-10-17 15:30:00Z', 'COMPLETED', NULL),
('20202020-2020-2020-2020-202020202003', '17171717-1717-1717-1717-171717171703', 'Key insight: Elimination of data egress lock-ins enables multi-cloud routing where prompt generation and batch training reside on lowest-cost hardware tiers.', 'gpt-4o', 'v2.1-executive-brief', 135, '2026-10-18 07:45:00Z', 'COMPLETED', NULL),
('20202020-2020-2020-2020-202020202004', '17171717-1717-1717-1717-171717171704', 'Key insight: Senior secured credit spreads widen, offering family offices an asymmetric yield buffer of 450 bps above US Treasuries with covenant protection.', 'gpt-4o', 'v2.1-executive-brief', 160, '2026-10-16 13:00:00Z', 'COMPLETED', NULL),
('20202020-2020-2020-2020-202020202005', '17171717-1717-1717-1717-171717171705', 'Key insight: Solid-state battery pack architecture enables 12-minute 10-to-80% ultra-fast charging without dendrite degradation under continuous 4C discharge.', 'gpt-4o', 'v2.1-executive-brief', 140, '2026-10-17 17:15:00Z', 'COMPLETED', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO intelligence_runs (id, run_date, started_at, completed_at, status, feeds_attempted, feeds_succeeded, feeds_failed, articles_seen, articles_new, articles_deduplicated, articles_classified, articles_selected, summaries_generated, summaries_failed, failure_message) VALUES 
('21212121-2121-2121-2121-212121212101', CURRENT_DATE, '2026-10-18 05:00:00Z', '2026-10-18 05:12:30Z', 'COMPLETED', 5, 5, 0, 185, 12, 8, 12, 5, 5, 0, NULL),
('21212121-2121-2121-2121-212121212102', CURRENT_DATE - 1, '2026-10-17 05:00:00Z', '2026-10-17 05:14:10Z', 'COMPLETED', 5, 5, 0, 210, 15, 11, 15, 5, 5, 0, NULL),
('21212121-2121-2121-2121-212121212103', CURRENT_DATE - 2, '2026-10-16 05:00:00Z', '2026-10-16 05:11:45Z', 'COMPLETED', 5, 5, 0, 160, 9, 6, 9, 5, 5, 0, NULL),
('21212121-2121-2121-2121-212121212104', CURRENT_DATE - 3, '2026-10-15 05:00:00Z', '2026-10-15 05:13:00Z', 'COMPLETED', 5, 5, 0, 195, 14, 9, 14, 5, 5, 0, NULL),
('21212121-2121-2121-2121-212121212105', CURRENT_DATE - 4, '2026-10-14 05:00:00Z', '2026-10-14 05:10:20Z', 'COMPLETED', 5, 5, 0, 175, 11, 7, 11, 5, 5, 0, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO intelligence_briefs (id, title, topic_badge, impact_badge, impact_color, time_ago, summary, why_it_matters, why_it_matters_highlight, source, read_time, category, cross_link_label, cross_link_module, bookmarked, bookmarked_at, is_read, read_at, radar_x, radar_y, gravity, urgency_score, color) VALUES 
(
  'brief-1',
  'Autonomous Context Distillation & Hybrid Planning Loops',
  'AI Systems Architecture',
  'High Strategic Impact',
  'warning',
  '2h ago',
  'New technical evaluations demonstrate that hybrid inference-time compute scaling yields a 42% reduction in autonomous failure modes across complex multi-step developer workflows.',
  'Directly informs our architecture for Monday’s Agenda Engine priority scoring model. We can replace brittle prompt heuristics with structured verification steps.',
  'Monday’s Agenda Engine priority scoring model',
  'MIT Technology Review / arXiv',
  'Read Dossier (4 min)',
  'AI',
  'Link to Projects',
  'projects',
  TRUE,
  '2026-10-18 10:15:00Z',
  TRUE,
  '2026-10-18 10:30:00Z',
  28.50,
  32.00,
  'HIGH',
  88,
  'cyan'
),
(
  'brief-2',
  'SQLite 3.47 Introduces Enhanced JSONB Storage Engine',
  'Database Architecture & Systems',
  'Operational Relevance',
  'success',
  '4h ago',
  'The new release significantly lowers lock contention during heavy concurrent write sequences, yielding up to a 3.2x throughput increase for embedded structured record storage.',
  'Validates our choice of local-first SQLite persistence for Monday''s offline vehicle telemetry and transaction ledgers.',
  'local-first SQLite persistence',
  'Hacker News / SQLite Release Notes',
  '3 min read',
  'Systems',
  'Export Notes',
  'projects',
  FALSE,
  NULL,
  FALSE,
  NULL,
  75.00,
  68.50,
  'MEDIUM',
  72,
  'amber'
),
(
  'brief-3',
  'Major Cloud Providers Revise Egress Pricing and Tier-1 GPU Allocation',
  'Macro Tech & Cloud',
  'Capital Efficiency',
  'warning',
  '6h ago',
  'Hyperscalers have begun relaxing data egress fees in response to antitrust regulatory frameworks, while introducing spot commitments for dedicated inference clusters.',
  'Directly impacts our monthly AWS cloud infrastructure budget. Egress waiver allows routing inference queries to cheaper specialized endpoints.',
  'monthly AWS cloud infrastructure budget',
  'The Verge / Stratechery',
  '5 min read',
  'Venture',
  'Cross-link to Finance',
  'finance',
  FALSE,
  NULL,
  FALSE,
  NULL,
  45.00,
  55.00,
  'LOW',
  60,
  'emerald'
),
(
  'brief-4',
  'Private Credit Spreads Widen: Senior Secured Tranches Yield 9.8%',
  'Sovereign Wealth & Private Markets',
  'Capital Allocation',
  'success',
  '8h ago',
  'Tier-1 direct lenders report senior secured debt tranches offering generationally attractive risk-adjusted yield buffers against fluctuating benchmark rates.',
  'Opportunity to deploy $5M from overnight treasury reserves into high-quality senior secured credit facility.',
  'senior secured credit facility',
  'Financial Times / Bloomberg',
  '4 min read',
  'Finance',
  'Link to Portfolio',
  'finance',
  TRUE,
  '2026-10-18 12:00:00Z',
  FALSE,
  NULL,
  62.00,
  41.50,
  'HIGH',
  85,
  'purple'
),
(
  'brief-5',
  'Solid-State Battery Pack Achieves 450 Wh/kg in Shakedown Trials',
  'Automotive & Advanced Energy',
  'Fleet Technology',
  'info',
  '12h ago',
  'Track trials confirm solid electrolyte ceramic separators withstand rigorous high-temperature shakedowns while enabling ultra-fast charging curves.',
  'Future-proofs battery replacement expectations for fleet performance EVs and long-range logistics.',
  'fleet performance EVs',
  'Ars Technica / Nature Energy',
  '3 min read',
  'Automotive',
  'Link to Fleet',
  'vehicles',
  FALSE,
  NULL,
  TRUE,
  '2026-10-18 08:00:00Z',
  82.00,
  25.00,
  'MEDIUM',
  68,
  'blue'
)
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title,
  topic_badge = EXCLUDED.topic_badge,
  impact_badge = EXCLUDED.impact_badge,
  impact_color = EXCLUDED.impact_color,
  time_ago = EXCLUDED.time_ago,
  summary = EXCLUDED.summary,
  why_it_matters = EXCLUDED.why_it_matters,
  why_it_matters_highlight = EXCLUDED.why_it_matters_highlight,
  source = EXCLUDED.source,
  read_time = EXCLUDED.read_time,
  category = EXCLUDED.category,
  cross_link_label = EXCLUDED.cross_link_label,
  cross_link_module = EXCLUDED.cross_link_module,
  bookmarked = EXCLUDED.bookmarked,
  bookmarked_at = EXCLUDED.bookmarked_at,
  is_read = EXCLUDED.is_read,
  read_at = EXCLUDED.read_at,
  radar_x = EXCLUDED.radar_x,
  radar_y = EXCLUDED.radar_y,
  gravity = EXCLUDED.gravity,
  urgency_score = EXCLUDED.urgency_score,
  color = EXCLUDED.color;
