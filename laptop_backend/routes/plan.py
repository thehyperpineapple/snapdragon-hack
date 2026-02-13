"""
AI-Enhanced Plan Management Blueprint
Uses Gemini LLM for intelligent plan generation
"""

from flask import Blueprint, jsonify, request
import logging
import time
from dotenv import load_dotenv
load_dotenv()

from services.plan_service import create_plan, get_plan, update_plan, delete_plan
from services.health_service import get_health_data
from services.nutrition_service import get_nutrition_data
from ai import get_gemini_engine
from ai.prompts import plan_prompts
from ai.utils.model_utils import format_response, merge_user_context, format_error_response

logger = logging.getLogger(__name__)
ai_logger = logging.getLogger('ai')

plan_bp = Blueprint('plan', __name__, url_prefix='/users')


@plan_bp.route('/<user_id>/plan', methods=['POST'])
def create_user_plan_ai(user_id):
    """
    Create a new diet/workout plan using Gemini LLM.

    Request Body:
        {
            "plan_type": "combined",  // "diet", "workout", or "combined"
            "duration_weeks": 2,      // Keep low (1-2) for faster AI generation
            "intensity": "moderate",
            "specific_goals": ["goal1", "goal2"],
            "use_ai": true  // Set to false to use mock data (faster)
        }
    """
    data = request.get_json(silent=True) or {}

    plan_type = data.get('plan_type', 'combined')
    if plan_type not in ['diet', 'workout', 'combined']:
        return jsonify({'error': 'Invalid plan_type'}), 400

    use_ai = data.get('use_ai', True)
    duration_weeks = min(data.get('duration_weeks', 2), 4)  # Cap at 4 weeks for performance

    plan_data = {
        'plan_type': plan_type,
        'preferences': {
            'duration_weeks': duration_weeks,
            'intensity': data.get('intensity', 'moderate'),
            'specific_goals': data.get('specific_goals', [])
        }
    }

    if use_ai:
        try:
            ai_logger.info(f"AI_USAGE: Initiating AI plan generation - userId={user_id}, plan_type={plan_type}, weeks={duration_weeks}")
            start_time = time.time()

            # Get Gemini engine instance
            ai_engine = get_gemini_engine()

            # Fetch user context
            health_response, health_status = get_health_data(user_id)
            nutrition_response, nutrition_status = get_nutrition_data(user_id)

            if health_status != 200 or nutrition_status != 200:
                ai_logger.warning(f"AI_USAGE: Incomplete user data for userId={user_id}, using available data")

            user_context = merge_user_context(
                health_response.get('profile', {}),
                nutrition_response.get('nutrition', {}),
                plan_data
            )

            # Generate prompt
            prompt = plan_prompts.generate_plan_prompt(user_context, plan_type)

            ai_logger.info(f"AI_INFERENCE: Starting inference for userId={user_id}, plan_type={plan_type}")

            # Generate plan using Gemini
            raw_output = ai_engine.generate(
                prompt=prompt,
                max_new_tokens=1000,  # Reduced for faster response
                temperature=0.7,
                use_cache=True
            )

            # Parse and format response
            formatted = format_response(raw_output, expected_format="json")

            if not formatted['success']:
                ai_logger.warning(f"AI_INFERENCE: Failed to parse AI response for userId={user_id}, falling back to mock data")
                # Fallback to mock data if AI output is invalid
                if plan_type in ['diet', 'combined']:
                    plan_data['diet'] = generate_mock_diet_plan(duration_weeks)
                if plan_type in ['workout', 'combined']:
                    plan_data['workouts'] = generate_mock_workout_plan(duration_weeks)
                plan_data['ai_generated'] = False
                plan_data['fallback_reason'] = 'AI output parsing failed'
            else:
                ai_plan = formatted['data']
                elapsed_time = time.time() - start_time

                # Extract diet and workout plans
                if 'diet' in ai_plan:
                    plan_data['diet'] = ai_plan['diet']
                if 'workouts' in ai_plan:
                    plan_data['workouts'] = ai_plan['workouts']

                # Add AI metadata
                plan_data['ai_generated'] = True
                plan_data['generation_method'] = 'gemini'
                plan_data['generation_time'] = f"{elapsed_time:.2f}s"

                ai_logger.info(f"AI_INFERENCE: Inference completed successfully for userId={user_id}, elapsed_time={elapsed_time:.2f}s")

        except Exception as e:
            ai_logger.error(f"AI_INFERENCE: AI generation error for userId={user_id}: {e}, falling back to mock data")
            # Fallback to mock data on any error (including timeout)
            if plan_type in ['diet', 'combined']:
                plan_data['diet'] = generate_mock_diet_plan(duration_weeks)
            if plan_type in ['workout', 'combined']:
                plan_data['workouts'] = generate_mock_workout_plan(duration_weeks)
            plan_data['ai_generated'] = False
            plan_data['fallback_reason'] = str(e)
    else:
        ai_logger.info(f"AI_NON_USAGE: Mock data used instead of AI for userId={user_id}, plan_type={plan_type}")
        # Fallback to mock data
        duration_weeks = data.get('duration_weeks', 4)
        if plan_type in ['diet', 'combined']:
            plan_data['diet'] = generate_mock_diet_plan(duration_weeks)
        if plan_type in ['workout', 'combined']:
            plan_data['workouts'] = generate_mock_workout_plan(duration_weeks)
        plan_data['ai_generated'] = False

    # Save plan to database
    result, status = create_plan(user_id, plan_data)
    return jsonify(result), status


@plan_bp.route('/<user_id>/plan', methods=['GET'])
def get_user_plan(user_id):
    """Get existing plan for a user."""
    result, status = get_plan(user_id)
    return jsonify(result), status


@plan_bp.route('/<user_id>/plan', methods=['DELETE'])
def delete_user_plan(user_id):
    """Delete user's plan."""
    result, status = delete_plan(user_id)
    return jsonify(result), status


@plan_bp.route('/<user_id>/plan/validate', methods=['POST'])
def validate_user_plan(user_id):
    """
    Validate and analyze an existing plan using AI.

    Returns insights and recommendations.
    """
    ai_logger.info(f"AI_INFERENCE: Starting plan validation for userId={user_id}")
    start_time = time.time()
    try:
        # Get current plan
        plan_response, plan_status = get_plan(user_id)
        if plan_status != 200:
            ai_logger.warning(f"AI_INFERENCE: No plan found for validation - userId={user_id}")
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
        ai_engine = get_gemini_engine()
        raw_output = ai_engine.generate(
            prompt=prompt,
            max_new_tokens=800,
            temperature=0.5,  # Lower temp for more consistent analysis
            use_cache=True
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Plan validation analysis failed for userId={user_id}")
            return jsonify({
                'error': 'Analysis failed',
                'details': formatted['error']
            }), 500

        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Plan validation completed for userId={user_id}, elapsed_time={elapsed_time:.2f}s")

        return jsonify({
            'validation': formatted['data'],
            'timestamp': 'now'
        }), 200

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Plan validation error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "plan validation")), 500


@plan_bp.route('/<user_id>/plan/adjust', methods=['PUT'])
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

    ai_logger.info(f"AI_INFERENCE: Starting plan adjustment for userId={user_id}, request={data.get('adjustment_request')}")
    start_time = time.time()

    try:
        # Get current plan
        plan_response, plan_status = get_plan(user_id)
        if plan_status != 200:
            ai_logger.warning(f"AI_INFERENCE: No plan found for adjustment - userId={user_id}")
            return jsonify({'error': 'No plan found'}), 404

        current_plan = plan_response.get('plan', {})

        # Generate adjustment prompt
        prompt = plan_prompts.adjust_plan_prompt(
            current_plan,
            data['adjustment_request'],
            data.get('user_feedback', '')
        )

        # Get AI adjustments
        ai_engine = get_gemini_engine()
        raw_output = ai_engine.generate(
            prompt=prompt,
            max_new_tokens=1500,
            temperature=0.7,
            use_cache=False  # Don't cache adjustments
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Plan adjustment failed for userId={user_id}")
            return jsonify({
                'error': 'Adjustment failed',
                'details': formatted['error']
            }), 500

        adjusted_plan = formatted['data']
        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Plan adjustment completed for userId={user_id}, elapsed_time={elapsed_time:.2f}s")

        # Update plan in database
        result, status = update_plan(user_id, adjusted_plan)
        return jsonify(result), status

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Plan adjustment error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "plan adjustment")), 500


@plan_bp.route('/<user_id>/plan/workout/adjust', methods=['PUT'])
def adjust_workout_plan(user_id):
    """
    Adjust workout plan for current week when user skips exercises.

    Request Body:
        {
            "week_name": "Week 1",
            "skipped_workouts": ["w1", "w2"],  // workout IDs
            "reason": "Too intense" // Optional user feedback
        }
    """
    data = request.get_json(silent=True) or {}

    # Validate required fields
    week_name = data.get('week_name')
    skipped_workouts = data.get('skipped_workouts', [])
    reason = data.get('reason', '')

    if not week_name:
        return jsonify({'error': 'week_name is required'}), 400

    if not skipped_workouts or not isinstance(skipped_workouts, list):
        return jsonify({'error': 'skipped_workouts array is required'}), 400

    ai_logger.info(f"AI_INFERENCE: Starting workout adjustment for userId={user_id}, week={week_name}, skipped={len(skipped_workouts)}")
    start_time = time.time()

    try:
        # Get current plan
        plan_response, plan_status = get_plan(user_id)
        if plan_status != 200:
            ai_logger.warning(f"AI_INFERENCE: No plan found for adjustment - userId={user_id}")
            return jsonify({'error': 'No plan found'}), 404

        current_plan = plan_response.get('plan', {})
        workouts = current_plan.get('workouts', [])

        # Find current week (support both 'week' and 'weekName' keys)
        current_week = None
        for week in workouts:
            if week.get('week') == week_name or week.get('weekName') == week_name:
                current_week = week
                break

        if not current_week:
            ai_logger.warning(f"AI_INFERENCE: Week {week_name} not found - userId={user_id}")
            return jsonify({'error': f'Week {week_name} not found'}), 404

        # Get remaining exercises (not yet completed)
        all_exercises = current_week.get('exercises', [])
        remaining_exercises = [ex for ex in all_exercises if not ex.get('completed', False)]

        if not remaining_exercises:
            ai_logger.info(f"AI_INFERENCE: No remaining exercises for week {week_name} - userId={user_id}")
            return jsonify({'message': 'No remaining exercises to adjust'}), 200

        # Get user health context
        health_response, _ = get_health_data(user_id)
        user_context = health_response.get('profile', {})

        # Generate adjustment prompt
        prompt = plan_prompts.adjust_workout_week_prompt(
            {'profile': user_context},
            current_week,
            skipped_workouts,
            remaining_exercises,
            reason
        )

        # Get AI adjustments
        ai_engine = get_gemini_engine()
        raw_output = ai_engine.generate(
            prompt=prompt,
            max_new_tokens=800,
            temperature=0.7,
            use_cache=False  # Don't cache adjustments
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Workout adjustment failed for userId={user_id}")
            return jsonify({
                'error': 'Adjustment failed',
                'details': formatted['error']
            }), 500

        adjusted_data = formatted['data']
        adjusted_exercises = adjusted_data.get('exercises', [])

        if not adjusted_exercises:
            ai_logger.error(f"AI_INFERENCE: No exercises returned from AI - userId={user_id}")
            return jsonify({'error': 'AI did not return valid exercises'}), 500

        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Workout adjustment completed for userId={user_id}, week={week_name}, elapsed_time={elapsed_time:.2f}s")

        # Update plan with new exercises for this week
        from services.plan_service import update_week_workouts
        result, status = update_week_workouts(user_id, week_name, adjusted_exercises)

        if status == 200:
            return jsonify({
                'message': f'Workout plan adjusted for {week_name}',
                'week': week_name,
                'adjusted_count': len(adjusted_exercises),
                'adjusted_exercises': adjusted_exercises
            }), 200
        else:
            return jsonify(result), status

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Workout adjustment error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "workout adjustment")), 500


@plan_bp.route('/<user_id>/plan/nutrition/adjust', methods=['PUT'])
def adjust_nutrition_plan(user_id):
    """
    Adjust nutrition plan for current week when user eats extra calories.

    Request Body:
        {
            "week_name": "Week 1",
            "extra_calories": 500,
            "day_of_week": 2,  // Which day (0-6) they ate extra
            "notes": "Had extra snacks" // Optional
        }
    """
    data = request.get_json(silent=True) or {}

    # Validate required fields
    week_name = data.get('week_name')
    extra_calories = data.get('extra_calories')
    day_of_week = data.get('day_of_week')
    notes = data.get('notes', '')

    if not week_name:
        return jsonify({'error': 'week_name is required'}), 400

    if extra_calories is None or extra_calories <= 0:
        return jsonify({'error': 'extra_calories must be a positive number'}), 400

    if day_of_week is None or not (0 <= day_of_week <= 6):
        return jsonify({'error': 'day_of_week must be 0-6'}), 400

    ai_logger.info(f"AI_INFERENCE: Starting nutrition adjustment for userId={user_id}, week={week_name}, extra_calories={extra_calories}")
    start_time = time.time()

    try:
        # Get current plan
        plan_response, plan_status = get_plan(user_id)
        if plan_status != 200:
            ai_logger.warning(f"AI_INFERENCE: No plan found for adjustment - userId={user_id}")
            return jsonify({'error': 'No plan found'}), 404

        current_plan = plan_response.get('plan', {})
        diet = current_plan.get('diet', [])

        # Find current week (support both 'week' and 'weekName' keys)
        current_week = None
        for week in diet:
            if week.get('week') == week_name or week.get('weekName') == week_name:
                current_week = week
                break

        if not current_week:
            ai_logger.warning(f"AI_INFERENCE: Week {week_name} not found - userId={user_id}")
            return jsonify({'error': f'Week {week_name} not found'}), 404

        # Check if there are remaining days to adjust
        if day_of_week >= 6:
            ai_logger.info(f"AI_INFERENCE: No remaining days to adjust (last day of week) - userId={user_id}")
            return jsonify({'message': 'No remaining days in week to adjust'}), 200

        # Get remaining meals (days after the surplus day)
        remaining_days = []
        days_of_week = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
        for i in range(day_of_week + 1, 7):
            remaining_days.append(f"Day {i} ({days_of_week[i]})")

        # Get user nutrition context
        nutrition_response, _ = get_nutrition_data(user_id)
        user_context = {
            'nutrition': nutrition_response.get('nutrition', {})
        }

        # Generate adjustment prompt
        prompt = plan_prompts.adjust_nutrition_week_prompt(
            user_context,
            current_week,
            extra_calories,
            day_of_week,
            remaining_days,
            notes
        )

        # Get AI adjustments
        ai_engine = get_gemini_engine()
        raw_output = ai_engine.generate(
            prompt=prompt,
            max_new_tokens=1000,
            temperature=0.7,
            use_cache=False  # Don't cache adjustments
        )

        formatted = format_response(raw_output, expected_format="json")

        if not formatted['success']:
            ai_logger.error(f"AI_INFERENCE: Nutrition adjustment failed for userId={user_id}")
            return jsonify({
                'error': 'Adjustment failed',
                'details': formatted['error']
            }), 500

        adjusted_data = formatted['data']
        adjusted_meals = adjusted_data.get('adjusted_meals', {})

        if not adjusted_meals:
            ai_logger.error(f"AI_INFERENCE: No meals returned from AI - userId={user_id}")
            return jsonify({'error': 'AI did not return valid meals'}), 500

        elapsed_time = time.time() - start_time
        ai_logger.info(f"AI_INFERENCE: Nutrition adjustment completed for userId={user_id}, week={week_name}, elapsed_time={elapsed_time:.2f}s")

        # Update current week's meals with adjusted meals
        updated_meals = current_week.get('meals', {})
        updated_meals.update(adjusted_meals)

        # Update plan with new meals for this week
        from services.plan_service import update_week_meals
        result, status = update_week_meals(user_id, week_name, updated_meals)

        if status == 200:
            return jsonify({
                'message': f'Nutrition plan adjusted for {week_name}',
                'week': week_name,
                'calorie_adjustment': -extra_calories,
                'adjusted_meals': adjusted_meals
            }), 200
        else:
            return jsonify(result), status

    except Exception as e:
        ai_logger.error(f"AI_INFERENCE: Nutrition adjustment error for userId={user_id}: {e}", exc_info=True)
        return jsonify(format_error_response(e, "nutrition adjustment")), 500


# Fallback mock functions (for non-AI mode)
def generate_mock_diet_plan(weeks=4):
    """Mock diet plan generator matching Nutrition Architect format."""
    diet_plans = [
        {
            'week': 'Week 1',
            'breakfast': {
                'dishName': 'Oatmeal with Fresh Berries and Almonds',
                'calories': '350',
                'carbs': '52g',
                'fats': '10g',
                'protein': '12g',
                'completed': False
            },
            'lunch': {
                'dishName': 'Grilled Chicken Caesar Salad',
                'calories': '450',
                'carbs': '20g',
                'fats': '22g',
                'protein': '42g',
                'completed': False
            },
            'dinner': {
                'dishName': 'Baked Salmon with Roasted Vegetables',
                'calories': '520',
                'carbs': '28g',
                'fats': '24g',
                'protein': '45g',
                'completed': False
            }
        },
        {
            'week': 'Week 2',
            'breakfast': {
                'dishName': 'Greek Yogurt Parfait with Granola and Honey',
                'calories': '380',
                'carbs': '48g',
                'fats': '12g',
                'protein': '20g',
                'completed': False
            },
            'lunch': {
                'dishName': 'Quinoa Buddha Bowl with Chickpeas',
                'calories': '480',
                'carbs': '58g',
                'fats': '16g',
                'protein': '22g',
                'completed': False
            },
            'dinner': {
                'dishName': 'Turkey Stir-Fry with Brown Rice',
                'calories': '490',
                'carbs': '45g',
                'fats': '14g',
                'protein': '48g',
                'completed': False
            }
        },
        {
            'week': 'Week 3',
            'breakfast': {
                'dishName': 'Veggie Omelette with Whole Wheat Toast',
                'calories': '420',
                'carbs': '32g',
                'fats': '22g',
                'protein': '28g',
                'completed': False
            },
            'lunch': {
                'dishName': 'Mediterranean Wrap with Hummus',
                'calories': '440',
                'carbs': '42g',
                'fats': '18g',
                'protein': '24g',
                'completed': False
            },
            'dinner': {
                'dishName': 'Grilled Chicken with Sweet Potato',
                'calories': '510',
                'carbs': '38g',
                'fats': '12g',
                'protein': '52g',
                'completed': False
            }
        },
        {
            'week': 'Week 4',
            'breakfast': {
                'dishName': 'Protein Smoothie Bowl with Banana',
                'calories': '360',
                'carbs': '45g',
                'fats': '8g',
                'protein': '25g',
                'completed': False
            },
            'lunch': {
                'dishName': 'Tuna Poke Bowl with Edamame',
                'calories': '470',
                'carbs': '40g',
                'fats': '16g',
                'protein': '38g',
                'completed': False
            },
            'dinner': {
                'dishName': 'Beef and Vegetable Stew with Quinoa',
                'calories': '530',
                'carbs': '42g',
                'fats': '18g',
                'protein': '46g',
                'completed': False
            }
        }
    ]
    return diet_plans[:weeks]


def generate_mock_workout_plan(weeks=4):
    """Mock workout plan generator matching frontend format."""
    workout_plans = [
        {
            'week': 'Week 1',
            'workoutName': 'Full Body Foundation',
            'completed': False,
            'exercises': [
                {'name': 'Push-ups', 'sets': '3', 'reps': '12', 'completed': False},
                {'name': 'Bodyweight Squats', 'sets': '3', 'reps': '15', 'completed': False},
                {'name': 'Plank Hold', 'sets': '3', 'reps': '30 sec', 'completed': False},
                {'name': 'Lunges', 'sets': '3', 'reps': '10 each', 'completed': False},
                {'name': 'Mountain Climbers', 'sets': '3', 'reps': '20', 'completed': False}
            ]
        },
        {
            'week': 'Week 2',
            'workoutName': 'Upper Body Strength',
            'completed': False,
            'exercises': [
                {'name': 'Diamond Push-ups', 'sets': '3', 'reps': '10', 'completed': False},
                {'name': 'Pike Push-ups', 'sets': '3', 'reps': '8', 'completed': False},
                {'name': 'Tricep Dips', 'sets': '3', 'reps': '12', 'completed': False},
                {'name': 'Superman Hold', 'sets': '3', 'reps': '20 sec', 'completed': False},
                {'name': 'Arm Circles', 'sets': '3', 'reps': '30 sec', 'completed': False}
            ]
        },
        {
            'week': 'Week 3',
            'workoutName': 'Lower Body Power',
            'completed': False,
            'exercises': [
                {'name': 'Jump Squats', 'sets': '4', 'reps': '12', 'completed': False},
                {'name': 'Walking Lunges', 'sets': '3', 'reps': '20', 'completed': False},
                {'name': 'Glute Bridges', 'sets': '3', 'reps': '15', 'completed': False},
                {'name': 'Calf Raises', 'sets': '3', 'reps': '20', 'completed': False},
                {'name': 'Wall Sit', 'sets': '3', 'reps': '45 sec', 'completed': False}
            ]
        },
        {
            'week': 'Week 4',
            'workoutName': 'Core & Cardio Blast',
            'completed': False,
            'exercises': [
                {'name': 'Burpees', 'sets': '3', 'reps': '10', 'completed': False},
                {'name': 'Bicycle Crunches', 'sets': '3', 'reps': '20', 'completed': False},
                {'name': 'High Knees', 'sets': '3', 'reps': '30 sec', 'completed': False},
                {'name': 'Russian Twists', 'sets': '3', 'reps': '20', 'completed': False},
                {'name': 'Plank to Push-up', 'sets': '3', 'reps': '10', 'completed': False}
            ]
        }
    ]
    return workout_plans[:weeks]
