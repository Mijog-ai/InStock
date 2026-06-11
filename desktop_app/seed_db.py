"""Seed Firestore with grid config, sample users, and bookings."""
import json
import os
import bcrypt
import db_repository as repo
from firestore_client import db


def seed_grid_config():
    """Upload grid_config.json to Firestore."""
    config_path = os.path.join(os.path.dirname(__file__), "..", "grid_config.json")
    with open(config_path, "r") as f:
        config = json.load(f)
    db.collection("app_config").document("grid_config").set(config)
    print("Seeded grid_config to Firestore.")


def seed():
    seed_grid_config()

    users = [
        ("admin", "admin123", "Admin"),
        ("logistics1", "log123", "Logistics"),
        ("logistics2", "log123", "Logistics"),
        ("consumer1", "con123", "Consumer"),
        ("consumer2", "con123", "Consumer"),
    ]
    for uname, pwd, role in users:
        if not repo.get_user(uname):
            pw_hash = bcrypt.hashpw(pwd.encode(), bcrypt.gensalt()).decode()
            repo.create_user(uname, pw_hash, role)
    print(f"Seeded {len(users)} users.")

    existing = repo.get_recent_movements(1)
    if not existing:
        samples = [
            ("G01-01-01", "RM-1001", "Raw", "B-2026-001", 250, "Steel bolts M8", "logistics1"),
            ("G01-02-03", "RM-1002", "Raw", "B-2026-002", 500, "Copper wire 2mm", "logistics1"),
            ("G02-01-01", "RM-1003", "Raw", "B-2026-003", 100, "Aluminum sheets", "logistics2"),
            ("G02-03-02", "RM-1004", "Raw", "B-2026-004", 75, "Rubber seals", "logistics1"),
            ("G03-01-04", "RM-1005", "Raw", "B-2026-005", 300, "Plastic granules", "logistics2"),
            ("G04-05-02", "RM-1006", "Raw", "B-2026-006", 180, "Nylon washers", "logistics1"),
            ("G05-10-03", "RM-1007", "Raw", "B-2026-007", 90, "Brass fittings", "logistics2"),
            ("P01-03-01", "RM-1008", "Raw", "B-2026-008", 400, "PVC tubing 10mm", "logistics1"),
            ("P01-08-04", "RM-1009", "Raw", "B-2026-009", 65, "Silicone gaskets", "logistics2"),
            ("K03-02-01", "RM-1010", "Raw", "B-2026-010", 220, "Carbon fiber strips", "logistics1"),
            ("K03-15-06", "RM-1011", "Raw", "B-2026-011", 150, "Stainless rods 6mm", "logistics2"),
            ("G01-05-02", "RM-1012", "Raw", "B-2026-012", 330, "Zinc coated bolts", "logistics1"),
            ("G03-20-01", "RM-1013", "Raw", "B-2026-013", 45, "Titanium plates", "logistics1"),
            ("G05-25-05", "RM-1014", "Raw", "B-2026-014", 200, "HDPE pellets", "logistics2"),
            ("P01-15-03", "RM-1015", "Raw", "B-2026-015", 110, "Ceramic inserts", "logistics1"),
        ]
        for loc, item, itype, batch, qty, desc, user in samples:
            repo.book_in(loc, item, itype, batch, qty, desc, user)
        print(f"Seeded {len(samples)} bookings.")
    else:
        print("Bookings already exist, skipping seed.")


if __name__ == "__main__":
    seed()
