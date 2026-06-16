"""Seed SQL Server with users table and default users."""
import bcrypt
import db_repository as repo


def seed():
    repo.init_db()
    print("Ensured users table exists.")

    users = [
        ("admin", "admin123", "Admin"),
        ("logistics1", "log123", "Logistics"),
        ("logistics2", "log123", "Logistics"),
        ("consumer1", "con123", "Consumer"),
        ("consumer2", "con123", "Consumer"),
    ]
    created = 0
    for uname, pwd, role in users:
        if not repo.get_user(uname):
            pw_hash = bcrypt.hashpw(pwd.encode(), bcrypt.gensalt()).decode()
            repo.create_user(uname, pw_hash, role)
            created += 1
    print(f"Seeded users: {created} new, {len(users) - created} already existed.")


if __name__ == "__main__":
    seed()
