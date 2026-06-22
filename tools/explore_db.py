import pyodbc
import pandas as pd


def ConnString():
    return (
        f'DRIVER={{SQL Server}};SERVER=DEBLNSVCS01;DATABASE=Rohteillager;'
        f'UID=guehringCS01;PWD=M+5WGWa..sD?gfC~RGHz'
    )


def explore_table(cursor, table_name):
    print("=" * 80)
    print(f"  TABLE: {table_name}")
    print("=" * 80)

    # Column info
    cursor.execute(
        "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE "
        "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
        table_name,
    )
    columns = cursor.fetchall()
    print(f"\n--- Columns ({len(columns)}) ---")
    print(f"{'Column':<30} {'Type':<15} {'Max Length':<12} {'Nullable':<10}")
    print("-" * 67)
    for col in columns:
        max_len = str(col.CHARACTER_MAXIMUM_LENGTH) if col.CHARACTER_MAXIMUM_LENGTH else "-"
        print(f"{col.COLUMN_NAME:<30} {col.DATA_TYPE:<15} {max_len:<12} {col.IS_NULLABLE:<10}")

    # Row count
    cursor.execute(f"SELECT COUNT(*) FROM [dbo].[{table_name}]")
    count = cursor.fetchone()[0]
    print(f"\n--- Total rows: {count} ---")

    # Sample data (first 10 rows)
    df = pd.read_sql(f"SELECT TOP 10 * FROM [dbo].[{table_name}]", cursor.connection)
    print(f"\n--- Sample data (first 10 rows) ---")
    pd.set_option("display.max_columns", None)
    pd.set_option("display.width", 200)
    pd.set_option("display.max_colwidth", 40)
    print(df.to_string(index=False))
    print()


def main():
    print("Connecting to SQL Server...")
    conn = pyodbc.connect(ConnString())
    cursor = conn.cursor()
    print("Connected successfully!\n")

    tables = ["rohteillager", "journal", "lagerorte"]

    for table in tables:
        try:
            explore_table(cursor, table)
        except Exception as e:
            print(f"Error reading table '{table}': {e}\n")

    # Show relationships via foreign keys
    print("=" * 80)
    print("  FOREIGN KEY RELATIONSHIPS")
    print("=" * 80)
    cursor.execute(
        """
        SELECT
            fk.name AS FK_Name,
            tp.name AS Parent_Table,
            cp.name AS Parent_Column,
            tr.name AS Referenced_Table,
            cr.name AS Referenced_Column
        FROM sys.foreign_keys fk
        JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
        JOIN sys.tables tp ON fkc.parent_object_id = tp.object_id
        JOIN sys.columns cp ON fkc.parent_object_id = cp.object_id AND fkc.parent_column_id = cp.column_id
        JOIN sys.tables tr ON fkc.referenced_object_id = tr.object_id
        JOIN sys.columns cr ON fkc.referenced_object_id = cr.object_id AND fkc.referenced_column_id = cr.column_id
        ORDER BY tp.name
        """
    )
    fks = cursor.fetchall()
    if fks:
        for fk in fks:
            print(f"  {fk.Parent_Table}.{fk.Parent_Column}  -->  {fk.Referenced_Table}.{fk.Referenced_Column}  ({fk.FK_Name})")
    else:
        print("  No foreign keys found.")

    print()
    conn.close()
    print("Connection closed.")


if __name__ == "__main__":
    main()
