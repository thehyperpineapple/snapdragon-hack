"""
Plan service - handles diet and workout plan management.
"""

import logging
from extensions import db

logger = logging.getLogger('database')


def create_plan(user_id, plan_data):
    """Initialize the diet and workouts arrays on a user document."""
    logger.info(f"DB_WRITE: Creating plan for userId={user_id}, ai_generated={plan_data.get('ai_generated', False)}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for plan creation - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.set({
            "diet": plan_data.get("diet", []),
            "workouts": plan_data.get("workouts", []),
            "activePlan": True,
        }, merge=True)
        logger.info(f"DB_WRITE: Plan created successfully for userId={user_id}")
        return {"message": "Plan created successfully", "plan": plan_data}, 201
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to create plan - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def get_plan(user_id):
    """Retrieve the diet and workouts from a user document."""
    logger.info(f"DB_READ: Fetching plan for userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_READ: User not found - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        plan = {
            "diet": doc_data.get("diet", []),
            "workouts": doc_data.get("workouts", []),
        }

        if not plan["diet"] and not plan["workouts"]:
            logger.debug(f"DB_READ: No active plan found for userId={user_id}")
            return {"error": "No active plan found"}, 404

        logger.debug(f"DB_READ: Plan retrieved successfully for userId={user_id}")
        return {"plan": plan}, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to retrieve plan - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def update_plan(user_id, plan_data):
    """Update an existing plan."""
    logger.info(f"DB_WRITE: Updating plan for userId={user_id} with fields: {list(plan_data.keys())}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for plan update - userId={user_id}")
            return {"error": "User not found"}, 404

        update_data = {}
        if "diet" in plan_data:
            update_data["diet"] = plan_data["diet"]
        if "workouts" in plan_data:
            update_data["workouts"] = plan_data["workouts"]
        if "status" in plan_data:
            update_data["planStatus"] = plan_data["status"]

        doc_ref.set(update_data, merge=True)
        logger.info(f"DB_WRITE: Plan updated successfully for userId={user_id}")
        return {"message": "Plan updated successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to update plan - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def delete_plan(user_id):
    """Remove the plan from a user document."""
    logger.info(f"DB_WRITE: Deleting plan for userId={user_id}")
    try:
        from google.cloud import firestore as fs

        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for plan deletion - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.update({
            "diet": fs.DELETE_FIELD,
            "workouts": fs.DELETE_FIELD,
            "activePlan": False,
        })
        logger.info(f"DB_WRITE: Plan deleted successfully for userId={user_id}")
        return {"message": "Plan deleted successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to delete plan - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500
