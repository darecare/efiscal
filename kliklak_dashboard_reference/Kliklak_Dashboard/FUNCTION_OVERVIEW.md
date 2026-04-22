# Functions overview for Kliklak_Dashboard project

List of functions that need to be implemented:
- **Users login** – Users log in via the login page. There are **3 types of users**: **superuser** (admin), **user** (regular), and **dobavljac** (supplier). Only superusers see the "Korisnici" (Users) tab and can create/edit/delete users (including other superusers and dobavljac accounts with a vendor name).
- After user login, the dashboard opens with a left sidebar. Menu options:
    1. **Kontrolni panel** (Dashboard)
    2. **Obrada porudžbina** (Sales order product processing)
    3. **Nalog** (Account)
    4. **Korisnici** (Users) – visible only to superusers
- **Sales order product processing** – Two areas: filters and table. **Superuser and user** see all orders and all filters (including Dobavljač and Komercijalista). **Dobavljac** users see only orders for their vendor, must choose a start date (max 2 weeks back), and do not see vendor/commercialist filters or the "Preuzmite fajlove" action.
- Sales order product processing screen (continued) > screen will have 2 areas. First area for filters and second area with table to show fetched data.
    - **Bulk selection (implemented):** Checkboxes per row, "Select all on page", "Select all (all pages)". Base for actions.
    - **Grouped mode (implemented):** User can toggle grouped-by-order view. Selection is preserved when switching views; switching from grouped view back to product view expands selected orders to all item rows in those orders.
    - In filters area will be also action buttons
    - Filters are used to fetch data via API from online shop on Merhcantpro platform. API documentation is on this URL: https://docs.merchantpro.com/api/
    API endpoint that we will use is order endpoint: https://docs.merchantpro.com/api/endpoints/orders
    - Action button **Preuzmite porudžbine** ("Fetch Orders") > after filters (date, shipping status) are applied, app calls GET to fetch sales orders from shop.
    - Action button **Preuzmite za obradu** > one-click fetch of orders ready for processing: app sends three GET calls (awaiting+cash_delivery, paid+wire, paid+intesa, all with shipping_status=awaiting), merges results into one table. Optional date filter applies when set. Hidden for dobavljac users.
    - When bulk actions run while this combined view is active, refresh uses the same three combinations so update scope stays aligned with the displayed table.
    - Fetched data will be displayed in table area. Details will be explained in separate function spec document.
    - User selects rows and picks actions from the action bar (product view: status update / file download; grouped view: add tags / mark as collected). Detailed flow is in function specs.
    - Action dropdown is view-aware: product actions are shown in product view; order actions are shown in grouped view.
    - For selected orders and products, app will call API PATCH function, for each order, there will be one api call.
    - If response is ok, app will refresh table with orders and products.