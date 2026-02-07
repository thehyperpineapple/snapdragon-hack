"""
AI Layer for LLM Inference
Uses Ollama for local model serving
"""

from .inference.ollama_engine import OllamaEngine, get_ollama_engine, get_npu_engine
from .prompts import plan_prompts, nutrition_prompts, health_prompts


__all__ = [
    'OllamaEngine',
    'get_ollama_engine',
    'get_npu_engine',  # Backward compatibility alias
    'plan_prompts',
    'nutrition_prompts',
    'health_prompts'
]

__version__ = '2.0.0'
