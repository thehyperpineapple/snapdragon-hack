"""
Services module - Business logic layer.
"""

from .user_service import register_user_entry, get_user, delete_user
from .health_service import post_health_data, get_health_data, patch_health_data, delete_health_data
from .nutrition_service import (
    post_nutrition_data,
    get_nutrition_data,
    update_nutrition_data,
    delete_nutrition_data,
    add_diet_entry
)
from .plan_service import create_plan, get_plan, update_plan, delete_plan
from .tracking_service import (
    update_meal_completion,
    toggle_workout_status,
    log_daily_meal,
    log_daily_workout
)

__all__ = [
    # User
    'register_user_entry',
    'get_user',
    'delete_user',
    # Health
    'post_health_data',
    'get_health_data',
    'patch_health_data',
    'delete_health_data',
    # Nutrition
    'post_nutrition_data',
    'get_nutrition_data',
    'update_nutrition_data',
    'delete_nutrition_data',
    'add_diet_entry',
    # Plan
    'create_plan',
    'get_plan',
    'update_plan',
    'delete_plan',
    # Tracking
    'update_meal_completion',
    'toggle_workout_status',
    'log_daily_meal',
    'log_daily_workout',
]
