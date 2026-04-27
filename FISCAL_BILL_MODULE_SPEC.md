# FISCAL BILL MODULE SPECIFICATION

## 1. Purpose
Define detailed functional and technical specification for the Fiscal Bill module in eFiscal.

This module is responsible for:
- creating fiscal invoices via Serbian Tax Authority API
- handling invoice type variations (Normal, Advance, Copy, Proforma, Training, Refund variants)
- tracking status and retries
- storing auditable request/response references
- supporting manual and order-based fiscalization flows

## 2. Scope

In scope:
- Create fiscal bill from selected sales order(s)
- Create fiscal bill via manual entry
- Retry failed fiscal submissions with idempotency
- Store fiscal request/response payloads and key provider references
- Provide status visibility to users
- Support email sending after successful fiscalization (using org-level config)
- Enforce role/action permissions and client/org scope

Out of scope for this module spec:
- Product sync internals unrelated to fiscalization
- Courier/ERP module-specific workflows

## 3. Integration References

Provider documentation references:
- Serbian Tax Authority eInvoice Create endpoint docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/1522287161/Create-Invoice/en-US
- Serbian Tax Authority fiscal bill example (Normal Sale): https://tap.sandbox.suf.purs.gov.rs/Help/view/535663692/Normal-Sale/en-US
- Serbian Tax Authority tax model/example docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/417621922/Model-and-Example/en-US
- Serbian Tax Authority tax amounts docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/1034863356/Tax-Amounts/en-US
- Get Status API request docs: https://tap.sandbox.suf.purs.gov.rs/Help/view/1522287161/Get-Status/en-US

Reference-only implementation sources:
- Legacy process/service flow for fiscalization logic (no direct copy):
  - legacy/org.elef.processes/src/org/elef/efiscal/PostFiscalBill.java
  - legacy/org.elef.processes/src/org/elef/efiscal/FiscalBillService.java
  - legacy/org.elef.processes/src/org/elef/efiscal/eFiscalUtils.java

## 4. Business Flows

### 4.1 Order-Based Fiscalization
1. User fetches and filters orders.
2. User selects one or more orders for fiscalization.
3. System validates mandatory fiscal fields.
4. System creates fiscal request with idempotency key.
5. System calls Serbian Tax Authority Create Invoice endpoint.
6. System persists response payload and status.
7. User sees success/failed/pending result.

### 4.2 Manual Fiscal Bill Creation
1. User opens manual fiscal bill form.
2. User enters header, items, payment, and invoice metadata.
3. System validates request and submits to provider.
4. System stores result and displays status.

### 4.3 Retry Failed Fiscalization
1. User or scheduler picks failed fiscal records.
2. System validates retry eligibility.
3. System resubmits safely with retry rules.
4. System updates attempt count, status, and audit trail.

## 5. Supported Invoice and Transaction Types

Invoice types to support:
- Normal
- Advance
- Copy
- Proforma
- Training

Transaction types to support:
- Sale
- Refund

Notes:
- Module must support combinations allowed by provider documentation.
- New type combinations must be extensible by configuration where possible.

## 6. Request/Response Field Expectations

### 6.1 Request Core Fields
Minimum expected request structure includes:
- dateAndTimeOfIssue
- cashier
- buyerId (when applicable)
- invoiceType
- transactionType
- payment[]
  - amount
  - paymentType
- invoiceNumber
- items[]
  - name
  - quantity
  - unitPrice
  - labels[]
  - totalAmount

### 6.2 Response Core Fields
Expected response capture includes:
- requestedBy
- sdcDateTime
- invoiceCounter
- invoiceCounterExtension
- invoiceNumber
- taxItems[]
- verificationUrl
- verificationQRCode
- messages
- signedBy
- encryptedInternalData
- signature
- totalCounter
- transactionTypeCounter
- totalAmount
- taxGroupRevision
- mrc

## 7. API Contract Alignment

Module endpoints are aligned with API contract:
- POST /fiscalbill
- GET /fiscalbill/{id}
- POST /fiscalbill/{id}/retry

Behavior rules:
- Use Idempotency-Key for write/retry operations.
- Return standardized error model on non-2xx responses.
- Enforce action-based authorization and scope checks.

## 8. Authorization and Scope

Required action codes (examples):
- FISCAL_CREATE_BILL
- FISCAL_RETRY_BILL
- FISCAL_VIEW_STATUS
- FISCAL_MANUAL_CREATE

Access decision:
- role has required action
- user has organization access
- organization belongs to active client context

Bootstrap rule:
- initial deployment includes one global SuperAdmin with full module privileges.

## 9. Data Model Mapping

Primary tables:
- fiscal_bill
- fiscal_tax
- sales_orders (source linkage)
- fiscalbillconfig (org-level fiscal settings)

Important persisted attributes:
- idempotency_key
- status
- provider_reference
- request_payload
- response_payload
- last_error
- attempt_count
- fiscalized_at

Audit requirements:
- Keep traceable history for status transitions and retries.
- Keep provider response references needed for compliance and troubleshooting.

## 10. Reliability and Operational Rules

- Provider call timeout required.
- Retry only for transient failures.
- Circuit breaker required for provider instability.
- Log correlation ID for all fiscal calls.
- Never log secrets or sensitive credential material.

## 11. Email and Document Output

After successful fiscalization:
- Module can send email to customer based on org-level fiscal/email config.
- Use configured mail template and optional PDF attachment.
- Failed email should not invalidate successful fiscalization status.

## 12. Scheduler Support

Module tasks that can run manually should also be schedulable:
- retry failed fiscal bills
- status reconciliation jobs
- batch fiscalization (where business-approved)

Scheduler tasks must support:
- cron definition
- predefined filter/parameter sets
- run order sequencing

## 13. Acceptance Criteria

- User can create fiscal bill from order and receive status.
- User can manually create fiscal bill and receive status.
- Failed fiscal bill can be retried safely without duplicate submission.
- Provider request/response references are persisted for audit.
- Authorization is enforced by action + client/org scope.
- Module supports required invoice/transaction type combinations.
- Initial deployment SuperAdmin can operate all fiscal module actions.

## 14. Open Items

- Confirm final provider-required field matrix by invoice type combination.
- Confirm fiscal PDF generation format responsibilities (provider vs local rendering).
- Confirm final status enum list and transition constraints.
- Confirm retry caps and backoff policy defaults for production.
