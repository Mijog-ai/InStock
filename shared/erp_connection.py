"""Database connection: SQL Server first, local SQLite fallback."""
import os

try:
    import pyodbc
    _HAS_PYODBC = True
except ImportError:
    pyodbc = None
    _HAS_PYODBC = False

CONN_STRING = (
    'DRIVER={SQL Server};'
    f'SERVER={os.environ.get("ERP_DB_SERVER", "DEBLNSVERP01")};'
    f'DATABASE={os.environ.get("ERP_DB_NAME", "XALinl")};'
    f'UID={os.environ.get("ERP_DB_USER", "XAL_ODBC")};'
    f'PWD={os.environ.get("ERP_DB_PASS", "XAL_ODBC")}'
)

_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SQLITE_DB_PATH = os.path.join(_PROJECT_ROOT, "database", "xal_local1.db")

FORCE_SQLITE = os.environ.get("PLANNER_FORCE_SQLITE") == "1"


def _can_connect() -> bool:
    if FORCE_SQLITE or not _HAS_PYODBC:
        return False
    try:
        pyodbc.connect(CONN_STRING, timeout=3).close()
        return True
    except Exception:
        return False


_server_ok = _can_connect()
IS_SQLITE = not _server_ok


if _server_ok:
    def get_erp_connection():
        return pyodbc.connect(CONN_STRING)
else:
    import sqlite3
    print(f"[ERP] SQL Server unavailable - using local SQLite: {SQLITE_DB_PATH}")

    def get_erp_connection():
        conn = sqlite3.connect(SQLITE_DB_PATH)
        conn.row_factory = sqlite3.Row
        return conn


__all__ = ["get_erp_connection", "IS_SQLITE", "SQLITE_DB_PATH", "CONN_STRING"]
