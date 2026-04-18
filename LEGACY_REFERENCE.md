# LEGACY REFERENCE

## 1. Purpose

This document defines how the new eFiscal app uses the code in /legacy as a reference.

Policy:
- Use /legacy to understand business behavior, flow, data semantics, and edge cases.
- Do not copy code, class hierarchies, or framework runtime patterns from /legacy.
- Reimplement behavior in the modern target stack and architecture.

## 2. Reference Scope

In scope for reference:
- Fiscal bill issuance flow and validation logic.
- Tax item aggregation and payment mapping behavior.
- External platform integration patterns and process sequencing.
- Data concepts for fiscal bill, fiscal tax, and platform connection metadata.

Out of scope for reuse:
- iDempiere PO/X_/I_ model inheritance and generated classes.
- OSGI factory wiring and iDempiere process runtime APIs.
- Direct SQL/process code transplanting.
- Legacy auth or SSL handling implementation details as-is.

## 3. Allowed vs Prohibited Usage

Allowed:
1. Extract behavior rules and acceptance criteria.
2. Reuse naming at domain level where meaningful (for business clarity).
3. Rebuild algorithms in modern idioms (service-layer, transactional boundaries, retries).
4. Use legacy files as evidence for test-case design.

Prohibited:
1. Copy-paste methods, classes, SQL blocks, or XML descriptors.
2. Import or depend on packages under org.elef.model or org.elef.processes in new app code.
3. Reproduce iDempiere-specific lifecycle patterns in the new architecture.
4. Treat legacy schemas/classes as immutable templates for new persistence models.

## 4. Legacy Capability Inventory (Observed)

The following capabilities are implemented in legacy and should be used as behavioral reference:

1. Fiscal bill issuing process orchestration.
2. Duplicate detection for bill submission scenarios.
3. Request payload construction with header, buyer, payment, line items, and tax data.
4. Support for multiple invoice and transaction types (normal, proforma, copy, training, advance; sale/refund).
5. API connection abstraction with auth and certificate-oriented settings.
6. Response mapping and persistence of fiscal bill artifacts (signature, QR, verification URL, tax breakdown).
7. Repository lookup patterns for finding existing fiscal bill entries.
8. Status flow extension point (stubbed in legacy, still useful as a conceptual hook).

## 5. Legacy Sources of Truth (Reference Evidence)

Primary process and service flow:
- legacy/org.elef.processes/src/org/elef/efiscal/PostFiscalBill.java
- legacy/org.elef.processes/src/org/elef/efiscal/FiscalBillService.java

Persistence and lookup behavior:
- legacy/org.elef.processes/src/org/elef/efiscal/FiscalBillRepository.java
- legacy/org.elef.processes/src/org/elef/efiscal/eFiscalUtils.java

Domain model concepts:
- legacy/org.elef.model/src/org/elef/model/I_ELF_ApiConn.java
- legacy/org.elef.model/src/org/elef/model/I_ELF_FiscalBill.java
- legacy/org.elef.model/src/org/elef/model/I_ELF_FiscalTax.java

Framework-specific patterns to avoid copying:
- legacy/org.elef.model/src/org/elef/model/factory/ElfModelFactory.java
- legacy/org.elef.processes/src/org/elef/processes/factories/ProcessFactory.java

## 6. Adaptation Map (Legacy -> Modern)

| Legacy pattern | Modern implementation target | Adaptation rule |
| --- | --- | --- |
| SvrProcess-driven orchestration | Spring service + explicit application use case orchestration | Keep flow intent, redesign execution lifecycle |
| iDempiere PO/X_/I_ generated model pattern | Plain domain entities + JPA mappings | Keep semantics, not inheritance model |
| OSGI factories (model/process) | Spring DI configuration | Preserve extension intent with modern DI |
| Ad-hoc error handling in process/service | Typed error model from API_CONTRACT + structured logging | Preserve failure semantics, improve reliability |
| Duplicate check via repository query | Idempotency key + uniqueness constraints + service guard | Preserve dedup intent with explicit API contract |
| Connection metadata in legacy model | Secure connection profile in new config model | Move secrets to secure config handling |

## 7. Initial Traceability Matrix

This starter matrix maps new-spec behaviors to legacy references. Expand it during implementation planning.

| New app spec area | Legacy behavioral reference | How to use it |
| --- | --- | --- |
| POST /fiscalbill behavior | PostFiscalBill + FiscalBillService | Rebuild end-to-end issuance workflow in modern service layer |
| Retry and duplicate prevention | FiscalBillRepository + service checks | Define idempotency and dedup rules without copying query code |
| Fiscal bill persistence fields | I_ELF_FiscalBill + eFiscalUtils | Derive field semantics and response capture expectations |
| Tax breakdown persistence | I_ELF_FiscalTax + eFiscalUtils | Recreate tax-result capture model suitable for new schema |
| Platform connection profile | I_ELF_ApiConn | Redesign secure connection entity and validation rules |

## 8. Delivery Controls

Definition of Done checks:
1. No direct code copied from /legacy in implementation files.
2. Each implemented behavior cites one row in Section 7.
3. Code review confirms behavioral parity and structural reimplementation.
4. Error handling and idempotency follow API_CONTRACT, not legacy runtime assumptions.
5. Data modeling follows DATA_MODEL decisions even when legacy differs.

PR template recommendation:
- Reference row from Section 7:
- Legacy evidence file(s):
- Modern adaptation note (what changed and why):

## 9. Open Items for Next Iteration

1. Add per-endpoint deep mapping once final API surface is frozen.
2. Add per-entity mapping once DATA_MODEL reaches v1 freeze.
3. Add test-case matrix (happy path, duplicate, timeout, external API error, partial persistence failure).
