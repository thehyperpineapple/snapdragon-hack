"""
Blueprint registration module for the Nutrition & Workout API.
"""

from .auth import auth_bp
from .user import user_bp
from .plan import plan_bp  # AI-powered plan routes
from .tracking import tracking_bp
from .user_ai import user_ai_bp  # AI-enhanced health & nutrition routes
from .agents import agents_bp  # Fitness & nutrition agents

__all__ = ['auth_bp', 'user_bp', 'plan_bp', 'tracking_bp', 'user_ai_bp', 'agents_bp']
