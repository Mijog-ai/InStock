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
        "item_type": _strip(raw.get("Art", "")),
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


def get_recent_inputs(limit=20):
    with get_cursor() as cur:
        cur.execute(
            f"SELECT TOP {int(limit)} * FROM journal WHERE RTRIM(Bemerkung) LIKE 'Ein%' ORDER BY Rownumber DESC"
        )
        cols = _columns(cur)
        return [_row_to_movement(r, cols) for r in cur.fetchall()]


def get_recent_consumes(limit=20):
    with get_cursor() as cur:
        cur.execute(
            f"SELECT TOP {int(limit)} * FROM journal WHERE RTRIM(Bemerkung) LIKE 'Aus%' ORDER BY Rownumber DESC"
        )
        cols = _columns(cur)
        return [_row_to_movement(r, cols) for r in cur.fetchall()]


# ── Revert consume ──

def revert_consume(journal_id, user):
    with get_cursor() as cur:
        cur.execute("SELECT * FROM journal WHERE Rownumber = ?", int(journal_id))
        cols = _columns(cur)
        row = cur.fetchone()
        if row is None:
            return False
        m = _row_to_movement(row, cols)

        cur.execute(
            """INSERT INTO rohteillager
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?)""",
            m["item_number"][:20], m["description"][:40], m["location_code"][:20],
            str(m["qty"])[:10], m["item_type"][:20], m["batch_number"][:20], user[:20],
        )

        cur.execute("DELETE FROM journal WHERE Rownumber = ?", int(journal_id))
        return True


# ── FIFO lookup ──

def search_fifo(item_number):
    with get_cursor() as cur:
        cur.execute(
            """SELECT * FROM rohteillager
               WHERE RTRIM(Artikelnummer) = ? AND CAST(RTRIM(Menge) AS INT) > 0
               ORDER BY Datum ASC, Rownumber ASC""",
            item_number,
        )
        cols = _columns(cur)
        return [_row_to_booking(r, cols) for r in cur.fetchall()]


# ── Relocate ──

def search_stock_for_relocation(item_number):
    pattern = f"%{item_number}%"
    with get_cursor() as cur:
        cur.execute(
            """SELECT * FROM rohteillager
               WHERE CAST(RTRIM(Menge) AS INT) > 0
                 AND (RTRIM(Artikelnummer) LIKE ? OR RTRIM(Bezeichnung) LIKE ?)
               ORDER BY Datum ASC, Rownumber ASC""",
            pattern, pattern,
        )
        cols = _columns(cur)
        return [_row_to_booking(r, cols) for r in cur.fetchall()]


def get_location_suggestions(item_number, source_date):
    with get_cursor() as cur:
        cur.execute(
            """SELECT TOP 3 RTRIM(Lagerort) AS loc, Datum
               FROM rohteillager
               WHERE RTRIM(Artikelnummer) = ?
                 AND CAST(RTRIM(Menge) AS INT) > 0
               ORDER BY ABS(DATEDIFF(DAY, Datum, ?)) ASC""",
            item_number.strip(), source_date,
        )
        results = []
        for row in cur.fetchall():
            d = row[1]
            date_str = d.isoformat() if isinstance(d, (date, datetime)) else str(d or "")
            results.append({"location": row[0], "date": date_str})
        return results


def get_all_cell_slot_counts():
    with get_cursor() as cur:
        cur.execute(
            """SELECT RTRIM(Lagerort) AS loc, COUNT(*) AS cnt
               FROM rohteillager
               WHERE CAST(RTRIM(Menge) AS INT) > 0
               GROUP BY RTRIM(Lagerort)"""
        )
        return {row[0]: row[1] for row in cur.fetchall()}


def get_cell_slot_counts(shelf_code):
    with get_cursor() as cur:
        cur.execute(
            """SELECT RTRIM(Lagerort) AS loc, COUNT(*) AS cnt
               FROM rohteillager
               WHERE RTRIM(Lagerort) LIKE ? AND CAST(RTRIM(Menge) AS INT) > 0
               GROUP BY RTRIM(Lagerort)""",
            f"{shelf_code}-%",
        )
        return {row[0]: row[1] for row in cur.fetchall()}


def relocate(source_booking_id, dest_location_code, qty, user):
    with get_cursor() as cur:
        cur.execute("SELECT * FROM rohteillager WHERE Rownumber = ?", int(source_booking_id))
        cols = _columns(cur)
        row = cur.fetchone()
        if row is None:
            return {"ok": False, "error": "Source booking not found"}
        source = _row_to_booking(row, cols)
        actual_qty = min(qty, source["qty"])
        new_source_qty = source["qty"] - actual_qty

        if new_source_qty <= 0:
            cur.execute("DELETE FROM rohteillager WHERE Rownumber = ?", int(source_booking_id))
        else:
            cur.execute("UPDATE rohteillager SET Menge = ? WHERE Rownumber = ?",
                        str(new_source_qty), int(source_booking_id))

        cur.execute(
            """INSERT INTO rohteillager
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?)""",
            source["item_number"][:20], source["description"][:40], dest_location_code[:20],
            str(actual_qty)[:10], source["item_type"][:20], source["batch_number"][:20], user[:20],
        )
        cur.execute("SELECT SCOPE_IDENTITY()")
        new_id = cur.fetchone()[0]
        booking_id = str(int(new_id)) if new_id else "0"

        cur.execute(
            """INSERT INTO journal
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)""",
            source["item_number"][:20], source["description"][:40], source["location_code"][:20],
            str(actual_qty)[:10], source["item_type"][:20], source["batch_number"][:20], user[:20],
            f"Uml. aus -{actual_qty}"[:20],
        )

        cur.execute(
            """INSERT INTO journal
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)""",
            source["item_number"][:20], source["description"][:40], dest_location_code[:20],
            str(actual_qty)[:10], source["item_type"][:20], source["batch_number"][:20], user[:20],
            f"Uml. ein +{actual_qty}"[:20],
        )

        return {"ok": True, "booking_id": booking_id}


def get_recent_relocations(limit=20):
    with get_cursor() as cur:
        cur.execute(
            """SELECT j_in.Artikelnummer, j_in.Bezeichnung, j_in.Lagerort AS dest,
                      j_out.Lagerort AS src, j_in.Menge, j_in.Zusatz1, j_in.Datum,
                      r.Rownumber AS destRowNum
               FROM journal j_in
               INNER JOIN journal j_out
                   ON j_out.Artikelnummer = j_in.Artikelnummer
                   AND j_out.Menge = j_in.Menge
                   AND j_out.Datum = j_in.Datum
                   AND j_out.Zusatz1 = j_in.Zusatz1
                   AND RTRIM(j_out.Bemerkung) LIKE 'Uml. aus%'
                   AND RTRIM(j_in.Bemerkung) LIKE 'Uml. ein%'
               INNER JOIN rohteillager r
                   ON RTRIM(r.Artikelnummer) = RTRIM(j_in.Artikelnummer)
                   AND RTRIM(r.Lagerort) = RTRIM(j_in.Lagerort)
                   AND CAST(RTRIM(r.Menge) AS INT) > 0
               WHERE RTRIM(j_in.Bemerkung) LIKE 'Uml. ein%'
               ORDER BY j_in.Datum DESC"""
        )
        cols = _columns(cur)
        results = []
        seen = set()
        for row in cur.fetchall():
            raw = dict(zip(cols, row))
            dest_row = raw["destRowNum"]
            if dest_row in seen:
                continue
            seen.add(dest_row)
            results.append({
                "dest_booking_id": str(dest_row),
                "item_number": _strip(raw["Artikelnummer"]),
                "description": _strip(raw.get("Bezeichnung", "")),
                "from_location": _strip(raw["src"]),
                "to_location": _strip(raw["dest"]),
                "qty": int(_strip(raw.get("Menge", "0")) or 0),
                "moved_by": _strip(raw.get("Zusatz1", "")),
                "moved_at": raw["Datum"].isoformat() if isinstance(raw.get("Datum"), (date, datetime)) else str(raw.get("Datum", "")),
            })
            if len(results) >= limit:
                break
        return results


def revert_relocation(dest_booking_id, original_source_location, qty, user):
    with get_cursor() as cur:
        cur.execute("SELECT * FROM rohteillager WHERE Rownumber = ?", int(dest_booking_id))
        cols = _columns(cur)
        row = cur.fetchone()
        if row is None:
            return {"ok": False, "error": "Destination booking not found"}
        dest = _row_to_booking(row, cols)
        actual_qty = min(qty, dest["qty"])
        new_dest_qty = dest["qty"] - actual_qty

        if new_dest_qty <= 0:
            cur.execute("UPDATE rohteillager SET Lagerort = ? WHERE Rownumber = ?",
                        original_source_location[:20], int(dest_booking_id))
        else:
            cur.execute("UPDATE rohteillager SET Menge = ? WHERE Rownumber = ?",
                        str(new_dest_qty), int(dest_booking_id))
            cur.execute(
                """INSERT INTO rohteillager
                   (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1)
                   SELECT Artikelnummer, Bezeichnung, ?, ?, Datum, Art, Lieferschein, Zusatz1
                   FROM rohteillager WHERE Rownumber = ?""",
                original_source_location[:20], str(actual_qty)[:10], int(dest_booking_id),
            )

        cur.execute(
            """INSERT INTO journal
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)""",
            dest["item_number"][:20], dest["description"][:40], dest["location_code"][:20],
            str(actual_qty)[:10], dest["item_type"][:20], dest["batch_number"][:20], user[:20],
            f"Storno Uml. -{actual_qty}"[:20],
        )

        cur.execute(
            """INSERT INTO journal
               (Artikelnummer, Bezeichnung, Lagerort, Menge, Datum, Art, Lieferschein, Zusatz1, Bemerkung)
               VALUES (?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)""",
            dest["item_number"][:20], dest["description"][:40], original_source_location[:20],
            str(actual_qty)[:10], dest["item_type"][:20], dest["batch_number"][:20], user[:20],
            f"Storno Uml. +{actual_qty}"[:20],
        )

        return {"ok": True}


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
