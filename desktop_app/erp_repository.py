"""Fetch recent raw material deliveries from the ERP system."""
from datetime import datetime, date
from decimal import Decimal
from erp_connection import get_erp_connection, IS_SQLITE


def _serialize_row(row):
    """Convert non-JSON-serializable types from DB results."""
    out = {}
    for k, v in row.items():
        if isinstance(v, (datetime, date)):
            out[k] = v.isoformat()
        elif isinstance(v, Decimal):
            out[k] = float(v)
        else:
            out[k] = v
    return out


_SQL_SERVER_QUERY = """
SELECT dbo.CREDDLVJOUR.LASTCHANGED,
       dbo.CREDDLVJOUR.PURCHASENUMBER,
       dbo.CREDDLVTRANS.ITEMNUMBER,
       dbo.CREDDLVTRANS.ORDERED,
       dbo.CREDDLVJOUR.DELIVERYNOTE,
       dbo.CREDDLVJOUR.EXTERNALDELIVERYNOTE,
       dbo.CREDTABLE.NAME,
       dbo.CREDDLVTRANS.DELIVERYDATE
FROM dbo.CREDDLVJOUR
INNER JOIN dbo.CREDDLVTRANS
    ON dbo.CREDDLVJOUR.DELIVERYNOTE = dbo.CREDDLVTRANS.DELIVERYNOTE
INNER JOIN dbo.CREDTABLE
    ON LTRIM(RTRIM(dbo.CREDDLVJOUR.ORDERACCOUNT)) = dbo.CREDTABLE.ACCOUNTNUMBER
WHERE dbo.CREDDLVTRANS.DELIVERYDATE >= CURRENT_TIMESTAMP - 70
  AND (dbo.CREDTABLE.NAME LIKE '%Hengli%' OR dbo.CREDTABLE.NAME LIKE '%Wenling%')
ORDER BY dbo.CREDDLVJOUR.LASTCHANGED
"""

_SQLITE_QUERY = """
SELECT CREDDLVJOUR.LASTCHANGED,
       CREDDLVJOUR.PURCHASENUMBER,
       CREDDLVTRANS.ITEMNUMBER,
       CREDDLVTRANS.ORDERED,
       CREDDLVJOUR.DELIVERYNOTE,
       CREDDLVJOUR.EXTERNALDELIVERYNOTE,
       CREDTABLE.NAME,
       CREDDLVTRANS.DELIVERYDATE
FROM CREDDLVJOUR
INNER JOIN CREDDLVTRANS
    ON CREDDLVJOUR.DELIVERYNOTE = CREDDLVTRANS.DELIVERYNOTE
INNER JOIN CREDTABLE
    ON TRIM(CREDDLVJOUR.ORDERACCOUNT) = CREDTABLE.ACCOUNTNUMBER
WHERE CREDDLVTRANS.DELIVERYDATE >= date('now', '-70 days')
  AND (CREDTABLE.NAME LIKE '%Hengli%' OR CREDTABLE.NAME LIKE '%Wenling%')
ORDER BY CREDDLVJOUR.LASTCHANGED
"""


def get_recent_deliveries():
    """Return recent raw material deliveries from Hengli/Wenling suppliers."""
    try:
        conn = get_erp_connection()
        query = _SQLITE_QUERY if IS_SQLITE else _SQL_SERVER_QUERY
        cursor = conn.execute(query)

        if IS_SQLITE:
            rows = cursor.fetchall()
            results = [_serialize_row(dict(r)) for r in rows]
        else:
            columns = [desc[0] for desc in cursor.description]
            results = [_serialize_row(dict(zip(columns, row))) for row in cursor.fetchall()]

        conn.close()
        return results
    except Exception as e:
        print(f"[ERP] Query error: {e}")
        return []


