"""
Prompt templates for diet and workout plan generation
Optimized for smaller LLMs (1B-3B parameters)
"""

from typing import Dict, Any, List
import json


def generate_nutrition_prompt(user_data: Dict[str, Any], weeks: int = 2) -> str:
    """
    Generate a simple prompt for nutrition plan.
    Optimized for smaller LLMs.
    """
    profile = user_data.get('profile', {})
    nutrition = user_data.get('nutrition', {})
    
    calories = nutrition.get('calorie_goal', 2000)
    goal = profile.get('fitness_goal', 'maintenance')
    diet_type = nutrition.get('diet_type', 'standard')
    allergies = nutrition.get('allergies', [])
    
    restrictions = ', '.join(allergies) if allergies else 'none'
    
    # Adjust calories based on goal
    if 'loss' in goal.lower() or 'lose' in goal.lower():
        calories = int(calories * 0.85)
    elif 'gain' in goal.lower() or 'muscle' in goal.lower():
        calories = int(calories * 1.15)
    
    prompt = f"""Create a {weeks}-week meal plan.
Calories: {calories}/day. Diet: {diet_type}. Allergies: {restrictions}.

Return JSON only:
{{"diet":[{{"week":"Week 1","breakfast":{{"dishName":"Oatmeal with Berries","calories":"400","protein":"15g","carbs":"60g","fats":"12g","completed":false}},"lunch":{{"dishName":"Grilled Chicken Salad","calories":"500","protein":"40g","carbs":"30g","fats":"20g","completed":false}},"dinner":{{"dishName":"Salmon with Vegetables","calories":"550","protein":"45g","carbs":"25g","fats":"25g","completed":false}}}}]}}

Generate {weeks} weeks with different meals:"""
    
    return prompt


def generate_fitness_prompt(user_data: Dict[str, Any], weeks: int = 2, exercise_db: List[Dict] = None) -> str:
    """
    Generate a simple prompt for workout plan.
    Optimized for smaller LLMs.
    """
    profile = user_data.get('profile', {})
    preferences = user_data.get('preferences', {})
    
    goal = profile.get('fitness_goal', 'general health')
    workouts_per_week = profile.get('workouts_per_day', 3)
    intensity = preferences.get('intensity', 'moderate')
    
    prompt = f"""Create a {weeks}-week workout plan.
Goal: {goal}. Days/week: {workouts_per_week}. Intensity: {intensity}.

Return JSON only:
{{"workouts":[{{"week":"Week 1","workoutName":"Full Body A","completed":false,"exercises":[{{"name":"Squats","sets":"3","reps":"10","completed":false}},{{"name":"Bench Press","sets":"3","reps":"10","completed":false}},{{"name":"Rows","sets":"3","reps":"10","completed":false}},{{"name":"Shoulder Press","sets":"3","reps":"10","completed":false}},{{"name":"Plank","sets":"3","reps":"30s","completed":false}}]}}]}}

Generate {weeks} weeks with varied exercises:"""
    
    return prompt


def generate_plan_prompt(user_data: Dict[str, Any], plan_type: str) -> str:
    """
    Generate prompt based on plan type.
    """
    preferences = user_data.get('preferences', {})
    duration_weeks = min(preferences.get('duration_weeks', 2), 4)
    
    if plan_type == 'diet':
        return generate_nutrition_prompt(user_data, duration_weeks)
    elif plan_type == 'workout':
        return generate_fitness_prompt(user_data, duration_weeks)
    else:
        return generate_combined_prompt(user_data, duration_weeks)


def generate_combined_prompt(user_data: Dict[str, Any], weeks: int = 2) -> str:
    """
    Generate a simple prompt for combined diet AND workout plan.
    Optimized for smaller LLMs.
    """
    profile = user_data.get('profile', {})
    nutrition = user_data.get('nutrition', {})
    preferences = user_data.get('preferences', {})
    
    calories = nutrition.get('calorie_goal', 2000)
    goal = profile.get('fitness_goal', 'maintenance')
    diet_type = nutrition.get('diet_type', 'standard')
    workouts_per_week = profile.get('workouts_per_day', 3)
    
    # Adjust calories
    if 'loss' in goal.lower() or 'lose' in goal.lower():
        calories = int(calories * 0.85)
    elif 'gain' in goal.lower() or 'muscle' in goal.lower():
        calories = int(calories * 1.15)
    
    prompt = f"""Create a {weeks}-week fitness plan with meals and workouts.
Calories: {calories}/day. Goal: {goal}. Diet: {diet_type}. Workouts: {workouts_per_week}/week.

Return ONLY valid JSON:
{{"diet":[{{"week":"Week 1","breakfast":{{"dishName":"Oatmeal","calories":"400","protein":"15g","carbs":"60g","fats":"12g","completed":false}},"lunch":{{"dishName":"Chicken Salad","calories":"500","protein":"40g","carbs":"30g","fats":"20g","completed":false}},"dinner":{{"dishName":"Salmon Veggies","calories":"550","protein":"45g","carbs":"25g","fats":"25g","completed":false}}}}],"workouts":[{{"week":"Week 1","workoutName":"Full Body","completed":false,"exercises":[{{"name":"Squats","sets":"3","reps":"10","completed":false}},{{"name":"Push-ups","sets":"3","reps":"12","completed":false}},{{"name":"Rows","sets":"3","reps":"10","completed":false}}]}}]}}

Generate {weeks} different weeks:"""
    
    return prompt


def validate_plan_prompt(user_data: Dict[str, Any], plan_data: Dict[str, Any]) -> str:
    """
    Generate prompt for validating an existing plan.
    """
    profile = user_data.get('profile', {})
    goal = profile.get('fitness_goal', 'general health')
    
    prompt = f"""Analyze this fitness plan for goal: {goal}.

Plan: {json.dumps(plan_data)[:500]}

Return JSON:
{{"score":8,"feedback":"Brief assessment","improvements":["tip1","tip2"]}}"""
    
    return prompt


def adjust_plan_prompt(current_plan: Dict[str, Any], adjustment_request: str, user_feedback: str = "") -> str:
    """
    Generate prompt for adjusting an existing plan based on user feedback.
    """
    prompt = f"""Adjust this fitness plan based on the request.

Request: {adjustment_request}
{f"Feedback: {user_feedback}" if user_feedback else ""}

Current plan: {json.dumps(current_plan)[:800]}

Return the adjusted plan in the same JSON format with updated diet and/or workouts arrays.
Keep the same structure but modify based on the request."""
    
    return prompt


def adjust_workout_week_prompt(
    user_data: Dict[str, Any], 
    current_week: Dict[str, Any], 
    skipped_workouts: List[str] = None,
    remaining_exercises: List[Dict] = None,
    feedback: str = ""
) -> str:
    """
    Generate prompt for adjusting a specific workout week.
    """
    goal = user_data.get('profile', {}).get('fitness_goal', 'general health')
    week_name = current_week.get('week', current_week.get('weekName', 'Week 1'))
    
    skipped_info = f"Skipped workouts: {skipped_workouts}" if skipped_workouts else ""
    remaining_info = f"Remaining exercises to adjust: {len(remaining_exercises or [])}" if remaining_exercises else ""
    
    prompt = f"""Adjust this workout week based on feedback.
Goal: {goal}
{skipped_info}
{remaining_info}
{f"Reason: {feedback}" if feedback else ""}
Current week: {json.dumps(current_week)[:400]}

Return JSON with exercises array containing adjusted workouts:
{{"exercises":[{{"name":"Exercise Name","sets":"3","reps":"10","completed":false}}]}}"""
    
    return prompt


def adjust_nutrition_week_prompt(
    user_data: Dict[str, Any], 
    current_week: Dict[str, Any], 
    extra_calories: int = 0,
    day_of_week: int = 0,
    remaining_days: List[str] = None,
    notes: str = ""
) -> str:
    """
    Generate prompt for adjusting a specific nutrition week after calorie surplus.
    """
    goal = user_data.get('profile', {}).get('fitness_goal', 'general health')
    diet_type = user_data.get('nutrition', {}).get('diet_type', 'standard')
    week_name = current_week.get('week', current_week.get('weekName', 'Week 1'))
    
    days_info = f"Remaining days to adjust: {', '.join(remaining_days)}" if remaining_days else ""
    
    prompt = f"""Adjust this meal plan to compensate for extra calories consumed.
Goal: {goal}. Diet: {diet_type}
Extra calories to offset: {extra_calories}
Day of surplus: {day_of_week}
{days_info}
{f"Notes: {notes}" if notes else ""}
Current week: {json.dumps(current_week)[:400]}

Return JSON with adjusted meals for remaining days, reducing calories to compensate:
{{"adjusted_meals":{{"breakfast":{{"dishName":"Light Meal","calories":"300","protein":"20g","carbs":"30g","fats":"10g","completed":false}}}}}}"""
    
    return prompt


def generate_daily_recommendation_prompt(user_data: Dict[str, Any], current_plan: Dict[str, Any], tracking_data: Dict[str, Any]) -> str:
    """
    Generate prompt for daily recommendations.
    """
    goal = user_data.get('profile', {}).get('fitness_goal', 'general health')
    calories_today = tracking_data.get('calories_consumed', 0)
    target = tracking_data.get('calorie_goal', 2000)
    
    prompt = f"""Give daily tip for goal: {goal}.
Calories: {calories_today}/{target} today.

Return JSON:
{{"tip":"Brief actionable advice","priority":"high/medium/low"}}"""
    
    return prompt
