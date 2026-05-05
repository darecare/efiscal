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
- View fiscal bills with related tax and payment rows
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
8. If response is 200, data is saved into tables fiscalbill and fiscalbilltax.

### 4.1.1 Rules to prepare API request
1. Reuse code and logic from /legacy folder, from classes PostFiscalBill.java and FiscalBillService.java
2. Read POST API request spec: https://tap.sandbox.suf.purs.gov.rs/Help/view/1522287161/Create-Invoice/en-US

### 4.1.2 Rules to prepare request body document header data
1. When user starts action for creating new fiscal bill, on modal page user will need to select data from 2 dropdown fields:
invoiceType // 0-Normal, 4-Advance
transactionType = 0; // 0-Sale, 1-Refund
field in request "invoiceNumber" to be populated from table fiscalbillconfig.esirno
2. Date and time are taken from system datetime Belgrade timezone

### 4.1.3 rules to prepare fiscalbill line items
1. Reuse code from /legacy FiscalbillService.java from function setLineItems() for invoicetype = 0
2. If invoiceType = 4, then reuse code /legacy FiscalbillService.java from function setAdvanceLineItems(). When type is Advance, then we are sending summarized lines based on different tax rates (if different rates exist on acutal line items from sales order)
2.1 line items for advance type of fiscal bill have predefined naming of field name.
It can be read here (on serbian language) in spec:
https://tap.sandbox.suf.purs.gov.rs/Help/view/638196160/%D0%98%D0%B7%D0%B4%D0%B0%D0%B2%D0%B0%D1%9A%D0%B5-%D1%84%D0%B8%D1%81%D0%BA%D0%B0%D0%BB%D0%BD%D0%B8%D1%85-%D1%80%D0%B0%D1%87%D1%83%D0%BD%D0%B0-%D1%83-%D1%81%D0%BB%D1%83%D1%87%D0%B0%D1%98%D1%83-%D0%BD%D0%B0%D0%BF%D0%BB%D0%B0%D1%82%D0%B5-%D0%B0%D0%B2%D0%B0%D0%BD%D1%81%D0%B0-%D1%83-%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D1%83-%D0%B5%D0%A4%D0%B8%D1%81%D0%BA%D0%B0%D0%BB%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%98%D0%B5/sr-Cyrl-RS

Name of advance item is read from tax table configuration, not hardcoded:
- tax.efiscal_advanceprefix + tax.efiscal_advancename
- mapping key is tax label used on the grouped advance line
- each label used for advance invoice must have one active tax row with both fields populated
- if any order line is missing product_tax_percent or product_tax_name, request must fail with validation error

### 4.1.4 Reference to already issued fiscal bills
In fiscalbill api request body, 2 fields for reference document must be set, if any of these conditions is met:

**Reference Logic by Invoice/Transaction Type:**
- **Advance Sale (invoiceType=4, transactionType=0)**: Can reference to last existing Advance Sale for the same order (for chained advances; e.g., SO total=1000, first Advance=500, second Advance=500).
- **Advance Refund (invoiceType=4, transactionType=1)**: Must reference to the last issued Advance Sale document for the advance closing chain.
- **Normal Sale (invoiceType=0, transactionType=0)**: Must reference to the last issued Advance Refund document (if one exists for the order).
- **Normal Refund (invoiceType=0, transactionType=1)**: Must reference to the Normal Sale document.
- **Copy Sale (invoiceType=2, transactionType=0)**: References to Normal Sale or Advance Sale (depending which copy is for).

Reference fields in api request body:
1. referentDocumentNumber - field eFiscal_sdc_invoiceno from ficalbill table
2. referentDocumentDT - field eFiscal_sdcdatetime from ficalbill table

Check reference function on /legacy setReferentFields function

### 4.1.5 Advance fiscalbill closing chain
If api request is to create Normal Sale invoice and if in database exists previously issued Advance Sale fiscalbills for same sales order, system must first create Advance Refund document.
- Advance Refund document must summarize all previous Advance Normal line items and close so that total on Advance Refund must be equal to sum of all previous Advance Normal fiscalbills.
- After Advance Refund is created, then Normal Sale document will be created and it reference fields will be populated with data from Advance Refund document

### 4.1.6 Payment items array in API request body
Reference class is /legacy FiscalBillService.java, method setPayment()
- From Sales Order, based on type of payment, element for payment array will be created.
List of type of payments from MerchantPro sales order field payment_method_code:


From Fiscal Bill spec list of payment types:
Payment Type enumeration value: 0 - Other, 1 - Cash, 2 - Card, 3 - Check, 4 - Wire Transfer, 5 - Voucher, 6 - Mobile Money

cash_delivery - 1
wire - 4
intesa - 2
raiffeisen_upc - 2

- Payment type mapping to be per client.
- Payment type mapping to be stored in a separate table - table name paytype_map
- Add config screen for payment mapping, to be linked to new table with payment type mapping 


### 4.1.7 FiscalBill with customer ID in body request
Read spec from this link:
https://tap.sandbox.suf.purs.gov.rs/Help/view/984275480/%D0%A0%D0%B0%D1%87%D1%83%D0%BD-%D1%81%D0%B0-%D0%B8%D0%B4%D0%B5%D0%BD%D1%82%D0%B8%D1%84%D0%B8%D0%BA%D0%B0%D1%86%D0%B8%D1%98%D0%BE%D0%BC-%D0%BA%D1%83%D0%BF%D1%86%D0%B0/sr-Cyrl-RS
Fiscal bill can have buyerId field which identifies customer.
- when creating fiscal bill from Sales order from MerchantPro system, if customer is legal entity field billing_type = company
then buyerid field will be populated with fixed part "10:" + order.billing_company_vat


### 4.1.8 Tax items array in API request body

### 4.2 Manual Fiscal Bill Creation
Users will use page to create manually fiscal bill and send it to Tax Authority to be fiscalized.
1. User opens manual fiscal bill form.
2. User enters header, items, payment, and invoice metadata.
3. System validates request and submits to provider.
4. System stores result and displays status.

### 4.2.1 Header data
- Input fields for creating fiscal bill:
1. Optional - buyerid:
- For manual creation of fiscal bill on Fiscal Bill page there will be a dropdown field to select type of buyer. And a separate field to enter company VAT ID.

2. Optional - Sales Order ID - if user enters manually Sales Order ID, system must perform all checks that apply for process of creating fiscal bill from Sales Order 4.1 item(and subitems)


### 4.2.2 Items list
Product items list
- List table to view all added product items
- Button to add new product - will open modal screen.
Products can be searched directly from MerchantPro database using APICongif for MerchantPro and endpoint GET for products.
- Search fields:
1. ID
2. SKU
3. EAN/barcode
3. Product Name
- When user select product from dropdown search, system adds it to fiscal bill items list, with tax taken from MerchantPro. 
Default qty = 1, price taken from MerchantPro product.price_gross

### 4.2.3 Payment types
System will allow adding more payment types for one fiscal bill.
- add separate tab, where payment type(s) will be added. It should be a table view with option to add new rows. 
- Add a field to add amount for each row with payment type
- Create new table fiscalbillpay
-2. Payment type - dropdown list to select value
Payment Type enumeration value: 0 - Other, 1 - Cash, 2 - Card, 3 - Check, 4 - Wire Transfer, 5 - Voucher, 6 - Mobile Money
- Add a validation that sum of all rows amount equals sum of all items/products - total of fiscal bill. If total of payment types is not equal to total of Fiscal bill, show error message to user.

### 4.2.3 Fiscalization process
- With a click on a button "Create fiscal bill" system will do POST method to Tax Authority and if response is 200, it will store data in fiscalbill, fiscalbilltax and fiscalbillpay tables.


### 4.3 Retry Failed Fiscalization
1. User or scheduler picks failed fiscal records.
2. System validates retry eligibility.
3. System resubmits safely with retry rules.
4. System updates attempt count, status, and audit trail.

### 4.4 Fiscal Bills Page
1. System provides a dedicated Fiscal Bills page under the Fiscal Bills menu.
2. User selects an organization and loads fiscal bills from table fiscalbill filtered by org_id.
3. Page shows a master table with fiscal bill records.
4. Selecting a fiscal bill loads related rows from fiscalbilltax and fiscalbillpay.
5. Related rows are shown in a second table area with tabs:
  - Tax Items tab shows fiscalbilltax rows for the selected fiscal bill
  - Payment Items tab shows fiscalbillpay rows for the selected fiscal bill
6. Fiscal bill list should show at minimum:
  - fiscalbill_id
  - order_id
  - status
  - customer name
  - invoice type
  - transaction type
  - Tax Authority invoice number
  - total amount
  - created timestamp

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
- GET /fiscalbill?orgId={orgId}
- GET /fiscalbill/{id}/details
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
- fiscalbill
- fiscalbilltax
- fiscalbillline
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
- User can browse fiscal bills and inspect related tax/payment rows.
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
