"""
Nutrition service - handles user nutrition preferences and diet data.
"""

from google.cloud import firestore
from extensions import db


def post_nutrition_data(user_id, nutrition_data):
    """Create or set nutrition preferences on a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_ref.set({"nutrition": nutrition_data}, merge=True)
        return {"message": "Nutrition profile created successfully", "nutrition": nutrition_data}, 201
    except Exception as e:
        return {"error": str(e)}, 500


def get_nutrition_data(user_id):
    """Retrieve nutrition preferences from a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404
        
        doc_data = doc.to_dict()
        nutrition = doc_data.get("nutrition")
        if not nutrition:
            return {"error": "Nutrition profile not found"}, 404
            
        return {"nutrition": nutrition}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def update_nutrition_data(user_id, data):
    """Update specific fields in nutrition profile."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        update_fields = {f"nutrition.{key}": value for key, value in data.items()}
        doc_ref.update(update_fields)
        return {"message": "Nutrition profile updated successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def delete_nutrition_data(user_id):
    """Remove nutrition data from a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_ref.update({"nutrition": firestore.DELETE_FIELD})
        return {"message": "Nutrition profile deleted successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def add_diet_entry(user_id, week_data):
    """Append a new meal/diet entry to the diet array."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_ref.update({"diet": firestore.ArrayUnion([week_data])})
        return {"message": "Diet entry added successfully"}, 201
    except Exception as e:
        return {"error": str(e)}, 500
