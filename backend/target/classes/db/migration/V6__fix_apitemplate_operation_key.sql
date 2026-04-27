-- Fix operationKey for MerchantPro orders fetch template to use standardised code format
UPDATE apitemplate
SET operation_key = 'FETCH_ORDERS'
WHERE operation_key = 'Get Orders';
