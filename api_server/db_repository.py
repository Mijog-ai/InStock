"""WMS data layer — SQL Server backend (Rohteillager database)."""
from datetime import date, datetime
from sql_client import get_cursor


def _strip(val):
    return val.strip() if isinstance(val, str) else val


def _row_to_booking(row, columns):
    raw = dict(zip(columns, row))
    return {
        "id": str(raw["Rownumber"]),
        "location_code": _strip(raw.get("Lagerort", "")),
        "item_number": _strip(raw.get("Artikelnummer", "")),
        "item_type": _strip(raw.get("Art", "")),
        "batch_number": _strip(raw.get("Lieferschein", "")),
        "qty": int(_strip(raw.get("Menge", "0")) or 0),
        "description": _strip(raw.get("Bezeichnung", "")),
        "booked_by": _strip(raw.get("Zusatz1", "")),
        "booked_at": raw["Datum"].isoformat() if isinstance(raw.get("Datum"), (date, datetime)) else str(raw.get("Datum", "")),
        "status": "Active",
        "batch_charge": _strip(raw.get("Chargennummer", "")),
    }


def _row_to_movement(row, columns):
    raw = dict(zip(columns, row))
    return {
        "id": str(raw["Rownumber"]),
        "action": _strip(raw.get("Bemerkung", "")),
        "location_code": _strip(raw.get("Lagerort", "")),
        "item_number": _strip(raw.get("Artikelnummer", "")),
        "batch_number": _strip(raw.get("Lieferschein", "")),
        "qty": int(_strip(raw.get("Menge", "0")) or 0),
        "user": _strip(raw.get("Zusatz1", "")),
        "timestamp": raw["Datum"].isoformat() if isinstance(raw.get("Datum"), (date, datetime)) else str(raw.get("Datum", "")),
        "description": _strip(raw.get("Bezeichnung", "")),
    }


def _columns(cursor):
    return [desc[0] for desc in cursor.description]


# ── Users ──

def init_db():
    with get_cursor() as cur:
        cur.execute("""
            IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'users')
            CREATE TABLE users (
                username NCHAR(20) NOT NULL PRIMARY KEY,
                password_hash NVARCHAR(200) NOT NULL,
                role NCHAR(20) NOT NULL
            )
        """)


def get_user(username):
    with get_cursor() as cur:
        cur.execute("SELECT username, password_hash, role FROM users WHERE RTRIM(username) = ?", username)
        row = cur.fetchone()
        if row is None:
            return None
        return {
            "id": _strip(row[0]),
            "username": _strip(row[0]),
            "password_hash": _strip(row[1]),
            "role": _strip(row[2]),
        }


def get_all_users():
    with get_cursor() as cur:
        cur.execute("SELECT username, role FROM users ORDER BY username")
        return [
            {"id": _strip(r[0]), "username": _strip(r[0]), "role": _strip(r[1])}
            for r in cur.fetchall()
        ]


def create_user(username, password_hash, role):
    with get_cursor() as cur:
        cur.execute("INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                     username, password_hash, role)


def update_user(user_id, username=None, password_hash=None, role=None):
    parts, params = [], []
    if username:
        parts.append("username = ?")
        params.append(username)
    if password_hash:
        parts.append("password_hash = ?")
        params.append(password_hash)
    if role:
        parts.append("role = ?")
        params.append(role)
    if not parts:
        return
    params.append(user_id)
    with get_cursor() as cur:
        cur.execute(f"UPDATE users SET {', '.join(parts)} WHERE RTRIM(username) = ?", *params)


def delete_user(user_id):
    with get_cursor() as cur:
        cur.execute("DELETE FROM users WHERE RTRIM(username) = ?", user_id)


# ── Inventory (rohteillager) ──

def get_cell_contents(location_code):
    with get_cursor() as cur:
        cur.execute(
            "SELECT * FROM rohteillager WHERE RTRIM(Lagerort) = ? AND CAST(RTRIM(Menge) AS INT) > 0",
            location_code,
        )
        cols = _columns(cur)
        return sorted(
            [_row_to_booking(r, cols) for r in cur.fetchall()],
            key=lambda x: x.get("booked_at", ""),
        )


def get_active_slot_count(location_code):
    with get_cursor() as cur:
        cur.execute(
            "SELECT COUNT(*) FROM rohteillager WHERE RTRIM(Lagerort) = ? AND CAST(RTRIM(Menge) AS INT) > 0",
            location_code,
        )
        return cur.fetchone()[0]


def book_in(location_code, item_number, item_type, batch_number, qty, description, user):
    with get_cursor() as cur:
        cur.execute(
            """INSERT INTO rohteillager
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?)""",
            item_number[:20], description[:40], location_code[:20], str(qty)[:10], item_type[:20], batch_number[:20], user[:20],
        )
        cur.execute("SELECT SCOPE_IDENTITY()")
        new_id = cur.fetchone()[0]
        booking_id = str(int(new_id)) if new_id else "0"

        cur.execute(
            """INSERT INTO journal
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)""",
            item_number[:20], description[:40], location_code[:20], str(qty)[:10], item_type[:20], batch_number[:20], user[:20],
            f"Ein {qty} Stk"[:20],
        )
        return booking_id


def consume(booking_id, qty, user):
    with get_cursor() as cur:
        cur.execute("SELECT * FROM rohteillager WHERE Rownumber = ?", int(booking_id))
        cols = _columns(cur)
        row = cur.fetchone()
        if row is None:
            return False
        booking = _row_to_booking(row, cols)
        current_qty = booking["qty"]
        new_qty = current_qty - qty
        consumed_qty = qty if new_qty > 0 else current_qty

        if new_qty <= 0:
            cur.execute("DELETE FROM rohteillager WHERE Rownumber = ?", int(booking_id))
        else:
            cur.execute(
                "UPDATE rohteillager SET Menge = ? WHERE Rownumber = ?",
                str(new_qty), int(booking_id),
            )

        cur.execute(
            """INSERT INTO journal
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)""",
            booking["item_number"][:20], booking["description"][:40], booking["location_code"][:20],
            str(consumed_qty)[:10], booking["item_type"][:20], booking["batch_number"][:20], user[:20],
            f"Aus -{consumed_qty} Stk"[:20],
        )
        return True


# ── Zone / shelf queries ──

def get_zone_occupancy(zone_code, shelf_codes):
    if not shelf_codes:
        return 0
    placeholders = " OR ".join(["RTRIM(Lagerort) LIKE ?"] * len(shelf_codes))
    params = [f"{sc}-%" for sc in shelf_codes]
    with get_cursor() as cur:
        cur.execute(
            f"""SELECT COUNT(DISTINCT RTRIM(Lagerort))
                FROM rohteillager
                WHERE CAST(RTRIM(Menge) AS INT) > 0 AND ({placeholders})""",
            *params,
        )
        return cur.fetchone()[0]


def get_occupied_cells(shelf_code):
    with get_cursor() as cur:
        cur.execute(
            """SELECT DISTINCT RTRIM(Lagerort)
               FROM rohteillager
               WHERE RTRIM(Lagerort) LIKE ? AND CAST(RTRIM(Menge) AS INT) > 0""",
            f"{shelf_code}-%",
        )
        return [row[0] for row in cur.fetchall()]


# ── Audit / journal ──

def get_recent_movements(limit=20):
    with get_cursor() as cur:
        cur.execute(f"SELECT TOP {int(limit)} * FROM journal ORDER BY Rownumber DESC")
        cols = _columns(cur)
        return [_row_to_movement(r, cols) for r in cur.fetchall()]


# ── Search ──

def search_bookings(query):
    pattern = f"%{query}%"
    with get_cursor() as cur:
        cur.execute(
            """SELECT * FROM rohteillager
               WHERE CAST(RTRIM(Menge) AS INT) > 0
                 AND (RTRIM(Artikelnummer) LIKE ?
                   OR RTRIM(Bezeichnung) LIKE ?
                   OR RTRIM(Lagerort) LIKE ?
                   OR RTRIM(Lieferschein) LIKE ?
                   OR RTRIM(Chargennummer) LIKE ?)
               ORDER BY Datum DESC""",
            pattern, pattern, pattern, pattern, pattern,
        )
        cols = _columns(cur)
        return [_row_to_booking(r, cols) for r in cur.fetchall()]
