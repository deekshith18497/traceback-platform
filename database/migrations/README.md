# TraceBack OTP database migration

Back up the database before running any migration.

## MySQL or MariaDB

Run:

`otp-auth-migration-mysql.sql`

## PostgreSQL

Run:

`otp-auth-migration-postgresql.sql`

Run only the script that matches the database selected by `DB_URL`.

The migration:
- Allows `users.email` to be NULL for mobile-only accounts.
- Creates the `otp_challenges` table.
- Adds an index for OTP lookup.

Do not delete the existing users, lost-item, found-item, inbox, match,
notification, or watchlist tables.

After the migration, verify that `otp_challenges` exists and that the
`users.email` column allows NULL.