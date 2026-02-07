"""
NPU Inference Engine for Qualcomm Snapdragon HTP
Optimized for 8B LLMs using QNN ExecutionProvider

Author: Senior Embedded AI Engineer
Stack: QAIRT 2.26+ / onnxruntime-qnn / Python 3.10+
"""

import os
import threading
import logging
from typing import Dict, List, Optional
import numpy as np
from pathlib import Path

from .cache_manager import KVCacheManager

try:
    import onnxruntime as ort
    from transformers import AutoTokenizer
except ImportError as e:
    raise ImportError(
        "Required packages not installed. Run:\n"
        "pip install onnxruntime-qnn transformers torch"
    ) from e

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('ai')


class NPUInferenceEngine:
    """
    Singleton inference engine for Qualcomm Snapdragon NPU.
    Loads quantized LLM via QNNExecutionProvider with HTP backend.
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        """Singleton pattern implementation."""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(NPUInferenceEngine, cls).__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        """Initialize the NPU engine (called once via singleton)."""
        if self._initialized:
            return

        logger.info("="*60)
        logger.info("Initializing NPU Inference Engine")
        logger.info("="*60)

        # Load environment configuration
        self.qnn_sdk_root = os.getenv("QNN_SDK_ROOT", "/opt/qcom/aistack/qairt/2.26.0")
        self.model_context_path = os.getenv(
            "QNN_MODEL_CONTEXT",
            "./models/llama3_htp_context/llama3_8b_w4a16.bin"
        )
        self.tokenizer_path = os.getenv("TOKENIZER_PATH", "./models/llama3_8b_onnx")

        # Inference parameters
        self.max_context_length = int(os.getenv("MAX_CONTEXT_LENGTH", "2048"))
        self.max_new_tokens = int(os.getenv("MAX_NEW_TOKENS", "512"))
        self.temperature = float(os.getenv("TEMPERATURE", "0.7"))
        self.top_p = float(os.getenv("TOP_P", "0.9"))

        # Initialize components
        self.session: Optional[ort.InferenceSession] = None
        self.tokenizer = None
        self.kv_cache = KVCacheManager(
            max_cache_size_mb=int(os.getenv("KV_CACHE_SIZE_MB", "512"))
        )

        self._setup_onnxruntime_session()
        self._load_tokenizer()

        self._initialized = True
        logger.info("✓ NPU Inference Engine initialized successfully")
        logger.info("="*60)

    def _setup_onnxruntime_session(self):
        """
        Configure ONNX Runtime with QNN Execution Provider.
        Links to Hexagon HTP backend (libQnnHtp.so).
        """
        try:
            logger.info("Setting up ONNX Runtime with QNN Execution Provider...")

            # Set QNN library paths
            qnn_lib_path = os.path.join(
                self.qnn_sdk_root, "lib", "aarch64-android"
            )

            current_ld_path = os.environ.get('LD_LIBRARY_PATH', '')
            os.environ["LD_LIBRARY_PATH"] = f"{qnn_lib_path}:{current_ld_path}"
            logger.info(f"LD_LIBRARY_PATH: {qnn_lib_path}")

            # QNN Execution Provider options
            qnn_options = {
                "backend_path": os.path.join(qnn_lib_path, "libQnnHtp.so"),
                "qnn_context_cache_enable": "1",
                "qnn_context_cache_path": os.path.dirname(self.model_context_path),
                "htp_performance_mode": "burst",  # Options: burst, balanced, default, sustained_high_performance
                "htp_graph_finalization_optimization_mode": "3",  # Max optimization
                "qnn_saver_path": "./qnn_logs",  # For debugging
                "profiling_level": "basic"
            }

            # Session options
            sess_options = ort.SessionOptions()
            sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
            sess_options.log_severity_level = 3  # 0=Verbose, 3=Warning
            sess_options.enable_profiling = False  # Set to True for performance analysis

            # Create session with QNN EP
            providers = [
                ("QNNExecutionProvider", qnn_options),
                "CPUExecutionProvider"  # Fallback
            ]

            # Check if model exists
            if not Path(self.model_context_path).exists():
                logger.warning(
                    f"QNN context binary not found: {self.model_context_path}\n"
                    "Running in simulation mode without QNN backend."
                )
                self.session = None
                return

            self.session = ort.InferenceSession(
                self.model_context_path,
                sess_options=sess_options,
                providers=providers
            )

            # Verify QNN EP is active
            active_providers = self.session.get_providers()
            if "QNNExecutionProvider" not in active_providers:
                logger.warning(
                    f"⚠ QNN EP not active. Using: {active_providers}\n"
                    "Check libQnnHtp.so availability."
                )
            else:
                logger.info("✓ QNN Execution Provider (HTP) active")

        except Exception as e:
            logger.error(f"Failed to initialize ONNX Runtime session: {e}")
            logger.info("Continuing in simulation mode...")
            self.session = None

    def _load_tokenizer(self):
        """Load the tokenizer for the LLM."""
        try:
            logger.info(f"Loading tokenizer from: {self.tokenizer_path}")

            if not Path(self.tokenizer_path).exists():
                logger.warning(f"Tokenizer path not found: {self.tokenizer_path}")
                logger.info("Using GPT-2 tokenizer as fallback")
                self.tokenizer = AutoTokenizer.from_pretrained("gpt2")
            else:
                self.tokenizer = AutoTokenizer.from_pretrained(
                    self.tokenizer_path,
                    trust_remote_code=True
                )

            if self.tokenizer.pad_token is None:
                self.tokenizer.pad_token = self.tokenizer.eos_token

            logger.info(f"✓ Tokenizer loaded (vocab size: {self.tokenizer.vocab_size})")
        except Exception as e:
            logger.error(f"Failed to load tokenizer: {e}")
            raise

    def _prepare_inputs(
        self,
        prompt: str,
        past_key_values: Optional[List[np.ndarray]] = None
    ) -> Dict[str, np.ndarray]:
        """
        Tokenize input and prepare ONNX Runtime inputs.

        Args:
            prompt: Input text prompt
            past_key_values: Previous KV cache (for incremental generation)

        Returns:
            Dictionary of input tensors
        """
        # Tokenize
        encoded = self.tokenizer(
            prompt,
            return_tensors="np",
            max_length=self.max_context_length,
            truncation=True,
            padding="max_length"
        )

        inputs = {
            "input_ids": encoded["input_ids"].astype(np.int64),
            "attention_mask": encoded["attention_mask"].astype(np.int64)
        }

        # Add past KV cache if available
        if past_key_values is not None:
            for i, kv in enumerate(past_key_values):
                inputs[f"past_key_values.{i}.key"] = kv[0]
                inputs[f"past_key_values.{i}.value"] = kv[1]

        return inputs

    def _sample_token(
        self,
        logits: np.ndarray,
        temperature: float,
        top_p: float
    ) -> int:
        """
        Sample next token using temperature and nucleus (top-p) sampling.

        Args:
            logits: Model output logits [batch, seq_len, vocab_size]
            temperature: Sampling temperature (higher = more random)
            top_p: Nucleus sampling threshold

        Returns:
            Sampled token ID
        """
        # Get last token logits
        logits = logits[0, -1, :] / temperature

        # Apply softmax
        probs = np.exp(logits - np.max(logits))
        probs = probs / np.sum(probs)

        # Top-p (nucleus) sampling
        sorted_indices = np.argsort(probs)[::-1]
        sorted_probs = probs[sorted_indices]
        cumulative_probs = np.cumsum(sorted_probs)

        # Find cutoff index
        cutoff_idx = np.searchsorted(cumulative_probs, top_p)
        nucleus_indices = sorted_indices[:cutoff_idx + 1]
        nucleus_probs = probs[nucleus_indices]
        nucleus_probs = nucleus_probs / np.sum(nucleus_probs)

        # Sample from nucleus
        sampled_idx = np.random.choice(nucleus_indices, p=nucleus_probs)
        return int(sampled_idx)

    def generate(
        self,
        prompt: str,
        max_new_tokens: Optional[int] = None,
        temperature: Optional[float] = None,
        top_p: Optional[float] = None,
        use_cache: bool = True
    ) -> str:
        """
        Generate text using the NPU-accelerated LLM.

        Args:
            prompt: Input text prompt
            max_new_tokens: Maximum tokens to generate (default from env)
            temperature: Sampling temperature (default from env)
            top_p: Nucleus sampling threshold (default from env)
            use_cache: Whether to use KV cache

        Returns:
            Generated text
        """
        if self.tokenizer is None:
            raise RuntimeError("Engine not initialized. Tokenizer not loaded.")

        max_new_tokens = max_new_tokens or self.max_new_tokens
        temperature = temperature or self.temperature
        top_p = top_p or self.top_p

        logger.info(f"AI_INFERENCE: Starting text generation with prompt: '{prompt[:100]}...'")

        # Check cache
        cache_key = f"{prompt}_{max_new_tokens}_{temperature}_{top_p}"
        if use_cache:
            cached_result = self.kv_cache.get(cache_key)
            if cached_result is not None:
                logger.info(f"AI_INFERENCE: Cache HIT - returning cached response (prompt_len={len(prompt)}, max_tokens={max_new_tokens})")
                return str(cached_result)

        # If session not available (simulation mode), return mock response
        if self.session is None:
            logger.warning(f"AI_INFERENCE: Running in SIMULATION mode (no QNN backend) - prompt_len={len(prompt)}")
            mock_response = self._generate_mock_response(prompt)
            if use_cache:
                self.kv_cache.put(cache_key, np.array(mock_response))
            logger.info(f"AI_INFERENCE: Simulation mode response generated (response_len={len(mock_response)})")
            return mock_response

        # Prepare inputs
        inputs = self._prepare_inputs(prompt)
        generated_ids = inputs["input_ids"][0].tolist()

        past_key_values = None

        # Autoregressive generation loop
        for step in range(max_new_tokens):
            try:
                # Run inference on NPU
                outputs = self.session.run(None, inputs)

                # outputs[0] = logits, outputs[1:] = new KV cache
                logits = outputs[0]
                new_kv_cache = outputs[1:] if len(outputs) > 1 else None

                # Sample next token
                next_token = self._sample_token(logits, temperature, top_p)
                generated_ids.append(next_token)

                # Check for EOS
                if next_token == self.tokenizer.eos_token_id:
                    break

                # Prepare next iteration inputs (incremental generation)
                inputs = {
                    "input_ids": np.array([[next_token]], dtype=np.int64),
                    "attention_mask": np.ones((1, len(generated_ids)), dtype=np.int64)
                }

                # Update KV cache
                if use_cache and new_kv_cache:
                    past_key_values = new_kv_cache
                    for i, kv in enumerate(past_key_values):
                        inputs[f"past_key_values.{i}.key"] = kv
                        inputs[f"past_key_values.{i}.value"] = kv

                if step % 50 == 0 and step > 0:
                    logger.debug(f"AI_INFERENCE: Generated {step} tokens so far...")

            except Exception as e:
                logger.error(f"AI_INFERENCE: Inference failed at step {step}: {e}", exc_info=True)
                break

        # Decode generated tokens
        generated_text = self.tokenizer.decode(
            generated_ids,
            skip_special_tokens=True
        )

        # Cache result
        if use_cache:
            self.kv_cache.put(cache_key, np.array(generated_text))

        logger.info(f"AI_INFERENCE: Generation complete. Total tokens generated: {len(generated_ids)}, response_length: {len(generated_text)} chars")
        return generated_text

    def _generate_mock_response(self, prompt: str) -> str:
        """
        Generate mock response for simulation mode.

        Args:
            prompt: Input prompt

        Returns:
            Mock generated text
        """
        return (
            f"[SIMULATION MODE] Mock response to prompt: '{prompt[:100]}...'\n\n"
            "This is a simulated response. To use actual NPU acceleration:\n"
            "1. Quantize your model using QNN tools\n"
            "2. Set QNN_MODEL_CONTEXT environment variable\n"
            "3. Ensure libQnnHtp.so is in LD_LIBRARY_PATH"
        )

    def health_check(self) -> Dict:
        """
        Perform health check on the NPU engine.

        Returns:
            Status dictionary with engine metrics
        """
        try:
            # Test inference
            test_prompt = "Hello, this is a health check test."
            test_output = self.generate(
                test_prompt,
                max_new_tokens=10,
                use_cache=False
            )

            return {
                "status": "healthy",
                "engine": "NPU (Qualcomm HTP)",
                "mode": "production" if self.session else "simulation",
                "model_loaded": self.session is not None,
                "tokenizer_loaded": self.tokenizer is not None,
                "test_inference": "passed",
                "test_output": test_output[:100],
                "kv_cache_stats": self.kv_cache.get_stats()
            }
        except Exception as e:
            return {
                "status": "unhealthy",
                "error": str(e)
            }

    def get_model_info(self) -> Dict:
        """Return model metadata."""
        return {
            "model_path": self.model_context_path,
            "tokenizer_path": self.tokenizer_path,
            "max_context_length": self.max_context_length,
            "max_new_tokens": self.max_new_tokens,
            "temperature": self.temperature,
            "top_p": self.top_p,
            "quantization": "W4A16",
            "backend": "Qualcomm HTP (Hexagon)",
            "execution_provider": "QNNExecutionProvider",
            "mode": "production" if self.session else "simulation"
        }

    def __repr__(self) -> str:
        mode = "production" if self.session else "simulation"
        return f"NPUInferenceEngine(mode={mode}, initialized={self._initialized})"
