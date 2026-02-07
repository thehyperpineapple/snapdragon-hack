# Snapdragon NPU Integration Guide
## 8B LLM on Qualcomm Hexagon HTP via QAIRT/QNN

**Author:** Senior Embedded AI Engineer
**Stack:** QAIRT 2.26+ / onnxruntime-qnn / Flask / Python 3.10+
**Target Hardware:** Snapdragon 8 Gen 2/3, X Elite (with Hexagon NPU)

---

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [Model Preparation](#model-preparation)
5. [Backend Integration](#backend-integration)
6. [API Endpoints](#api-endpoints)
7. [Performance Tuning](#performance-tuning)
8. [Troubleshooting](#troubleshooting)

---

## Overview

This guide provides step-by-step instructions for integrating an 8B parameter LLM (Llama 3) to run on Qualcomm's Snapdragon NPU (Hexagon Tensor Processor) using the QNN (Qualcomm Neural Network) SDK.

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Flask Backend                        │
├─────────────────────────────────────────────────────────┤
│  Routes (plan_ai.py, user_ai.py, tracking_ai.py)      │
│              ↓                                          │
│  AI Layer (Prompts + NPU Engine + Cache Manager)       │
│              ↓                                          │
│  ONNX Runtime with QNNExecutionProvider                 │
│              ↓                                          │
│  Qualcomm QNN SDK (libQnnHtp.so)                       │
│              ↓                                          │
│  Snapdragon Hexagon HTP (NPU Hardware)                  │
└─────────────────────────────────────────────────────────┘
```

### Key Features
- **W4A16 Quantization:** 4-bit weights, 16-bit activations
- **KV-Cache Management:** Efficient autoregressive generation
- **Singleton Pattern:** Single engine instance across requests
- **Graceful Fallback:** Simulation mode if NPU unavailable
- **Streaming Support:** Token-by-token generation

---

## Prerequisites

### Hardware Requirements
- **Device:** Snapdragon 8 Gen 2+, Snapdragon X Elite, or equivalent
- **NPU:** Hexagon HTP with minimum 4GB dedicated memory
- **RAM:** 12GB+ recommended for 8B model
- **Storage:** 10GB free space for model artifacts

### Software Requirements
- **OS:** Ubuntu 22.04 LTS or Android 13+ (for on-device)
- **Python:** 3.10 or 3.11
- **QNN SDK:** QAIRT 2.26.0+ (download from Qualcomm Developer Network)
- **ONNX Runtime:** 1.16.0+ with QNN EP support

---

## Environment Setup

### Step 1: Install Qualcomm AI Stack (QAIRT)

```bash
# Download QAIRT from Qualcomm Developer Network
# https://developer.qualcomm.com/software/qualcomm-ai-stack

# Extract SDK
cd /opt/qcom/aistack
tar -xzf qairt-2.26.0.tar.gz

# Set environment variables
export QNN_SDK_ROOT="/opt/qcom/aistack/qairt/2.26.0"
export LD_LIBRARY_PATH="${QNN_SDK_ROOT}/lib/x86_64-linux-clang:${LD_LIBRARY_PATH}"
export PATH="${QNN_SDK_ROOT}/bin/x86_64-linux-clang:${PATH}"

# Verify installation
qnn-platform-validator
```

### Step 2: Install Python Dependencies

```bash
cd /path/to/laptop_backend

# Create virtual environment
python3.10 -m venv venv_qnn
source venv_qnn/bin/activate

# Install dependencies
pip install --upgrade pip
pip install -r requirements.txt

# Install ONNX Runtime with QNN support
# Download from: https://github.com/microsoft/onnxruntime/releases
pip install onnxruntime-qnn-1.16.0-linux-aarch64.whl
```

### Step 3: Configure Environment Variables

Create `.env.qnn` file:

```bash
# QNN SDK Paths
QNN_SDK_ROOT=/opt/qcom/aistack/qairt/2.26.0
QNN_HTP_BACKEND=${QNN_SDK_ROOT}/lib/aarch64-android/libQnnHtp.so
QNN_SYSTEM_LIB=${QNN_SDK_ROOT}/lib/aarch64-android/libQnnSystem.so

# Model Paths
QNN_MODEL_CONTEXT=./models/llama3_htp_context/llama3_8b_w4a16.bin
TOKENIZER_PATH=./models/llama3_8b_onnx

# Inference Configuration
MAX_CONTEXT_LENGTH=2048
MAX_NEW_TOKENS=512
TEMPERATURE=0.7
TOP_P=0.9
KV_CACHE_SIZE_MB=512

# Flask Configuration
FLASK_HOST=0.0.0.0
FLASK_PORT=5000
FLASK_DEBUG=false
```

Load environment:
```bash
source .env.qnn
```

---

## Model Preparation

### Step 1: Export Llama 3 8B to ONNX

```bash
# Create model directory
mkdir -p models/llama3_8b_onnx

# Export using Optimum
python - <<EOF
from optimum.onnxruntime import ORTModelForCausalLM
from transformers import AutoTokenizer

model_id = "meta-llama/Meta-Llama-3-8B-Instruct"
onnx_path = "./models/llama3_8b_onnx"

print("Exporting Llama 3 8B to ONNX format...")
model = ORTModelForCausalLM.from_pretrained(
    model_id,
    export=True,
    use_merged=True  # Merge decoder blocks for efficiency
)
model.save_pretrained(onnx_path)

tokenizer = AutoTokenizer.from_pretrained(model_id)
tokenizer.save_pretrained(onnx_path)
print(f"✓ Model exported to {onnx_path}")
EOF
```

### Step 2: Quantize to W4A16 (4-bit weights, 16-bit activations)

```bash
# Convert ONNX to QNN IR
${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-onnx-converter \
    --input_network ./models/llama3_8b_onnx/decoder_model_merged.onnx \
    --output_path ./models/llama3_qnn.cpp \
    --input_list input_ids,attention_mask \
    --quantization_overrides "weights:4" \
    --act_bitwidth 16 \
    --float_fallback \
    --keep_quant_nodes

echo "✓ Model converted to QNN IR with W4A16 quantization"
```

### Step 3: Compile to HTP Context Binary

```bash
mkdir -p models/llama3_htp_context

# Generate context binary for Hexagon HTP
${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
    --backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtp.so \
    --model ./models/llama3_qnn.cpp \
    --output_dir ./models/llama3_htp_context \
    --binary_file llama3_8b_w4a16.bin \
    --log_level verbose \
    --config_file htp_config.json

echo "✓ HTP context binary generated"
```

### Step 4: Model Sharding (for memory optimization)

Create `htp_config.json`:

```json
{
  "sharding": {
    "enabled": true,
    "strategy": "layer_wise",
    "num_shards": 4,
    "shard_boundaries": [0, 8, 16, 24, 32],
    "memory_per_shard_mb": 900
  },
  "performance": {
    "mode": "burst",
    "optimization_level": 3,
    "precision": "mixed_w4a16"
  }
}
```

---

## Backend Integration

### Step 1: Verify AI Layer Structure

```bash
ai/
├── __init__.py
├── inference/
│   ├── __init__.py
│   ├── npu_engine.py       # NPU inference engine
│   └── cache_manager.py    # KV cache management
├── prompts/
│   ├── __init__.py
│   ├── plan_prompts.py     # Diet/workout plan prompts
│   ├── nutrition_prompts.py # Nutrition analysis prompts
│   └── health_prompts.py   # Health metrics prompts
└── utils/
    ├── __init__.py
    └── model_utils.py      # Helper functions
```

### Step 2: Register AI-Enhanced Blueprints

Edit `app.py`:

```python
def create_app():
    app = Flask(__name__)
    CORS(app)

    # Register existing blueprints
    from routes import auth_bp, user_bp, plan_bp, tracking_bp
    app.register_blueprint(auth_bp)
    app.register_blueprint(user_bp)
    app.register_blueprint(plan_bp)
    app.register_blueprint(tracking_bp)

    # Register AI-enhanced blueprints
    from routes.plan_ai import plan_ai_bp
    from routes.user_ai import user_ai_bp
    app.register_blueprint(plan_ai_bp, url_prefix='/ai/users')
    app.register_blueprint(user_ai_bp, url_prefix='/ai/users')

    # NPU health check endpoint
    @app.route('/ai/health', methods=['GET'])
    def ai_health_check():
        from ai import get_npu_engine
        engine = get_npu_engine()
        return jsonify(engine.health_check()), 200

    return app
```

### Step 3: Initialize NPU Engine on Startup

Add to `app.py`:

```python
# Before app.run()
if __name__ == '__main__':
    print("Initializing NPU Inference Engine...")
    from ai import get_npu_engine
    engine = get_npu_engine()
    print("✓ NPU Engine ready")
    print(engine.get_model_info())

    app.run(host=host, port=port, debug=debug)
```

---

## API Endpoints

### AI-Enhanced Plan Generation

**POST** `/ai/users/<user_id>/plan`

```bash
curl -X POST http://localhost:5000/ai/users/user123/plan \
  -H "Content-Type: application/json" \
  -d '{
    "plan_type": "combined",
    "duration_weeks": 4,
    "intensity": "moderate",
    "specific_goals": ["weight_loss", "muscle_gain"],
    "use_ai": true
  }'
```

Response:
```json
{
  "message": "Plan created successfully",
  "plan": {
    "plan_type": "combined",
    "ai_generated": true,
    "generation_method": "npu_llm",
    "diet": [...],
    "workouts": [...]
  }
}
```

### AI Health Analysis

**GET** `/ai/users/<user_id>/health/analyze`

```bash
curl http://localhost:5000/ai/users/user123/health/analyze
```

Response:
```json
{
  "analysis": {
    "bmi_assessment": {
      "category": "Normal",
      "description": "Your BMI is within healthy range"
    },
    "health_insights": [...],
    "recommendations": [...]
  }
}
```

### AI Nutrition Recommendations

**PUT** `/ai/users/<user_id>/nutrition`

```bash
curl -X PUT http://localhost:5000/ai/users/user123/nutrition \
  -H "Content-Type: application/json" \
  -d '{
    "calorie_goal": 2000,
    "diet_type": "vegetarian",
    "generate_recommendations": true
  }'
```

---

## Performance Tuning

### HTP Performance Modes

Edit environment variable:
```bash
# Burst mode (highest performance, higher power)
HTP_PERFORMANCE_MODE=burst

# Balanced mode (good performance, moderate power)
HTP_PERFORMANCE_MODE=balanced

# Sustained mode (steady performance, lower power)
HTP_PERFORMANCE_MODE=sustained_high_performance
```

### KV Cache Optimization

Adjust cache size based on available memory:
```bash
# For 12GB RAM devices
KV_CACHE_SIZE_MB=512

# For 16GB+ RAM devices
KV_CACHE_SIZE_MB=1024
```

### Token Generation Speed

Expected performance on Snapdragon 8 Gen 3:
- **First token:** ~200-300ms
- **Subsequent tokens:** ~30-50ms/token
- **Total (100 tokens):** ~3-5 seconds

### Memory Usage

Typical memory footprint:
- **Model (W4A16):** ~4.5GB
- **KV Cache:** 512MB
- **Runtime overhead:** ~500MB
- **Total:** ~5.5GB

---

## Troubleshooting

### Issue: QNN EP not loading

```bash
# Check library availability
ldd ${QNN_SDK_ROOT}/lib/aarch64-android/libQnnHtp.so

# Verify LD_LIBRARY_PATH
echo $LD_LIBRARY_PATH
```

### Issue: Model compilation fails

```bash
# Check QNN SDK version
${QNN_SDK_ROOT}/bin/qnn-platform-validator --version

# Verify ONNX model integrity
python -c "import onnx; onnx.checker.check_model('models/llama3_8b_onnx/decoder_model_merged.onnx')"
```

### Issue: Out of memory during inference

**Solution:** Enable model sharding or reduce context length

```bash
# Reduce context window
MAX_CONTEXT_LENGTH=1024
MAX_NEW_TOKENS=256

# Enable aggressive cache eviction
KV_CACHE_SIZE_MB=256
```

### Issue: Slow inference speed

**Solution:** Optimize HTP configuration

```bash
# Use burst mode
HTP_PERFORMANCE_MODE=burst

# Increase graph optimization
HTP_GRAPH_FINALIZATION_OPTIMIZATION_MODE=3

# Disable profiling
ENABLE_PROFILING=false
```

---

## Testing

### Test NPU Engine

```bash
python - <<EOF
from ai import get_npu_engine

engine = get_npu_engine()
print(engine.get_model_info())

# Test generation
output = engine.generate("Hello, how are you?", max_new_tokens=50)
print(output)

# Check cache stats
print(engine.kv_cache.get_stats())
EOF
```

### Run Health Check

```bash
curl http://localhost:5000/ai/health
```

Expected output:
```json
{
  "status": "healthy",
  "engine": "NPU (Qualcomm HTP)",
  "mode": "production",
  "model_loaded": true,
  "tokenizer_loaded": true,
  "test_inference": "passed"
}
```

---

## Production Deployment

### On-Device (Android)

1. Build ONNX Runtime for Android
2. Package QNN libraries in APK
3. Use NDK for native integration
4. Deploy Flask as Android service

### Edge Server (Linux ARM64)

```bash
# Install systemd service
sudo cp laptop_backend.service /etc/systemd/system/
sudo systemctl enable laptop_backend
sudo systemctl start laptop_backend
```

### Docker Deployment

```dockerfile
FROM arm64v8/ubuntu:22.04

# Install QNN SDK
COPY qairt-2.26.0.tar.gz /opt/qcom/
RUN cd /opt/qcom && tar -xzf qairt-2.26.0.tar.gz

# Install Python dependencies
COPY requirements.txt /app/
RUN pip install -r /app/requirements.txt

# Copy application
COPY . /app/
WORKDIR /app

CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:5000", "app:app"]
```

---

## References

- **Qualcomm AI Stack:** https://developer.qualcomm.com/software/qualcomm-ai-stack
- **ONNX Runtime QNN EP:** https://onnxruntime.ai/docs/execution-providers/QNN-ExecutionProvider.html
- **Llama 3 Model Card:** https://huggingface.co/meta-llama/Meta-Llama-3-8B-Instruct

---

**For support, contact the development team or file an issue on GitHub.**
