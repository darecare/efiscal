from pydantic import BaseModel
from typing import List, Optional

class BulkUpdateStatusRequest(BaseModel):
    selected_ids: List[str]  # format: "order_id-product_id-index"
    status_id: int
    filters: Optional[dict] = None  # filters used in original fetch (for refresh)

class UpdateResult(BaseModel):
    order_id: str
    success: bool
    error: Optional[str] = None

class BulkUpdateStatusResponse(BaseModel):
    total_orders: int
    successful_updates: int
    failed_updates: int
    results: List[UpdateResult]


class BulkUpdateTagsRequest(BaseModel):
    selected_ids: List[str]
    tags: List[str]


class BulkUpdateTagsResponse(BaseModel):
    total_orders: int
    successful_updates: int
    failed_updates: int
    results: List[UpdateResult]
