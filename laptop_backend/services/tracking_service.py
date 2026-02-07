"""
Tracking service - handles daily meal and workout tracking.
"""

from extensions import db


def update_meal_completion(user_id, week_name, meal_type, actual_meal):
    """Update the actualMeal field for a specific week and meal type in the diet array."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        diet = doc_data.get("diet", [])

        updated = False
        for entry in diet:
            if entry.get("weekName") == week_name:
                meals = entry.get("meals", {})
                if meal_type not in meals:
                    return {"error": f"Meal type '{meal_type}' not found"}, 404
                meals[meal_type]["actualMeal"] = actual_meal
                meals[meal_type]["completed"] = True
                updated = True
                break

        if not updated:
            return {"error": f"Week '{week_name}' not found"}, 404

        doc_ref.set({"diet": diet}, merge=True)
        return {"message": "Meal completion updated successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def toggle_workout_status(user_id, week_name, workout_id, is_completed):
    """Update the completed boolean for a specific workout entry."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        workouts = doc_data.get("workouts", [])

        updated = False
        for entry in workouts:
            if entry.get("weekName") == week_name:
                exercises = entry.get("exercises", [])
                for exercise in exercises:
                    if exercise.get("workoutId") == workout_id:
                        exercise["completed"] = is_completed
                        updated = True
                        break
                break

        if not updated:
            return {"error": f"Workout '{workout_id}' in week '{week_name}' not found"}, 404

        doc_ref.set({"workouts": workouts}, merge=True)
        return {"message": "Workout status updated successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def log_daily_meal(user_id, date, meal_type, items):
    """Log meals eaten for a specific day."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        # Get or create daily logs
        daily_logs = doc.to_dict().get("dailyLogs", {})
        
        if date not in daily_logs:
            daily_logs[date] = {"meals": {}, "workout": None}
        
        daily_logs[date]["meals"][meal_type] = {
            "items": items,
            "completed": True
        }
        
        doc_ref.set({"dailyLogs": daily_logs}, merge=True)
        return {"message": f"{meal_type} logged successfully", "dailyLog": daily_logs[date]}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def log_daily_workout(user_id, date, workout_data):
    """Log workout status for a specific day."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        # Get or create daily logs
        daily_logs = doc.to_dict().get("dailyLogs", {})
        
        if date not in daily_logs:
            daily_logs[date] = {"meals": {}, "workout": None}
        
        daily_logs[date]["workout"] = {
            "completed": workout_data.get("completed", True),
            "exercises": workout_data.get("exercises", []),
            "duration": workout_data.get("duration_minutes", 0),
            "notes": workout_data.get("notes", "")
        }
        
        doc_ref.set({"dailyLogs": daily_logs}, merge=True)
        return {"message": "Workout logged successfully", "dailyLog": daily_logs[date]}, 200
    except Exception as e:
        return {"error": str(e)}, 500
