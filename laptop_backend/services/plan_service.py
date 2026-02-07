"""
Plan service - handles diet and workout plan management.
"""

from extensions import db


def create_plan(user_id, plan_data):
    """Initialize the diet and workouts arrays on a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404
            
        doc_ref.set({
            "diet": plan_data.get("diet", []),
            "workouts": plan_data.get("workouts", []),
            "activePlan": True,
        }, merge=True)
        return {"message": "Plan created successfully", "plan": plan_data}, 201
    except Exception as e:
        return {"error": str(e)}, 500


def get_plan(user_id):
    """Retrieve the diet and workouts from a user document."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        plan = {
            "diet": doc_data.get("diet", []),
            "workouts": doc_data.get("workouts", []),
        }
        
        if not plan["diet"] and not plan["workouts"]:
            return {"error": "No active plan found"}, 404
            
        return {"plan": plan}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def update_plan(user_id, plan_data):
    """Update an existing plan."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        update_data = {}
        if "diet" in plan_data:
            update_data["diet"] = plan_data["diet"]
        if "workouts" in plan_data:
            update_data["workouts"] = plan_data["workouts"]
        if "status" in plan_data:
            update_data["planStatus"] = plan_data["status"]
            
        doc_ref.set(update_data, merge=True)
        return {"message": "Plan updated successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def delete_plan(user_id):
    """Remove the plan from a user document."""
    try:
        from google.cloud import firestore as fs
        
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_ref.update({
            "diet": fs.DELETE_FIELD,
            "workouts": fs.DELETE_FIELD,
            "activePlan": False,
        })
        return {"message": "Plan deleted successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500
