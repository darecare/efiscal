import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from sqlalchemy import create_engine, text
from app.core.config import settings

VALID_ROLES = ("superuser", "user", "dobavljac")

def migrate():
    print(f"Connecting to database at {settings.DATABASE_URL}")
    engine = create_engine(settings.DATABASE_URL)

    with engine.connect() as conn:
        result = conn.execute(text(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'users';"
        ))
        columns = [row[0] for row in result.fetchall()]

        if "role" not in columns:
            print("Adding 'role' column...")
            conn.execute(text("ALTER TABLE users ADD COLUMN role VARCHAR NOT NULL DEFAULT 'user';"))
        else:
            print("'role' column already exists.")

        if "vendor_name" not in columns:
            print("Adding 'vendor_name' column...")
            conn.execute(text("ALTER TABLE users ADD COLUMN vendor_name VARCHAR;"))
        else:
            print("'vendor_name' column already exists.")

        # Backfill: set role='superuser' for any existing rows where is_superuser=true
        updated = conn.execute(text(
            "UPDATE users SET role = 'superuser' WHERE is_superuser = true AND role != 'superuser';"
        ))
        print(f"Backfilled {updated.rowcount} superuser(s) with role='superuser'.")

        conn.commit()
        print("Migration complete.")

if __name__ == "__main__":
    migrate()
