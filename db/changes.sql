-- ============================================================
-- changes.sql - Deterministic incremental changes
-- Applied BETWEEN two /ingest runs to validate incrementality
-- ============================================================

-- 1. Update 5 existing cases: advance updated_at and change status
UPDATE cases
SET    updated_at = now(),
       status     = 'closed'
WHERE  case_id IN (1, 5, 12, 23, 47);

-- 2. Insert 2 new customers
INSERT INTO customers (name, email, country, updated_at) VALUES
('Eleanor Voss',   'eleanor.voss@example.com',   'CH', now()),
('Marcus Webb',    'marcus.webb@example.com',     'SG', now());

-- 3. Insert 10 new cases (6 linked to existing customers, 4 to the new ones)
INSERT INTO cases (customer_id, title, description, status, updated_at) VALUES
(1,  'Billing reconciliation post-upgrade',    'Following system upgrade, billing reconciliation shows 3 new unmatched items requiring investigation and resolution.', 'open',        now()),
(5,  'AML alert post-changes review',          'AML monitoring system triggered an alert for this account following recent transaction pattern changes. Review initiated.', 'in_progress', now()),
(10, 'Fraud investigation follow-up',          'Follow-up investigation on previously flagged fraud case reveals additional linked transactions requiring documentation.', 'open',        now()),
(15, 'Compliance re-certification required',   'Regulatory change requires customers in this category to complete re-certification. Outreach campaign being prepared.', 'in_progress', now()),
(20, 'Payments settlement dispute',            'Disputed settlement amount from last week''s batch identified during daily reconciliation. Correspondent bank contacted.', 'open',        now()),
(25, 'Audit evidence supplemental request',    'External auditor submitted supplemental evidence request covering 3 additional control areas not in original scope.', 'in_progress', now()),
-- New customers (customer_id 31 and 32 — identity columns assigned in order)
(31, 'New customer onboarding KYC',            'Initial KYC check for new customer Eleanor Voss. Document collection and identity verification steps in progress.', 'in_progress', now()),
(31, 'Billing plan selection for new account', 'New customer selecting billing plan. Enterprise tier recommended based on projected usage. Contract under review.', 'open',        now()),
(32, 'New customer fraud pre-screening',       'Standard pre-onboarding fraud pre-screening for Marcus Webb. PEP and sanctions check clear. Device check pending.', 'in_progress', now()),
(32, 'Compliance initial risk rating',         'Initial compliance risk rating assignment for new customer. Based on business type and jurisdiction: medium risk category.', 'open',        now());
