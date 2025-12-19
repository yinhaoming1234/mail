-- ===========================================
-- Migration: Add is_admin column to users table
-- Date: 2025-12-19
-- Purpose: Fix schema validation error for User entity
-- ===========================================

-- Add is_admin column to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT false;

-- Add comment to the column
COMMENT ON COLUMN users.is_admin IS '是否为管理员';

-- Update any existing admin users (if you know which ones should be admins)
-- Uncomment and modify as needed:
-- UPDATE users SET is_admin = true WHERE email = 'admin@localhost';

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'is_admin';

SELECT '迁移完成：is_admin 列已添加到 users 表' AS status;
