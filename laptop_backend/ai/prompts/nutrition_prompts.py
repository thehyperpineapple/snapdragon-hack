"""
Prompt templates for nutrition analysis and recommendations
"""

from typing import Dict, Any, List


def analyze_nutrition_profile_prompt(nutrition_data: Dict[str, Any], health_data: Dict[str, Any]) -> str:
    """
    Generate prompt for analyzing nutrition profile and providing recommendations.

    Args:
        nutrition_data: User's nutrition preferences
        health_data: User's health profile

    Returns:
        Formatted prompt string
    """
    prompt = f"""You are a certified nutritionist AI. Analyze the user's nutrition profile and provide personalized recommendations.

NUTRITION PROFILE:
- Diet Type: {nutrition_data.get('diet_type', 'standard')}
- Calorie Goal: {nutrition_data.get('calorie_goal', 2000)} kcal/day
- Protein Goal: {nutrition_data.get('protein_goal', 'Not set')}g
- Carb Goal: {nutrition_data.get('carb_goal', 'Not set')}g
- Fat Goal: {nutrition_data.get('fat_goal', 'Not set')}g
- Meals Per Day: {nutrition_data.get('meals_per_day', 3)}
- Allergies: {', '.join(nutrition_data.get('allergies', [])) or 'None'}
- Dietary Restrictions: {', '.join(nutrition_data.get('dietary_restrictions', [])) or 'None'}
- Cuisine Preferences: {', '.join(nutrition_data.get('cuisine_preferences', [])) or 'Any'}

HEALTH PROFILE:
- Age: {health_data.get('age', 'N/A')}
- Weight: {health_data.get('weight', 'N/A')} kg
- Height: {health_data.get('height', 'N/A')} cm
- BMI: {health_data.get('bmi', 'N/A')}
- Activity Level: {health_data.get('activity_level', 'moderate')}
- Fitness Goal: {health_data.get('fitness_goal', 'general health')}

TASK: Provide analysis and recommendations in JSON format:
{{
  "analysis": {{
    "calorie_appropriateness": "Assessment of calorie goal",
    "macro_balance": "Assessment of macro distribution",
    "diet_compatibility": "How well diet type fits goals"
  }},
  "recommendations": [
    "Specific recommendation 1",
    "Specific recommendation 2"
  ],
  "suggested_adjustments": {{
    "calorie_goal": 2000,
    "protein_goal": 150,
    "carb_goal": 200,
    "fat_goal": 65
  }},
  "meal_timing_tips": ["Tip 1", "Tip 2"],
  "supplement_suggestions": ["Suggestion 1"] or []
}}

Return ONLY valid JSON:"""

    return prompt


def analyze_meal_log_prompt(meal_data: List[Dict], daily_goals: Dict[str, Any]) -> str:
    """
    Generate prompt for analyzing meal logs against daily goals.

    Args:
        meal_data: List of meals logged for the day
        daily_goals: Daily nutrition goals

    Returns:
        Formatted prompt string
    """
    meals_str = "\n".join([
        f"- {meal.get('meal_type', 'Unknown')}: {meal.get('name', 'Unknown')} "
        f"({meal.get('calories', 0)} kcal, P:{meal.get('protein', 0)}g, "
        f"C:{meal.get('carbs', 0)}g, F:{meal.get('fats', 0)}g)"
        for meal in meal_data
    ])

    prompt = f"""You are a nutrition tracking AI. Analyze today's meals against the user's goals.

TODAY'S MEALS:
{meals_str or 'No meals logged yet'}

DAILY GOALS:
- Calorie Goal: {daily_goals.get('calorie_goal', 2000)} kcal
- Protein Goal: {daily_goals.get('protein_goal', 'Not set')}g
- Carb Goal: {daily_goals.get('carb_goal', 'Not set')}g
- Fat Goal: {daily_goals.get('fat_goal', 'Not set')}g

TASK: Provide analysis in JSON format:
{{
  "totals": {{
    "calories": 0,
    "protein": 0,
    "carbs": 0,
    "fats": 0
  }},
  "progress": {{
    "calories_percentage": 0,
    "protein_percentage": 0,
    "carbs_percentage": 0,
    "fats_percentage": 0
  }},
  "status": "On track/Over target/Under target",
  "remaining": {{
    "calories": 0,
    "protein": 0,
    "carbs": 0,
    "fats": 0
  }},
  "suggestions": [
    "What to eat for remaining meals"
  ]
}}

Return ONLY valid JSON:"""

    return prompt


def generate_meal_suggestions_prompt(
    nutrition_prefs: Dict[str, Any],
    remaining_macros: Dict[str, int],
    meal_type: str
) -> str:
    """
    Generate prompt for suggesting meals based on remaining macros.

    Args:
        nutrition_prefs: User's nutrition preferences
        remaining_macros: Remaining calories/macros for the day
        meal_type: breakfast, lunch, dinner, or snack

    Returns:
        Formatted prompt string
    """
    prompt = f"""You are a meal planning AI. Suggest {meal_type} options that fit the user's remaining macros.

NUTRITION PREFERENCES:
- Diet Type: {nutrition_prefs.get('diet_type', 'standard')}
- Allergies: {', '.join(nutrition_prefs.get('allergies', [])) or 'None'}
- Dietary Restrictions: {', '.join(nutrition_prefs.get('dietary_restrictions', [])) or 'None'}

REMAINING MACROS:
- Calories: {remaining_macros.get('calories', 0)} kcal
- Protein: {remaining_macros.get('protein', 0)}g
- Carbs: {remaining_macros.get('carbs', 0)}g
- Fats: {remaining_macros.get('fats', 0)}g

MEAL TYPE: {meal_type}

TASK: Suggest 3 meal options in JSON format:
{{
  "suggestions": [
    {{
      "name": "Meal Name",
      "description": "Brief description",
      "calories": 500,
      "protein": 30,
      "carbs": 50,
      "fats": 20,
      "ingredients": ["ingredient 1", "ingredient 2"],
      "prep_time": "15 minutes"
    }}
  ]
}}

Return ONLY valid JSON:"""

    return prompt
