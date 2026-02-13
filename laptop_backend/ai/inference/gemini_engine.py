"""
Gemini Inference Engine
Fast LLM inference using Google's Gemini API
"""

import os
import logging
import threading
import hashlib
from typing import Dict, Optional
import google.generativeai as genai

logger = logging.getLogger('ai')


class GeminiEngine:
    """
    Inference engine using Google's Gemini API.
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        """Singleton pattern implementation."""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(GeminiEngine, cls).__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        """Initialize the Gemini engine."""
        if self._initialized:
            return

        logger.info("=" * 60)
        logger.info("Initializing Gemini Inference Engine")
        logger.info("=" * 60)

        # Configuration from environment
        self.api_key = os.getenv("GEMINI_API_KEY")
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY environment variable is required")

        self.model_name = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")
        self.max_new_tokens = int(os.getenv("MAX_NEW_TOKENS", "1024"))
        self.temperature = float(os.getenv("TEMPERATURE", "0.7"))
        self.timeout = int(os.getenv("GEMINI_TIMEOUT", "60"))

        # Configure the API
        genai.configure(api_key=self.api_key)

        # Initialize the model
        self.model = genai.GenerativeModel(
            model_name=self.model_name,
            generation_config={
                "temperature": self.temperature,
                "max_output_tokens": self.max_new_tokens,
                "top_p": 0.9,
            }
        )

        # Simple response cache
        self._cache: Dict[str, str] = {}
        self._cache_hits = 0
        self._cache_misses = 0

        # Test connection
        self._test_connection()

        self._initialized = True
        logger.info(f"Model: {self.model_name}")
        logger.info("Gemini Inference Engine initialized successfully")
        logger.info("=" * 60)

    def _test_connection(self):
        """Test connection to Gemini API."""
        try:
            # Quick test with a simple prompt
            response = self.model.generate_content(
                "Say 'OK' if you are working.",
                generation_config={"max_output_tokens": 10}
            )
            if response.text:
                logger.info(f"Connected to Gemini API. Model: {self.model_name}")
            else:
                logger.warning("Gemini API returned empty response")
        except Exception as e:
            logger.warning(f"Error testing Gemini connection: {e}")

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
        Generate text using Gemini.

        Args:
            prompt: Input text prompt
            max_new_tokens: Maximum tokens to generate
            temperature: Sampling temperature
            top_p: Nucleus sampling threshold
            use_cache: Whether to use response cache

        Returns:
            Generated text
        """
        max_new_tokens = max_new_tokens or self.max_new_tokens
        temperature = temperature or self.temperature
        top_p = top_p or 0.9

        logger.info(f"AI_INFERENCE: Starting generation with Gemini model={self.model_name}")

        # Check cache
        if use_cache:
            cache_key = self._get_cache_key(prompt, max_new_tokens, temperature)
            if cache_key in self._cache:
                self._cache_hits += 1
                logger.info("AI_INFERENCE: Cache HIT - returning cached response")
                return self._cache[cache_key]
            self._cache_misses += 1

        try:
            # Create generation config for this request
            generation_config = {
                "temperature": temperature,
                "max_output_tokens": max_new_tokens,
                "top_p": top_p,
            }

            response = self.model.generate_content(
                prompt,
                generation_config=generation_config
            )

            # Handle the response
            if not response.text:
                # Check if blocked by safety filters
                if response.prompt_feedback:
                    error_msg = f"Gemini blocked the prompt: {response.prompt_feedback}"
                    logger.error(f"AI_INFERENCE: {error_msg}")
                    raise RuntimeError(error_msg)
                error_msg = "Gemini returned empty response"
                logger.error(f"AI_INFERENCE: {error_msg}")
                raise RuntimeError(error_msg)

            generated_text = response.text

            # Cache the result
            if use_cache:
                self._cache[cache_key] = generated_text

            logger.info(
                f"AI_INFERENCE: Generation complete. "
                f"Response length: {len(generated_text)} chars"
            )
            return generated_text

        except Exception as e:
            logger.error(f"AI_INFERENCE: Generation error: {e}", exc_info=True)
            raise

    def health_check(self) -> Dict:
        """
        Perform health check on the Gemini engine.

        Returns:
            Status dictionary with engine metrics
        """
        try:
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
                "engine": "Gemini",
                "model": self.model_name,
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
                "engine": "Gemini",
                "model": self.model_name
            }

    def get_model_info(self) -> Dict:
        """Return model metadata."""
        return {
            "engine": "Gemini",
            "model": self.model_name,
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
        return f"GeminiEngine(model={self.model_name})"


# Singleton instance getter
def get_gemini_engine() -> GeminiEngine:
    """
    Get the singleton Gemini engine instance.

    Returns:
        Initialized GeminiEngine instance
    """
    return GeminiEngine()
