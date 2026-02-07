"""
Health service - handles user health profile data.
"""

import logging
from google.cloud import firestore
from extensions import db

logger = logging.getLogger('database')


def post_health_data(user_id, data):
    """Create or overwrite the profile object on a user document."""
    logger.info(f"DB_WRITE: Creating health profile for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for health profile creation - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.set({"profile": data}, merge=True)
        logger.info(f"DB_WRITE: Health profile created successfully for userId={user_id}")
        return {"message": "Profile created successfully", "profile": data}, 201
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to create health profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def get_health_data(user_id):
    """Retrieve the health profile from a user document."""
    logger.info(f"DB_READ: Fetching health profile for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_READ: User not found - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        profile = doc_data.get("profile")
        if not profile:
            logger.debug(f"DB_READ: Health profile not found for userId={user_id}")
            return {"error": "Health profile not found"}, 404

        logger.debug(f"DB_READ: Health profile retrieved successfully for userId={user_id}")
        return {"profile": profile}, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to retrieve health profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def patch_health_data(user_id, data):
    """Update specific fields inside profile using dot notation."""
    logger.info(f"DB_WRITE: Updating health profile for userId={user_id} with fields: {list(data.keys())}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for health profile update - userId={user_id}")
            return {"error": "User not found"}, 404

        update_fields = {f"profile.{key}": value for key, value in data.items()}
        doc_ref.update(update_fields)
        logger.info(f"DB_WRITE: Health profile updated successfully for userId={user_id}")
        return {"message": "Profile updated successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to update health profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def delete_health_data(user_id):
    """Remove the profile map from a user document."""
    logger.info(f"DB_WRITE: Deleting health profile for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for health profile deletion - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.update({"profile": firestore.DELETE_FIELD})
        logger.info(f"DB_WRITE: Health profile deleted successfully for userId={user_id}")
        return {"message": "Profile deleted successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to delete health profile - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500
