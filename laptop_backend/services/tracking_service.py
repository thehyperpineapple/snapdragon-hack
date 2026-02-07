"""
Tracking service - handles daily meal and workout tracking.
"""

import logging
from extensions import db

logger = logging.getLogger('database')


def update_meal_completion(user_id, week_name, meal_type, actual_meal):
    """Update the actualMeal field for a specific week and meal type in the diet array."""
    logger.info(f"DB_WRITE: Updating meal completion for userId={user_id}, week={week_name}, meal={meal_type}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for meal completion update - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        diet = doc_data.get("diet", [])

        updated = False
        for entry in diet:
            if entry.get("weekName") == week_name:
                meals = entry.get("meals", {})
                if meal_type not in meals:
                    logger.warning(f"DB_WRITE: Meal type '{meal_type}' not found for userId={user_id}")
                    return {"error": f"Meal type '{meal_type}' not found"}, 404
                meals[meal_type]["actualMeal"] = actual_meal
                meals[meal_type]["completed"] = True
                updated = True
                break

        if not updated:
            logger.warning(f"DB_WRITE: Week '{week_name}' not found for userId={user_id}")
            return {"error": f"Week '{week_name}' not found"}, 404

        doc_ref.set({"diet": diet}, merge=True)
        logger.info(f"DB_WRITE: Meal completion updated successfully for userId={user_id}")
        return {"message": "Meal completion updated successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to update meal completion - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def toggle_workout_status(user_id, week_name, workout_id, is_completed):
    """Update the completed boolean for a specific workout entry."""
    logger.info(f"DB_WRITE: Toggling workout status for userId={user_id}, week={week_name}, workout={workout_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for workout toggle - userId={user_id}")
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
            logger.warning(f"DB_WRITE: Workout '{workout_id}' in week '{week_name}' not found for userId={user_id}")
            return {"error": f"Workout '{workout_id}' in week '{week_name}' not found"}, 404

        doc_ref.set({"workouts": workouts}, merge=True)
        logger.info(f"DB_WRITE: Workout status updated successfully for userId={user_id}")
        return {"message": "Workout status updated successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to toggle workout status - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def log_daily_meal(user_id, date, meal_type, items):
    """Log meals eaten for a specific day."""
    logger.info(f"DB_WRITE: Logging daily meal for userId={user_id}, date={date}, meal_type={meal_type}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for daily meal log - userId={user_id}")
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
        logger.info(f"DB_WRITE: Daily meal logged successfully for userId={user_id}")
        return {"message": f"{meal_type} logged successfully", "dailyLog": daily_logs[date]}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to log daily meal - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def log_daily_workout(user_id, date, workout_data):
    """Log workout status for a specific day."""
    logger.info(f"DB_WRITE: Logging daily workout for userId={user_id}, date={date}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for daily workout log - userId={user_id}")
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
        logger.info(f"DB_WRITE: Daily workout logged successfully for userId={user_id}")
        return {"message": "Workout logged successfully", "dailyLog": daily_logs[date]}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to log daily workout - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500
