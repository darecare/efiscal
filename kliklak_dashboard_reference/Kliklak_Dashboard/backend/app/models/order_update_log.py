from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Index
from sqlalchemy.sql import func
from app.db.database import Base

class OrderUpdateLog(Base):
    __tablename__ = "order_update_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    order_id = Column(String(50), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
    old_data = Column(Text, nullable=True)
    new_data = Column(Text, nullable=True)
    response_code = Column(Integer, nullable=True)

    __table_args__ = (
        Index('idx_order_id', 'order_id'),
        Index('idx_user_id', 'user_id'),
        Index('idx_updated_at', 'updated_at'),
    )
