"""
Inference submodule - Ollama engine for LLM inference
"""

from .ollama_engine import OllamaEngine, get_ollama_engine, get_npu_engine

__all__ = ['OllamaEngine', 'get_ollama_engine', 'get_npu_engine']
