"""
Flask extensions - Firebase/Firestore initialization.

This module initializes the Firebase Admin SDK and exposes the Firestore
database client as `db` for use across the application.
"""

import glob
import json
import os

import firebase_admin
from firebase_admin import firestore

# Firestore client - initialized on module load
db = None


def _get_credentials():
    """Get Firebase credentials from env or default file."""
    # Option 1: Service account JSON file path
    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if cred_path and os.path.exists(cred_path):
        return firebase_admin.credentials.Certificate(cred_path)

    # Option 2: Service account JSON as string in env
    if os.getenv("FIREBASE_SERVICE_ACCOUNT"):
        try:
            service_account = json.loads(os.getenv("FIREBASE_SERVICE_ACCOUNT"))
            return firebase_admin.credentials.Certificate(service_account)
        except (json.JSONDecodeError, TypeError):
            pass

    # Option 3: Default - look for firestore_key.json or *-firebase-adminsdk-*.json in laptop_backend dir
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    for filename in ("firestore_key.json",):
        default_path = os.path.join(backend_dir, filename)
        if os.path.exists(default_path):
            return firebase_admin.credentials.Certificate(default_path)

    for path in glob.glob(os.path.join(backend_dir, "*-firebase-adminsdk-*.json")):
        return firebase_admin.credentials.Certificate(path)

    return None


# Initialize Firebase and set db on module load
try:
    firebase_admin.get_app()
except ValueError:
    cred = _get_credentials()
    if cred is None:
        raise RuntimeError(
            "Firebase credentials not found. Set GOOGLE_APPLICATION_CREDENTIALS, "
            "FIREBASE_SERVICE_ACCOUNT, or place firestore_key.json in laptop_backend."
        )
    firebase_admin.initialize_app(cred)

db = firestore.client()
