# MerchantPro Order Normalization

This document captures the **order-to-fiscal line normalization rules** previously implemented in the removed iDempiere `MPGetOrders` process. It preserves non-obvious MerchantPro edge-case behavior that is not spelled out elsewhere.

**Audience:** Backend developers implementing order-based fiscalization (`FiscalBillService.createFiscalBillFromOrder`) or a future `sales_orders` import pipeline.

**Related specs:**
- `FISCAL_BILL_MODULE_SPEC.md` §4.1 — fiscal request construction from orders
- `DATA_MODEL.md` §2.8 — `sales_orders` (planned, not yet implemented)
- `API_CONTRACT.md` — `GET /api/v1/merchantpro/orders`

---

## 1. Problem statement

MerchantPro order JSON is **not** a ready-made fiscal payload. Raw `line_items` can include:

- Separate rows for discounts/coupons (`item_type` ≠ product)
- Wallet credits applied at order level (`wallet_amount`)
- Shipping charged separately from product lines (`shipping_amount`, `shipping_tax_amount`)

If eFiscal maps `line_items` directly to Tax Authority `items[]` (as `MerchantProOrderService.mapOrder()` and `Orders.jsx` do today), fiscal line totals can **diverge** from the order's payable amount (`total_amount` / `grand_total`) and from the single payment row built from `paytype_map`.

**Goal of normalization:** Produce a list of **fiscal-eligible lines** whose summed `quantity × unitPrice` (per tax rules) reconciles with what the customer actually paid, after discounts and wallet credits, and includes shipping when applicable.

---

## 2. Current eFiscal state (gap)

| Layer | File | Behavior today |
|-------|------|----------------|
| Fetch | `MerchantProOrderService.fetchOrders()` | `GET` with `include=line_items`; maps each raw line to `OrderLineView` |
| Display | `frontend/src/pages/Orders.jsx` | Shows nested lines from fetch; no filtering of discount rows |
| Fiscalize | `Orders.jsx` → `fiscalBillApi.createFromOrder()` | Builds `items[]` from raw lines: `unitPrice × quantity`; **no** discount/wallet/shipping normalization |

**Not implemented:** Any of the algorithms in §4–§6.

**Recommended target:** A dedicated `MerchantProOrderNormalizationService` (or equivalent) called:
1. After fetch (optional persist to `sales_orders`), and/or
2. Immediately before `resolveVatLabelsForOrderItems()` in `FiscalBillService`.

---

## 3. MerchantPro order fields used

These fields appear in legacy import logic and are relevant for normalization. Names follow MerchantPro Orders API (`include=line_items`).

### 3.1 Order header

| Field | Type | Usage |
|-------|------|-------|
| `id` | string/int | External order ID |
| `date_created` | ISO datetime | Display / filtering |
| `payment_method_code` | string | Maps to fiscal payment via `paytype_map` (separate concern) |
| `payment_method_name` | string | Display only |
| `shipping_status` | string | Fetch filter |
| `shipping_amount` | decimal | Freight total (gross) |
| `shipping_tax_amount` | decimal | Tax portion of shipping |
| `shipping_method_name` | string | Label for synthetic shipping line |
| `wallet_amount` | decimal | Store-credit applied to order (typically negative or positive; legacy uses `.abs()`) |
| `total_amount` / `total` | decimal | Order total — use for **reconciliation check** after normalization |
| `billing_name`, `billing_type`, `billing_company_vat` | string | Buyer / fiscal customer (handled separately in `FiscalBillService`) |
| `line_items` | array | See §3.2 |

### 3.2 Line item (`line_items[]`)

| Field | Type | Usage |
|-------|------|-------|
| `product_id` | int/string | Product identifier; used to match `applied_discounts[].product_id` |
| `product_name` / `name` | string | Fiscal line `name` |
| `quantity` | decimal | Fiscal line `quantity` |
| `unit_price_gross` | decimal | Unit price when prices are tax-included |
| `unit_price_net` | decimal | Unit price when prices are tax-excluded |
| `product_tax_percent` | int | Tax rate (e.g. 20, 10, 0) |
| `product_tax_name` | string | Tax category name — required by eFiscal `resolveVatLabelsForOrderItems()` |
| `product_sku` / `sku` | string | Optional GTIN/SKU lookup |
| `item_type` | string | **Critical:** distinguishes product lines from discount lines (§4) |
| `applied_discounts` | array | On discount-type lines only (§4) |
| `status` | object | Legacy ERP metadata; **not** needed for fiscalization |

### 3.3 Discount line `applied_discounts[]` entry

| Field | Type | Usage |
|-------|------|-------|
| `product_id` | int | Target product receiving the discount |
| `amount` | decimal | Discount amount (net basis before gross-up; see §4.2) |

---

## 4. Discount line handling

**Source:** `MPGetOrders.getDiscountLines()`, `isDiscountLine()`, `getDiscountAmount()`, `addOrderLines()`

### 4.1 Discount `item_type` values

These line types are **not** fiscal products. They must be **excluded** from the output line list and processed only for allocation:

```
promo_cart
promo_product
discount
coupon
```

**Rule:** When iterating `line_items`, skip any row where `item_type` is in the list above.

**Additional guard (from Kliklak production):** Skip product rows where `quantity < 0` (refund/adjustment lines that should not be fiscalized from the processing table).

### 4.2 Allocating discounts to product lines

Legacy used a process parameter `IncludeDiscount` (default `false`). For fiscalization, normalization should **always** apply discounts when discount lines are present.

**Algorithm:**

```
INPUT:  order_lines[], target_product_id (MP product id as string), is_tax_included
OUTPUT: total_discount_for_product (BigDecimal)

1. discount_lines = all lines where item_type ∈ DISCOUNT_TYPES
2. total = 0
3. FOR EACH discount_line IN discount_lines:
     line_amount = 0
     FOR EACH entry IN discount_line.applied_discounts:
       IF str(entry.product_id) == target_product_id:
         line_amount += entry.amount
     IF is_tax_included:
       tax_pct = discount_line.product_tax_percent OR 20  // default 20 if missing
       line_amount = round(line_amount × (1 + tax_pct/100), 2, HALF_UP)
     total += line_amount
4. RETURN total
```

**Apply to unit price** (per product line):

```
allocated_discount = getDiscountAmount(discount_lines, product_id, is_tax_included)
unit_price = base_unit_price - (allocated_discount / quantity)
```

Where `base_unit_price` is:
- `unit_price_gross` if `is_tax_included == true`
- `unit_price_net` if `is_tax_included == false`

**Matching key:** Compare `str(applied_discount.product_id)` to the **MerchantPro `product_id`** on the product line (legacy matched via iDempiere `M_Product.value`, which was set to MP product id).

### 4.3 Tax-included vs tax-excluded

Legacy read `MPriceList.isTaxIncluded()` from the ERP price list. eFiscal has no price list entity; choose one of:

| Option | Recommendation |
|--------|----------------|
| Org-level config flag (`prices_include_tax`) | Preferred — explicit per shop |
| Infer from MerchantPro order/line fields | Fallback if API exposes it |
| Default `true` for Serbian B2C shops | Document assumption; validate against sample orders |

Gross-up in §4.2 only applies when `is_tax_included == true`.

---

## 5. Shipping as a synthetic line

**Source:** `MPGetOrders.addShipmentLine()`

Shipping is **not** always represented as a normal `line_items` product row. When `shipping_amount > 0`, add a **synthetic fiscal line**:

```
INPUT:  order (shipping_amount, shipping_tax_amount, shipping_method_name), is_tax_included
OUTPUT: optional shipping line

IF shipping_amount <= 0: RETURN null

shipping_tax = shipping_tax_amount OR 0
shipping_net   = shipping_amount - shipping_tax

IF is_tax_included:
  unit_price = shipping_amount          // gross
ELSE:
  unit_price = shipping_net

tax_percent = (shipping_tax > 0) ? 20 : 0

RETURN {
  name:       shipping_method_name OR "Shipping",
  quantity:   1,
  unit_price: unit_price,
  total:      unit_price,
  tax_percent: tax_percent,
  tax_name:   map from tax_percent,   // must resolve via org tax table
  is_shipping: true
}
```

**Notes:**
- Legacy also set order-header `freightAmt`; for fiscalization the synthetic line is what matters for `items[]`.
- Legacy resolved a catalog product for shipping by `shipping_method_name`; eFiscal can use the method name directly as the fiscal line `name` without a local product row.
- Shipping line is **excluded** from wallet distribution (§6).

---

## 6. Wallet credit distribution

**Source:** `MPGetOrders.applyWalletAmountToOrderLines()`

When `wallet_amount` is present and non-zero, reduce product line prices proportionally. Legacy applied this **after** product lines and shipping line were created.

```
INPUT:  wallet_amount, normalized_lines[] (including shipping flag per line)
OUTPUT: lines with reduced unit prices

IF wallet_amount IS NULL OR wallet_amount == 0: RETURN unchanged

remaining = abs(wallet_amount)

// Step 1: compute total value of eligible lines (exclude shipping)
total_value = SUM(line.unit_price × line.quantity FOR lines WHERE NOT line.is_shipping)

// Step 2: distribute across lines in order
FOR EACH line IN lines (in original order):
  IF line.is_shipping: CONTINUE

  line_value = line.unit_price × line.quantity

  IF remaining <= 0:
    discount = 0
  ELSE IF line_value >= remaining:
    discount = remaining
    remaining = 0
  ELSE:
    // Legacy rule: leave minimum 1.00 per unit on the line
    discount = line_value - (line.quantity × 1.00)
    IF discount <= 0: CONTINUE
    remaining -= line_value

  unit_discount = round(discount / line.quantity, 2, HALF_UP)
  line.unit_price = max(line.unit_price - unit_discount, 0)

RETURN lines
```

**Business note:** The `line_value - (qty × 1)` branch is legacy behavior (comment: *"minus 1 per each qty"*). Confirm with stakeholders whether eFiscal should preserve this minimum-unit-price rule or use full proportional absorption. Document the choice in implementation PR.

**Sign:** Legacy uses `wallet_amount.abs()`. Confirm whether MerchantPro sends wallet as negative; normalize with `abs()` before distribution.

---

## 7. Normalization pipeline (order of operations)

Apply steps in this sequence:

```
1. Parse raw MerchantPro order JSON
2. Determine is_tax_included (org config or default)
3. Collect discount_lines from line_items (§4)
4. FOR EACH product line in line_items:
     a. SKIP if item_type ∈ DISCOUNT_TYPES
     b. SKIP if quantity < 0
     c. Compute base unit price (gross vs net)
     d. Subtract allocated discount per §4.2
     e. Emit NormalizedLine(product fields, tax_percent, tax_name)
5. Append shipping synthetic line if applicable (§5)
6. Apply wallet distribution (§6)
7. Reconciliation check (§8)
8. Return NormalizedOrder { lines[], payment_method_code, totals, ... }
```

---

## 8. Reconciliation and validation

After normalization, validate before calling Tax Authority:

| Check | Rule | On failure |
|-------|------|------------|
| Line sum vs order total | `abs(sum(line.quantity × line.unit_price) - order.total_amount)` ≤ tolerance | Warn or block; tolerance TBD (e.g. 0.01 RSD) |
| Tax fields present | Every line has `product_tax_percent` and `product_tax_name` | `400` — already enforced in `resolveVatLabelsForOrderItems()` |
| Empty line list | At least one line after filtering | `400` |
| Payment amount | Single payment from `paytype_map` should match order total | Align with `FISCAL_BILL_MODULE_SPEC.md` §4.1.6 |
| Discount rows not duplicated | Discount `item_type` rows never appear in output | Assert in tests |

**Legacy did not** perform an explicit sum check; eFiscal should add one because fiscal compliance depends on it.

---

## 9. Mapping normalized lines to fiscal request

Normalized output should map directly to `FiscalBillItemRequest`:

| Normalized field | `FiscalBillItemRequest` field |
|------------------|-------------------------------|
| name | `name` |
| quantity | `quantity` |
| unit_price | `unitPrice` |
| quantity × unit_price | `totalAmount` |
| product_tax_name | `taxCategoryName` |
| product_tax_percent | `taxValue` |
| sku | `sku` |
| product_id | `productId` |

Tax label resolution stays in `FiscalBillService.resolveVatLabelsForOrderItems()` (maps `product_tax_name` + `product_tax_percent` → org `tax` table → eFiscal label).

**Advance invoices (type 4):** Normalization still runs on full product lines first; `buildAdvanceLineItems()` groups by resolved tax label afterward (already implemented in eFiscal).

---

## 10. Payment method codes (reference only)

Normalization does **not** map payment types. That is handled by `paytype_map` at fiscalization time.

Legacy hardcoded mapping in `MPGetOrders.addOrder()` (replaced in eFiscal by configurable `paytype_map`):

| `payment_method_code` | Legacy ERP `PaymentRule` | eFiscal default (`FISCAL_BILL_MODULE_SPEC` §4.1.6) |
|-----------------------|--------------------------|-----------------------------------------------------|
| `cash_delivery` | B (Cash) | 1 (Cash) |
| `wire` | S (Wire) | 4 (Wire Transfer) |
| `intesa` | C (Card) | 2 (Card) |
| `raiffeisen_credit` | S | 2 (Card) |
| *(default)* | B | 1 |

---

## 11. Out of scope for this document

The following `MPGetOrders` behaviors are **ERP import concerns**, not fiscal line normalization. eFiscal does not need them for Tax Authority requests:

- Creating/updating iDempiere `C_Order`, `C_BPartner`, `MUser`
- Skipping orders that already exist (`documentno` duplicate check)
- Auto-fetching missing products via `MPGetProducts` subprocess
- `shipping_substatus_id` fetch filter (optional fetch param; not normalization)
- Line-item `status` / `PO_Status` metadata

---

## 12. Suggested implementation checklist

- [ ] Add `MerchantProOrderNormalizationService` with unit tests using fixture JSON (orders with coupon, wallet, shipping)
- [ ] Add org-level `prices_include_tax` (or equivalent) to org/shop config
- [ ] Call normalization from `createFiscalBillFromOrder` path (server-side; do not trust client-sent items for order-based fiscalization)
- [ ] Optionally persist normalized result to `sales_orders` + line table when that schema is implemented
- [ ] Add reconciliation warning in UI when line sum ≠ order total
- [ ] Confirm wallet minimum-unit-price rule with business stakeholders (§6)
- [ ] Filter `quantity < 0` lines in normalization and UI

---

## 13. Test fixtures to create

| Scenario | Key JSON traits | Expected outcome |
|----------|-----------------|------------------|
| Plain order | Product lines only, no wallet/shipping | Lines match raw products |
| Cart coupon | `item_type: promo_cart` + `applied_discounts` | Discount allocated; coupon row absent from output |
| Wallet only | `wallet_amount` set | Unit prices reduced; shipping unchanged |
| Shipping only | `shipping_amount > 0` | Extra synthetic line with correct tax |
| Combined | Discount + wallet + shipping | All three rules applied in pipeline order |
| Negative qty line | `quantity: -1` on product row | Row excluded |
| Tax-included gross-up | Discount on tax-included shop | Discount gross-up applied per §4.2 |

---

## 14. Source provenance

| Rule | Legacy method | Lines (approx.) |
|------|---------------|-----------------|
| Discount types | `getDiscountLines`, `isDiscountLine` | 474–489 |
| Discount allocation | `getDiscountAmount` | 491–514 |
| Product line pricing | `addOrderLines` | 408–467 |
| Shipping line | `addShipmentLine` | 369–406 |
| Wallet distribution | `applyWalletAmountToOrderLines` | 516–557 |
| Tax-included flag | `addOrder` (price list) | 335–336, 438–441 |

**Original source:** iDempiere `MPGetOrders.java` (removed from repo).  
**Status:** Behavioral reference captured here; implement in eFiscal or defer explicitly.

---

*Last updated: 2026-06-10*
