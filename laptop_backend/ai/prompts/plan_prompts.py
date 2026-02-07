"""
Prompt templates for diet and workout plan generation
"""

from typing import Dict, Any


def generate_plan_prompt(user_data: Dict[str, Any], plan_type: str) -> str:
    """
    Generate prompt for creating diet/workout plans.

    Args:
        user_data: User profile and preferences
        plan_type: 'diet', 'workout', or 'combined'

    Returns:
        Formatted prompt string
    """
    profile = user_data.get('profile', {})
    nutrition = user_data.get('nutrition', {})
    preferences = user_data.get('preferences', {})

    prompt = f"""You are a professional nutrition and fitness coach AI. Generate a personalized {plan_type} plan.

USER PROFILE:
- Age: {profile.get('age', 'N/A')}
- Gender: {profile.get('gender', 'N/A')}
- Weight: {profile.get('weight', 'N/A')} kg
- Height: {profile.get('height', 'N/A')} cm
- BMI: {profile.get('bmi', 'N/A')}
- Activity Level: {profile.get('activity_level', 'moderate')}
- Fitness Goal: {profile.get('fitness_goal', 'general health')}

NUTRITION PREFERENCES:
- Diet Type: {nutrition.get('diet_type', 'standard')}
- Allergies: {', '.join(nutrition.get('allergies', [])) or 'None'}
- Dietary Restrictions: {', '.join(nutrition.get('dietary_restrictions', [])) or 'None'}
- Calorie Goal: {nutrition.get('calorie_goal', 2000)} kcal/day
- Meals Per Day: {nutrition.get('meals_per_day', 3)}

PLAN PREFERENCES:
- Duration: {preferences.get('duration_weeks', 4)} weeks
- Intensity: {preferences.get('intensity', 'moderate')}
- Specific Goals: {', '.join(preferences.get('specific_goals', [])) or 'General wellness'}

TASK: Create a detailed {plan_type} plan in JSON format:
"""

    if plan_type in ['diet', 'combined']:
        prompt += """
DIET PLAN FORMAT:
{
  "diet": [
    {
      "weekName": "Week 1",
      "meals": {
        "breakfast": {"name": "...", "calories": 400, "protein": 20, "carbs": 50, "fats": 15, "completed": false},
        "lunch": {"name": "...", "calories": 500, "protein": 30, "carbs": 60, "fats": 20, "completed": false},
        "dinner": {"name": "...", "calories": 500, "protein": 30, "carbs": 60, "fats": 20, "completed": false},
        "snack": {"name": "...", "calories": 200, "protein": 10, "carbs": 25, "fats": 8, "completed": false}
      }
    }
  ]
}
"""

    if plan_type in ['workout', 'combined']:
        prompt += """
WORKOUT PLAN FORMAT:
{
  "workouts": [
    {
      "weekName": "Week 1",
      "exercises": [
        {"workoutId": "w1", "name": "Exercise Name", "sets": 3, "reps": 12, "duration": "N/A", "completed": false},
        {"workoutId": "w2", "name": "Cardio Exercise", "sets": 1, "reps": null, "duration": "30min", "completed": false}
      ]
    }
  ]
}
"""

    prompt += """
IMPORTANT:
- Ensure all meals meet the calorie and macro goals
- Respect dietary restrictions and allergies
- Make workouts appropriate for the user's fitness level
- Provide variety across weeks
- Return ONLY valid JSON, no additional text

Generate the plan now:"""

    return prompt


def validate_plan_prompt(user_data: Dict[str, Any], plan_data: Dict[str, Any]) -> str:
    """
    Generate prompt for validating/analyzing an existing plan.

    Args:
        user_data: User profile
        plan_data: Existing plan to validate

    Returns:
        Formatted prompt string
    """
    profile = user_data.get('profile', {})

    prompt = f"""You are a nutrition and fitness expert. Analyze the following plan and provide feedback.

USER PROFILE:
- Age: {profile.get('age', 'N/A')}
- Weight: {profile.get('weight', 'N/A')} kg
- BMI: {profile.get('bmi', 'N/A')}
- Fitness Goal: {profile.get('fitness_goal', 'general health')}

CURRENT PLAN:
{plan_data}

TASK: Provide analysis in JSON format:
{{
  "overall_assessment": "Brief summary",
  "calorie_balance": "Adequate/Too high/Too low",
  "macro_distribution": "Balanced/Needs adjustment",
  "workout_intensity": "Appropriate/Too intense/Too light",
  "recommendations": ["Suggestion 1", "Suggestion 2"],
  "safety_concerns": ["Concern 1"] or []
}}

Return ONLY valid JSON:"""

    return prompt


def adjust_plan_prompt(
    current_plan: Dict[str, Any],
    adjustment_request: str,
    user_feedback: str = ""
) -> str:
    """
    Generate prompt for adjusting an existing plan based on user feedback.

    Args:
        current_plan: Current plan data
        adjustment_request: What needs to be changed
        user_feedback: Optional user feedback

    Returns:
        Formatted prompt string
    """
    prompt = f"""You are a fitness and nutrition coach. Adjust the following plan based on the user's request.

CURRENT PLAN:
{current_plan}

ADJUSTMENT REQUEST:
{adjustment_request}

USER FEEDBACK:
{user_feedback or 'None provided'}

TASK: Return the UPDATED plan in the same JSON format as the original, incorporating the requested changes.

IMPORTANT:
- Maintain the overall structure
- Only modify what's requested
- Ensure nutritional/fitness balance
- Return ONLY valid JSON

Updated plan:"""

    return prompt
