"""
AI-Enhanced Plan Management Blueprint
Uses NPU-accelerated LLM for intelligent plan generation
"""

from flask import Blueprint, jsonify, request
import logging

from services.plan_service import create_plan, get_plan, update_plan, delete_plan
from services.health_service import get_health_data
from services.nutrition_service import get_nutrition_data
from ai import get_npu_engine
from ai.prompts import plan_prompts
from ai.utils.model_utils import format_response, merge_user_context, format_error_response

logger = logging.getLogger(__name__)

plan_ai_bp = Blueprint('plan_ai', __name__, url_prefix='/users')


@plan_ai_bp.route('/<user_id>/plan', methods=['POST'])
def create_user_plan_ai(user_id):
    """
    Create a new diet/workout plan using NPU-accelerated LLM.

    Request Body:
        {
            "plan_type": "combined",  // "diet", "workout", or "combined"
            "duration_weeks": 4,
            "intensity": "moderate",
            "specific_goals": ["goal1", "goal2"],
            "use_ai": true  // Set to false to use mock data
        }
    """
    data = request.get_json(silent=True) or {}

    plan_type = data.get('plan_type', 'combined')
    if plan_type not in ['diet', 'workout', 'combined']:
        return jsonify({'error': 'Invalid plan_type'}), 400

    use_ai = data.get('use_ai', True)

    plan_data = {
        'plan_type': plan_type,
        'preferences': {
            'duration_weeks': data.get('duration_weeks', 4),
            'intensity': data.get('intensity', 'moderate'),
            'specific_goals': data.get('specific_goals', [])
        }
    }

    if use_ai:
        try:
            # Get NPU engine instance
            npu_engine = get_npu_engine()

            # Fetch user context
            health_response, health_status = get_health_data(user_id)
            nutrition_response, nutrition_status = get_nutrition_data(user_id)

            if health_status != 200 or nutrition_status != 200:
                logger.warning(f"Incomplete user data for {user_id}, using available data")

            user_context = merge_user_context(
                health_response.get('profile', {}),
                nutrition_response.get('nutrition', {}),
                plan_data
            )

            # Generate prompt
            prompt = plan_prompts.generate_plan_prompt(user_context, plan_type)

            logger.info(f"Generating {plan_type} plan for user {user_id} using NPU")

            # Generate plan using NPU
            raw_output = npu_engine.generate(
                prompt=prompt,
                max_new_tokens=1500,  # Allow longer responses for full plans
                temperature=0.7,
                use_cache=True
            )

            # Parse and format response
            formatted = format_response(raw_output, expected_format="json")

            if not formatted['success']:
                logger.error(f"Failed to parse AI response: {formatted['error']}")
                return jsonify({
                    'error': 'AI generation failed',
                    'details': formatted['error'],
                    'raw_output': formatted['raw_output']
                }), 500

            ai_plan = formatted['data']

            # Extract diet and workout plans
            if 'diet' in ai_plan:
                plan_data['diet'] = ai_plan['diet']
            if 'workouts' in ai_plan:
                plan_data['workouts'] = ai_plan['workouts']

            # Add AI metadata
            plan_data['ai_generated'] = True
            plan_data['generation_method'] = 'npu_llm'

        except Exception as e:
            logger.error(f"AI generation error: {e}", exc_info=True)
            return jsonify(format_error_response(e, "AI plan generation")), 500
    else:
        # Fallback to mock data
        if plan_type in ['diet', 'combined']:
            plan_data['diet'] = generate_mock_diet_plan()
        if plan_type in ['workout', 'combined']:
            plan_data['workouts'] = generate_mock_workout_plan()
        plan_data['ai_generated'] = False

    # Save plan to database
    result, status = create_plan(user_id, plan_data)
    return jsonify(result), status


@plan_ai_bp.route('/<user_id>/plan/validate', methods=['POST'])
def validate_user_plan(user_id):
    """
    Validate and analyze an existing plan using AI.

    Returns insights and recommendations.
    """
    try:
        # Get current plan
        plan_response, plan_status = get_plan(user_id)
        if plan_status != 200:
            return jsonify({'error': 'No plan found'}), 404

        # Get user context
        health_response, _ = get_health_data(user_id)
        nutrition_response, _ = get_nutrition_data(user_id)

        user_context = merge_user_context(
            health_response.get('profile', {}),
            nutrition_response.get('nutrition', {})
        )

        # Generate validation prompt
        prompt = plan_prompts.validate_plan_prompt(
            user_context,
            plan_response.get('plan', {})
        )

        # Get AI analysis
        npu_engine = get_npu_engine()
        raw_output = npu_engine.generate(
            prompt=prompt,
            max_new_tokens=800,
            temperature=0.5,  # Lower temp for more consistent analysis
            use_cache=True
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            return jsonify({
                'error': 'Analysis failed',
                'details': formatted['error']
            }), 500

        return jsonify({
            'validation': formatted['data'],
            'timestamp': 'now'
        }), 200

    except Exception as e:
        logger.error(f"Plan validation error: {e}", exc_info=True)
        return jsonify(format_error_response(e, "plan validation")), 500


@plan_ai_bp.route('/<user_id>/plan/adjust', methods=['PUT'])
def adjust_user_plan(user_id):
    """
    Adjust existing plan based on user feedback using AI.

    Request Body:
        {
            "adjustment_request": "Make workouts less intense",
            "user_feedback": "The current plan is too hard"
        }
    """
    data = request.get_json(silent=True) or {}

    if not data.get('adjustment_request'):
        return jsonify({'error': 'adjustment_request required'}), 400

    try:
        # Get current plan
        plan_response, plan_status = get_plan(user_id)
        if plan_status != 200:
            return jsonify({'error': 'No plan found'}), 404

        current_plan = plan_response.get('plan', {})

        # Generate adjustment prompt
        prompt = plan_prompts.adjust_plan_prompt(
            current_plan,
            data['adjustment_request'],
            data.get('user_feedback', '')
        )

        # Get AI adjustments
        npu_engine = get_npu_engine()
        raw_output = npu_engine.generate(
            prompt=prompt,
            max_new_tokens=1500,
            temperature=0.7,
            use_cache=False  # Don't cache adjustments
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            return jsonify({
                'error': 'Adjustment failed',
                'details': formatted['error']
            }), 500

        adjusted_plan = formatted['data']

        # Update plan in database
        result, status = update_plan(user_id, adjusted_plan)
        return jsonify(result), status

    except Exception as e:
        logger.error(f"Plan adjustment error: {e}", exc_info=True)
        return jsonify(format_error_response(e, "plan adjustment")), 500


# Fallback mock functions (for non-AI mode)
def generate_mock_diet_plan():
    """Mock diet plan generator."""
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
    """Mock workout plan generator."""
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
