"""
Ollama Inference Engine
Simple LLM inference using local Ollama server
"""

import os
import logging
import threading
import hashlib
from typing import Dict, Optional
import requests

logger = logging.getLogger('ai')


class OllamaEngine:
    """
    Inference engine using local Ollama server.
    Drop-in replacement for the NPU engine with the same interface.
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        """Singleton pattern implementation."""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(OllamaEngine, cls).__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        """Initialize the Ollama engine."""
        if self._initialized:
            return

        logger.info("=" * 60)
        logger.info("Initializing Ollama Inference Engine")
        logger.info("=" * 60)

        # Configuration from environment
        self.base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        self.model = os.getenv("OLLAMA_MODEL", "llama3.2")
        self.max_new_tokens = int(os.getenv("MAX_NEW_TOKENS", "512"))
        self.temperature = float(os.getenv("TEMPERATURE", "0.7"))
        self.timeout = int(os.getenv("OLLAMA_TIMEOUT", "120"))

        # Simple response cache
        self._cache: Dict[str, str] = {}
        self._cache_hits = 0
        self._cache_misses = 0

        # Test connection
        self._test_connection()

        self._initialized = True
        logger.info(f"Model: {self.model}")
        logger.info("Ollama Inference Engine initialized successfully")
        logger.info("=" * 60)

    def _test_connection(self):
        """Test connection to Ollama server."""
        try:
            response = requests.get(f"{self.base_url}/api/tags", timeout=5)
            if response.status_code == 200:
                models = response.json().get("models", [])
                model_names = [m.get("name", "") for m in models]
                logger.info(f"Connected to Ollama. Available models: {model_names}")

                # Check if configured model is available
                if not any(self.model in name for name in model_names):
                    logger.warning(
                        f"Model '{self.model}' not found. "
                        f"Run: ollama pull {self.model}"
                    )
            else:
                logger.warning(f"Ollama server responded with status {response.status_code}")
        except requests.exceptions.ConnectionError:
            logger.warning(
                f"Cannot connect to Ollama at {self.base_url}. "
                "Make sure Ollama is running: ollama serve"
            )
        except Exception as e:
            logger.warning(f"Error testing Ollama connection: {e}")

    def _get_cache_key(self, prompt: str, max_new_tokens: int, temperature: float) -> str:
        """Generate a cache key for the request."""
        key_str = f"{prompt}_{max_new_tokens}_{temperature}"
        return hashlib.md5(key_str.encode()).hexdigest()

    def generate(
        self,
        prompt: str,
        max_new_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        top_p: Optional[float] = None,
        use_cache: bool = True
    ) -> str:
        """
        Generate text using Ollama.

        Args:
            prompt: Input text prompt
            max_new_tokens: Maximum tokens to generate
            temperature: Sampling temperature
            top_p: Nucleus sampling threshold (passed to Ollama)
            use_cache: Whether to use response cache

        Returns:
            Generated text
        """
        max_new_tokens = max_new_tokens or self.max_new_tokens
        temperature = temperature or self.temperature
        top_p = top_p or 0.9

        logger.info(f"AI_INFERENCE: Starting generation with Ollama model={self.model}")

        # Check cache
        if use_cache:
            cache_key = self._get_cache_key(prompt, max_new_tokens, temperature)
            if cache_key in self._cache:
                self._cache_hits += 1
                logger.info("AI_INFERENCE: Cache HIT - returning cached response")
                return self._cache[cache_key]
            self._cache_misses += 1

        try:
            response = requests.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": prompt,
                    "stream": False,
                    "options": {
                        "num_predict": max_new_tokens,
                        "temperature": temperature,
                        "top_p": top_p
                    }
                },
                timeout=self.timeout
            )

            if response.status_code != 200:
                error_msg = f"Ollama API error: {response.status_code} - {response.text}"
                logger.error(f"AI_INFERENCE: {error_msg}")
                raise RuntimeError(error_msg)

            result = response.json()
            generated_text = result.get("response", "")

            # Cache the result
            if use_cache:
                self._cache[cache_key] = generated_text

            logger.info(
                f"AI_INFERENCE: Generation complete. "
                f"Response length: {len(generated_text)} chars"
            )
            return generated_text

        except requests.exceptions.Timeout:
            error_msg = f"Ollama request timed out after {self.timeout}s"
            logger.error(f"AI_INFERENCE: {error_msg}")
            raise RuntimeError(error_msg)
        except requests.exceptions.ConnectionError:
            error_msg = f"Cannot connect to Ollama at {self.base_url}"
            logger.error(f"AI_INFERENCE: {error_msg}")
            raise RuntimeError(error_msg)
        except Exception as e:
            logger.error(f"AI_INFERENCE: Generation error: {e}", exc_info=True)
            raise

    def health_check(self) -> Dict:
        """
        Perform health check on the Ollama engine.

        Returns:
            Status dictionary with engine metrics
        """
        try:
            # Test Ollama connection
            response = requests.get(f"{self.base_url}/api/tags", timeout=5)
            ollama_status = "connected" if response.status_code == 200 else "error"

            # Test inference
            test_output = self.generate(
                "Say 'OK' if you are working.",
                max_new_tokens=10,
                use_cache=False
            )

            total_requests = self._cache_hits + self._cache_misses
            hit_rate = self._cache_hits / total_requests if total_requests > 0 else 0

            return {
                "status": "healthy",
                "engine": "Ollama",
                "model": self.model,
                "base_url": self.base_url,
                "ollama_status": ollama_status,
                "test_inference": "passed",
                "test_output": test_output[:100],
                "cache_stats": {
                    "hits": self._cache_hits,
                    "misses": self._cache_misses,
                    "hit_rate": f"{hit_rate:.2%}",
                    "entries": len(self._cache)
                }
            }
        except Exception as e:
            return {
                "status": "unhealthy",
                "error": str(e),
                "engine": "Ollama",
                "model": self.model
            }

    def get_model_info(self) -> Dict:
        """Return model metadata."""
        return {
            "engine": "Ollama",
            "model": self.model,
            "base_url": self.base_url,
            "max_new_tokens": self.max_new_tokens,
            "temperature": self.temperature,
            "timeout": self.timeout
        }

    def clear_cache(self):
        """Clear the response cache."""
        self._cache.clear()
        self._cache_hits = 0
        self._cache_misses = 0
        logger.info("AI_CACHE: Response cache cleared")

    def __repr__(self) -> str:
        return f"OllamaEngine(model={self.model}, base_url={self.base_url})"


# Singleton instance getter
def get_ollama_engine() -> OllamaEngine:
    """
    Get the singleton Ollama engine instance.

    Returns:
        Initialized OllamaEngine instance
    """
    return OllamaEngine()


# Alias for backward compatibility
def get_npu_engine() -> OllamaEngine:
    """
    Backward-compatible alias for get_ollama_engine.
    Routes using get_npu_engine() will work without changes.
    """
    return get_ollama_engine()
