from fastapi import APIRouter, Depends, HTTPException, Query
from typing import Optional, Dict
from sqlalchemy.orm import Session
import json
import httpx
import logging
from datetime import datetime, timedelta
from app.api.auth import get_current_user
from app.models.user import User
from app.models.order_update_log import OrderUpdateLog
from app.services.orders import orders_service
from app.services.merchantpro import merchantpro_service
from app.schemas.orders import BulkUpdateStatusRequest, BulkUpdateStatusResponse, BulkUpdateTagsRequest, BulkUpdateTagsResponse, UpdateResult
from app.db.database import get_db

logger = logging.getLogger(__name__)

router = APIRouter()

MAX_SUPPLIER_DATE_RANGE_DAYS = 14


def _enforce_supplier_date(created_after: Optional[str]) -> str:
    """Clamp created_after to at most MAX_SUPPLIER_DATE_RANGE_DAYS ago."""
    floor = (datetime.now() - timedelta(days=MAX_SUPPLIER_DATE_RANGE_DAYS)).strftime("%Y-%m-%d")
    if not created_after or created_after < floor:
        return floor
    return created_after


@router.get("/", response_model=Dict)
async def get_orders(
    created_after: Optional[str] = Query(None, description="Filter by creation date (YYYY-MM-DD)"),
    shipping_status: Optional[str] = Query(None, description="Filter by shipping status"),
    payment_status: Optional[str] = Query(None, description="Filter by payment status"),
    payment_method_code: Optional[str] = Query(None, description="Filter by payment method code"),
    start: int = Query(0, ge=0, description="Pagination start"),
    limit: int = Query(100, ge=1, le=100, description="Records per page (max 100)"),
    sort: Optional[str] = Query(None, description="Sort parameter"),
    current_user: User = Depends(get_current_user)
):
    """
    Fetch sales orders from MerchantPro API (denormalized by line items).
    Dobavljac users are restricted to their own vendor and the last 2 weeks.
    """
    is_supplier = current_user.role == "dobavljac"

    if is_supplier:
        if not current_user.vendor_name:
            raise HTTPException(status_code=400, detail="Supplier user has no vendor name configured")
        created_after = _enforce_supplier_date(created_after)

    try:
        result = await orders_service.fetch_orders(
            created_after=created_after,
            shipping_status=shipping_status,
            payment_status=payment_status,
            payment_method_code=payment_method_code,
            start=start,
            limit=limit,
            sort=sort,
        )

        orders = result["orders"]

        if is_supplier:
            orders = [o for o in orders if o.get("vendor") == current_user.vendor_name]

        return {
            "data": orders,
            "meta": {
                "start": start,
                "limit": limit,
                "count": len(orders),
                "total": result.get("total", len(result["orders"])),
            },
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/bulk-update-status", response_model=BulkUpdateStatusResponse)
async def bulk_update_status(
    request: BulkUpdateStatusRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Bulk update product status for selected orders.
    """
    is_supplier = current_user.role == "dobavljac"

    if is_supplier and not current_user.vendor_name:
        raise HTTPException(status_code=400, detail="Supplier user has no vendor name configured")

    try:
        logger.info("=" * 80)
        logger.info("BULK UPDATE STATUS REQUEST")
        logger.info("=" * 80)
        logger.info(f"User: {current_user.username} (ID: {current_user.id}, role: {current_user.role})")
        logger.info(f"Status ID to set: {request.status_id}")
        logger.info(f"Selected IDs: {request.selected_ids}")
        logger.info(f"Filters: {request.filters}")
        logger.info("=" * 80)

        # Step 1: Refresh order data using same filters - fetch ALL pages
        filters = request.filters or {}

        if is_supplier:
            filters["created_after"] = _enforce_supplier_date(filters.get("created_after"))

        try:
            async def fetch_all_raw_orders(fetch_filters: Dict) -> list:
                all_orders_data = []
                start = 0
                limit = 100

                while True:
                    page_filters = {**fetch_filters, "start": start, "limit": limit}
                    refresh_result = await orders_service.fetch_orders(**page_filters)
                    page_orders = refresh_result.get("raw_orders", [])

                    if not page_orders:
                        break

                    all_orders_data.extend(page_orders)

                    # If we got fewer than limit, we've reached the end.
                    if len(page_orders) < limit:
                        break

                    start += limit

                return all_orders_data

            combined_combos = filters.get("combined_combos")
            base_filters = {
                k: v for k, v in filters.items()
                if k not in {"combined_combos", "start", "limit"}
            }

            if isinstance(combined_combos, list) and combined_combos:
                # In combined processing mode, refresh each combo separately
                # to match the frontend "Preuzmite za obradu" selection scope.
                merged_by_order_id = {}
                for combo in combined_combos:
                    combo_filters = dict(base_filters)
                    if isinstance(combo, dict):
                        if combo.get("payment_status"):
                            combo_filters["payment_status"] = combo.get("payment_status")
                        if combo.get("payment_method_code"):
                            combo_filters["payment_method_code"] = combo.get("payment_method_code")

                    combo_orders = await fetch_all_raw_orders(combo_filters)
                    for order in combo_orders:
                        oid = str(order.get("id"))
                        if oid and oid not in merged_by_order_id:
                            merged_by_order_id[oid] = order

                orders_data = list(merged_by_order_id.values())
            else:
                orders_data = await fetch_all_raw_orders(base_filters)
            logger.info(f"Refreshed {len(orders_data)} total orders for bulk update")
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to refresh order data: {str(e)}")

        # Step 2: Group selected items by order_id
        selected_items = {}  # {order_id: [line_item_ids]}
        for selected_id in request.selected_ids:
            parts = selected_id.split('-')
            if len(parts) >= 2:
                order_id = parts[0]
                item_id = parts[1]
                if order_id not in selected_items:
                    selected_items[order_id] = []
                selected_items[order_id].append(item_id)

        # Step 3: Update each order sequentially
        results = []
        successful_count = 0
        failed_count = 0

        for order_id, product_ids in selected_items.items():
            try:
                order = next((o for o in orders_data if str(o.get('id')) == order_id), None)
                if not order:
                    results.append(UpdateResult(order_id=order_id, success=False, error="Order not found in refresh"))
                    failed_count += 1
                    continue

                old_data = json.dumps(order, default=str)

                line_items = []
                updated_line_item_count = 0
                for item in order.get('line_items', []):
                    item_copy = item.copy()
                    line_item_id = item.get('id')
                    product_id = str(item.get('product_id'))
                    is_selected = str(line_item_id) in product_ids or product_id in product_ids

                    # Ensure line item has an id (use row_id if id is missing)
                    if not item_copy.get('id') and item_copy.get('row_id'):
                        item_copy['id'] = item_copy['row_id']
                    
                    # Remove row_id field as it's not needed in PATCH
                    item_copy.pop('row_id', None)

                    if is_selected:
                        # Supplier may only update items belonging to their vendor
                        if is_supplier:
                            meta = item.get("meta_fields") or {}
                            if meta.get("dobavljac") != current_user.vendor_name:
                                line_items.append(item_copy)
                                continue

                        item_copy['status'] = {'id': request.status_id}
                        updated_line_item_count += 1
                        logger.info(f"Order {order_id}: Updating line_item {line_item_id} to status {request.status_id}")
                    else:
                        # Ensure unchanged items keep their existing status
                        if not item_copy.get('status'):
                            # If no status exists, add a default or skip setting it
                            # MerchantPro might require status, so we preserve existing or omit
                            pass

                    line_items.append(item_copy)

                if updated_line_item_count == 0:
                    logger.warning(f"Order {order_id}: No line items were updated (selected IDs may not match)")
                    results.append(UpdateResult(order_id=order_id, success=False, error="No matching line items found"))
                    failed_count += 1
                    continue

                patch_data = {'line_items': line_items}
                new_data = json.dumps(patch_data, default=str)

                logger.info(f"Order {order_id}: Sending PATCH with {len(line_items)} line items ({updated_line_item_count} updated)")
                await merchantpro_service.patch_order(order_id, patch_data)
                response_code = 200

                log_entry = OrderUpdateLog(
                    user_id=current_user.id,
                    order_id=order_id,
                    old_data=old_data,
                    new_data=new_data,
                    response_code=response_code,
                )
                db.add(log_entry)
                db.commit()

                results.append(UpdateResult(order_id=order_id, success=True))
                successful_count += 1
                logger.info(f"Order {order_id}: Successfully updated")

            except httpx.HTTPStatusError as e:
                error_msg = f"HTTP {e.response.status_code}: {e.response.text}"
                logger.error(f"Order {order_id}: PATCH failed - {error_msg}")
                results.append(UpdateResult(order_id=order_id, success=False, error=error_msg))
                failed_count += 1

                log_entry = OrderUpdateLog(
                    user_id=current_user.id,
                    order_id=order_id,
                    old_data=old_data if 'old_data' in locals() else None,
                    new_data=new_data if 'new_data' in locals() else None,
                    response_code=e.response.status_code,
                )
                db.add(log_entry)
                db.commit()

            except Exception as e:
                error_msg = str(e)
                results.append(UpdateResult(order_id=order_id, success=False, error=error_msg))
                failed_count += 1

        return BulkUpdateStatusResponse(
            total_orders=len(selected_items),
            successful_updates=successful_count,
            failed_updates=failed_count,
            results=results,
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Bulk update failed: {str(e)}")


@router.post("/bulk-update-tags", response_model=BulkUpdateTagsResponse)
async def bulk_update_tags(
    request: BulkUpdateTagsRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Add tags to orders. Extracts distinct order IDs from selected_ids (row IDs)
    and calls MerchantPro tag endpoint for each order and each tag.
    """
    order_ids = {sid.split("-")[0] for sid in request.selected_ids if "-" in sid}

    results = []
    successful = 0
    failed = 0

    for order_id in order_ids:
        try:
            for tag_name in request.tags:
                await merchantpro_service.add_order_tag(order_id, tag_name)
            results.append(UpdateResult(order_id=order_id, success=True))
            successful += 1
        except Exception as e:
            results.append(UpdateResult(order_id=order_id, success=False, error=str(e)))
            failed += 1

    return BulkUpdateTagsResponse(
        total_orders=len(order_ids),
        successful_updates=successful,
        failed_updates=failed,
        results=results,
    )
