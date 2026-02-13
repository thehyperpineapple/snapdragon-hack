"""
Fitness and Nutrition agent routes.
- Fitness agent: generates workouts from activity_level, fitness_goal, workout_duration, workout_days.
- Nutrition agent: generates food plan from dietary_restrictions and caloric_intake.
"""

from flask import Blueprint, jsonify, request
import logging
import time

from ai import get_gemini_engine
from ai.prompts import agents_prompts
from ai.utils.model_utils import format_response, format_error_response

logger = logging.getLogger(__name__)
ai_logger = logging.getLogger('ai')

agents_bp = Blueprint('agents', __name__, url_prefix='/users')


@agents_bp.route('/<user_id>/agents/fitness', methods=['POST'])
def fitness_agent(user_id):
    """
    Fitness agent: generate workouts based on activity level, fitness goal,
    workout duration, and workout days per week.

    Request Body:
        {
            "activity_level": "moderate",   // e.g. sedentary, light, moderate, active, very_active
            "fitness_goal": "weight_loss",  // e.g. weight_loss, muscle_gain, endurance, general
            "workout_duration": 45,          // minutes per session
            "workout_days": 4               // days per week
        }
    """
    data = request.get_json(silent=True) or {}

    activity_level = data.get('activity_level') or 'moderate'
    fitness_goal = data.get('fitness_goal') or 'general health'
    workout_duration = data.get('workout_duration')
    workout_days = data.get('workout_days')

    if workout_duration is None:
        workout_duration = 45
    else:
        try:
            workout_duration = int(workout_duration)
            workout_duration = max(10, min(120, workout_duration))
        except (TypeError, ValueError):
            workout_duration = 45

    if workout_days is None:
        workout_days = 3
    else:
        try:
            workout_days = int(workout_days)
            workout_days = max(1, min(7, workout_days))
        except (TypeError, ValueError):
            workout_days = 3

    ai_logger.info(
        f"AI_INFERENCE: Fitness agent userId={user_id} "
        f"activity={activity_level} goal={fitness_goal} duration={workout_duration} days={workout_days}"
    )
    start_time = time.time()

    try:
        prompt = agents_prompts.fitness_agent_prompt(
            activity_level=str(activity_level),
            fitness_goal=str(fitness_goal),
            workout_duration=workout_duration,
            workout_days=workout_days
        )
        ai_engine = get_gemini_engine()
        raw_output = ai_engine.generate(
            prompt=prompt,
            max_new_tokens=1500,
            temperature=0.6,
            use_cache=False
        )
        formatted = format_response(raw_output, expected_format='json')

        if not formatted['success']:
            ai_logger.warning(f"AI_INFERENCE: Fitness agent parse failed for userId={user_id}")
            return jsonify({
                'error': 'Failed to generate workouts',
                'details': formatted.get('error', 'Invalid response format')
            }), 500

        result = formatted['data']
        if 'workouts' not in result:
            result = {'workouts': result.get('workouts', [])}

        elapsed = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Fitness agent completed for userId={user_id}, elapsed={elapsed:.2f}s")

        return jsonify({
            'user_id': user_id,
            'workouts': result['workouts'],
            'generation_time_seconds': round(elapsed, 2)
        }), 200

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Fitness agent error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, 'fitness agent')), 500


@agents_bp.route('/<user_id>/agents/nutrition', methods=['POST'])
def nutrition_agent(user_id):
    """
    Nutrition agent: generate food plan based on dietary restrictions and
    daily caloric intake.

    Request Body:
        {
            "dietary_restrictions": ["vegetarian", "no nuts"],  // or string
            "caloric_intake": 2000   // kcal per day
        }
    """
    data = request.get_json(silent=True) or {}

    dietary_restrictions = data.get('dietary_restrictions')
    if dietary_restrictions is None:
        dietary_restrictions = []
    if isinstance(dietary_restrictions, str):
        dietary_restrictions = [dietary_restrictions] if dietary_restrictions.strip() else []

    caloric_intake = data.get('caloric_intake')
    if caloric_intake is None:
        caloric_intake = 2000
    else:
        try:
            caloric_intake = int(caloric_intake)
            caloric_intake = max(800, min(5000, caloric_intake))
        except (TypeError, ValueError):
            caloric_intake = 2000

    ai_logger.info(
        f"AI_INFERENCE: Nutrition agent userId={user_id} "
        f"restrictions={dietary_restrictions} calories={caloric_intake}"
    )
    start_time = time.time()

    try:
        prompt = agents_prompts.nutrition_agent_prompt(
            dietary_restrictions=dietary_restrictions,
            caloric_intake=caloric_intake
        )
        ai_engine = get_gemini_engine()
        raw_output = ai_engine.generate(
            prompt=prompt,
            max_new_tokens=1200,
            temperature=0.5,
            use_cache=False
        )
        formatted = format_response(raw_output, expected_format='json')

        if not formatted['success']:
            ai_logger.warning(f"AI_INFERENCE: Nutrition agent parse failed for userId={user_id}")
            return jsonify({
                'error': 'Failed to generate food plan',
                'details': formatted.get('error', 'Invalid response format')
            }), 500

        result = formatted['data']
        if 'food_plan' not in result:
            result = {'food_plan': result.get('food_plan', []), 'daily_total_calories': caloric_intake}

        elapsed = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Nutrition agent completed for userId={user_id}, elapsed={elapsed:.2f}s")

        return jsonify({
            'user_id': user_id,
            'food_plan': result.get('food_plan', []),
            'daily_total_calories': result.get('daily_total_calories', caloric_intake),
            'generation_time_seconds': round(elapsed, 2)
        }), 200

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Nutrition agent error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, 'nutrition agent')), 500
