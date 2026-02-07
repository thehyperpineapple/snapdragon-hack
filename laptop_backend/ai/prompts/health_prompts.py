"""
Prompt templates for health metrics analysis and insights
"""

from typing import Dict, Any, List


def analyze_health_metrics_prompt(health_data: Dict[str, Any]) -> str:
    """
    Generate prompt for analyzing health metrics and providing insights.

    Args:
        health_data: User's health profile data

    Returns:
        Formatted prompt string
    """
    prompt = f"""You are a health and fitness AI coach. Analyze the user's health metrics and provide insights.

HEALTH METRICS:
- Age: {health_data.get('age', 'N/A')} years
- Gender: {health_data.get('gender', 'N/A')}
- Weight: {health_data.get('weight', 'N/A')} kg
- Height: {health_data.get('height', 'N/A')} cm
- BMI: {health_data.get('bmi', 'N/A')}
- Activity Level: {health_data.get('activity_level', 'moderate')}
- Fitness Goal: {health_data.get('fitness_goal', 'general health')}

ADDITIONAL METRICS (if available):
- Resting Heart Rate: {health_data.get('resting_heart_rate', 'N/A')} bpm
- Blood Pressure: {health_data.get('blood_pressure', 'N/A')}
- Body Fat Percentage: {health_data.get('body_fat_percentage', 'N/A')}%

TASK: Provide comprehensive health analysis in JSON format:
{{
  "bmi_assessment": {{
    "category": "Underweight/Normal/Overweight/Obese",
    "description": "Detailed interpretation"
  }},
  "health_insights": [
    "Insight about current health status 1",
    "Insight 2"
  ],
  "risk_factors": [
    "Identified risk factor 1"
  ] or [],
  "recommendations": [
    "Specific recommendation 1",
    "Specific recommendation 2"
  ],
  "optimal_ranges": {{
    "weight_range": "Min-Max kg",
    "bmi_range": "Min-Max",
    "recommended_activity": "Description"
  }},
  "priority_actions": [
    "Action item 1"
  ]
}}

Return ONLY valid JSON:"""

    return prompt


def track_progress_prompt(
    current_health: Dict[str, Any],
    historical_data: List[Dict[str, Any]],
    goal: str
) -> str:
    """
    Generate prompt for tracking health progress over time.

    Args:
        current_health: Current health metrics
        historical_data: Historical health measurements
        goal: User's fitness goal

    Returns:
        Formatted prompt string
    """
    history_str = "\n".join([
        f"- {data.get('date', 'Unknown')}: Weight {data.get('weight', 'N/A')}kg, "
        f"BMI {data.get('bmi', 'N/A')}"
        for data in historical_data[-10:]  # Last 10 entries
    ])

    prompt = f"""You are a fitness progress tracking AI. Analyze the user's health journey.

CURRENT METRICS:
- Weight: {current_health.get('weight', 'N/A')} kg
- BMI: {current_health.get('bmi', 'N/A')}
- Body Fat: {current_health.get('body_fat_percentage', 'N/A')}%

HISTORICAL DATA (Recent):
{history_str or 'No historical data available'}

FITNESS GOAL: {goal}

TASK: Provide progress analysis in JSON format:
{{
  "progress_summary": "Overall assessment of progress",
  "trend_analysis": {{
    "weight_trend": "Increasing/Decreasing/Stable",
    "rate_of_change": "X kg per week/month",
    "trajectory": "On track/Ahead/Behind schedule"
  }},
  "achievements": [
    "Milestone 1 reached"
  ],
  "areas_for_improvement": [
    "Area 1"
  ],
  "next_milestones": [
    "Next goal to achieve"
  ],
  "motivational_message": "Personalized encouragement",
  "recommended_adjustments": [
    "Adjustment suggestion 1"
  ]
}}

Return ONLY valid JSON:"""

    return prompt


def wellness_insights_prompt(wellness_data: Dict[str, Any]) -> str:
    """
    Generate prompt for analyzing wellness metrics (sleep, mood, energy).

    Args:
        wellness_data: Daily wellness metrics

    Returns:
        Formatted prompt string
    """
    prompt = f"""You are a wellness coach AI. Analyze the user's wellness metrics and provide insights.

WELLNESS METRICS:
- Sleep Hours: {wellness_data.get('sleep_hours', 'N/A')} hours
- Mood: {wellness_data.get('mood', 'N/A')}
- Energy Level: {wellness_data.get('energy_level', 'N/A')}/5
- Water Intake: {wellness_data.get('water_ml', 'N/A')} ml
- Stress Level: {wellness_data.get('stress_level', 'N/A')}

ACTIVITY TODAY:
- Workout Completed: {wellness_data.get('workout_completed', False)}
- Steps: {wellness_data.get('steps', 'N/A')}
- Active Minutes: {wellness_data.get('active_minutes', 'N/A')}

TASK: Provide wellness insights in JSON format:
{{
  "overall_wellness_score": 0-100,
  "sleep_analysis": {{
    "quality": "Adequate/Insufficient/Excessive",
    "recommendation": "Specific sleep recommendation"
  }},
  "energy_insights": "Analysis of energy level and factors",
  "mood_factors": [
    "Potential factor affecting mood"
  ],
  "hydration_status": "Well hydrated/Needs improvement/Dehydrated",
  "recommendations": [
    "Actionable recommendation 1",
    "Actionable recommendation 2"
  ],
  "correlations": [
    "Observed pattern between metrics"
  ],
  "tips": [
    "Practical wellness tip 1"
  ]
}}

Return ONLY valid JSON:"""

    return prompt


def generate_health_goals_prompt(
    current_health: Dict[str, Any],
    desired_goal: str,
    timeframe: str
) -> str:
    """
    Generate prompt for creating realistic health goals.

    Args:
        current_health: Current health metrics
        desired_goal: User's desired outcome
        timeframe: Target timeframe

    Returns:
        Formatted prompt string
    """
    prompt = f"""You are a fitness goal-setting AI. Create realistic, achievable health goals for the user.

CURRENT HEALTH:
- Weight: {current_health.get('weight', 'N/A')} kg
- BMI: {current_health.get('bmi', 'N/A')}
- Activity Level: {current_health.get('activity_level', 'moderate')}

DESIRED GOAL: {desired_goal}
TIMEFRAME: {timeframe}

TASK: Create structured goals in JSON format:
{{
  "primary_goal": {{
    "description": "Main goal description",
    "target_metric": "Specific measurable target",
    "timeframe": "{timeframe}",
    "is_realistic": true/false,
    "rationale": "Why this is appropriate"
  }},
  "milestone_goals": [
    {{
      "week": 1,
      "target": "Specific milestone",
      "metric": "Measurable indicator"
    }}
  ],
  "supporting_goals": [
    "Secondary goal 1",
    "Secondary goal 2"
  ],
  "action_plan": [
    "Specific action step 1",
    "Specific action step 2"
  ],
  "success_metrics": [
    "How to measure success 1"
  ],
  "potential_challenges": [
    "Challenge 1 and how to overcome it"
  ]
}}

Return ONLY valid JSON:"""

    return prompt
