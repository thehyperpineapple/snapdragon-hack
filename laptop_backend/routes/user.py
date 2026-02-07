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
            "fitness_goal": "weight_loss"
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
    """
    Create nutrition profile.
    
    Request Body:
        {
            "allergies": ["peanuts"],
            "diet_type": "vegetarian",
            "calorie_goal": 2000,
            "meals_per_day": 4
        }
    """
    data = request.get_json(silent=True) or {}
    
    nutrition_data = {
        'allergies': data.get('allergies', []),
        'diet_type': data.get('diet_type', 'standard'),
        'calorie_goal': data.get('calorie_goal', 2000),
        'protein_goal': data.get('protein_goal'),
        'carb_goal': data.get('carb_goal'),
        'fat_goal': data.get('fat_goal'),
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
