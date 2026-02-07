"""
AI-Enhanced User Profile Management
Provides intelligent health and nutrition insights
"""

from flask import Blueprint, jsonify, request
import logging
import time

from services.health_service import (
    post_health_data, get_health_data, patch_health_data, delete_health_data
)
from services.nutrition_service import (
    post_nutrition_data, get_nutrition_data, update_nutrition_data, delete_nutrition_data
)
from ai import get_npu_engine
from ai.prompts import health_prompts, nutrition_prompts
from ai.utils.model_utils import format_response, format_error_response

logger = logging.getLogger(__name__)
ai_logger = logging.getLogger('ai')

user_ai_bp = Blueprint('user_ai', __name__, url_prefix='/users')


# ============================================================================
# AI-Enhanced Health Profile Endpoints
# ============================================================================

@user_ai_bp.route('/<user_id>/health', methods=['PUT'])
def update_health_profile_ai(user_id):
    """
    Update health profile with AI-powered insights and recommendations.

    Request Body:
        {
            "weight": 75.5,
            "height": 180,
            "generate_insights": true  // Set to true for AI analysis
        }
    """
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({'error': 'No update data provided'}), 400

    generate_insights = data.pop('generate_insights', False)

    # Recalculate BMI if needed
    if 'weight' in data or 'height' in data:
        current, status = get_health_data(user_id)
        if status == 200:
            profile = current.get('profile', {})
            weight = float(data.get('weight', profile.get('weight', 0)))
            height = float(data.get('height', profile.get('height', 1)))
            if height > 0:
                data['bmi'] = round(weight / ((height / 100) ** 2), 2)

    # Update health data
    result, status = patch_health_data(user_id, data)

    if status != 200:
        return jsonify(result), status

    # Generate AI insights if requested
    if generate_insights:
        ai_logger.info(f"AI_INFERENCE: Starting health insights generation for userId={user_id}")
        start_time = time.time()
        try:
            # Get updated health data
            health_response, _ = get_health_data(user_id)
            health_data = health_response.get('profile', {})

            # Generate prompt
            prompt = health_prompts.analyze_health_metrics_prompt(health_data)

            # Get AI analysis
            npu_engine = get_npu_engine()
            raw_output = npu_engine.generate(
                prompt=prompt,
                max_new_tokens=800,
                temperature=0.6,
                use_cache=True
            )

            formatted = format_response(raw_output, expected_format="json")

            if formatted['success']:
                result['ai_insights'] = formatted['data']
                elapsed_time = time.time() - start_time
                ai_logger.info(f"AI_INFERENCE: Health insights generated successfully for userId={user_id}, elapsed_time={elapsed_time:.2f}s")
            else:
                ai_logger.error(f"AI_INFERENCE: Health insights generation failed for userId={user_id}")
                result['ai_insights'] = {
                    'error': 'Failed to generate insights',
                    'details': formatted['error']
                }

        except Exception as e:
            ai_logger.error(f"AI_INFERENCE: Health insights error for userId={user_id}: {e}", exc_info=True)
            result['ai_insights'] = {
                'error': str(e)
            }
    else:
        ai_logger.info(f"AI_NON_USAGE: Health insights not requested for userId={user_id}")

    return jsonify(result), 200


@user_ai_bp.route('/<user_id>/health/analyze', methods=['GET'])
def analyze_health_metrics(user_id):
    """
    Get comprehensive AI analysis of health metrics.
    """
    ai_logger.info(f"AI_INFERENCE: Starting comprehensive health analysis for userId={user_id}")
    start_time = time.time()
    try:
        # Get health data
        health_response, status = get_health_data(user_id)
        if status != 200:
            ai_logger.warning(f"AI_INFERENCE: Health profile not found for userId={user_id}")
            return jsonify({'error': 'Health profile not found'}), 404

        health_data = health_response.get('profile', {})

        # Generate prompt
        prompt = health_prompts.analyze_health_metrics_prompt(health_data)

        # Get AI analysis
        npu_engine = get_npu_engine()
        raw_output = npu_engine.generate(
            prompt=prompt,
            max_new_tokens=1000,
            temperature=0.5,
            use_cache=True
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Health analysis failed for userId={user_id}")
            return jsonify({
                'error': 'Analysis failed',
                'details': formatted['error']
            }), 500

        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Health analysis completed for userId={user_id}, elapsed_time={elapsed_time:.2f}s")

        return jsonify({
            'analysis': formatted['data'],
            'user_id': user_id
        }), 200

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Health analysis error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "health analysis")), 500


# ============================================================================
# AI-Enhanced Nutrition Profile Endpoints
# ============================================================================

@user_ai_bp.route('/<user_id>/nutrition', methods=['PUT'])
def update_nutrition_profile_ai(user_id):
    """
    Update nutrition profile with AI recommendations.

    Request Body:
        {
            "calorie_goal": 2000,
            "diet_type": "vegetarian",
            "generate_recommendations": true  // Set to true for AI analysis
        }
    """
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({'error': 'No update data provided'}), 400

    generate_recommendations = data.pop('generate_recommendations', False)

    # Update nutrition data
    result, status = update_nutrition_data(user_id, data)

    if status != 200:
        return jsonify(result), status

    # Generate AI recommendations if requested
    if generate_recommendations:
        ai_logger.info(f"AI_INFERENCE: Starting nutrition recommendations generation for userId={user_id}")
        start_time = time.time()
        try:
            # Get updated data
            nutrition_response, _ = get_nutrition_data(user_id)
            health_response, _ = get_health_data(user_id)

            nutrition_data = nutrition_response.get('nutrition', {})
            health_data = health_response.get('profile', {})

            # Generate prompt
            prompt = nutrition_prompts.analyze_nutrition_profile_prompt(
                nutrition_data,
                health_data
            )

            # Get AI analysis
            npu_engine = get_npu_engine()
            raw_output = npu_engine.generate(
                prompt=prompt,
                max_new_tokens=1000,
                temperature=0.6,
                use_cache=True
            )

            formatted = format_response(raw_output, expected_format="json")

            if formatted['success']:
                result['ai_recommendations'] = formatted['data']
                elapsed_time = time.time() - start_time
                ai_logger.info(f"AI_INFERENCE: Nutrition recommendations generated for userId={user_id}, elapsed_time={elapsed_time:.2f}s")
            else:
                ai_logger.error(f"AI_INFERENCE: Nutrition recommendations generation failed for userId={user_id}")
                result['ai_recommendations'] = {
                    'error': 'Failed to generate recommendations',
                    'details': formatted['error']
                }

        except Exception as e:
            ai_logger.error(f"AI_INFERENCE: Nutrition recommendations error for userId={user_id}: {e}", exc_info=True)
            result['ai_recommendations'] = {
                'error': str(e)
            }
    else:
        ai_logger.info(f"AI_NON_USAGE: Nutrition recommendations not requested for userId={user_id}")

    return jsonify(result), 200


@user_ai_bp.route('/<user_id>/nutrition/analyze', methods=['GET'])
def analyze_nutrition_profile(user_id):
    """
    Get comprehensive AI analysis of nutrition profile.
    """
    ai_logger.info(f"AI_INFERENCE: Starting comprehensive nutrition analysis for userId={user_id}")
    start_time = time.time()
    try:
        # Get nutrition and health data
        nutrition_response, nutrition_status = get_nutrition_data(user_id)
        health_response, health_status = get_health_data(user_id)

        if nutrition_status != 200:
            ai_logger.warning(f"AI_INFERENCE: Nutrition profile not found for userId={user_id}")
            return jsonify({'error': 'Nutrition profile not found'}), 404

        nutrition_data = nutrition_response.get('nutrition', {})
        health_data = health_response.get('profile', {}) if health_status == 200 else {}

        # Generate prompt
        prompt = nutrition_prompts.analyze_nutrition_profile_prompt(
            nutrition_data,
            health_data
        )

        # Get AI analysis
        npu_engine = get_npu_engine()
        raw_output = npu_engine.generate(
            prompt=prompt,
            max_new_tokens=1200,
            temperature=0.5,
            use_cache=True
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Nutrition analysis failed for userId={user_id}")
            return jsonify({
                'error': 'Analysis failed',
                'details': formatted['error']
            }), 500

        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Nutrition analysis completed for userId={user_id}, elapsed_time={elapsed_time:.2f}s")

        return jsonify({
            'analysis': formatted['data'],
            'user_id': user_id
        }), 200

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Nutrition analysis error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "nutrition analysis")), 500


@user_ai_bp.route('/<user_id>/nutrition/meal-suggestions', methods=['POST'])
def get_meal_suggestions(user_id):
    """
    Get AI-powered meal suggestions based on remaining macros.

    Request Body:
        {
            "meal_type": "dinner",
            "remaining_macros": {
                "calories": 600,
                "protein": 40,
                "carbs": 60,
                "fats": 20
            }
        }
    """
    data = request.get_json(silent=True) or {}

    meal_type = data.get('meal_type')
    remaining_macros = data.get('remaining_macros', {})

    if not meal_type:
        return jsonify({'error': 'meal_type required'}), 400

    ai_logger.info(f"AI_INFERENCE: Starting meal suggestions generation for userId={user_id}, meal_type={meal_type}")
    start_time = time.time()

    try:
        # Get nutrition preferences
        nutrition_response, status = get_nutrition_data(user_id)
        if status != 200:
            ai_logger.warning(f"AI_INFERENCE: Nutrition profile not found for userId={user_id}")
            return jsonify({'error': 'Nutrition profile not found'}), 404

        nutrition_prefs = nutrition_response.get('nutrition', {})

        # Generate prompt
        prompt = nutrition_prompts.generate_meal_suggestions_prompt(
            nutrition_prefs,
            remaining_macros,
            meal_type
        )

        # Get AI suggestions
        npu_engine = get_npu_engine()
        raw_output = npu_engine.generate(
            prompt=prompt,
            max_new_tokens=800,
            temperature=0.8,  # Higher temp for variety
            use_cache=False  # Don't cache meal suggestions
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Meal suggestions generation failed for userId={user_id}")
            return jsonify({
                'error': 'Suggestions failed',
                'details': formatted['error']
            }), 500

        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Meal suggestions generated for userId={user_id}, elapsed_time={elapsed_time:.2f}s")

        return jsonify(formatted['data']), 200

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Meal suggestions error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "meal suggestions")), 500
