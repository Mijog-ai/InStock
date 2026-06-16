import os
import sys
import webview

from api import WmsApi
from seed_db import seed

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
