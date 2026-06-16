import bcrypt
import db_repository as repo
import config_loader as config
import erp_repository as erp


class WmsApi:
    def __init__(self):
        self._username = None
        self._role = None

    def login(self, username, password):
        user = repo.get_user(username)
        if user and bcrypt.checkpw(password.encode(), user["password_hash"].encode()):
            self._username = username
            self._role = user["role"]
            return {"ok": True, "username": username, "role": user["role"]}
        return {"ok": False, "error": "Invalid credentials"}

    def logout(self):
        self._username = None
        self._role = None
        return {"ok": True}

    def get_session(self):
        if self._username:
            return {"loggedIn": True, "username": self._username, "role": self._role}
        return {"loggedIn": False}

    def get_config(self):
        return config.load_config()

    def get_zones_overview(self):
        zones = config.get_zones()
        result = []
        for code, z in zones.items():
            total = config.get_total_cells(code)
            shelf_codes = list(z["shelves"].keys())
            occupied = repo.get_zone_occupancy(code, shelf_codes)
            pct = round(occupied / total * 100) if total > 0 else 0
            result.append({
                "code": code,
                "display_name": z["display_name"],
                "occupied": occupied,
                "total": total,
                "percent": pct
            })
        return result

    def get_shelves(self, zone_code):
        shelves = config.get_shelves(zone_code)
        return list(shelves.keys())

    def get_shelf_grid(self, zone_code, shelf_code):
        shelf_cfg = config.get_shelf_config(zone_code, shelf_code)
        occupied = repo.get_occupied_cells(shelf_code)
        return {
            "sections": shelf_cfg["sections"],
            "rows": shelf_cfg["rows"],
            "occupied": occupied
        }

    def get_cell_contents(self, location_code):
        return repo.get_cell_contents(location_code)

    def book_in(self, location_code, item_number, item_type, batch_number, qty, description):
        max_slots = config.get_max_slots()
        current = repo.get_active_slot_count(location_code)
        if current >= max_slots:
            return {"ok": False, "error": f"Cell at max capacity ({max_slots} slots)"}
        bid = repo.book_in(location_code, item_number, item_type, batch_number, qty, description, self._username)
        return {"ok": True, "booking_id": bid}

    def consume(self, booking_id, qty):
        ok = repo.consume(booking_id, qty, self._username)
        return {"ok": ok}

    def revert_consume(self, journal_id):
        ok = repo.revert_consume(journal_id, self._username)
        return {"ok": ok}

    def get_recent_movements(self, limit=20):
        return repo.get_recent_movements(limit)

    def get_recent_inputs(self, limit=15):
        return repo.get_recent_inputs(limit)

    def get_recent_consumes(self, limit=15):
        return repo.get_recent_consumes(limit)

    def search(self, query):
        return repo.search_bookings(query)

    def search_fifo(self, item_number):
        return repo.search_fifo(item_number)

    def consume_fifo(self, plan):
        """plan is a list of {booking_id, qty} dicts."""
        for item in plan:
            repo.consume(item["booking_id"], item["qty"], self._username)
        return {"ok": True}

    def get_erp_deliveries(self):
        return erp.get_recent_deliveries()

    def get_all_users(self):
        return repo.get_all_users()

    def create_user(self, username, password, role):
        if repo.get_user(username):
            return {"ok": False, "error": "Username already exists"}
        pw_hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
        repo.create_user(username, pw_hash, role)
        return {"ok": True}

    def update_user_role(self, user_id, role):
        repo.update_user(user_id, role=role)
        return {"ok": True}

    def delete_user(self, user_id):
        repo.delete_user(user_id)
        return {"ok": True}

    def get_max_slots(self):
        return config.get_max_slots()
