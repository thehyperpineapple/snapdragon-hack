"""
Plan Management Blueprint - Diet & Workout plans.
"""

from flask import Blueprint, jsonify, request
from services.plan_service import create_plan, get_plan, update_plan, delete_plan

plan_bp = Blueprint('plan', __name__, url_prefix='/users')


def generate_mock_diet_plan():
    """Mock AI diet plan generator - TODO: Replace with actual AI agent."""
    return [
        {
            'weekName': 'Week 1',
            'meals': {
                'breakfast': {'name': 'Oatmeal with Berries', 'calories': 350, 'completed': False},
                'lunch': {'name': 'Grilled Chicken Salad', 'calories': 450, 'completed': False},
                'dinner': {'name': 'Salmon with Vegetables', 'calories': 500, 'completed': False},
                'snack': {'name': 'Greek Yogurt', 'calories': 200, 'completed': False}
            }
        }
    ]


def generate_mock_workout_plan():
    """Mock AI workout plan generator - TODO: Replace with actual AI agent."""
    return [
        {
            'weekName': 'Week 1',
            'exercises': [
                {'workoutId': 'w1', 'name': 'Push-ups', 'sets': 3, 'reps': 12, 'completed': False},
                {'workoutId': 'w2', 'name': 'Squats', 'sets': 4, 'reps': 12, 'completed': False},
                {'workoutId': 'w3', 'name': 'Planks', 'sets': 3, 'duration': '45s', 'completed': False}
            ]
        }
    ]


@plan_bp.route('/<user_id>/plan', methods=['POST'])
def create_user_plan(user_id):
    """
    Create a new diet/workout plan.
    
    Request Body:
        {
            "plan_type": "combined",  // "diet", "workout", or "combined"
            "duration_weeks": 4,
            "intensity": "moderate"
        }
    """
    data = request.get_json(silent=True) or {}
    
    plan_type = data.get('plan_type', 'combined')
    if plan_type not in ['diet', 'workout', 'combined']:
        return jsonify({'error': 'Invalid plan_type'}), 400
    
    plan_data = {
        'plan_type': plan_type,
        'preferences': {
            'duration_weeks': data.get('duration_weeks', 4),
            'intensity': data.get('intensity', 'moderate'),
            'specific_goals': data.get('specific_goals', [])
        }
    }
    
    # TODO: Replace with actual AI agent calls
    if plan_type in ['diet', 'combined']:
        plan_data['diet'] = generate_mock_diet_plan()
    
    if plan_type in ['workout', 'combined']:
        plan_data['workouts'] = generate_mock_workout_plan()
    
    result, status = create_plan(user_id, plan_data)
    return jsonify(result), status


@plan_bp.route('/<user_id>/plan', methods=['GET'])
def get_user_plan(user_id):
    """Get active plan."""
    result, status = get_plan(user_id)
    return jsonify(result), status


@plan_bp.route('/<user_id>/plan', methods=['PUT'])
def update_user_plan(user_id):
    """Update existing plan."""
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({'error': 'No update data provided'}), 400
    
    result, status = update_plan(user_id, data)
    return jsonify(result), status


@plan_bp.route('/<user_id>/plan', methods=['DELETE'])
def delete_user_plan(user_id):
    """Delete plan."""
    result, status = delete_plan(user_id)
    return jsonify(result), status
