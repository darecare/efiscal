# FRONTEND STRUCTURE

## 1. General Rules
- Use a left sidebar as the primary navigation menu.
- Keep sidebar menu in a separate reusable component file included on all main pages.
- Sidebar must be collapsible (full mode and icon-only mode) to maximize workspace.
- Sidebar menu supports tree navigation (main items with subitems).

## 2. Shared Layout Components

### 2.1 Header
- Keep header in a separate reusable component file included on all main pages.
- Header contains global options and additional quick-access menu.
- Header menu is separate from sidebar navigation.
- Header quick-access menu includes a compact About/help control rendered as a small `i` icon button that opens a localized About modal.

### 2.2 Footer
- Keep footer in a separate reusable component file included on all main pages.
- Footer content is TBD and will be finalized after MVP operations review.

## 2A. Internationalization (i18n)

- **Stack:** `i18next`, `react-i18next`, `i18next-browser-languagedetector`
- **Supported UI languages:** `en` (fallback), `sr` — registered in [`frontend/src/i18n/index.js`](frontend/src/i18n/index.js)
- **Init:** import `./i18n` once in [`frontend/src/main.jsx`](frontend/src/main.jsx) before render
- **Catalog:** [`frontend/src/locales/<lng>.json`](frontend/src/locales/en.json) — nested keys by feature (`common`, `nav`, `auth`, page namespaces such as `orders`, `roles`, `fiscalBills`). Keep key sets identical across locale files and preserve `{{placeholder}}` names
- **Usage:** `useTranslation()` + `t('key')` in components; `i18next.t()` in non-React code (e.g. `AuthContext`). Use `count` for plurals and `{{var}}` for interpolation
- **Plurals:** English uses `*_one` / `*_other` suffix keys (for example `common.counts.orders_one`). Serbian adds `*_few` for Slavic plural rules (2–4); define all three suffixes in `sr.json` when adding new counted strings
- **Language switcher:** [`LanguageSwitcher`](frontend/src/components/AppShell.jsx) in the top bar — custom dropdown with `aria-expanded`, `role="listbox"`, and localized option labels (`common.languages.*`). Persists choice via detector `localStorage` key `efiscal_lang` (also honors browser language on first visit)
- **Organization switcher:** [`OrganizationSwitcher`](frontend/src/components/AppShell.jsx) in the top bar — global active organization for operational pages. Backed by [`OrgContext`](frontend/src/contexts/OrgContext.jsx) (`activeOrgId`, `activeOrg`, `orgs`, `loading`, `error`, `setActiveOrgId`, `refreshOrgs`). Loads via `orgsApi.myAccess()` for normal users or `orgsApi.list()` for SuperAdmin (role check via [`isSuperAdmin()`](frontend/src/utils/permissions.js), not inline role-name strings). Persists selection in `localStorage` keyed per user (`activeOrg:<userId>`); stale stored ids are cleared when no longer in the accessible list. Single-org users see a static label; multi-org users get a dropdown. On load failure, `error` is `'loadFailed'` and the switcher shows `orgSwitcher.loadFailed` (`.org-switcher__pill--error`) instead of the empty-org pill. Pages that need org scope read `useOrg()` instead of local `<select>` controls.
- **About modal:** Header help menu exposes an About modal with manufacturer, serial number, and software version pulled from backend app info endpoint.
- **Settings menu:** [`AppShell`](frontend/src/components/AppShell.jsx) groups organization-facing admin tools under a Settings menu, including API Configuration, Email Templates, and Payment Type Mapping.
- **RBAC labels:** Backend action codes stay stable API identifiers. Display names and tooltips come from locale maps keyed by `actionCode`: `roles.actionLabels`, `roles.permissionDescriptions`, and `roles.permissionModules` (see [`Roles.jsx`](frontend/src/pages/Roles.jsx)); use `defaultValue` from API metadata when a new action has no catalog entry yet
- **Lint:** `npm run lint` in `frontend/` runs ESLint with `eslint-plugin-i18next` (`jsx-text-only` mode; also checks `title`, `placeholder`, `aria-label`, `alt`). Fix hardcoded user-visible strings before merge
- **Serbian maintenance script:** [`frontend/scripts/build-sr-locale.mjs`](frontend/scripts/build-sr-locale.mjs) regenerates `sr.json` from `en.json` plus translation tables (run manually: `node frontend/scripts/build-sr-locale.mjs` after large English catalog changes; review output and hand-edit domain-specific strings as needed)
- **IDE:** [i18n Ally](https://marketplace.visualstudio.com/items?itemName=Lokalise.i18n-ally) paths configured in [`.vscode/settings.json`](.vscode/settings.json)
- **Do not translate:** `className`, `id`, API paths, enum/code `value` attributes, object keys, `console` output, server error pass-through, MerchantPro/Tax API payload field names
- **Accessibility:** Icon-only controls (for example modal `×` close) keep the glyph but require localized `aria-label` (typically `common.close`) in every supported language
- **Route guard feedback:** [`ActionProtectedRoute`](frontend/src/components/ActionProtectedRoute.jsx) shows `common.permissionDenied` once per denied navigation signature (avoids duplicate toasts on re-render) and uses `common.loading` for the loading state

## 3. Core ERP UI Patterns

### 3.1 App Shell Pattern
- Standard shell for authenticated pages:
	- Header (top) — includes global organization switcher and language switcher
	- Sidebar (left, collapsible)
	- Main content area (right)
	- Optional footer (bottom)

### 3.1A Active Organization Context Pattern
- [`OrgProvider`](frontend/src/contexts/OrgContext.jsx) wraps authenticated routes (`AuthProvider` → `OrgProvider` → `SyncProvider`).
- Operational pages consume `useOrg()` for `activeOrgId` / `activeOrg` instead of page-local org state.
- **Migrated pages:** Orders, Fiscal Bills, Get Status, Products, Create Fiscal Bill, Email Templates (list filter only).
- **Intentionally local scope:** ApiConfig (connection `orgId` on create/edit), PayTypeMap (client-scoped), Organizations/Users admin forms, Taxes import modal org picker, Email Templates add/edit modal org picker (admin may assign a different org than the list filter).
- **No-org UX:** When no org is selected, show a single top-of-page hint — `<p className="muted org-scope-hint">` with `orgSwitcher.selectPrompt`. Do not duplicate with a second empty-state message in the table/content area. Disable primary fetch/submit actions until an org is active.
- **Org change:** Migrated pages reset fetched data, pagination, and in-progress form state when `activeOrgId` changes (Create Fiscal Bill clears items, payments, validation, and result).
- **Validation copy:** Org-required messages on migrated pages must reference the header switcher (`orgSwitcher.selectPrompt`), not page-local org dropdown wording.
- **Tests:** [`OrgContext.test.js`](frontend/src/contexts/OrgContext.test.js) covers `storageKey`, `resolveInitialOrgId`, provider API selection, and load-failure state.

### 3.2 List Workspace Pattern
- For operational pages (Orders, Fiscal Bills, Products, Users):
	- Fetch Filters section
	- Actions Bar section
	- Table section
	- Pagination section

### 3.3 Two-Phase Filtering Pattern
- Phase 1: server-side fetch filters (date/status/org/platform) before loading data.
- Phase 2: client-side filters on fetched data for quick narrowing.

### 3.4 Summary-to-Detail Pattern
- Show grouped summary rows first (for example by order/document).
- Expand row or open detail pane to show line-level details.
- Use this pattern to reduce visual noise on large datasets.

### 3.5 Bulk Action Pattern
- User selects rows/items.
- User selects action from actions bar.
- Show preview/confirmation modal with affected-item count.
- Execute action and show success/error summary.

### 3.6 Status Chip Pattern
- Use consistent status chips (color + label) across all modules.

### 3.7 Products and Create Fiscal Bill (catalog integration)

**Route:** `/fiscal-bills/products` — guarded by `FISCAL_MANAGE_PRODUCTS` ([`Products.jsx`](frontend/src/pages/Products.jsx)).

- Organization selector + product table (CRUD, search, bulk actions). Uses global active org from shell (no page-level org dropdown).
- **Pull from Shop:** [`productsApi.syncStream`](frontend/src/services/api.js) calls `GET /api/v1/products/sync?mode=AUTO` using `fetch` + ReadableStream (Bearer token in header; not native `EventSource`). Handles `409` via `onConflict` (re-attaches to running job).
- **Rebuild from Shop:** secondary action calls `GET /api/v1/products/sync?mode=RESET_FULL` after confirmation; restores hidden shop-synced products and refreshes the full catalog.
- **Sync recovery:** [`SyncContext`](frontend/src/contexts/SyncContext.jsx) polls `GET /products/sync/status` every 2.5s while syncing; `checkSyncStatus` on org change restores in-progress UI after page refresh.
- **Cancel sync:** UI cancel calls `POST /products/sync/cancel` then clears local state.
- **Per-org sync:** user may sync different orgs concurrently; guard blocks duplicate start for the same org only.
- Progress UI: sync type label (`products.syncTypeFull` / `products.syncTypeIncremental` / `products.syncTypeResetFull`), `<progress>` bar with `products.syncStarting` / `products.syncingProgress`.
- Local delete copy: removing shop-synced products hides them from the catalog; use Rebuild from Shop to restore (`products.deleteConfirm`, `products.deleteSelectedConfirm`, `products.fullRefreshConfirm`).
- Last sync line: `products.lastSyncAt` when latest job is `DONE` and idle.

**Create Fiscal Bill** ([`CreateFiscalBill.jsx`](frontend/src/pages/CreateFiscalBill.jsx)) — guarded by `FISCAL_CREATE_BILL`.

- **Layout:** Uses a modern side-by-side split view (via [`CreateFiscalBill.css`](frontend/src/pages/CreateFiscalBill.css)) putting items on the left and a sticky payments/summary sidebar on the right, eliminating tab context switching.
- **Auto-Calculations:** `totalAmount` is automatically calculated from `quantity * unitPrice`. The first payment row automatically syncs to match the remaining items total unless split explicitly by the user.
- **Validation:** Structured inline field errors with scroll-to-first-error; submit disabled until payment balance is zero; status-aware success/failure result panel.
- **Buyer ID Inference:** Entering a 9-digit or 13-digit `buyerIdValue` automatically pre-selects the corresponding `buyerType` (PIB or JMBG) using functional state updates to avoid stale closure bugs.
- **Tax labels:** Item tax-label dropdown is populated from `GET /taxes` via `taxApi.list()`, showing `label - rate%` (e.g. `A - 10%`). Static fallback labels are used if the request fails.
- **Email:** Optional `sendEmail` checkbox and `customerEmail` field are sent on `POST /fiscalbill/manual` when enabled.
- **Post-success actions:** Success result card offers verification link (when present), PDF download, navigation to fiscal bills list, and a reset flow for creating another bill.
- **Payment match:** The `=` match-total control updates the payment amount without disabling single-payment auto-sync.
- Line item **Name** field is a combobox-style inline search (no separate search button or modal).
- Requires selected organization; debounced search after 2+ characters via `GET /products/search?q=…`.
- Dropdown lists name + SKU/EAN; selecting a row fills the line and triggers live price lookup (`GET /products/lookup`).
- Styles: `.product-name-combobox`, `.product-suggest-list` in [`styles.css`](frontend/src/styles.css).
- Related locale keys: `createFiscalBill.searchPlaceholder`, `searchMinChars`, `searchNoResults`, `priceVerifying`, `priceVerified`, `priceUnverified`.
- Same status must always keep same chip style.

### 3.7 Form Section Pattern
- Split longer forms into logical sections (General, Financial, Tax, Integration, Notes).
- Keep form actions visible (sticky or clear end-of-form action block).
- Include inline validation and clear error messages.

### 3.8 Role-Aware UI Pattern
- Keep same page structure for all users.
- Show/hide actions and sensitive fields based on role and permissions.
- Do not duplicate pages only for role differences.
- UI authorization must be action-based (module action codes), not only role name checks.
- UI must respect active client/organization scope and hide disabled actions outside user scope.

### 3.9 Data Table Usability Pattern
- Use sticky header for long tables.
- Use sticky horizontal scrollbar for wide ERP tables.
- Keep key columns visible when possible.

### 3.10 Saved Views Pattern (Post-MVP)
- Allow users to save common filter/sort/column combinations.
- Example: "Pending fiscalization today", "Failed submissions", "Refund queue".

### 3.11 Audit/Timeline Pattern
- Provide audit panel or modal for important entity changes.
- Include who changed what, when, and resulting status.

### 3.12 State Feedback Pattern
- Loading: skeletons or clear loading state.
- Empty state: explain why list is empty and what action to take.
- Error state: clear message with retry option and reference ID if available.

## 4. Current Reuse Scope From Kliklak_Dashboard
- Account page: reuse baseline layout and interaction style, then extend with eFiscal-specific fields.
- Users page: reuse baseline users-management page structure and interaction style.
- Orders page: partial reuse of structure only:
	- Fetch Filters section
	- Actions Bar section
	- Summary Table view (grouped by order, expandable details)
	- Issue Fiscal Bill modal includes a default-checked `Send email` checkbox for order-linked issuance.

## 4A. Access Management Pages
- Add dedicated Role Definition page:
	- Role list/create/update/delete.
	- Action assignment per role.
	- Module/action grouping (for example MerchantPro, Fiscalization, Users).
	- **Role Deletion Flow**:
		- Clicking Delete on a role always opens a confirmation modal. The modal includes an optional reassignment selector: a dropdown listing active roles in the same client scope or global roles that can receive the users.
		- If the role is in use and no reassignment role is selected, the backend returns `409 Conflict` and the error is shown inline in the modal.
		- When a reassignment role is selected, the request is `DELETE /roles/{roleId}?reassignToRoleId={newRoleId}`.
		- Built-in system roles (`SUPERADMIN`, `CLIENT_ADMIN`, `OPERATOR`) show an error and skip the modal entirely — deletion is blocked client-side before the request is made.
	- **Table row actions**: Edit/Delete buttons in list tables must live inside a `.table-row-actions` wrapper within the `<td>` (never apply flex directly on the cell). Destructive row actions use `.secondary-button.danger`; modal confirm buttons use `.primary-button.danger`.
- User management page must support User CRUD (list, create, read, update, delete), role assignment, and organization access assignment.
	- **Users list data loading**: Initial page load must not depend on `GET /orgs` (requires `ORGS_MANAGE`). Users with only `USERS_MANAGE` should still load the user list; organization name labels are optional and omitted when org listing is forbidden.
	- **Organization access picker in user form**: The organization multi-select is only rendered when the caller has `ORGS_MANAGE` or is SuperAdmin. For users without that permission the picker is hidden, but the form still carries the user's existing `orgIds` pre-loaded from the server and sends them unchanged on save — preserving existing org access without exposing the picker.
	- **User Deletion Flow**:
		- User list interface must provide a delete button for each user.
		- Clicking delete triggers a confirmation modal to perform a soft-delete (calling `DELETE /users/{userId}`).
		- **Self-deletion Protection**: The UI must disable or hide the delete action for the currently logged-in user to prevent accidental self-account lockout.
- Organizations page edit modal must provide four tabs for existing organizations:
	- **Main** tab: general organization data (`clientId`, `name`, `taxId`, status/currency/active flags).
	- **Payment Types** tab: allowed payment type mapping.
	- **Email Settings** tab: SMTP server, SMTP port, from email, username, password, and connection security (`STARTTLS` or `SSL/TLS`).
	- **Advertisement** tab: `advertisementEnabled` toggle (checkbox) and `advertisementHtml` textarea. When enabled and non-empty, the HTML is injected into the `{{ADVERTISEMENT_BLOCK}}` placeholder in generated PDF fiscal bills.
- Email Templates page should provide org-scoped Add/Edit/Delete forms with a raw HTML textarea for the body field. The body editor does not need WYSIWYG in the first version, but it must preserve HTML markup and placeholder tokens such as `{{ customername }}`.
- Email settings form behavior:
	- All email settings fields are optional.
	- SMTP port input is numeric and constrained to valid SMTP range (`1..65535`).
	- Connection security is selected from a fixed option list (`STARTTLS`, `SSL/TLS`).
	- Password input is masked and initialized empty on edit; backend does not return stored SMTP password.
- Role Definition page and action assignment actions are restricted to authorized admin roles.

## 5. Governance
- Kliklak_Dashboard is a design and behavior reference, not a runtime dependency.
- Reimplement cleanly in eFiscal frontend.
- Do not copy code blocks directly; preserve UX intent and adapt to eFiscal domain.
