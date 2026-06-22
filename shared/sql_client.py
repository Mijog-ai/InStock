"""SQL Server connection factory for Rohteillager database."""
import os
import pyodbc

CONN_STRING = (
    'DRIVER={SQL Server};'
    f'SERVER={os.environ.get("WMS_DB_SERVER", "DEBLNSVCS01")};'
    f'DATABASE={os.environ.get("WMS_DB_NAME", "Rohteillager")};'
    f'UID={os.environ.get("WMS_DB_USER", "guehringCS01")};'
    f'PWD={os.environ.get("WMS_DB_PASS", "")}'
)


def get_connection():
    return pyodbc.connect(CONN_STRING)


class get_cursor:
    """Context manager that provides a cursor and auto-commits/closes."""

    def __init__(self):
        self.conn = None
        self.cursor = None

    def __enter__(self):
        self.conn = get_connection()
        self.cursor = self.conn.cursor()
        return self.cursor

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is None:
            self.conn.commit()
        self.cursor.close()
        self.conn.close()
        return False
