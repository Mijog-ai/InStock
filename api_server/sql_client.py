"""SQL Server connection factory for Rohteillager database."""
import pyodbc

CONN_STRING = (
    'DRIVER={SQL Server};'
    'SERVER=DEBLNSVCS01;'
    'DATABASE=Rohteillager;'
    'UID=guehringCS01;'
    'PWD=M+5WGWa..sD?gfC~RGHz'
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
