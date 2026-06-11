"""WMS data layer — Firestore backend."""
from datetime import datetime
from firestore_client import db
from google.cloud.firestore_v1 import FieldFilter


def init_db():
    pass


def get_user(username):
    doc = db.collection("users").document(username).get()
    if doc.exists:
        data = doc.to_dict()
        data["id"] = doc.id
        return data
    return None


def get_all_users():
    docs = db.collection("users").stream()
    result = []
    for doc in docs:
        d = doc.to_dict()
        result.append({"id": doc.id, "username": d.get("username", doc.id), "role": d.get("role", "")})
    return sorted(result, key=lambda u: u["username"])


def create_user(username, password_hash, role):
    db.collection("users").document(username).set({
        "username": username,
        "password_hash": password_hash,
        "role": role,
    })


def update_user(user_id, username=None, password_hash=None, role=None):
    updates = {}
    if username:
        updates["username"] = username
    if password_hash:
        updates["password_hash"] = password_hash
    if role:
        updates["role"] = role
    if updates:
        db.collection("users").document(user_id).update(updates)


def delete_user(user_id):
    db.collection("users").document(user_id).delete()


def get_cell_contents(location_code):
    docs = (
        db.collection("bookings")
        .where(filter=FieldFilter("location_code", "==", location_code))
        .where(filter=FieldFilter("status", "==", "Active"))
        .stream()
    )
    result = []
    for doc in docs:
        d = doc.to_dict()
        d["id"] = doc.id
        result.append(d)
    result.sort(key=lambda x: x.get("booked_at", ""))
    return result


def get_active_slot_count(location_code):
    docs = (
        db.collection("bookings")
        .where(filter=FieldFilter("location_code", "==", location_code))
        .where(filter=FieldFilter("status", "==", "Active"))
        .stream()
    )
    return sum(1 for _ in docs)


def book_in(location_code, item_number, item_type, batch_number, qty, description, user):
    now = datetime.now().isoformat(timespec="seconds")
    booking_ref = db.collection("bookings").document()
    booking_ref.set({
        "location_code": location_code,
        "item_number": item_number,
        "item_type": item_type,
        "batch_number": batch_number,
        "qty": qty,
        "description": description,
        "booked_by": user,
        "booked_at": now,
        "status": "Active",
    })
    db.collection("movements").add({
        "booking_id": booking_ref.id,
        "action": "BookIn",
        "qty": qty,
        "user": user,
        "timestamp": now,
    })
    return booking_ref.id


def consume(booking_id, qty, user):
    now = datetime.now().isoformat(timespec="seconds")
    ref = db.collection("bookings").document(booking_id)
    doc = ref.get()
    if not doc.exists:
        return False
    data = doc.to_dict()
    if data.get("status") != "Active":
        return False
    new_qty = data["qty"] - qty
    if new_qty <= 0:
        ref.update({"qty": 0, "status": "Consumed"})
        consumed_qty = data["qty"]
    else:
        ref.update({"qty": new_qty})
        consumed_qty = qty
    db.collection("movements").add({
        "booking_id": booking_id,
        "action": "Consume",
        "qty": consumed_qty,
        "user": user,
        "timestamp": now,
    })
    return True


def get_zone_occupancy(zone_code, shelf_codes):
    """Count occupied cells across all shelves in a zone. Uses single-field query + client filter."""
    all_active = (
        db.collection("bookings")
        .where(filter=FieldFilter("status", "==", "Active"))
        .stream()
    )
    prefixes = tuple(f"{sc}-" for sc in shelf_codes)
    occupied = set()
    for doc in all_active:
        loc = doc.to_dict().get("location_code", "")
        if loc.startswith(prefixes):
            occupied.add(loc)
    return len(occupied)


def get_occupied_cells(shelf_code):
    """Get occupied cells for a specific shelf. Uses single-field query + client filter."""
    all_active = (
        db.collection("bookings")
        .where(filter=FieldFilter("status", "==", "Active"))
        .stream()
    )
    prefix = f"{shelf_code}-"
    return list({doc.to_dict()["location_code"] for doc in all_active if doc.to_dict().get("location_code", "").startswith(prefix)})


def get_recent_movements(limit=20):
    docs = (
        db.collection("movements")
        .order_by("timestamp", direction="DESCENDING")
        .limit(limit)
        .stream()
    )
    results = []
    for doc in docs:
        m = doc.to_dict()
        m["id"] = doc.id
        booking_doc = db.collection("bookings").document(m["booking_id"]).get()
        if booking_doc.exists:
            b = booking_doc.to_dict()
            m["location_code"] = b.get("location_code", "")
            m["item_number"] = b.get("item_number", "")
            m["batch_number"] = b.get("batch_number", "")
        results.append(m)
    return results


def search_bookings(query):
    q_lower = query.lower()
    docs = (
        db.collection("bookings")
        .where(filter=FieldFilter("status", "==", "Active"))
        .stream()
    )
    results = []
    for doc in docs:
        d = doc.to_dict()
        d["id"] = doc.id
        searchable = f"{d.get('location_code','')} {d.get('batch_number','')} {d.get('item_number','')} {d.get('description','')}".lower()
        if q_lower in searchable:
            for k, v in d.items():
                if hasattr(v, "isoformat"):
                    d[k] = v.isoformat()
            results.append(d)
    return sorted(results, key=lambda x: x.get("booked_at", ""), reverse=True)
