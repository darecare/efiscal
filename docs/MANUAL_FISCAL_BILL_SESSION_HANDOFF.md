# Manual Fiscal Bill — Session Handoff

Compressed context for continuing work on **manual fiscal bill creation** (`/fiscal-bills/create`, spec §4.2). Branch at time of handoff: `pdf-email-izmene-strahinja` (uncommitted UI work + critical/high fixes).

## Artifact

Live review canvas (updated after implementation):

`/home/strax/.cursor/projects/home-strax-MerchantPro-efiscal/canvases/manual-fiscal-bill-review.canvas.tsx`

Original audit covered `CreateFiscalBill.jsx`, `POST /api/v1/fiscalbill/manual`, `FiscalBillService.createManualFiscalBill`, and `FISCAL_BILL_MODULE_SPEC.md` §4.2.

## Page architecture (current)

- **Layout:** Split view — items/header left (`fiscal-main-column`), sticky payments + summary + submit right (`fiscal-sidebar`). Tabs removed.
- **Auto-calc:** `totalAmount = quantity × unitPrice`; first payment row auto-syncs to items total unless user splits payments (`userModifiedPayment`).
- **Submit:** Disabled until balance due ≈ 0 (`isSettled`).
- **Validation:** `validateForm()` → `fieldErrors` / `itemErrors` / `paymentErrors` + top banner + `scrollToFirstError()`.
- **Result:** `fiscal-result-card--success` vs `--failed`; `502` with fiscal `status` in body → failure card.

## Finding status (F01–F17)

| ID | Sev | Title | Status |
|----|-----|-------|--------|
| F01 | Critical | Payment total not enforced server-side | **Fixed** — `validateManualPaymentTotals()` |
| F02 | Critical | Advance-close + orderId breaks on manual items | **Fixed** — `resolveItemsForFiscalChain()` |
| F03 | Critical | Weak pre-submit validation | **Fixed** — `validateForm()` + inline errors |
| F04 | High | orderId missing full §4.1 checks | **Fixed (scoped)** — chain checks only, documented; no MP fetch |
| F05 | High | Manual referent missing `referentDocumentDT` | **Fixed** — `applyManualReferentFields()` |
| F06 | High | Payments hidden in tabs | **Fixed** — split layout |
| F07 | High | Green result card on failure | **Fixed** — status-aware card + 502 handling |
| F08 | Medium | `taxPrefix` UI field unused in TA payload | **Deferred** — remove/wire alters field semantics |
| F09 | Medium | Hardcoded `TAX_LABELS` vs `/taxes` config | **Fixed** — `taxApi.list()` + fallback |
| F10 | Medium | No qty×price auto-calc | **Fixed** |
| F11 | Medium | No email send UI (API supports it) | **Fixed** — `sendEmail` + `customerEmail` |
| F12 | Medium | Copy invoice type on manual form | **Deferred** — workflow change |
| F13 | Medium | “Close Advance” checkbox unclear | **Deferred** — help text risks misleading semantics |
| F14 | Low | Dev copy (“spec 4.2”, enum codes in labels) | **Deferred** — label rewrites alter perceived semantics |
| F15 | Low | Org not auto-selected | **Fixed** |
| F16 | Low | No post-success actions (view/PDF/another) | **Fixed** — success action row |
| F17 | Low | Tests for manual path | **Expanded** — backend + frontend utils tests |

### Follow-ups from code review (N01–N05)

| ID | Status | Note |
|----|--------|------|
| N01 | Fixed | Summary uses `createFiscalBill.summary.*` keys |
| N02 | Fixed | Combobox `aria-autocomplete` / `aria-expanded` restored |
| N03 | Fixed | Result/price colors moved to CSS classes |
| N04 | Fixed | `handleBuyerIdChange` uses functional `setBuyerType` |
| N05 | Fixed | `matchPaymentToRemaining` preserves auto-sync for single payment |

## Backend changes (implemented)

**Files:** `FiscalBillService.java`, `FiscalBillRepository.java`, `FiscalBillServiceManualTest.java`

- **`validateManualPaymentTotals`** — sums items vs payments (±0.01), positive payment amounts, `400` on mismatch.
- **`resolveItemsForFiscalChain`** — manual items use `taxLabel`/`labels`; fallback to `resolveVatLabelsForOrderItems` only if all items have `taxValue` + `taxCategoryName`.
- **`applyManualOrderLinkedChecks`** — duplicate bill check scoped by `orgId` + order + invoice/transaction type (`409` if SUCCESS exists).
- **`applyManualReferentFields`** — lookup by `orgId` + `efiscal_sdc_invoiceno`; sets `referentDocumentDT`; `400` if not found or datetime missing.
- **Org-scoped repo methods:** `findByOrgIdAndOrderIdAndInvoiceTypeAndTransactionType`, `findLatestByOrgAndOrderAndType`, `findFirstByOrgIdAndEfiscalSdcInvoicenoOrderByCreatedDesc`.
- **Order flow** also updated to org-scoped duplicate/advance/referent lookups (`setReferentFields(body, orgId, ...)`).

**Intentional product decision (F04):** Manual `orderId` = **fiscal-chain linkage** (duplicate, advance-close, auto-referent). **Does not** fetch/validate MerchantPro orders. Full §4.1 → `POST /fiscalbill/from-order`.

## Frontend changes (implemented)

**Files:** `CreateFiscalBill.jsx`, `CreateFiscalBill.css`, `en.json`, `sr.json`, `FRONTEND_STRUCTURE.md`

Key symbols: `validateForm`, `scrollToFirstError`, `isFiscalResultSuccess` / `isFiscalResultFailed`, `calcTotalAmount`, `handleBuyerIdChange` (9→PIB/10, 13→JMBG/11).

Locale additions: `createFiscalBill.validation.*`, `summary.*`, `resultSuccessTitle`, `resultFailedTitle`, `submitDisabledBalance`.

## Docs updated

- `API_CONTRACT.md` — manual endpoint: payment validation, referent DT resolution, orderId chain semantics, `SUCCESS` status, `502` failed body.
- `FISCAL_BILL_MODULE_SPEC.md` — §4.2.1 orderId scope clarified; §4.2.3 server-side payment validation.

## Tests & verification

```bash
cd backend && mvn test -Dtest=FiscalBillServiceManualTest   # 10 tests pass
cd backend && mvn compile
cd frontend && npm run lint
cd frontend && npm run test
```

`FiscalBillServiceManualTest` covers: payment mismatch `400`, unknown referent `400`, referent missing datetime `400`, referent DT happy path, duplicate order `409`, empty items/payments `400`, mixed tax fields on order chain `400`, advance-close with label-only items (2 TA calls), send-email side effect. Frontend `createFiscalBillUtils.test.js` covers buyer inference, payment match, tax label normalization, and result helpers.

## Remaining work (recommended priority)

**Phase A — UX polish (medium/low):** F09 tax labels from API, F12 remove Copy from manual form, F11 email UI, F16 post-success links, F14 plain-language labels, F13 advance-close help.

**Phase B — cleanup:** F08 remove or wire `taxPrefix`; N04 buyerType functional setState.

**Phase C — hardening:** Expand backend tests (advance chain, referent DT happy path); frontend component tests; fix unrelated `AppShell.jsx` lint.

## Key code locations

```
frontend/src/pages/CreateFiscalBill.jsx     # manual form UI
frontend/src/pages/CreateFiscalBill.css       # split layout, result cards, invalid states
backend/.../FiscalBillService.java            # createManualFiscalBill, validators
backend/.../FiscalBillRepository.java         # org-scoped order/referent queries
backend/.../controller/FiscalBillController.java  # POST /manual
API_CONTRACT.md § POST /fiscalbill/manual
FISCAL_BILL_MODULE_SPEC.md §4.2
```

## API quick reference

`POST /api/v1/fiscalbill/manual?orgId=&clientId=` + `Idempotency-Key`

- Payments must equal items total (server + client).
- `referentDocumentNumber` → server resolves `referentDocumentDT` from local `fiscalbill` in same org.
- `orderId` optional → org-scoped chain rules only.
- Success: `201`, `status: "SUCCESS"`. TA failure: `502` with fiscal bill body `status: "FAILED"`.
