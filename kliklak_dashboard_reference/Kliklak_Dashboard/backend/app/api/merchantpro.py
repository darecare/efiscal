from fastapi import APIRouter, Depends, HTTPException, status
from typing import Dict, Any, Optional
from app.api.auth import get_current_user
from app.models.user import User
from app.services.merchantpro import merchantpro_service

router = APIRouter()

@router.get("/orders")
async def get_orders(
    order_status: Optional[str] = None,
    limit: Optional[int] = 50,
    current_user: User = Depends(get_current_user)
) -> Dict[str, Any]:
    """Get orders from MerchantPro"""
    try:
        params = {"limit": limit}
        if order_status:
            params["status"] = order_status
        orders = await merchantpro_service.get_orders(params)
        return orders
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to fetch orders: {str(e)}"
        )

@router.get("/orders/{order_id}")
async def get_order(
    order_id: str,
    current_user: User = Depends(get_current_user)
) -> Dict[str, Any]:
    """Get a specific order from MerchantPro"""
    try:
        order = await merchantpro_service.get_order(order_id)
        return order
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to fetch order: {str(e)}"
        )

@router.get("/products")
async def get_products(
    limit: Optional[int] = 50,
    current_user: User = Depends(get_current_user)
) -> Dict[str, Any]:
    """Get products from MerchantPro"""
    try:
        params = {"limit": limit}
        products = await merchantpro_service.get_products(params)
        return products
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to fetch products: {str(e)}"
        )

@router.put("/orders/{order_id}/status")
async def update_order_status(
    order_id: str,
    status_update: Dict[str, str],
    current_user: User = Depends(get_current_user)
) -> Dict[str, Any]:
    """Update order status in MerchantPro"""
    try:
        result = await merchantpro_service.update_order_status(
            order_id, 
            status_update.get("status")
        )
        return result
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to update order status: {str(e)}"
        )
