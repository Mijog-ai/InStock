"""Flask REST API server for WMS Grid Locator — serves Android tablet over local network."""
import bcrypt
from flask import Flask, request, jsonify

import db_repository as repo
import config_loader as config
import erp_repository as erp

app = Flask(__name__)


@app.route('/api/login', methods=['POST'])
def login():
    data = request.json
    user = repo.get_user(data.get("username", ""))
    if user and bcrypt.checkpw(data.get("password", "").encode(), user["password_hash"].encode()):
        return jsonify({"ok": True, "username": user["username"], "role": user["role"]})
    return jsonify({"ok": False, "error": "Invalid credentials"})


@app.route('/api/config')
def get_config():
    return jsonify(config.load_config())


@app.route('/api/zones')
def get_zones_overview():
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
            "percent": pct,
        })
    return jsonify(result)


@app.route('/api/shelves/<zone_code>')
def get_shelves(zone_code):
    shelves = config.get_shelves(zone_code)
    return jsonify(sorted(shelves.keys()))


@app.route('/api/shelf-grid/<zone_code>/<shelf_code>')
def get_shelf_grid(zone_code, shelf_code):
    shelf_cfg = config.get_shelf_config(zone_code, shelf_code)
    occupied = repo.get_occupied_cells(shelf_code)
    return jsonify({
        "sections": shelf_cfg["sections"],
        "rows": shelf_cfg["rows"],
        "occupied": occupied,
    })


@app.route('/api/cell/<path:location_code>')
def get_cell_contents(location_code):
    return jsonify(repo.get_cell_contents(location_code))


@app.route('/api/book-in', methods=['POST'])
def book_in():
    data = request.json
    max_slots = config.get_max_slots()
    location_code = data["location_code"]
    current = repo.get_active_slot_count(location_code)
    if current >= max_slots:
        return jsonify({"ok": False, "error": f"Cell at max capacity ({max_slots} slots)"})
    booking_id = repo.book_in(
        location_code,
        data["item_number"],
        data.get("item_type", "Stück"),
        data["batch_number"],
        data["qty"],
        data.get("description", ""),
        data.get("user", ""),
    )
    return jsonify({"ok": True, "booking_id": booking_id})


@app.route('/api/consume', methods=['POST'])
def consume():
    data = request.json
    ok = repo.consume(data["booking_id"], data["qty"], data.get("user", ""))
    return jsonify({"ok": ok})


@app.route('/api/erp-deliveries')
def get_erp_deliveries():
    return jsonify(erp.get_recent_deliveries())


@app.route('/api/movements')
def get_movements():
    limit = request.args.get("limit", 20, type=int)
    return jsonify(repo.get_recent_movements(limit))


@app.route('/api/search')
def search():
    q = request.args.get("q", "")
    return jsonify(repo.search_bookings(q))


@app.route('/api/users')
def get_all_users():
    return jsonify(repo.get_all_users())


@app.route('/api/users', methods=['POST'])
def create_user():
    data = request.json
    if repo.get_user(data["username"]):
        return jsonify({"ok": False, "error": "Username already exists"})
    pw_hash = bcrypt.hashpw(data["password"].encode(), bcrypt.gensalt()).decode()
    repo.create_user(data["username"], pw_hash, data["role"])
    return jsonify({"ok": True})


@app.route('/api/users/<user_id>/role', methods=['PUT'])
def update_user_role(user_id):
    data = request.json
    repo.update_user(user_id, role=data["role"])
    return jsonify({"ok": True})


@app.route('/api/users/<user_id>', methods=['DELETE'])
def delete_user(user_id):
    repo.delete_user(user_id)
    return jsonify({"ok": True})


if __name__ == '__main__':
    repo.init_db()
    print("WMS API Server starting on http://0.0.0.0:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)
