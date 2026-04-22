-- Migration: Create order_update_logs table
-- Description: Add table to log all order product status updates for audit trail
-- Date: 2026-02-08

CREATE TABLE IF NOT EXISTS order_update_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    order_id VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    old_data TEXT,
    new_data TEXT,
    response_code INTEGER
);

-- Create indexes for better query performance
CREATE INDEX idx_order_update_logs_order_id ON order_update_logs(order_id);
CREATE INDEX idx_order_update_logs_user_id ON order_update_logs(user_id);
CREATE INDEX idx_order_update_logs_updated_at ON order_update_logs(updated_at);

-- Add comment to table
COMMENT ON TABLE order_update_logs IS 'Audit log for order product status updates';
COMMENT ON COLUMN order_update_logs.old_data IS 'Complete JSON of order before update';
COMMENT ON COLUMN order_update_logs.new_data IS 'Complete JSON of PATCH request body sent to MerchantPro';
COMMENT ON COLUMN order_update_logs.response_code IS 'HTTP response code from MerchantPro API';
