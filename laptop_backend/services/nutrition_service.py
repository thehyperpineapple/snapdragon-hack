"""
Nutrition service - handles user nutrition preferences and diet data.
"""

import logging
from google.cloud import firestore
from extensions import db

logger = logging.getLogger('database')


def post_nutrition_data(user_id, nutrition_data):
    """Create or set nutrition preferences on a user document."""
    logger.info(f"DB_WRITE: Creating nutrition profile for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for nutrition profile creation - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.set({"nutrition": nutrition_data}, merge=True)
        logger.info(f"DB_WRITE: Nutrition profile created successfully for userId={user_id}")
        return {"message": "Nutrition profile created successfully", "nutrition": nutrition_data}, 201
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to create nutrition profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def get_nutrition_data(user_id):
    """Retrieve nutrition preferences from a user document."""
    logger.info(f"DB_READ: Fetching nutrition profile for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_READ: User not found - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        nutrition = doc_data.get("nutrition")
        if not nutrition:
            logger.debug(f"DB_READ: Nutrition profile not found for userId={user_id}")
            return {"error": "Nutrition profile not found"}, 404

        logger.debug(f"DB_READ: Nutrition profile retrieved successfully for userId={user_id}")
        return {"nutrition": nutrition}, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to retrieve nutrition profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def update_nutrition_data(user_id, data):
    """Update specific fields in nutrition profile."""
    logger.info(f"DB_WRITE: Updating nutrition profile for userId={user_id} with fields: {list(data.keys())}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for nutrition profile update - userId={user_id}")
            return {"error": "User not found"}, 404

        update_fields = {f"nutrition.{key}": value for key, value in data.items()}
        doc_ref.update(update_fields)
        logger.info(f"DB_WRITE: Nutrition profile updated successfully for userId={user_id}")
        return {"message": "Nutrition profile updated successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to update nutrition profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def delete_nutrition_data(user_id):
    """Remove nutrition data from a user document."""
    logger.info(f"DB_WRITE: Deleting nutrition profile for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for nutrition profile deletion - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.update({"nutrition": firestore.DELETE_FIELD})
        logger.info(f"DB_WRITE: Nutrition profile deleted successfully for userId={user_id}")
        return {"message": "Nutrition profile deleted successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to delete nutrition profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def add_diet_entry(user_id, week_data):
    """Append a new meal/diet entry to the diet array."""
    logger.info(f"DB_WRITE: Adding diet entry for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for diet entry - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.update({"diet": firestore.ArrayUnion([week_data])})
        logger.info(f"DB_WRITE: Diet entry added successfully for userId={user_id}")
        return {"message": "Diet entry added successfully"}, 201
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to add diet entry - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500
