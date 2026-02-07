"""
Inference submodule - NPU engine and cache management
"""

from .npu_engine import NPUInferenceEngine, get_npu_engine
from .cache_manager import KVCacheManager

__all__ = ['NPUInferenceEngine', 'get_npu_engine', 'KVCacheManager']
