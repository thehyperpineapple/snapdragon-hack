"""
AI Layer for Snapdragon NPU Integration
Handles LLM inference on Qualcomm Hexagon HTP
"""

from .inference.npu_engine import NPUInferenceEngine, get_npu_engine
from .prompts import plan_prompts, nutrition_prompts, health_prompts

__all__ = [
    'NPUInferenceEngine',
    'get_npu_engine',
    'plan_prompts',
    'nutrition_prompts',
    'health_prompts'
]

__version__ = '1.0.0'
