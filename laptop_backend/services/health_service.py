"""
Health service - handles user health profile data.
"""

from google.cloud import firestore
from extensions import db


def post_health_data(user_id, data):
    """Create or overwrite the profile object on a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404
            
        doc_ref.set({"profile": data}, merge=True)
        return {"message": "Profile created successfully", "profile": data}, 201
    except Exception as e:
        return {"error": str(e)}, 500


def get_health_data(user_id):
    """Retrieve the health profile from a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404
        
        doc_data = doc.to_dict()
        profile = doc_data.get("profile")
        if not profile:
            return {"error": "Health profile not found"}, 404
            
        return {"profile": profile}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def patch_health_data(user_id, data):
    """Update specific fields inside profile using dot notation."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        update_fields = {f"profile.{key}": value for key, value in data.items()}
        doc_ref.update(update_fields)
        return {"message": "Profile updated successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def delete_health_data(user_id):
    """Remove the profile map from a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_ref.update({"profile": firestore.DELETE_FIELD})
        return {"message": "Profile deleted successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500
