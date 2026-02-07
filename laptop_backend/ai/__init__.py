"""
AI Layer for LLM Inference
Uses Google Gemini API for fast inference
"""

from .inference.gemini_engine import GeminiEngine, get_gemini_engine, get_ollama_engine, get_npu_engine
from .prompts import plan_prompts, nutrition_prompts, health_prompts


__all__ = [
    'GeminiEngine',
    'get_gemini_engine',
    'get_ollama_engine',  # Backward compatibility alias
    'get_npu_engine',     # Backward compatibility alias
    'plan_prompts',
    'nutrition_prompts',
    'health_prompts'
]

__version__ = '3.0.0'
