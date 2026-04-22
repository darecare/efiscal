# Kliklak Client Requests – Completion Status

Summary of requests from the email thread (Darko ↔ Milan Stanković / Kliklak, March 2026) and their implementation status.  
Status is based on a review of the current codebase.

**Legend:**

| Emoji | Status |
|-------|--------|
| 🔴 | Not implemented |
| 🟡 | Partially done |
| 🟢 | Done |

---

## 1. Column changes (orders table)

### 1.1 Remove column: **ID proizvoda** (Product ID)

**Request:** Remove the "ID proizvoda" column from the order processing table.

**Status:** 🟢 **Done**

**Evidence:** Current table headers in `frontend/src/pages/Orders.jsx` no longer include "ID proizvoda".

---

### 1.2 Remove column: **SKU proizvoda** (Product SKU)

**Request:** Remove the "SKU proizvoda" column from the order processing table.

**Status:** 🟢 **Done**

**Evidence:** Current table headers in `frontend/src/pages/Orders.jsx` no longer include "SKU proizvoda".

---

### 1.3 Add column: **Količina** (Quantity) before **Naziv proizvoda**

**Request:** Add a column "Količina" that shows the ordered quantity of the article, placed **before** "Naziv proizvoda" (Product name).

**Status:** 🟢 **Done**

**Evidence:** Product view table contains "Količina" before "Naziv proizvoda" in `frontend/src/pages/Orders.jsx`.

---

## 3. Tags (in same window as order processing)

**Request (from Milan):**
- Tags in the **existing** order processing screen (no extra tab).
- Flow: select orders → choose something like "Dodajte nove tagove" → popup to enter tag (similar to Merchant).
- In the same dropdown, add action **"Označi porudžbine kao preuzete"** that assigns the tag **"Preuzeto"** to selected orders.

**Status:** 🟢 **Done**

**Evidence:**
- Grouped-view action dropdown includes "Dodajte nove tagove" and "Označi porudžbine kao preuzete".
- UI includes tag modal flow in `frontend/src/pages/Orders.jsx`.
- Backend endpoint exists: `POST /api/orders/bulk-update-tags` in `backend/app/api/orders.py`.

---

## Summary table

| # | Request | Status |
|---|---------|--------|
| 1.1 | Remove column: ID proizvoda | 🟢 Done |
| 1.2 | Remove column: SKU proizvoda | 🟢 Done |
| 1.3 | Add column: Količina before Naziv proizvoda | 🟢 Done |
| 4 | Sort Dobavljač sections A–Z in TXT export | 🟢 Done |
| 4a | Single file with VP (flat) and standard (blocked) sections per supplier | 🟢 Done |
| 5 | Exclude negative quantity rows from processing table | 🟢 Done |
| 6 | Add field: Skladište (warehouse) in product table | 🟢 Done |
| 3 | Tags in same window + "Preuzeto" action | 🟢 Done |

---

*Last updated from current codebase review.*
