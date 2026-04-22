import httpx
import base64
import asyncio
import time
import json
import logging
from typing import Dict, List
from app.core.config import settings

logger = logging.getLogger(__name__)

class MerchantProService:
    def __init__(self):
        self.base_url = settings.MERCHANTPRO_API_URL.rstrip('/')
        self.username = settings.MERCHANTPRO_API_USERNAME
        self.password = settings.MERCHANTPRO_API_PASSWORD
        
        # Create Basic Auth credentials
        credentials = f"{self.username}:{self.password}"
        encoded = base64.b64encode(credentials.encode()).decode()
        self.auth_header = f"Basic {encoded}"
        
        # Rate limiting: 3 calls/sec, 90 calls/min
        self.last_call_time = 0
        self.call_count = 0
        self.minute_start_time = time.time()
    
    async def _rate_limit(self):
        """Enforce rate limits: 3 calls/sec, 90 calls/min"""
        current_time = time.time()
        
        # Reset minute counter if a minute has passed
        if current_time - self.minute_start_time >= 60:
            self.call_count = 0
            self.minute_start_time = current_time
        
        # Check minute limit
        if self.call_count >= 90:
            wait_time = 60 - (current_time - self.minute_start_time)
            if wait_time > 0:
                await asyncio.sleep(wait_time)
                self.call_count = 0
                self.minute_start_time = time.time()
        
        # Check per-second limit (3 calls/sec = ~0.334 seconds between calls)
        time_since_last_call = current_time - self.last_call_time
        if time_since_last_call < 0.334:
            await asyncio.sleep(0.334 - time_since_last_call)
        
        self.last_call_time = time.time()
        self.call_count += 1
    
    async def get_orders(self, params: Dict = None) -> Dict:
        """Fetch orders from MerchantPro"""
        await self._rate_limit()
        url = f"{self.base_url}/api/v2/orders"
        async with httpx.AsyncClient(verify=False) as client:
            response = await client.get(
                url,
                params=params,
                headers={"Authorization": self.auth_header},
                timeout=30.0
            )
            response.raise_for_status()
            return response.json()
    
    async def get_order(self, order_id: str) -> Dict:
        """Fetch a specific order from MerchantPro"""
        await self._rate_limit()
        async with httpx.AsyncClient(verify=False) as client:
            response = await client.get(
                f"{self.base_url}/api/v2/orders/{order_id}",
                headers={"Authorization": self.auth_header},
                timeout=30.0
            )
            response.raise_for_status()
            return response.json()
    
    async def patch_order(self, order_id: str, data: Dict) -> Dict:
        """Update order in MerchantPro using PATCH"""
        await self._rate_limit()
        
        # Log the PATCH request details
        logger.info(f"=" * 80)
        logger.info(f"PATCH REQUEST TO MERCHANTPRO API")
        logger.info(f"=" * 80)
        logger.info(f"Order ID: {order_id}")
        logger.info(f"URL: {self.base_url}/api/v2/orders/{order_id}")
        logger.info(f"Request Body (formatted):")
        logger.info(json.dumps(data, indent=2))
        logger.info(f"=" * 80)
        
        async with httpx.AsyncClient(verify=False) as client:
            response = await client.patch(
                f"{self.base_url}/api/v2/orders/{order_id}",
                headers={
                    "Authorization": self.auth_header,
                    "Content-Type": "application/json"
                },
                json=data,
                timeout=30.0
            )
            
            # Log the response
            logger.info(f"PATCH RESPONSE:")
            logger.info(f"Status Code: {response.status_code}")
            logger.info(f"Response Body (first 500 chars): {response.text[:500]}")
            logger.info(f"=" * 80)
            
            response.raise_for_status()
            return response.json()

    async def add_order_tag(self, order_id: str, tag_name: str) -> Dict:
        """Add a tag to an order via dedicated tag endpoint (POST /api/v2/orders/{id}/tags).
        Uses the full order_id in the URL (e.g. 95750870.3); the API returns 404 when using
        only the base part. Tries POST first; on 405 Method Not Allowed, retries with PATCH."""
        await self._rate_limit()
        url = f"{self.base_url}/api/v2/orders/{order_id}/tags"
        headers = {
            "Authorization": self.auth_header,
            "Content-Type": "application/json",
        }
        payload = {"name": tag_name}
        async with httpx.AsyncClient(verify=False) as client:
            response = await client.post(url, headers=headers, json=payload, timeout=30.0)
            if response.status_code == 405:
                response = await client.patch(url, headers=headers, json=payload, timeout=30.0)
            if response.status_code == 400:
                logger.warning(
                    "MerchantPro add_order_tag 400: order_id=%s url=%s body=%s",
                    order_id,
                    url,
                    response.text[:500] if response.text else None,
                )
            response.raise_for_status()
            return response.json()
    
    async def get_products(self, params: Dict = None) -> Dict:
        """Fetch products from MerchantPro"""
        await self._rate_limit()
        url = f"{self.base_url}/api/v2/products"
        async with httpx.AsyncClient(verify=False) as client:
            response = await client.get(
                url,
                params=params,
                headers={"Authorization": self.auth_header},
                timeout=30.0
            )
            response.raise_for_status()
            return response.json()
    
    async def update_order_status(self, order_id: str, status: str) -> Dict:
        """Update order status in MerchantPro"""
        await self._rate_limit()
        async with httpx.AsyncClient(verify=False) as client:
            response = await client.put(
                f"{self.base_url}/orders/{order_id}",
                headers=self.auth_header,
                json={"status": status},
                timeout=30.0
            )
            response.raise_for_status()
            return response.json()

merchantpro_service = MerchantProService()
