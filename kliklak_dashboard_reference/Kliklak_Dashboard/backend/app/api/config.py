from fastapi import APIRouter, HTTPException
from typing import List
from app.core.config import settings
import json
import os

router = APIRouter()

@router.get("/product-statuses")
async def get_product_statuses():
    """Get available product status values from config (environment-specific)"""
    try:
        config_dir = os.path.join(os.path.dirname(__file__), "..", "config")
        
        # Try environment-specific file first
        env_file = f"product_statuses_{settings.ENVIRONMENT}.json"
        env_path = os.path.join(config_dir, env_file)
        
        if os.path.exists(env_path):
            config_path = env_path
        else:
            # Fallback to default
            config_path = os.path.join(config_dir, "product_statuses.json")
        
        with open(config_path, 'r') as f:
            statuses = json.load(f)
        return statuses
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to load product statuses: {str(e)}")
