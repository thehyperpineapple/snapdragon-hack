"""
AI Layer for LLM Inference
Uses Google Gemini API for fast inference
"""

from .inference.gemini_engine import GeminiEngine, get_gemini_engine
from .prompts import plan_prompts, nutrition_prompts, health_prompts


__all__ = [
    'GeminiEngine',
    'get_gemini_engine',
    'plan_prompts',
    'nutrition_prompts',
    'health_prompts'
]

__version__ = '3.0.0'
