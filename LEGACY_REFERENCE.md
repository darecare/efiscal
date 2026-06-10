# Legacy & Reference Policy

The former `legacy/` (iDempiere Java) and `kliklak_dashboard_reference/` (Python/React dashboard) trees have been **removed from the repo**. Behavioral knowledge they carried now lives in the documents below.

## Where to look instead

| Topic | Authoritative doc |
| --- | --- |
| Fiscal bill flows, invoice types, referent docs, payments | `FISCAL_BILL_MODULE_SPEC.md` |
| REST contracts and error model | `API_CONTRACT.md` |
| Frontend page structure and UX patterns | `FRONTEND_STRUCTURE.md`, `PRODUCT_REQUIREMENTS.md` (FR-007–FR-009) |
| System layout and MerchantPro fetch parameters | `ARCHITECTURE.md` |
| Database schema and `apiconn` / `apitemplate` | `DATA_MODEL.md` |
| Backend services and integration patterns | `BACKEND_STRUCTURE.md` |
| **MerchantPro order normalization** (discounts, wallet, shipping) | `docs/MERCHANTPRO_ORDER_NORMALIZATION.md` |
| Product sync and incremental pull | `docs/PRODUCT_SYNC_JOBS_AND_INCREMENTAL_HANDOFF.md` |

## Implementation policy (unchanged)

1. **Specs govern behavior** — `PRODUCT_REQUIREMENTS.md`, `FISCAL_BILL_MODULE_SPEC.md`, and `API_CONTRACT.md` are the source of truth.
2. **No iDempiere/OSGI patterns** — use Spring services, JPA entities, and explicit DTOs.
3. **No reference-tree copy-paste** — reimplement in the modern stack; cite a spec section or doc above in PRs, not a deleted file path.
4. **Idempotency and errors** — follow `API_CONTRACT.md`, not old process-runtime assumptions.

## Modern equivalents (historical mapping)

| Former pattern | Current target |
| --- | --- |
| iDempiere `SvrProcess` orchestration | Spring `@Service` use cases (`FiscalBillService`, etc.) |
| PO / `X_ELF_*` generated models | JPA entities under `com.efiscal.backend.model` |
| OSGI factories | Spring DI (`@Configuration`, constructor injection) |
| Duplicate check on order + invoice type | Idempotency keys + `findLatestByOrderAndType` (SUCCESS-only conflict) |
| Hardcoded MP payment codes | `paytype_map` per client |
| Kliklak Orders UI (filters, actions bar, summary table) | `frontend/src/pages/Orders.jsx` + `FRONTEND_STRUCTURE.md` §3–4 |
| Template-driven API operations | `apiconn` + `apitemplate` tables; `apitemplate_param` when extended |

## Outstanding behavior not yet in code

Captured in docs, not in removed reference trees:

- **Order normalization** — `docs/MERCHANTPRO_ORDER_NORMALIZATION.md` (not yet implemented in `MerchantProOrderService`)
- **Dynamic `apitemplate_param` mapping** — `DATA_MODEL.md` (design only)
- **Orders Actions Bar** — `FRONTEND_STRUCTURE.md` §3.2 / FR-009 (partial)

## PR traceability

When implementing behavior that originated from the old reference code:

- Cite the **living spec or doc** (e.g. `FISCAL_BILL_MODULE_SPEC.md` §4.1.3, or `MERCHANTPRO_ORDER_NORMALIZATION.md` §4).
- Note any intentional deviation from the archived behavior and why.
