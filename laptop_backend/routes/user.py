"""
User Profile Blueprint - Health & Nutrition management.
"""

from flask import Blueprint, jsonify, request
from services.health_service import (
    post_health_data, get_health_data, patch_health_data, delete_health_data
)
from services.nutrition_service import (
    post_nutrition_data, get_nutrition_data, update_nutrition_data, delete_nutrition_data
)

user_bp = Blueprint('user', __name__, url_prefix='/users')


# ============================================================================
# Health Profile Endpoints
# ============================================================================

@user_bp.route('/<user_id>/health', methods=['POST'])
def create_health_profile(user_id):
    """
    Create health profile.
    
    Request Body:
        {
            "weight": 75.5,
            "height": 180,
            "age": 28,
            "gender": "male",
            "activity_level": "moderate",
            "fitness_goal": "weight_loss",
            "workouts_per_day": 3,
            "workouts_duration": 30,
        }
    """
    data = request.get_json(silent=True) or {}
    
    required = ['weight', 'height', 'age']
    missing = [f for f in required if f not in data]
    if missing:
        return jsonify({'error': 'Missing required fields', 'missing_fields': missing}), 400
    
    # Calculate BMI
    try:
        weight = float(data['weight'])
        height = float(data['height'])
        data['bmi'] = round(weight / ((height / 100) ** 2), 2)
    except (ValueError, ZeroDivisionError):
        return jsonify({'error': 'Invalid weight or height'}), 400
    
    result, status = post_health_data(user_id, data)
    return jsonify(result), status


@user_bp.route('/<user_id>/health', methods=['GET'])
def get_health_profile(user_id):
    """Get health profile."""
    result, status = get_health_data(user_id)
    return jsonify(result), status


@user_bp.route('/<user_id>/health', methods=['PUT'])
def update_health_profile(user_id):
    """Update health profile."""
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({'error': 'No update data provided'}), 400
    
    # Recalculate BMI if needed
    if 'weight' in data or 'height' in data:
        current, status = get_health_data(user_id)
        if status == 200:
            profile = current.get('profile', {})
            weight = float(data.get('weight', profile.get('weight', 0)))
            height = float(data.get('height', profile.get('height', 1)))
            if height > 0:
                data['bmi'] = round(weight / ((height / 100) ** 2), 2)
    
    result, status = patch_health_data(user_id, data)
    return jsonify(result), status


@user_bp.route('/<user_id>/health', methods=['DELETE'])
def remove_health_profile(user_id):
    """Delete health profile."""
    result, status = delete_health_data(user_id)
    return jsonify(result), status


# ============================================================================
# Nutrition Profile Endpoints
# ============================================================================

@user_bp.route('/<user_id>/nutrition', methods=['POST'])
def create_nutrition_profile(user_id):
    #TODO: FIX TO CALCULATE MACROS AND TDEE ON BACKEND
    """
    Create nutrition profile.
    
    Request Body:
        {
            "diet_type": "vegetarian",
            "meals_per_day": 4
        }
    """
    data = request.get_json(silent=True) or {}

    dbData, status = get_health_data(user_id)
    
    if status != 200:
        return jsonify({'error': 'Failed to get health data'}), 500
    
    health_data = dbData.get('profile', {})
    
    age = health_data.get('age', 0)
    gender = health_data.get('gender', 'male')
    weight = health_data.get('weight', 0)
    height = health_data.get('height', 0)

    workouts_per_day = health_data.get('workouts_per_day', 0)
    workouts_duration = health_data.get('workouts_duration', 0)

    total_workouts_duration = workouts_per_day * workouts_duration
    if total_workouts_duration < 0:
        activity_multiplier = 1.2
    elif total_workouts_duration < 150:
        activity_multiplier = 1.375
    elif total_workouts_duration < 300:
        activity_multiplier = 1.55
    elif total_workouts_duration < 450:
        activity_multiplier = 1.725
    else:
        activity_multiplier = 1.9

    
    if gender == 'male':
        bmr = 10 * weight + 6.25 * height - 5 * age + 5
    else:
        bmr = 10 * weight + 6.25 * height - 5 * age - 161

    tdee = bmr * activity_multiplier
    
    protein_goal = round(weight * 1.5, 0)
    fat_goal = tdee * 0.30
    carb_goal = tdee - protein_goal - fat_goal

    age = health_data.get('age', 0)

    if 'weight_loss' == health_data.get('fitness_goal', 'weight_loss') :
        calorie_goal = tdee - 500
    elif 'weight_gain' in health_data.get('fitness_goal', 'weight_loss')  :
        calorie_goal = tdee + 500
    else:
        calorie_goal = tdee



    nutrition_data = {
        'allergies': data.get('allergies', []),
        'diet_type': data.get('diet_type', 'standard'),
        'calorie_goal': calorie_goal,
        'protein_goal': protein_goal,
        'carb_goal': carb_goal,
        'fat_goal': fat_goal,
        'meals_per_day': data.get('meals_per_day', 3),
        'dietary_restrictions': data.get('dietary_restrictions', []),
        'cuisine_preferences': data.get('cuisine_preferences', [])
    }
    
    result, status = post_nutrition_data(user_id, nutrition_data)
    return jsonify(result), status


@user_bp.route('/<user_id>/nutrition', methods=['GET'])
def get_nutrition_profile(user_id):
    """Get nutrition profile."""
    result, status = get_nutrition_data(user_id)
    return jsonify(result), status


@user_bp.route('/<user_id>/nutrition', methods=['PUT'])
def update_nutrition_profile(user_id):
    """Update nutrition profile."""
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({'error': 'No update data provided'}), 400
    
    result, status = update_nutrition_data(user_id, data)
    return jsonify(result), status


@user_bp.route('/<user_id>/nutrition', methods=['DELETE'])
def remove_nutrition_profile(user_id):
    """Delete nutrition profile."""
    result, status = delete_nutrition_data(user_id)
    return jsonify(result), status
