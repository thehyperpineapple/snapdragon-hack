import os
import logging
import numpy as np
from dotenv import load_dotenv
load_dotenv()

try:
    import onnxruntime as ort
    from transformers import AutoTokenizer
except ImportError:
    raise ImportError("Install: pip install onnxruntime-qnn transformers")

logger = logging.getLogger('ai')

class GenAIEngine:
    def __init__(self):
        model_path = os.getenv("MODEL_CONTEXT_PATH")
        tokenizer_path = os.getenv("TOKENIZER_PATH")
        qnn_lib_path = os.getenv("QNN_LIB_PATH")
        
        logger.info("Loading model with QNN ExecutionProvider...")
        
        # QNN options
        qnn_options = {
            'backend_path': os.path.join(qnn_lib_path, 'QnnHtp.dll')
        }
        
        # Create session with QNN
        self.session = ort.InferenceSession(
            model_path,
            providers=[('QNNExecutionProvider', qnn_options), 'CPUExecutionProvider']
        )
        
        logger.info(f"Active providers: {self.session.get_providers()}")
        
        # Load tokenizer
        self.tokenizer = AutoTokenizer.from_pretrained(tokenizer_path)
        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token
            
        logger.info("✓ Model and tokenizer loaded on NPU!")
        
    def generate(self, prompt, max_new_tokens=50, temperature=0.7, **kwargs):
        logger.info(f"Generating on NPU: {prompt[:50]}...")
        
        # Tokenize
        tokens = self.tokenizer.encode(prompt)
        input_ids = np.array([tokens], dtype=np.int64)
        
        generated_tokens = tokens.copy()
        
        # Initialize empty KV cache (32 layers for Phi-3)
        past_kv = {
            f'past_key_values.{i}.key': np.zeros((1, 32, 0, 96), dtype=np.float32)
            for i in range(32)
        }
        past_kv.update({
            f'past_key_values.{i}.value': np.zeros((1, 32, 0, 96), dtype=np.float32)
            for i in range(32)
        })
        
        # Generate tokens
        for step in range(max_new_tokens):
            # Prepare inputs
            inputs = {
                'input_ids': input_ids,
                'attention_mask': np.ones((1, len(generated_tokens)), dtype=np.int64)
            }
            inputs.update(past_kv)
            
            # Run inference
            outputs = self.session.run(None, inputs)
            logits = outputs[0]
            
            # Sample next token
            next_token_logits = logits[0, -1, :] / temperature
            probs = np.exp(next_token_logits) / np.sum(np.exp(next_token_logits))
            next_token = np.random.choice(len(probs), p=probs)
            
            generated_tokens.append(next_token)
            
            # Check for EOS
            if next_token == self.tokenizer.eos_token_id:
                break
                
            # Update for next iteration
            input_ids = np.array([[next_token]], dtype=np.int64)
            
            # Update KV cache from outputs
            if len(outputs) > 1:
                for i in range(32):
                    past_kv[f'past_key_values.{i}.key'] = outputs[1 + i*2]
                    past_kv[f'past_key_values.{i}.value'] = outputs[1 + i*2 + 1]
        
        result = self.tokenizer.decode(generated_tokens, skip_special_tokens=True)
        logger.info("✓ Generation complete!")
        return result
