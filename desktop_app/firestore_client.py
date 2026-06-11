"""Firebase Firestore client — shared singleton for the desktop app."""
import os
import firebase_admin
from firebase_admin import credentials, firestore

_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_CRED_PATH = os.path.join(_ROOT, "firebase-credentials.json")

cred = credentials.Certificate(_CRED_PATH)
firebase_admin.initialize_app(cred, {"projectId": "instock-2c9f7"})

db = firestore.client()
