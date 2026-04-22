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

### 2.2 Footer
- Keep footer in a separate reusable component file included on all main pages.
- Footer content is TBD and will be finalized after MVP operations review.

## 3. Core ERP UI Patterns

### 3.1 App Shell Pattern
- Standard shell for authenticated pages:
	- Header (top)
	- Sidebar (left, collapsible)
	- Main content area (right)
	- Optional footer (bottom)

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

## 4A. Access Management Pages
- Add dedicated Role Definition page:
	- Role list/create/update
	- Action assignment per role
	- Module/action grouping (for example MerchantPro, Fiscalization, Users)
- User management page must support role assignment and organization access assignment.
- Role Definition page and action assignment actions are restricted to authorized admin roles.

## 5. Governance
- Kliklak_Dashboard is a design and behavior reference, not a runtime dependency.
- Reimplement cleanly in eFiscal frontend.
- Do not copy code blocks directly; preserve UX intent and adapt to eFiscal domain.
