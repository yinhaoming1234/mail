# ⚠️ IMPORTANT REMINDER ⚠️

## Temporary Configuration Change

The file `src/main/resources/application.properties` has been **temporarily** modified:

**Changed from:**
```properties
spring.jpa.hibernate.ddl-auto=validate
```

**Changed to:**
```properties
spring.jpa.hibernate.ddl-auto=update
```

## Why This Change?

This allows Hibernate to automatically add the missing `is_admin` column to the database when the application starts.

## What to Do Next

**After the application starts successfully:**

1. **Stop the application**
2. **Revert the configuration** back to:
   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```
3. Restart the application to verify everything works

## Why Revert?

Using `update` mode in production is **not recommended** because:
- Hibernate might make unintended schema changes
- Schema validation (`validate` mode) is safer for production environments
- Database migrations should be controlled and intentional

---

**Delete this file after reverting the configuration.**
