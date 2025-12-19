# Database Migration Guide

## Problem
The application fails to start with the following error:
```
Schema-validation: missing column [is_admin] in table [users]
```

This occurs because the `users` table in the database is missing the `is_admin` column that's defined in the `User` entity.

## Solution Options

### Option 1: Run SQL Migration Script (Recommended)

Run the migration script to add the missing column:

1. **Using psql command line:**
   ```bash
   psql -h localhost -p 5432 -U postgres -d maildb -f migration_add_is_admin.sql
   ```

2. **Using pgAdmin or any PostgreSQL client:**
   - Open the `migration_add_is_admin.sql` file
   - Execute the SQL commands in the `maildb` database

3. **Manual SQL execution:**
   Connect to your PostgreSQL database and run:
   ```sql
   ALTER TABLE users 
   ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT false;
   
   COMMENT ON COLUMN users.is_admin IS '是否为管理员';
   ```

### Option 2: Temporary Auto-Update (Quick Fix)

Temporarily change Hibernate to auto-update mode:

1. Edit `src/main/resources/application.properties`
2. Change line 28 from:
   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```
   to:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Start the application - Hibernate will automatically add the missing column
4. After successful startup, **change it back to `validate`** for production safety

### Option 3: Re-run Database Initialization

If this is a development environment and you don't have important data:

1. Drop and recreate the database:
   ```sql
   DROP DATABASE IF EXISTS maildb;
   CREATE DATABASE maildb;
   ```

2. Run the complete initialization script:
   ```bash
   psql -h localhost -p 5432 -U postgres -d maildb -f C:\Users\xiaoh\StudioProjects\mail\init.sql
   ```

## Verification

After applying any of the solutions, verify the column exists:

```sql
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'is_admin';
```

Expected result:
```
 column_name | data_type | is_nullable | column_default 
-------------+-----------+-------------+----------------
 is_admin    | boolean   | NO          | false
```

## Post-Migration

After the column is added, restart the Spring Boot application. It should start successfully without schema validation errors.
