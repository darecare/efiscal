from pydantic_settings import BaseSettings
from typing import List
import secrets

class Settings(BaseSettings):
    PROJECT_NAME: str = "Kliklak Dashboard"
    DATABASE_URL: str = "postgresql://kliklak:kliklak@db:5432/kliklak_dashboard"
    # Generate a secure secret key with: python -c "import secrets; print(secrets.token_urlsafe(32))"
    SECRET_KEY: str = secrets.token_urlsafe(32)
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    ALLOWED_ORIGINS: str = "http://localhost:5173,http://localhost:3000"
    
    # MerchantPro API settings
    MERCHANTPRO_API_URL: str = ""
    MERCHANTPRO_API_USERNAME: str = ""
    MERCHANTPRO_API_PASSWORD: str = ""
    
    # Environment (qa, prod, dev)
    ENVIRONMENT: str = "dev"
    
    @property
    def allowed_origins_list(self) -> List[str]:
        return [origin.strip() for origin in self.ALLOWED_ORIGINS.split(",")]
    
    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()
