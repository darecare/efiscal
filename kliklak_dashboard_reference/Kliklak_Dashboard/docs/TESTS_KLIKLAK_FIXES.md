# Verification tests – Kliklak email fixes

Use this checklist after implementing the fixes from the Darko–Nikola email to confirm everything works.

**Prerequisites:** Logged in to the dashboard; MerchantPro API and backend are running; you have orders loaded (click "Preuzmite porudžbine" on Obrada porudžbina).

---

## Plan 1 – Filters (Serbian labels + Status plaćanja + Način plaćanja)

**Goal:** All filters in Obrada porudžbina use Serbian labels; Status plaćanja and Način plaćanja are available and work in combination with other filters.

1. **Status isporuke labels**
   - Open Obrada porudžbina and expand the "Status isporuke" dropdown.
   - **Expected:** Current enabled options are Serbian labels for `awaiting` and `confirmed` (e.g. "Na čekanju", "Potvrđeno"). No English labels.

2. **Status plaćanja and Način plaćanja filters exist**
   - In the same Filters card, confirm two new dropdowns: "Status plaćanja" and "Način plaćanja".
   - After loading orders, open each dropdown.
   - **Expected:** Options are populated from data (e.g. "Plaćanje na čekanju", "Plaćanje pouzećem", "Plaćanje preko računa" for status; method names for način plaćanja).

3. **Payment filters affect the table**
   - Select a value in "Status plaćanja" (e.g. "Plaćanje pouzećem"). Click "Preuzmite porudžbine" if needed.
   - **Expected:** Table shows only rows with that payment status. URL contains `payment_status=...`.
   - Set "Status plaćanja" back to "Svi". Repeat for "Način plaćanja"; table and URL update accordingly.

4. **Combined filters**
   - Set Status isporuke (e.g. "Na čekanju"), Status plaćanja, and Način plaćanja to non-empty values.
   - **Expected:** Table shows only rows matching all selected filters. URL includes all three params.

---

## Plan 2 – Export text file (Šifra Dobavljača, Grad, one order one block)

**Goal:** Generated .txt uses Šifra Dobavljača after product name (not SKU), shows Grad (city) for the customer line (not Okrug), and one order with multiple items from the same vendor appears as one block.

1. **Šifra Dobavljača instead of SKU**
   - Select one or more rows that have "Šifra dobavljača" (sifra_dobavljaca) filled in the table. Generate the commercialist file (Preuzmite fajlove).
   - Open the downloaded .txt.
   - **Expected:** After each product name, the value in parentheses is the supplier code (Šifra dobavljača / sifra_dobavljaca), not the product SKU. If meta_field is not set, no parentheses or empty.

2. **Grad instead of Okrug**
   - In the same file, check the "Ime kupca: ..." lines.
   - **Expected:** The second part (after the comma) is city (e.g. "Beograd", "Niš") from shipping_city/billing_city, not district/state (e.g. "Grad Beograd" / Okrug).

3. **One order = one block**
   - Select rows that belong to **one order** and **one vendor** but **multiple line items** (e.g. two products from the same supplier in the same order). Generate the file.
   - **Expected:** In the file, that order appears as a single block: a block number line (e.g. "1."), followed by all product lines (e.g. "2 kom Product A (sifra)\n1 kom Product B (sifra)\n"), followed by **one** "Ime kupca: name, Grad" line. It does **not** appear as sub-indexed lines (1.1., 1.2.) or with separate customer lines per item.

4. **Dobavljači sorted A–Z (grouped TXT file)**
   - Select rows across **multiple** suppliers and generate the commercialist file.
   - **Expected:** Supplier blocks starting with `Dobavljač: ...` are ordered alphabetically A–Z by supplier name.

5. **Single file with VP and standard sections per supplier**
   - Select rows across suppliers with mixed `Skladište` values and generate the file.
   - **Expected:** One `.txt` file per commercialist is generated (no `-VPLager` separate file).
   - For a supplier with only VP Lager rows: flat product list (`X kom Naziv (Šifra)`), no block numbers, no customer line.
   - For a supplier with only standard (non-VP) rows: numbered order blocks with `Ime kupca: ...` line.
   - For a supplier with **mixed** rows:
     - VP rows appear first under a `VP:` label (flat list).
     - A `--------------------------------` separator line follows.
     - Standard rows appear under an `Ostale:` label (numbered blocks with customer line, no blank line between `Ostale:` and first block).
   - All suppliers are sorted A–Z within the file.

---

## Plan 3 – Table columns (Ime kupca, Šifra dobavljača)

**Goal:** The orders table shows customer name and supplier code (Šifra dobavljača).

1. **Ime kupca column**
   - Load orders and look at the table header.
   - **Expected:** A column "Ime kupca" is present (e.g. after "Šifra porudžbine"). Each row shows the customer name (shipping_name).

2. **Šifra dobavljača column**
   - **Expected:** A column "Šifra dobavljača" is present. Each row shows the supplier code (sifra_dobavljaca from line item meta_fields). Empty if not set in MerchantPro.

3. **Skladište column**
   - Load orders and look at the table header.
   - **Expected:** A column "Skladište" is present next to "Dobavljač". Each row shows the warehouse value (or empty if missing).

4. **Negative quantities excluded**
   - Find an order that contains a line item with negative quantity (Kliklak example: order like 82998018).
   - **Expected:** Those negative-quantity rows do not appear in the table and cannot be selected for bulk actions or export.

---

## Plan 4 – Status update bug (order 51233021 / line_item_id)

**Goal:** Updating product status for a selected row succeeds; no "Order not found in refresh" or HTTP 4xx due to ID mismatch (frontend sends order_id-line_item_id, backend matches by line_item id).

1. **Status update succeeds**
   - Load orders. Select one or more rows (e.g. for an order that previously failed, such as 51233021 if available). Choose action "Izmenite statuse proizvoda u porudžbinama", pick a new status, click Primenite.
   - **Expected:** The operation completes successfully. The success summary modal shows "Uspešno ažurirano X od Y porudžbina" (or similar). No error alert about "Neuspešno ažuriranje statusa proizvoda".

2. **Backend / log check (optional)**
   - In the backend or OrderUpdateLog (or network tab), inspect the bulk-update request and response for that order.
   - **Expected:** The PATCH for the order includes the selected line item(s) with the new status. No "Order not found in refresh" for that order. Response is 200 (or MerchantPro success). If you previously had failures for order 51233021, the same flow should now succeed.

---

## Plan 5 – User roles (superuser, user, dobavljac)

**Goal:** Three roles work as designed: superuser can manage users and see all orders; regular user sees all orders but no Users tab; dobavljac sees only their vendor’s orders and has a 2-week date limit.

1. **Superuser**
   - Log in as a user with `role: "superuser"`.
   - **Expected:** Sidebar shows "Korisnici". Opening it shows the user list; can add user (with role and vendor_name for dobavljac), edit, and delete (not self). On Obrada porudžbina, all filters (Dobavljač, Komercijalista) and actions (Preuzmite fajlove) are available.

2. **Regular user**
   - Log in as `role: "user"`.
   - **Expected:** No "Korisnici" in sidebar. Direct navigation to `/users` shows a permission message. Orders page works like superuser (all filters and actions).

3. **Dobavljac**
   - Log in as `role: "dobavljac"` with `vendor_name` set to a value that exists in order line items (e.g. "Master Team").
   - **Expected:** No "Korisnici" in sidebar. On Obrada porudžbina: "Datum od" is pre-filled and limited to the last 2 weeks; no Dobavljač or Komercijalista filter; table shows only rows for that vendor; no "Preuzmite fajlove" in the action dropdown. Bulk status update only affects their vendor’s line items.

4. **Migration**
   - After deploying, run `docker-compose exec backend python scripts/migrate_users.py`.
   - **Expected:** Script adds `role` and `vendor_name` if missing and backfills existing superusers. Safe to run multiple times.

---

## Quick reference

| Plan | What to check |
|------|----------------|
| 1 | Serbian labels in Status isporuke; Status plaćanja + Način plaćanja filters; URL and table update |
| 2 | Export: Šifra Dobavljača (not SKU), Grad (not Okrug), one order one block per vendor |
| 3 | Table columns: Ime kupca, Šifra dobavljača |
| 4 | Status update works; no ID mismatch errors |
| 5 | User roles: superuser (Users tab, full orders), user (no Users), dobavljac (vendor-only orders, 2-week date) |
| 6 | **Preuzmite za obradu**: button visible for superuser/user, hidden for dobavljac; merges 3 payment combos (cash_delivery+awaiting, wire+paid, intesa+paid) into one table; badge shown and dismissible |
