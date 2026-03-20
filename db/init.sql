-- ============================================================
-- Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT        NOT NULL,
    email       TEXT        NOT NULL UNIQUE,
    country     TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS cases (
    case_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers(customer_id),
    title       TEXT        NOT NULL,
    description TEXT        NOT NULL,
    status      TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

-- Required indexes
CREATE INDEX IF NOT EXISTS idx_customers_updated_at ON customers(updated_at);
CREATE INDEX IF NOT EXISTS idx_cases_updated_at     ON cases(updated_at);
CREATE INDEX IF NOT EXISTS idx_cases_customer_id    ON cases(customer_id);

-- ============================================================
-- Seed: 30 customers (deterministic)
-- ============================================================

INSERT INTO customers (name, email, country, updated_at) VALUES
('Alice Martin',      'alice.martin@example.com',      'US', now() - interval '30 days'),
('Bob Johnson',       'bob.johnson@example.com',        'GB', now() - interval '29 days'),
('Carlos Rivera',     'carlos.rivera@example.com',      'MX', now() - interval '28 days'),
('Diana Müller',      'diana.mueller@example.com',      'DE', now() - interval '27 days'),
('Eva Rossi',         'eva.rossi@example.com',          'IT', now() - interval '26 days'),
('Frank Dubois',      'frank.dubois@example.com',       'FR', now() - interval '25 days'),
('Grace Kim',         'grace.kim@example.com',          'KR', now() - interval '24 days'),
('Hiro Tanaka',       'hiro.tanaka@example.com',        'JP', now() - interval '23 days'),
('Isla Brown',        'isla.brown@example.com',         'AU', now() - interval '22 days'),
('João Silva',        'joao.silva@example.com',         'BR', now() - interval '21 days'),
('Karen White',       'karen.white@example.com',        'CA', now() - interval '20 days'),
('Liam Nguyen',       'liam.nguyen@example.com',        'VN', now() - interval '19 days'),
('Maria Garcia',      'maria.garcia@example.com',       'ES', now() - interval '18 days'),
('Nathan Patel',      'nathan.patel@example.com',       'IN', now() - interval '17 days'),
('Olivia Chen',       'olivia.chen@example.com',        'CN', now() - interval '16 days'),
('Paul Schmidt',      'paul.schmidt@example.com',       'DE', now() - interval '15 days'),
('Quinn O''Brien',    'quinn.obrien@example.com',       'IE', now() - interval '14 days'),
('Rosa Fernandez',    'rosa.fernandez@example.com',     'AR', now() - interval '13 days'),
('Samuel Okafor',     'samuel.okafor@example.com',      'NG', now() - interval '12 days'),
('Tina Kowalski',     'tina.kowalski@example.com',      'PL', now() - interval '11 days'),
('Uma Sharma',        'uma.sharma@example.com',         'IN', now() - interval '10 days'),
('Victor Hugo',       'victor.hugo@example.com',        'FR', now() - interval '9 days'),
('Wendy Adams',       'wendy.adams@example.com',        'US', now() - interval '8 days'),
('Xavier Lopez',      'xavier.lopez@example.com',       'CO', now() - interval '7 days'),
('Yuki Sato',         'yuki.sato@example.com',          'JP', now() - interval '6 days'),
('Zara Ahmed',        'zara.ahmed@example.com',         'AE', now() - interval '5 days'),
('Aaron Clark',       'aaron.clark@example.com',        'US', now() - interval '4 days'),
('Beatriz Costa',     'beatriz.costa@example.com',      'PT', now() - interval '3 days'),
('Cyrus Reeves',      'cyrus.reeves@example.com',       'ZA', now() - interval '2 days'),
('Santiago Toro',      'santiago.toros@example.com',       'ZA', now() - interval '2 days'),
('Delia Stone',       'delia.stone@example.com',        'NZ', now() - interval '1 day');

-- ============================================================
-- Seed: 200 cases with diverse keywords (deterministic)
-- ============================================================

INSERT INTO cases (customer_id, title, description, status, updated_at) VALUES
-- Customer 1 - Alice Martin
(1, 'Billing discrepancy on invoice #1001', 'Customer reports a billing discrepancy on their monthly invoice. Charges appear duplicated for the premium tier.', 'open', now() - interval '30 days'),
(1, 'Onboarding document verification failed', 'During onboarding the KYC document verification step failed due to an expired passport. Customer needs re-submission guidance.', 'in_progress', now() - interval '29 days'),
(1, 'AML alert triggered on account', 'Automated AML screening flagged a series of rapid transactions. Compliance team review required before releasing funds.', 'open', now() - interval '28 days'),

-- Customer 2 - Bob Johnson
(2, 'Payment gateway reconciliation error', 'Reconciliation between payment gateway and internal ledger shows a mismatch of $240. Investigation required.', 'in_progress', now() - interval '28 days'),
(2, 'Fraud detection false positive', 'Transaction flagged as potential fraud by the detection engine. Customer confirmed legitimate purchase. Need whitelist update.', 'closed', now() - interval '27 days'),
(2, 'Compliance audit data request', 'Regulatory compliance team requests full audit trail for the past 12 months of transactions for external auditor review.', 'open', now() - interval '26 days'),

-- Customer 3 - Carlos Rivera
(3, 'Chargeback dispute for order #5023', 'Customer raised a chargeback dispute with their bank. Documentation of original payment and delivery needed.', 'open', now() - interval '27 days'),
(3, 'Subscription billing cycle misalignment', 'Billing cycle does not align with contract start date. Pro-rated billing adjustment required for month one.', 'in_progress', now() - interval '25 days'),
(3, 'Payments API timeout during checkout', 'Integration partner reports repeated timeout errors on the payments API endpoint during peak checkout hours.', 'open', now() - interval '24 days'),

-- Customer 4 - Diana Müller
(4, 'GDPR data deletion request', 'Customer submitted a GDPR right-to-erasure request. All PII must be anonymised within 30 days per regulation.', 'in_progress', now() - interval '26 days'),
(4, 'Audit log export for SOC2 review', 'Internal security team needs full audit log export to satisfy annual SOC2 Type II audit requirements.', 'open', now() - interval '23 days'),
(4, 'Wire transfer reconciliation discrepancy', 'Incoming wire transfer amount does not match purchase order. Reconciliation team flagged a $1,200 variance.', 'open', now() - interval '22 days'),

-- Customer 5 - Eva Rossi
(5, 'Onboarding KYC manual review needed', 'Automated KYC check returned inconclusive result. Manual review by compliance analyst required before account activation.', 'in_progress', now() - interval '25 days'),
(5, 'Invoice tax calculation incorrect', 'Sales tax applied at incorrect rate for the customer jurisdiction. Credit note to be issued for overpaid tax amount.', 'closed', now() - interval '20 days'),
(5, 'Fraud investigation - account takeover', 'Multiple failed login attempts followed by password reset and large withdrawal. Suspected account takeover fraud case.', 'open', now() - interval '18 days'),

-- Customer 6 - Frank Dubois
(6, 'Reconciliation report generation failure', 'Scheduled end-of-month reconciliation report failed to generate due to a database timeout. Manual trigger needed.', 'open', now() - interval '24 days'),
(6, 'Billing plan upgrade request', 'Customer requests upgrade from Starter to Enterprise billing plan. Contract amendment and prorated invoice required.', 'closed', now() - interval '22 days'),
(6, 'AML transaction monitoring escalation', 'Ongoing AML monitoring identified a pattern of structuring transactions below reporting threshold. Escalated to compliance.', 'in_progress', now() - interval '19 days'),

-- Customer 7 - Grace Kim
(7, 'Payment refund not received', 'Customer initiated a refund 10 business days ago. Refund has not appeared in their bank account. Trace required.', 'open', now() - interval '23 days'),
(7, 'Compliance training certification upload', 'New employee needs to upload compliance training certification to complete onboarding requirements.', 'closed', now() - interval '21 days'),
(7, 'ACH payment returned - insufficient funds', 'ACH debit returned with R01 insufficient funds code. Customer needs to update payment method and repay balance.', 'in_progress', now() - interval '17 days'),

-- Customer 8 - Hiro Tanaka
(8, 'Audit trail integrity check failed', 'Automated integrity check on audit trail detected a gap in log sequence numbers. Security investigation required.', 'open', now() - interval '22 days'),
(8, 'Billing dispute - double charge', 'Customer was charged twice for same service period due to a system error. Duplicate charge must be reversed immediately.', 'closed', now() - interval '20 days'),
(8, 'Sanctions screening alert', 'Payment to a counterparty that appears on a sanctions watchlist was blocked. Compliance officer review required.', 'open', now() - interval '15 days'),

-- Customer 9 - Isla Brown
(9, 'Onboarding delayed - document backlog', 'Customer onboarding is delayed because the document verification backlog exceeds SLA. Escalation to ops manager needed.', 'in_progress', now() - interval '21 days'),
(9, 'Payments reconciliation month-end close', 'Month-end close process requires reconciliation of all payments processed in period. Report due in 48 hours.', 'open', now() - interval '18 days'),
(9, 'Fraud ring detection alert', 'Risk engine identified this account as potentially linked to a known fraud ring based on device fingerprint and IP cluster.', 'open', now() - interval '12 days'),

-- Customer 10 - João Silva
(10, 'Invoice PDF generation error', 'Customer unable to download invoice PDF from portal. Server-side rendering fails with 500 error for invoices over 50 items.', 'open', now() - interval '20 days'),
(10, 'AML periodic review due', 'Scheduled annual AML customer due diligence review is due. Enhanced due diligence required for this high-risk category.', 'in_progress', now() - interval '16 days'),
(10, 'Compliance policy update notification', 'Customer must acknowledge updated compliance and terms of service policy before next billing cycle to maintain account.', 'open', now() - interval '10 days'),

-- Customer 11 - Karen White
(11, 'Billing webhook delivery failure', 'Billing system webhook for payment events is failing with 503 errors on customer endpoint. Retry queue backing up.', 'open', now() - interval '19 days'),
(11, 'Chargeback second representment', 'First chargeback representment rejected by card network. Preparing second representment with additional evidence.', 'in_progress', now() - interval '15 days'),
(11, 'Reconciliation automation script error', 'Python reconciliation script fails on large files exceeding 100k rows. Memory optimisation or batch processing needed.', 'closed', now() - interval '11 days'),

-- Customer 12 - Liam Nguyen
(12, 'Fraud prevention rule tuning', 'High false positive rate on fraud prevention rules is blocking legitimate transactions. Rule threshold calibration required.', 'in_progress', now() - interval '18 days'),
(12, 'Audit log retention policy question', 'Customer asks whether audit logs are retained for 7 years per their internal compliance policy requirement.', 'closed', now() - interval '14 days'),
(12, 'Payments batch processing delay', 'Nightly batch payment processing job is running 3 hours behind schedule. Root cause analysis and remediation required.', 'open', now() - interval '9 days'),

-- Customer 13 - Maria Garcia
(13, 'Onboarding API integration issues', 'Third-party onboarding partner reports authentication errors on the integration API. OAuth token refresh failing intermittently.', 'open', now() - interval '17 days'),
(13, 'Billing currency conversion error', 'Invoice generated in USD but customer account is EUR. Currency conversion applied incorrect exchange rate for the period.', 'in_progress', now() - interval '13 days'),
(13, 'AML country risk re-assessment', 'Customer''s primary operating country was recently upgraded to high-risk. AML risk rating must be updated accordingly.', 'open', now() - interval '8 days'),

-- Customer 14 - Nathan Patel
(14, 'Reconciliation variance in FX trades', 'End-of-day reconciliation for FX trade positions shows a $3,500 variance. Trade confirmations need to be matched.', 'open', now() - interval '16 days'),
(14, 'Fraud alert - velocity rule triggered', 'Velocity fraud rule triggered: 15 transactions in 10 minutes across different merchants. Account temporarily suspended.', 'in_progress', now() - interval '12 days'),
(14, 'Compliance document expiry reminder', 'Customer compliance certification documents expire in 30 days. Renewal process must be initiated to avoid service interruption.', 'closed', now() - interval '7 days'),

-- Customer 15 - Olivia Chen
(15, 'Billing overpayment credit memo', 'Customer overpaid last invoice by $560. Credit memo to be applied to next billing cycle or refunded by wire transfer.', 'closed', now() - interval '15 days'),
(15, 'Onboarding UBO declaration missing', 'Ultimate Beneficial Owner declaration not submitted during onboarding. Required for regulatory compliance before account goes live.', 'in_progress', now() - interval '11 days'),
(15, 'Audit committee data package request', 'Board audit committee requests a comprehensive data package including transaction history and risk assessments for annual review.', 'open', now() - interval '6 days'),

-- Customer 16 - Paul Schmidt
(16, 'Payment processing fees audit', 'Customer requests itemised breakdown of all payment processing fees charged in Q4 to satisfy internal audit requirements.', 'open', now() - interval '14 days'),
(16, 'Fraud loss recovery process initiated', 'Fraud investigation confirmed. Loss recovery process initiated with acquiring bank and law enforcement referral filed.', 'in_progress', now() - interval '10 days'),
(16, 'AML high-risk transaction report', 'Large cash-equivalent transaction triggered mandatory AML reporting. Suspicious Activity Report (SAR) filed with regulator.', 'closed', now() - interval '5 days'),

-- Customer 17 - Quinn O''Brien
(17, 'Reconciliation for acquisition integration', 'Post-acquisition integration requires reconciliation of legacy billing system records with the new platform data.', 'open', now() - interval '13 days'),
(17, 'Billing SLA breach investigation', 'Customer claims invoice delivery SLA was breached three consecutive months. SLA report and remediation plan required.', 'in_progress', now() - interval '9 days'),
(17, 'Compliance gap analysis report', 'Pre-regulatory exam compliance gap analysis requested. Covers AML, KYC, fraud controls and data protection obligations.', 'open', now() - interval '4 days'),

-- Customer 18 - Rosa Fernandez
(18, 'Payments settlement delay', 'Settlement of last Friday''s payment batch was delayed by correspondent bank. Funds expected but not received by cut-off.', 'open', now() - interval '12 days'),
(18, 'Fraud chargeback ratio spike', 'Chargeback ratio exceeded 1% threshold this month. Card network may impose penalties. Remediation plan urgently required.', 'in_progress', now() - interval '8 days'),
(18, 'Audit findings remediation tracking', 'Internal audit issued five findings in last review. Remediation actions due. Status tracking required for next audit cycle.', 'open', now() - interval '3 days'),

-- Customer 19 - Samuel Okafor
(19, 'Onboarding video verification failure', 'Liveness detection during onboarding video verification is failing for customers using older mobile devices. SDK update needed.', 'in_progress', now() - interval '11 days'),
(19, 'Billing auto-renew cancellation dispute', 'Customer disputes auto-renewal charge claiming they cancelled before renewal date. Cancellation log investigation required.', 'closed', now() - interval '7 days'),
(19, 'AML adverse media screening hit', 'Adverse media screening returned a match for this customer. Human review required to determine if match is genuine.', 'open', now() - interval '2 days'),

-- Customer 20 - Tina Kowalski
(20, 'Reconciliation multi-currency ledger', 'Multi-currency ledger reconciliation failing due to rounding differences in FX conversion calculations. Fix required.', 'open', now() - interval '10 days'),
(20, 'Fraud model retraining request', 'Data science team requests labelled fraud case data export to retrain the ML fraud detection model for next quarter.', 'in_progress', now() - interval '6 days'),
(20, 'Compliance sanctions list update', 'OFAC and EU sanctions lists updated. Customer screening must be re-run against new lists within 24 hours per policy.', 'open', now() - interval '1 day'),

-- Customer 21 - Uma Sharma
(21, 'Billing integration with ERP system', 'Customer requests API integration between our billing system and their SAP ERP. Invoice data must sync automatically.', 'in_progress', now() - interval '10 days'),
(21, 'Payments instant transfer feature request', 'Customer requests access to the instant payment transfer feature for their enterprise account tier. Approval required.', 'open', now() - interval '8 days'),
(21, 'Audit log format compliance question', 'Customer''s auditor requires logs in CEF (Common Event Format). Current format is JSON. Conversion or export option needed.', 'closed', now() - interval '5 days'),

-- Customer 22 - Victor Hugo
(22, 'Fraud ring link analysis', 'Graph analysis reveals this account shares device IDs with 12 other accounts in a suspected fraud ring. Freeze recommended.', 'open', now() - interval '9 days'),
(22, 'Billing address verification failure', 'AVS check failing for customer billing address during card transactions. Address update and re-verification needed.', 'in_progress', now() - interval '5 days'),
(22, 'Reconciliation post-migration validation', 'Data migration to new core banking system requires full reconciliation to confirm all balances transferred correctly.', 'open', now() - interval '2 days'),

-- Customer 23 - Wendy Adams
(23, 'Compliance training gap identified', 'Annual compliance training completion check revealed 3 employees have not completed AML training. Escalation to HR.', 'closed', now() - interval '8 days'),
(23, 'Payments cross-border fee dispute', 'Customer disputes cross-border payment fee citing fee schedule at time of contract. Legal and billing review required.', 'open', now() - interval '4 days'),
(23, 'Onboarding risk score too high', 'Automated onboarding risk scoring returned a score above threshold. Enhanced due diligence interview required.', 'in_progress', now() - interval '1 day'),

-- Customer 24 - Xavier Lopez
(24, 'Billing tax exemption certificate upload', 'Customer submitting tax exemption certificate to remove sales tax from future invoices. Certificate must be validated.', 'open', now() - interval '7 days'),
(24, 'Fraud account rehabilitation request', 'Account was suspended after fraud alert. Customer provided evidence of innocence. Rehabilitation and re-activation requested.', 'in_progress', now() - interval '4 days'),
(24, 'AML transaction pattern analysis', 'Complex transaction pattern flagged for deeper AML analysis. Flow of funds investigation covering 90-day period.', 'open', now() - interval '2 days'),

-- Customer 25 - Yuki Sato
(25, 'Reconciliation EOD balances mismatch', 'End-of-day balance report shows $8,750 discrepancy vs core banking system. Urgent reconciliation required before market open.', 'open', now() - interval '6 days'),
(25, 'Compliance risk appetite review', 'Annual review of compliance risk appetite statement required. Board sign-off needed before Q1 begins.', 'in_progress', now() - interval '4 days'),
(25, 'Billing early termination fee calculation', 'Customer terminating contract early. Early termination fee must be calculated per contract terms and invoiced correctly.', 'closed', now() - interval '2 days'),

-- Customer 26 - Zara Ahmed
(26, 'Payments API rate limiting issue', 'Integration partner hitting rate limits on the payments API during high-volume batch processing. Limit increase or optimisation needed.', 'open', now() - interval '5 days'),
(26, 'Fraud pattern - synthetic identity', 'Risk team identified indicators of synthetic identity fraud on this account. Account review and potential closure required.', 'in_progress', now() - interval '3 days'),
(26, 'Audit readiness assessment', 'Upcoming regulatory audit in 60 days. Comprehensive audit readiness assessment and evidence collection programme required.', 'open', now() - interval '1 day'),

-- Customer 27 - Aaron Clark
(27, 'Billing dunning process failure', 'Automated dunning process for overdue invoices failed to send email reminders. Manual outreach required for 14 accounts.', 'in_progress', now() - interval '4 days'),
(27, 'Onboarding compliance checklist incomplete', 'Three mandatory onboarding compliance checklist items remain incomplete. Account cannot be activated until resolved.', 'open', now() - interval '2 days'),
(27, 'Reconciliation year-end close', 'Year-end accounting close requires full reconciliation of all ledger accounts. External auditor access needed for review.', 'open', now() - interval '1 day'),

-- Customer 28 - Beatriz Costa
(28, 'AML politically exposed person flag', 'Customer identified as Politically Exposed Person (PEP). Enhanced due diligence and senior management approval required.', 'in_progress', now() - interval '3 days'),
(28, 'Billing discount code not applied', 'Promotional discount code was not applied to the customer''s invoice despite being valid. Manual adjustment and credit needed.', 'closed', now() - interval '2 days'),
(28, 'Fraud early warning indicator triggered', 'Early warning fraud indicator triggered based on unusual login geolocation pattern. Customer verification call required.', 'open', now() - interval '1 day'),

-- Customer 29 - Cyrus Reeves
(29, 'Payments SWIFT message format error', 'Outgoing SWIFT payment message rejected due to incorrect field format in MT103. Correction and retransmission required.', 'open', now() - interval '2 days'),
(29, 'Compliance sanctions match review', 'Possible sanctions match identified for customer legal entity name. Legal and compliance review required within 24 hours.', 'in_progress', now() - interval '1 day'),
(29, 'Billing invoice dispute resolution', 'Long-running billing dispute from 90 days ago needs final resolution. Both parties agreed to a 50/50 split of the variance.', 'closed', now() - interval '1 day'),

-- Customer 30 - Delia Stone
(30, 'Onboarding biometric verification error', 'Biometric verification during onboarding returning false negatives. Root cause may be low image quality from mobile capture.', 'open', now() - interval '1 day'),
(30, 'Reconciliation for fund transfer', 'Large fund transfer to new custody account requires reconciliation confirmation from both sending and receiving institutions.', 'in_progress', now() - interval '1 day'),
(30, 'AML risk re-rating after FATF update', 'Following FATF grey-list update for customer''s home country, risk re-rating and enhanced monitoring programme required.', 'open', now() - interval '12 hours'),

-- Additional cases to reach 200 (filling out the count)
(1,  'Payment reconciliation for Q3',          'Quarterly payment reconciliation for Q3 shows minor discrepancies across 3 payment channels. Detailed report needed.', 'closed', now() - interval '28 days'),
(2,  'Billing portal login issue',              'Customer unable to log into the billing portal after password reset. Session token appears to be stale.', 'closed', now() - interval '26 days'),
(3,  'Fraud dispute documentation needed',      'Fraud dispute requires original transaction receipts and merchant confirmation. Documentation collection in progress.', 'in_progress', now() - interval '23 days'),
(4,  'Audit evidence collection for SOC2',      'SOC2 audit evidence collection phase underway. IT and operations teams providing requested artefacts to auditors.', 'in_progress', now() - interval '21 days'),
(5,  'Compliance certification renewal',        'Annual compliance certification renewal due. Employee must complete updated training modules before expiry.', 'open', now() - interval '19 days'),
(6,  'Billing statement formatting issue',      'Monthly billing statement PDF has formatting errors on page 3 when invoice count exceeds 50 line items.', 'closed', now() - interval '17 days'),
(7,  'Payments routing error',                  'Payment routed to incorrect beneficiary account due to incorrect IBAN entry. Recall request sent to correspondent bank.', 'open', now() - interval '16 days'),
(8,  'Onboarding API documentation gap',        'Partner onboarding API documentation is missing examples for edge cases. Update requested by integration team.', 'closed', now() - interval '15 days'),
(9,  'AML monitoring rule deployment',          'New AML transaction monitoring rule deployed to catch layering typology. Parallel run period before full activation.', 'in_progress', now() - interval '14 days'),
(10, 'Reconciliation between subsidiaries',     'Inter-company reconciliation between parent and two subsidiaries required for consolidated financial reporting.', 'open', now() - interval '13 days'),
(11, 'Fraud team training session request',     'Fraud operations team requests specialist training session on new card-not-present fraud typologies and detection methods.', 'closed', now() - interval '12 days'),
(12, 'Billing API versioning question',         'Customer asks whether current billing API v2 will be deprecated and what migration path is available to v3.', 'closed', now() - interval '11 days'),
(13, 'Audit trail search performance issue',    'Audit trail search tool taking over 30 seconds for queries spanning more than 90 days of logs. Indexing improvement needed.', 'in_progress', now() - interval '10 days'),
(14, 'Payments cutover to new processor',       'Cutover to new payment processor planned for next month. Parallel testing and reconciliation required during transition.', 'open', now() - interval '9 days'),
(15, 'Compliance monitoring dashboard setup',   'Real-time compliance monitoring dashboard configuration for new regulatory reporting requirements. Go-live in 2 weeks.', 'in_progress', now() - interval '8 days'),
(16, 'Billing customer dunning preferences',    'Customer requests custom dunning communication preferences: email only, no SMS, 14-day grace period before suspension.', 'closed', now() - interval '7 days'),
(17, 'Fraud notification configuration',        'Customer requests configurable fraud alert notifications via webhook rather than email. API integration required.', 'open', now() - interval '6 days'),
(18, 'AML typology training materials',         'Compliance team requests updated AML typology training materials including case studies on trade-based money laundering.', 'in_progress', now() - interval '5 days'),
(19, 'Reconciliation automation tool review',   'Quarterly review of automated reconciliation tool performance. Coverage rate and exception resolution time metrics required.', 'open', now() - interval '4 days'),
(20, 'Billing contract amendment processing',   'Contract amendment signed last week needs to be processed in billing system to reflect new pricing and service terms.', 'open', now() - interval '3 days'),
(21, 'Fraud transaction dispute filing',        'Customer filing formal dispute for three fraudulent transactions. Evidence package compiled and submitted to card network.', 'in_progress', now() - interval '3 days'),
(22, 'Audit sampling methodology question',     'External auditor asks about the statistical sampling methodology used for transaction audit testing. Documentation needed.', 'open', now() - interval '2 days'),
(23, 'Payments interoperability testing',       'New payments interoperability standard being tested with 5 partner banks. Test scenarios cover edge cases and failure modes.', 'in_progress', now() - interval '2 days'),
(24, 'Compliance risk assessment update',       'Quarterly compliance risk assessment update required following regulatory guidance changes published last week.', 'open', now() - interval '1 day'),
(25, 'Billing usage-based invoice correction',  'Usage-based billing invoice shows incorrect metered usage figures. Raw usage logs must be re-processed and invoice corrected.', 'in_progress', now() - interval '1 day'),
(26, 'Fraud watchlist hit confirmation',        'Automated fraud watchlist screening returned a possible hit. Manual confirmation and account action decision required.', 'open', now() - interval '18 hours'),
(27, 'AML case management system migration',   'Legacy AML case management data being migrated to new platform. Data integrity validation and reconciliation required.', 'in_progress', now() - interval '12 hours'),
(28, 'Billing cycle proration correction',      'Mid-cycle plan change resulted in incorrect proration calculation. Billing system bug confirmed. Credit note being issued.', 'open', now() - interval '10 hours'),
(29, 'Audit log export for external review',    'Customer legal team requests exportable audit log for external legal review in connection with ongoing litigation matter.', 'in_progress', now() - interval '8 hours'),
(30, 'Compliance hotline report investigation', 'Anonymous compliance hotline report received regarding potential policy violation. Internal investigation initiated.', 'open', now() - interval '6 hours'),
(1,  'Onboarding fraud screening step',         'Fraud screening during onboarding returned a soft decline. Manual underwriting review required before account creation.', 'in_progress', now() - interval '27 days'),
(2,  'Billing credits not reflected',           'Promotional credits applied to the account are not showing in the current billing statement. Technical correction needed.', 'open', now() - interval '25 days'),
(3,  'AML enhanced due diligence case',         'Enhanced due diligence case opened for high-risk customer. Financial intelligence unit review required before sign-off.', 'open', now() - interval '22 days'),
(4,  'Reconciliation interbank position',       'Daily interbank position reconciliation showing a failed nostro entry. Correspondent bank investigation required.', 'in_progress', now() - interval '20 days'),
(5,  'Fraud consortium data share request',     'Joining industry fraud consortium requires sharing anonymised fraud case data. Legal and privacy review before data export.', 'open', now() - interval '18 days'),
(6,  'Billing tax jurisdiction update',         'Customer relocated to a different tax jurisdiction. Billing tax rules must be updated and historical invoices reviewed.', 'closed', now() - interval '16 days'),
(7,  'Compliance vendor risk assessment',       'Third-party vendor compliance risk assessment due. Vendor questionnaire sent and responses under review by risk team.', 'in_progress', now() - interval '15 days'),
(8,  'Payments SEPA mandate collection',        'SEPA direct debit mandate collection from customer required before first recurring payment can be processed next week.', 'open', now() - interval '14 days'),
(9,  'Fraud velocity monitoring increase',      'Fraud velocity monitoring alert thresholds reduced following recent spike in card testing attacks across platform.', 'closed', now() - interval '13 days'),
(10, 'Audit committee minutes distribution',    'Quarterly audit committee meeting minutes ready for distribution. Requires CISO and CFO sign-off before release.', 'open', now() - interval '12 days'),
(11, 'Reconciliation P&L attribution',          'Month-end P&L attribution reconciliation between trading and finance teams showing $6,200 unexplained variance.', 'in_progress', now() - interval '11 days'),
(12, 'Billing dispute escalation path',         'Customer dissatisfied with billing dispute resolution. Requesting escalation to senior account manager for final decision.', 'open', now() - interval '10 days'),
(13, 'AML suspicious activity alert',           'Transaction monitoring system generated a suspicious activity alert for this customer. Alert review and disposition required.', 'in_progress', now() - interval '9 days'),
(14, 'Onboarding ID verification API failure',  'Third-party identity verification API returning HTTP 502 errors intermittently. Onboarding flow blocked for new customers.', 'open', now() - interval '8 days'),
(15, 'Fraud dispute pre-arbitration',           'Card network moved dispute to pre-arbitration stage. Legal team reviewing options before final arbitration decision.', 'in_progress', now() - interval '7 days'),
(16, 'Billing dunning final notice',            'Final dunning notice sent for 90-day overdue invoice. Account suspension will be triggered if payment not received in 7 days.', 'open', now() - interval '6 days'),
(17, 'Compliance DSAR processing',              'Data Subject Access Request received under GDPR. All personal data must be compiled and returned within 30 days.', 'in_progress', now() - interval '5 days'),
(18, 'Reconciliation cutover validation',       'System cutover reconciliation validation comparing legacy and new system totals. Sign-off required from CFO before go-live.', 'open', now() - interval '4 days'),
(19, 'Fraud case closure documentation',        'Fraud investigation concluded. Case closure documentation and lessons learned report to be filed in case management system.', 'closed', now() - interval '3 days'),
(20, 'AML risk model validation',               'Annual AML risk model validation by second line of defence. Model performance metrics and threshold review required.', 'in_progress', now() - interval '2 days'),
(21, 'Billing API error response codes',        'Customer''s development team reports billing API returning non-standard error codes. Documentation correction and SDK update needed.', 'open', now() - interval '2 days'),
(22, 'Audit evidence gap identified',           'Pre-audit evidence review identified a gap in logging for a 2-hour window 6 months ago. Root cause analysis required.', 'in_progress', now() - interval '1 day'),
(23, 'Payments correspondent bank change',      'Customer''s correspondent bank relationship changing. All payment routing rules must be updated before effective date.', 'open', now() - interval '1 day'),
(24, 'Compliance control testing results',      'Q3 compliance control testing complete. Three controls rated as partially effective. Remediation plans required within 30 days.', 'in_progress', now() - interval '23 hours'),
(25, 'Fraud alert - account compromise',        'Customer reports suspected account compromise after noticing unrecognised transactions. Emergency account freeze applied.', 'open', now() - interval '20 hours'),
(26, 'Billing webhook signature verification',  'Customer integration team unable to verify billing webhook HMAC signature. Secret rotation may have caused the mismatch.', 'in_progress', now() - interval '16 hours'),
(27, 'AML correspondent banking due diligence', 'New correspondent banking relationship requires full AML due diligence including SWIFT KYC registry review and policy review.', 'open', now() - interval '14 hours'),
(28, 'Reconciliation data quality issue',       'Automated reconciliation tool flagging data quality issues in source payment file. Null values in mandatory fields blocking match.', 'in_progress', now() - interval '10 hours'),
(29, 'Fraud prevention ML model alert',         'New ML fraud model has flagged 23 accounts in the last 24 hours. Human review pipeline overwhelmed. Triage protocol needed.', 'open', now() - interval '6 hours'),
(30, 'Billing entitlement mismatch',            'Customer''s provisioned product entitlements do not match their current billing plan. Entitlement sync job failed last night.', 'in_progress', now() - interval '2 hours');

-- Additional 50 cases to reach 200 total
INSERT INTO cases (customer_id, title, description, status, updated_at) VALUES
(1,  'Billing API key rotation',                 'Customer requests billing API key rotation after suspected key exposure in code repository. New key generation and distribution needed.', 'open', now() - interval '26 days'),
(2,  'Fraud case peer review',                   'Fraud case requires peer review by second analyst before final disposition. First analyst conclusion is account fraud confirmed.', 'in_progress', now() - interval '24 days'),
(3,  'AML name screening database update',       'Global name screening database updated with new entries. All active customer records must be re-screened against updated database.', 'open', now() - interval '21 days'),
(4,  'Compliance breach notification',           'Potential compliance breach identified in data handling process. Regulatory notification may be required within 72 hours.', 'in_progress', now() - interval '19 days'),
(5,  'Billing credit card expiry update',        'Customer credit card on file expired. Billing system sent update request but customer has not responded. Follow-up needed.', 'open', now() - interval '17 days'),
(6,  'Reconciliation bank statement import',     'Automated bank statement import failing for statements in new XML format. Parser update required before next reconciliation run.', 'closed', now() - interval '15 days'),
(7,  'Fraud device fingerprint block',           'Fraudulent device fingerprint identified and added to blocklist. 3 accounts linked to same device are being reviewed.', 'in_progress', now() - interval '14 days'),
(8,  'Audit report distribution',                'Quarterly internal audit report ready for distribution to board members. Requires redaction of sensitive operational details.', 'open', now() - interval '13 days'),
(9,  'Payments nostro reconciliation',           'Daily nostro account reconciliation showing two unmatched entries from yesterday afternoon batch. Investigation underway.', 'in_progress', now() - interval '12 days'),
(10, 'Compliance training completion tracking',  'Q3 mandatory compliance training completion tracking. 12% of staff not yet completed. Manager escalation letters being prepared.', 'open', now() - interval '11 days'),
(11, 'Billing overage charge dispute',           'Customer disputes overage charge on API usage billing. Logs show usage within limits during disputed period. Credit to be issued.', 'closed', now() - interval '10 days'),
(12, 'AML beneficial ownership update',          'Customer company underwent ownership restructuring. AML beneficial ownership register must be updated with new UBO information.', 'open', now() - interval '9 days'),
(13, 'Fraud BIN attack detection',               'BIN attack detected: 847 test transactions against card range in 4 hours. Rate limiting applied. Full incident report required.', 'in_progress', now() - interval '8 days'),
(14, 'Audit finding - missing controls',         'Audit finding identifies missing preventive controls in payment authorisation workflow. Remediation plan due within 45 days.', 'open', now() - interval '7 days'),
(15, 'Reconciliation failed assertion',          'Daily automated reconciliation check failed assertion: closing balance does not equal opening balance plus net movements.', 'in_progress', now() - interval '6 days'),
(16, 'Billing payment method fallback',          'Primary payment method declined. Automatic fallback to backup payment method succeeded. Customer notification sent.', 'closed', now() - interval '5 days'),
(17, 'Fraud scoring model drift alert',          'Model monitoring detected drift in fraud scoring model performance. False negative rate increased 2.3% over last 30 days.', 'open', now() - interval '4 days'),
(18, 'AML transaction monitoring tuning',        'Annual AML transaction monitoring rule tuning exercise. Reducing false positive rate while maintaining detection coverage.', 'in_progress', now() - interval '3 days'),
(19, 'Compliance policy attestation',            'Annual compliance policy attestation cycle open. All employees must attest to reading and understanding the updated policies.', 'open', now() - interval '2 days'),
(20, 'Billing statement email delivery issue',   'Monthly billing statement emails not delivered to customer domain. SPF/DKIM failure suspected. Email infrastructure check needed.', 'in_progress', now() - interval '1 day'),
(21, 'Fraud alert suppression request',          'Customer security team requests temporary suppression of fraud alerts during planned load test to avoid false positives.', 'open', now() - interval '30 hours'),
(22, 'Audit trail completeness verification',    'Third-party auditor requires completeness verification of audit trail covering 18-month period. Sampling methodology agreed.', 'in_progress', now() - interval '28 hours'),
(23, 'Payments mandate amendment',               'Direct debit mandate amendment requested by customer for updated bank account details. New mandate verification required.', 'open', now() - interval '26 hours'),
(24, 'Compliance incident post-mortem',          'Post-mortem review of last month''s compliance incident. Root cause analysis, timeline reconstruction, and prevention plan required.', 'in_progress', now() - interval '24 hours'),
(25, 'Billing tax remittance report',            'Monthly tax remittance report generation for multi-jurisdiction tax filing. Data from billing system required by tax team.', 'open', now() - interval '22 hours'),
(26, 'Fraud prevention controls review',         'Annual fraud prevention controls review covering card controls, behavioural biometrics, and device intelligence integration.', 'in_progress', now() - interval '20 hours'),
(27, 'AML typology update briefing',             'New AML typology briefing on cryptocurrency-linked money laundering schemes. Compliance team training session scheduled.', 'open', now() - interval '18 hours'),
(28, 'Reconciliation automated exception report','Automated reconciliation exception report showing 14 unmatched items. Manual investigation and resolution required by EOD.', 'in_progress', now() - interval '15 hours'),
(29, 'Billing loyalty points calculation error', 'Customer''s loyalty points balance does not match expected calculation based on eligible transactions. Audit and correction needed.', 'open', now() - interval '12 hours'),
(30, 'Fraud prevention API health check',        'Fraud prevention API health check failing in secondary region. Failover traffic routed to primary. Secondary investigation in progress.', 'in_progress', now() - interval '8 hours'),
(1,  'AML periodic review overdue',              'Customer AML periodic review is 15 days overdue. Relationship manager has been notified. Account restrictions applied until complete.', 'open', now() - interval '7 hours'),
(2,  'Billing coupon expiry dispute',            'Customer applied a coupon code that appeared valid in the portal but was rejected at checkout due to backend expiry. Fix needed.', 'closed', now() - interval '6 hours'),
(3,  'Compliance whistleblower case',            'Whistleblower report submitted via compliance hotline regarding potential AML violations. Confidential investigation being initiated.', 'in_progress', now() - interval '5 hours'),
(4,  'Payments RTGS cutoff missed',              'Payment instruction arrived after RTGS system cutoff time. Instruction queued for next business day processing. Customer notified.', 'open', now() - interval '4 hours'),
(5,  'Fraud account closure request',            'Following confirmed fraud loss, customer requests full account closure and data deletion. Fraud case must be closed first.', 'in_progress', now() - interval '3 hours'),
(6,  'Audit evidence retention policy',          'Clarification needed on audit evidence retention policy: how long must evidence be kept after audit closure for potential re-inspection.', 'open', now() - interval '2 hours 30 minutes'),
(7,  'Billing subscription pause request',       'Customer requests temporary subscription pause during planned system maintenance period. Billing proration during pause to be confirmed.', 'open', now() - interval '2 hours'),
(8,  'Reconciliation automated matching rate',   'Automated payment matching rate dropped below 95% SLA this week. Root cause investigation and matching rule improvement required.', 'in_progress', now() - interval '90 minutes'),
(9,  'AML de-risking decision review',           'Business decision to de-risk a customer segment under AML policy review. Exit strategy and customer communication plan needed.', 'open', now() - interval '75 minutes'),
(10, 'Compliance data residency question',       'Customer asks whether their data is stored exclusively within EU boundaries per data residency requirements in their contract.', 'closed', now() - interval '60 minutes'),
(11, 'Fraud liability shift confirmation',       'Confirmation needed on which party bears liability for disputed transaction under EMV liability shift rules. Legal review requested.', 'open', now() - interval '50 minutes'),
(12, 'Billing invoice archival process',         'Customer requests long-term invoice archival solution with 10-year retention for regulatory compliance purposes.', 'in_progress', now() - interval '40 minutes'),
(13, 'Onboarding source of funds verification',  'Source of funds declaration submitted by customer during onboarding requires independent verification documentation.', 'open', now() - interval '30 minutes'),
(14, 'Payments bulk disbursement review',        'Large bulk disbursement file submitted for processing. Pre-payment fraud check and beneficiary validation required before release.', 'in_progress', now() - interval '20 minutes'),
(15, 'Audit committee risk register update',     'Quarterly risk register update for audit committee presentation. New risks identified in fraud and compliance categories to be added.', 'open', now() - interval '15 minutes'),
(16, 'Fraud network analysis expansion',         'Fraud network analysis expanded to include 2 new clusters identified by graph analytics. Linked accounts under review.', 'in_progress', now() - interval '10 minutes'),
(17, 'Billing integration test failure',         'Pre-production billing integration test failing on edge case: zero-amount invoice with tax. Code fix and regression test needed.', 'open', now() - interval '8 minutes'),
(18, 'AML customer exit process',                'AML-driven customer exit process initiated. Detailed exit checklist including final transaction review and account balance transfer.', 'in_progress', now() - interval '5 minutes'),
(19, 'Compliance dashboard metrics refresh',     'Compliance KPI dashboard metrics not refreshing correctly. Data pipeline issue causing stale metrics to be displayed to management.', 'open', now() - interval '3 minutes'),
(20, 'Reconciliation final sign-off',            'Month-end reconciliation completed and reviewed. Final sign-off from Finance Controller required before books are closed.', 'closed', now() - interval '1 minute'),
(21, 'santi test',            'Santi Test', 'closed', now() - interval '1 minute');
