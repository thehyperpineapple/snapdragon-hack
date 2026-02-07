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


def log_food_items(user_id, date, meal_type, items):
    """Log individual food items the user ate, with full nutrition data."""
    logger.info(f"DB_WRITE: Logging food items for userId={user_id}, date={date}, meal_type={meal_type}, count={len(items)}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for food log - userId={user_id}")
            return {"error": "User not found"}, 404

        daily_logs = doc.to_dict().get("dailyLogs", {})

        if date not in daily_logs:
            daily_logs[date] = {"meals": {}, "workout": None, "food_log": []}

        if "food_log" not in daily_logs[date]:
            daily_logs[date]["food_log"] = []

        for item in items:
            # Parse calories - handle "Only available for premium subscribers." case
            calories_raw = item.get("calories", "N/A")
            calories_numeric = 0
            if isinstance(calories_raw, (int, float)):
                calories_numeric = float(calories_raw)
            elif isinstance(calories_raw, str) and calories_raw.replace(".", "", 1).isdigit():
                calories_numeric = float(calories_raw)
            
            entry = {
                "name": item.get("name", ""),
                "calories": calories_raw,
                "calories_numeric": calories_numeric,
                "serving_size_g": item.get("serving_size_g", 0),
                "fat_total_g": item.get("fat_total_g", 0),
                "protein_g": item.get("protein_g", "N/A"),
                "carbohydrates_total_g": item.get("carbohydrates_total_g", 0),
                "fiber_g": item.get("fiber_g", 0),
                "sugar_g": item.get("sugar_g", 0),
                "meal_type": meal_type
            }
            daily_logs[date]["food_log"].append(entry)

        doc_ref.set({"dailyLogs": daily_logs}, merge=True)
        logger.info(f"DB_WRITE: Food items logged successfully for userId={user_id}")

        food_log = daily_logs[date]["food_log"]
        total_cals = sum(e.get("calories_numeric", 0) for e in food_log)
        summary = {
            "total_items": len(food_log),
            "total_calories": round(total_cals, 1),
            "total_fat_g": sum(e.get("fat_total_g", 0) for e in food_log),
            "total_carbs_g": sum(e.get("carbohydrates_total_g", 0) for e in food_log)
        }

        return {
            "message": f"{len(items)} item(s) logged successfully",
            "food_log": food_log,
            "summary": summary
        }, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to log food items - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def get_food_log(user_id, date):
    """Get the food log for a specific date."""
    logger.info(f"DB_READ: Getting food log for userId={user_id}, date={date}")
    try:
        doc = db.collection("users").document(user_id).get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        daily_logs = doc.to_dict().get("dailyLogs", {})
        day_data = daily_logs.get(date, {})
        food_log = day_data.get("food_log", [])

        total_cals = sum(e.get("calories_numeric", 0) for e in food_log)
        summary = {
            "total_items": len(food_log),
            "total_calories": round(total_cals, 1),
            "total_fat_g": sum(e.get("fat_total_g", 0) for e in food_log),
            "total_carbs_g": sum(e.get("carbohydrates_total_g", 0) for e in food_log)
        }

        return {"date": date, "food_log": food_log, "summary": summary}, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to get food log - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def get_calorie_summary(user_id, date):
    """Get calorie goal vs consumed summary for a date."""
    logger.info(f"DB_READ: Getting calorie summary for userId={user_id}, date={date}")
    try:
        doc = db.collection("users").document(user_id).get()
        if not doc.exists:
            return {"error": "User not found"}, 404

        doc_data = doc.to_dict()
        
        # Get calorie goal from nutrition profile
        nutrition = doc_data.get("nutrition", {})
        calorie_goal = nutrition.get("calorie_goal", 2000)  # Default 2000 if not set
        
        # Calculate consumed calories
        daily_logs = doc_data.get("dailyLogs", {})
        day_data = daily_logs.get(date, {})
        food_log = day_data.get("food_log", [])
        
        calories_consumed = sum(e.get("calories_numeric", 0) for e in food_log)
        calories_remaining = max(0, calorie_goal - calories_consumed)
        
        return {
            "date": date,
            "calorie_goal": round(calorie_goal, 1),
            "calories_consumed": round(calories_consumed, 1),
            "calories_remaining": round(calories_remaining, 1),
            "percentage_consumed": round((calories_consumed / calorie_goal * 100) if calorie_goal > 0 else 0, 1),
            "total_items": len(food_log)
        }, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to get calorie summary - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500
