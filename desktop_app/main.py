import os
import sys

# Add project root to path so shared package is importable
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from shared.env import load_env
load_env()

import webview
from desktop_app.api import WmsApi
from shared.seed_db import seed

if getattr(sys, 'frozen', False):
    TEMPLATES_DIR = os.path.join(sys._MEIPASS, "templates")
else:
    TEMPLATES_DIR = os.path.join(os.path.dirname(__file__), "templates")


def main():
    seed()

    api = WmsApi()
    index_path = os.path.join(TEMPLATES_DIR, "index.html")
    window = webview.create_window(
        "WMS Grid Locator",
        url=index_path,
        js_api=api,
        width=1280,
        height=820,
        min_size=(1024, 680),
        text_select=True,
    )
    webview.start(debug="--debug" in sys.argv)


if __name__ == "__main__":
    main()
