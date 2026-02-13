"""
Daily Tracking Blueprint - Meals & Workout logging.
"""

from datetime import date
from flask import Blueprint, jsonify, request
from services.tracking_service import (
    update_meal_completion, toggle_workout_status,
    log_daily_meal, log_daily_workout,
    log_food_items, get_food_log, get_calorie_summary
)
from extensions import db

tracking_bp = Blueprint('tracking', __name__, url_prefix='/users')


def today():
    return date.today().strftime('%Y-%m-%d')


@tracking_bp.route('/<user_id>/tracking/meals', methods=['POST'])
def update_meals(user_id):
    """
    Log meal for the day.
    
    Request Body (daily logging):
        {
            "date": "2024-01-15",
            "meal_type": "breakfast",
            "items": [{"name": "Oatmeal", "calories": 300}]
        }
    
    Request Body (plan-based):
        {
            "week_name": "Week 1",
            "meal_type": "breakfast",
            "actual_meal": "Had eggs instead"
        }
    """
    data = request.get_json(silent=True) or {}
    
    meal_type = data.get('meal_type')
    valid_types = ['breakfast', 'lunch', 'dinner', 'snacks']
    
    if not meal_type or meal_type not in valid_types:
        return jsonify({'error': f'meal_type must be one of: {", ".join(valid_types)}'}), 400
    
    # Plan-based tracking
    if data.get('week_name'):
        result, status = update_meal_completion(
            user_id, data['week_name'], meal_type, data.get('actual_meal', '')
        )
    # Daily logging
    else:
        result, status = log_daily_meal(
            user_id, data.get('date', today()), meal_type, data.get('items', [])
        )
    
    return jsonify(result), status


@tracking_bp.route('/<user_id>/tracking/workout', methods=['POST'])
def update_workout(user_id):
    """
    Log workout for the day.
    
    Request Body (daily logging):
        {
            "date": "2024-01-15",
            "completed": true,
            "exercises": [{"name": "Push-ups", "sets": 3, "reps": 12}],
            "duration_minutes": 45
        }
    
    Request Body (plan-based):
        {
            "week_name": "Week 1",
            "workout_id": "w1",
            "completed": true
        }
    """
    data = request.get_json(silent=True) or {}
    
    # Plan-based tracking
    if data.get('week_name') and data.get('workout_id'):
        result, status = toggle_workout_status(
            user_id, data['week_name'], data['workout_id'], data.get('completed', True)
        )
    # Daily logging
    else:
        result, status = log_daily_workout(
            user_id, data.get('date', today()), {
                'completed': data.get('completed', True),
                'exercises': data.get('exercises', []),
                'duration_minutes': data.get('duration_minutes', 0),
                'notes': data.get('notes', '')
            }
        )
    
    return jsonify(result), status


@tracking_bp.route('/<user_id>/tracking/daily', methods=['GET'])
def get_daily_log(user_id):
    """Get daily log for a date."""
    log_date = request.args.get('date', today())
    
    try:
        doc = db.collection("users").document(user_id).get()
        if not doc.exists:
            return jsonify({'error': 'User not found'}), 404
        
        daily_logs = doc.to_dict().get("dailyLogs", {})
        if log_date not in daily_logs:
            return jsonify({'error': f'No log for {log_date}'}), 404
        
        return jsonify({'date': log_date, 'daily_log': daily_logs[log_date]}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@tracking_bp.route('/<user_id>/tracking/history', methods=['GET'])
def get_tracking_history(user_id):
    """Get tracking history."""
    limit = min(int(request.args.get('limit', 30)), 100)
    
    try:
        doc = db.collection("users").document(user_id).get()
        if not doc.exists:
            return jsonify({'error': 'User not found'}), 404
        
        daily_logs = doc.to_dict().get("dailyLogs", {})
        sorted_dates = sorted(daily_logs.keys(), reverse=True)[:limit]
        logs = [{'date': d, **daily_logs[d]} for d in sorted_dates]
        
        return jsonify({'daily_logs': logs, 'total': len(logs)}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@tracking_bp.route('/<user_id>/tracking/water', methods=['POST'])
def update_water_intake(user_id):
    """
    Log water intake.
    
    Request Body:
        {
            "amount_ml": 250,
            "set_total": false
        }
    """
    data = request.get_json(silent=True) or {}
    
    if 'amount_ml' not in data:
        return jsonify({'error': 'amount_ml required'}), 400
    
    try:
        amount = int(data['amount_ml'])
        log_date = data.get('date', today())
        
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return jsonify({'error': 'User not found'}), 404
        
        daily_logs = doc.to_dict().get("dailyLogs", {})
        if log_date not in daily_logs:
            daily_logs[log_date] = {"meals": {}, "workout": None, "water_ml": 0}
        
        if data.get('set_total'):
            daily_logs[log_date]["water_ml"] = amount
        else:
            daily_logs[log_date]["water_ml"] = daily_logs[log_date].get("water_ml", 0) + amount
        
        doc_ref.set({"dailyLogs": daily_logs}, merge=True)
        return jsonify({'water_intake_ml': daily_logs[log_date]["water_ml"]}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@tracking_bp.route('/<user_id>/tracking/wellness', methods=['POST'])
def update_wellness(user_id):
    """
    Log wellness metrics.
    
    Request Body:
        {
            "sleep_hours": 7.5,
            "mood": "good",
            "energy_level": 4
        }
    """
    data = request.get_json(silent=True) or {}
    log_date = data.get('date', today())
    
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return jsonify({'error': 'User not found'}), 404
        
        daily_logs = doc.to_dict().get("dailyLogs", {})
        if log_date not in daily_logs:
            daily_logs[log_date] = {"meals": {}, "workout": None}
        
        wellness = daily_logs[log_date].get("wellness", {})
        
        if 'sleep_hours' in data:
            wellness['sleep_hours'] = float(data['sleep_hours'])
        
        if 'mood' in data:
            valid_moods = ['great', 'good', 'okay', 'bad', 'terrible']
            if data['mood'] not in valid_moods:
                return jsonify({'error': f'mood must be: {", ".join(valid_moods)}'}), 400
            wellness['mood'] = data['mood']
        
        if 'energy_level' in data:
            energy = int(data['energy_level'])
            if not 1 <= energy <= 5:
                return jsonify({'error': 'energy_level must be 1-5'}), 400
            wellness['energy_level'] = energy
        
        daily_logs[log_date]["wellness"] = wellness
        doc_ref.set({"dailyLogs": daily_logs}, merge=True)
        
        return jsonify({'wellness': wellness}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@tracking_bp.route('/<user_id>/tracking/food-log', methods=['POST'])
def add_food_log(user_id):
    """
    Log food items the user actually ate.
    
    Request Body:
        {
            "date": "2024-01-15",
            "meal_type": "breakfast",
            "items": [
                {
                    "name": "brisket",
                    "calories": "300",
                    "serving_size_g": 453.592,
                    "fat_total_g": 82.9,
                    "protein_g": "50",
                    "carbohydrates_total_g": 0,
                    "fiber_g": 0,
                    "sugar_g": 0
                }
            ]
        }
    """
    data = request.get_json(silent=True) or {}
    
    meal_type = data.get('meal_type', 'snacks')
    items = data.get('items', [])
    log_date = data.get('date', today())
    
    if not items:
        return jsonify({'error': 'items list is required'}), 400
    
    result, status = log_food_items(user_id, log_date, meal_type, items)
    return jsonify(result), status


@tracking_bp.route('/<user_id>/tracking/food-log', methods=['GET'])
def get_food_log_route(user_id):
    """
    Get food log for a date.
    Query params: ?date=2024-01-15 (defaults to today)
    """
    log_date = request.args.get('date', today())
    result, status = get_food_log(user_id, log_date)
    return jsonify(result), status


@tracking_bp.route('/<user_id>/tracking/calories', methods=['GET'])
def get_calorie_status(user_id):
    """
    Get calorie summary: goal, consumed, remaining for a date.
    Query params: ?date=2024-01-15 (defaults to today)
    
    Returns:
        {
            "date": "2024-01-15",
            "calorie_goal": 2000,
            "calories_consumed": 1200,
            "calories_remaining": 800,
            "percentage_consumed": 60.0,
            "total_items": 5
        }
    """
    log_date = request.args.get('date', today())
    result, status = get_calorie_summary(user_id, log_date)
    return jsonify(result), status
