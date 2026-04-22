-- Migration: Add role-based user management columns
-- Run with: psql -U kliklak -d kliklak_dashboard -f migrate_users.sql

-- Add role column (superuser | user | dobavljac)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR NOT NULL DEFAULT 'user';

-- Add vendor_name for dobavljac users
ALTER TABLE users ADD COLUMN IF NOT EXISTS vendor_name VARCHAR;

-- Backfill existing superusers
UPDATE users SET role = 'superuser' WHERE is_superuser = true AND role != 'superuser';
