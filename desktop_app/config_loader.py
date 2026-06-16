"""Grid config loader — reads from local grid_config.json file."""
import json
import os

import sys

_config = None
if getattr(sys, 'frozen', False):
    _CONFIG_PATH = os.path.join(sys._MEIPASS, "grid_config.json")
else:
    _CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "grid_config.json")


def load_config():
    global _config
    if _config is None:
        with open(_CONFIG_PATH, "r") as f:
            _config = json.load(f)
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
