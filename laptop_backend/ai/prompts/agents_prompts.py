"""
Prompt templates for fitness and nutrition agents.
"""

from typing import List, Union


def fitness_agent_prompt(
    activity_level: str,
    fitness_goal: str,
    workout_duration: int,
    workout_days: int
) -> str:
    """
    Generate prompt for the fitness agent: workouts based on activity level,
    fitness goal, workout duration (minutes), and workout days per week.
    """
    prompt = f"""You are a fitness coach AI. Generate a weekly workout plan.

USER INPUTS:
- Activity level: {activity_level}
- Fitness goal: {fitness_goal}
- Workout duration per session: {workout_duration} minutes
- Workout days per week: {workout_days}

Create exactly {workout_days} distinct workouts for the week. Each workout should be designed to fit within {workout_duration} minutes. Match intensity and exercise selection to activity level and fitness goal.

Return ONLY valid JSON in this exact format:
{{
  "workouts": [
    {{
      "day": "Day 1",
      "workoutName": "Name of workout",
      "duration_minutes": {workout_duration},
      "completed": false,
      "exercises": [
        {{ "name": "Exercise name", "sets": "3", "reps": "10", "completed": false }},
        {{ "name": "Exercise with time", "sets": "3", "reps": "30s", "completed": false }}
      ]
    }}
  ]
}}

Generate {workout_days} workouts. Use "reps" for count (e.g. "12") or time (e.g. "30s"). Return only the JSON object, no other text."""

    return prompt


def nutrition_agent_prompt(
    dietary_restrictions: Union[List[str], str],
    caloric_intake: int
) -> str:
    """
    Generate prompt for the nutrition agent: food plan based on dietary
    restrictions and daily caloric intake.
    """
    restrictions_str = (
        ", ".join(dietary_restrictions)
        if isinstance(dietary_restrictions, list)
        else str(dietary_restrictions or "none")
    )

    prompt = f"""You are a nutritionist AI. Generate a daily food plan.

USER INPUTS:
- Dietary restrictions: {restrictions_str}
- Daily caloric intake: {caloric_intake} kcal

Create a one-day meal plan that respects the dietary restrictions and totals approximately {caloric_intake} calories. Include breakfast, lunch, dinner, and optional snacks. Split macros reasonably (e.g. protein 20-30%, carbs 40-50%, fats 20-35%).

Return ONLY valid JSON in this exact format:
{{
  "food_plan": [
    {{
      "meal": "breakfast",
      "dishName": "Meal name",
      "calories": "400",
      "protein": "15g",
      "carbs": "50g",
      "fats": "12g",
      "completed": false
    }},
    {{
      "meal": "lunch",
      "dishName": "Meal name",
      "calories": "550",
      "protein": "35g",
      "carbs": "45g",
      "fats": "22g",
      "completed": false
    }},
    {{
      "meal": "dinner",
      "dishName": "Meal name",
      "calories": "600",
      "protein": "40g",
      "carbs": "50g",
      "fats": "25g",
      "completed": false
    }}
  ],
  "daily_total_calories": {caloric_intake}
}}

Include breakfast, lunch, dinner, and up to 2 snacks if needed to hit the calorie target. All meals must respect the dietary restrictions. Return only the JSON object, no other text."""

    return prompt
