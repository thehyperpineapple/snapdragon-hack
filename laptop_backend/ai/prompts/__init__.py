"""
Prompt templates submodule - Structured prompts for different endpoints
"""

from . import plan_prompts
from . import nutrition_prompts
from . import health_prompts
from . import agents_prompts

__all__ = ['plan_prompts', 'nutrition_prompts', 'health_prompts', 'agents_prompts']
