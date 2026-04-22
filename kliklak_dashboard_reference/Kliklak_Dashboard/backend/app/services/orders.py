import httpx
import base64
from typing import Dict, List, Optional
from app.core.config import settings

class OrdersService:
    def __init__(self):
        self.base_url = settings.MERCHANTPRO_API_URL.rstrip('/')
        self.username = settings.MERCHANTPRO_API_USERNAME
        self.password = settings.MERCHANTPRO_API_PASSWORD
        
        # Create Basic Auth token
        credentials = f"{self.username}:{self.password}"
        encoded = base64.b64encode(credentials.encode()).decode()
        self.auth_header = f"Basic {encoded}"
    
    async def fetch_orders(
        self,
        created_after: Optional[str] = None,
        shipping_status: Optional[str] = None,
        payment_status: Optional[str] = None,
        payment_method_code: Optional[str] = None,
        start: int = 0,
        limit: int = 100,
        sort: Optional[str] = None,
        **kwargs
    ) -> Dict:
        """
        Fetch orders from MerchantPro API and denormalize by line items.
        payment_status and payment_method_code are passed through to MerchantPro filter params.
        """
        params = {
            "start": start,
            "limit": min(limit, 100),
            "include": "line_items,tags"
        }
        
        if created_after:
            params["created_after"] = created_after
        if shipping_status:
            params["shipping_status"] = shipping_status
        if payment_status:
            params["payment_status"] = payment_status
        if payment_method_code:
            params["payment_method_code"] = payment_method_code
        if sort:
            params["sort"] = sort
        
        # Build full URL with /api/v2/orders endpoint
        url = f"{self.base_url}/api/v2/orders"
        
        async with httpx.AsyncClient(verify=False, timeout=30.0) as client:
            try:
                response = await client.get(
                    url,
                    params=params,
                    headers={"Authorization": self.auth_header}
                )
                response.raise_for_status()
                data = response.json()
                
                denormalized_rows = []
                
                # MerchantPro API returns 'data' key, not 'orders'
                orders_list = data.get("data", [])
                
                for order in orders_list:
                    # Tags: MerchantPro returns list of { "id": int, "name": str }
                    raw_tags = order.get("tags") or []
                    order_tags = [t if isinstance(t, dict) else {"id": t, "name": str(t)} for t in raw_tags]
                    order_base = {
                        "order_id": order.get("id"),
                        "payment_status": order.get("payment_status_text"),
                        "payment_method": order.get("payment_method_name"),
                        "shipping_status": order.get("shipping_status_text"),
                        "date_created": order.get("date_created"),
                        "shipping_name": order.get("shipping_name") or order.get("billing_name"),
                        "shipping_state": order.get("shipping_state") or order.get("billing_state"),
                        "shipping_city": order.get("shipping_city") or order.get("billing_city"),
                        "tags": order_tags,
                    }
                    
                    line_items = order.get("line_items", [])
                    
                    # If no line items, create one row for the order itself
                    if not line_items:
                        denormalized_rows.append(order_base)
                    else:
                        # Denormalize by line items
                        for line_item in line_items:
                            meta = line_item.get("meta_fields") or {}
                            product = line_item.get("product") or {}
                            product_meta = product.get("meta_fields") or {}
                            sifra_dobavljaca = product_meta.get("sifra_dobavljaca") or meta.get("sifra_dobavljaca")
                            warehouse = (
                                product_meta.get("skladiste")
                                or product_meta.get("skladište")
                                or product_meta.get("Skladište")
                                or meta.get("skladiste")
                                or meta.get("skladište")
                                or meta.get("Skladište")
                                or product_meta.get("warehouse")
                                or meta.get("warehouse")
                            )
                            row = {
                                **order_base,
                                "line_item_id": line_item.get("id"),
                                "product_id": line_item.get("product_id"),
                                "product_sku": line_item.get("product_sku"),
                                "product_name": line_item.get("product_name"),
                                "quantity": line_item.get("quantity"),
                                "price": line_item.get("unit_price_gross"),
                                "total": line_item.get("line_subtotal_gross"),
                                "vendor": meta.get("dobavljac"),
                                "warehouse": warehouse,
                                "commercialist": meta.get("komercijalista"),
                                "sifra_dobavljaca": sifra_dobavljaca,
                                "status_name": line_item.get("status", {}).get("name") if isinstance(line_item.get("status"), dict) else None,
                                "status_color": line_item.get("status", {}).get("color") if isinstance(line_item.get("status"), dict) else None,
                            }
                            denormalized_rows.append(row)
                
                # Get total count from response metadata
                # MerchantPro returns: meta.count.total
                total_count = data.get("meta", {}).get("count", {}).get("total", len(denormalized_rows))
                
                return {
                    "orders": denormalized_rows,
                    "total": total_count,
                    "raw_orders": orders_list  # Include raw orders for bulk update refresh
                }
                
            except httpx.HTTPStatusError as e:
                if e.response.status_code == 401:
                    raise Exception("Unauthorized: Invalid API credentials")
                elif e.response.status_code == 429:
                    raise Exception("Rate limit exceeded. Please try again later.")
                else:
                    raise Exception(f"HTTP error: {e.response.status_code}")
            except httpx.TimeoutException:
                raise Exception("Request timeout: MerchantPro API did not respond in time")
            except httpx.ConnectError as e:
                raise Exception(f"Connection error: {str(e)}")
            except Exception as e:
                raise Exception(f"Failed to fetch orders: {str(e)}")

orders_service = OrdersService()
