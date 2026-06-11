"""Grid config loader — reads from Firestore grid_config collection."""
from firestore_client import db

_config = None


def load_config():
    global _config
    if _config is None:
        doc = db.collection("app_config").document("grid_config").get()
        if doc.exists:
            _config = doc.to_dict()
        else:
            _config = {"max_slots_per_cell": 2, "zones": {}}
    return _config


def reload_config():
    global _config
    _config = None
    return load_config()


def get_zones():
    return load_config()["zones"]


def get_shelves(zone_code):
    return get_zones()[zone_code]["shelves"]


def get_shelf_config(zone_code, shelf_code):
    return get_shelves(zone_code)[shelf_code]


def get_max_slots():
    return load_config().get("max_slots_per_cell", 2)


def get_total_cells(zone_code):
    total = 0
    for shelf in get_shelves(zone_code).values():
        total += shelf["sections"] * shelf["rows"]
    return total
