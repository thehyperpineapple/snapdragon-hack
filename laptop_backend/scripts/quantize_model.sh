#!/bin/bash
###############################################################################
# Model Quantization Script for Snapdragon NPU
# Converts Llama 3 8B to W4A16 QNN format and compiles for HTP
###############################################################################

set -e

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "============================================="
echo "Llama 3 8B NPU Quantization Script"
echo "============================================="
echo ""

# Check QNN SDK
if [ -z "$QNN_SDK_ROOT" ]; then
    echo -e "${RED}Error: QNN_SDK_ROOT not set${NC}"
    echo "Please set it in .env.qnn and run: source .env.qnn"
    exit 1
fi

if [ ! -d "$QNN_SDK_ROOT" ]; then
    echo -e "${RED}Error: QNN SDK not found at $QNN_SDK_ROOT${NC}"
    exit 1
fi

echo -e "${GREEN}✓ QNN SDK found at: $QNN_SDK_ROOT${NC}"
echo ""

# Set paths
MODEL_ID="meta-llama/Meta-Llama-3-8B-Instruct"
ONNX_PATH="./models/llama3_8b_onnx"
QNN_IR_PATH="./models/llama3_qnn.cpp"
HTP_OUTPUT_DIR="./models/llama3_htp_context"
HTP_BINARY="llama3_8b_w4a16.bin"

# Step 1: Export to ONNX
echo "Step 1: Exporting Llama 3 8B to ONNX format..."
if [ -f "$ONNX_PATH/decoder_model_merged.onnx" ]; then
    echo -e "${YELLOW}⚠ ONNX model already exists. Skipping export.${NC}"
    echo -e "${YELLOW}  Delete $ONNX_PATH to re-export.${NC}"
else
    python3 - <<EOF
from optimum.onnxruntime import ORTModelForCausalLM
from transformers import AutoTokenizer
import sys

try:
    print("Downloading and exporting model (this may take 10-15 minutes)...")
    model = ORTModelForCausalLM.from_pretrained(
        "$MODEL_ID",
        export=True,
        use_merged=True
    )
    model.save_pretrained("$ONNX_PATH")

    tokenizer = AutoTokenizer.from_pretrained("$MODEL_ID")
    tokenizer.save_pretrained("$ONNX_PATH")

    print("✓ Model exported successfully")
except Exception as e:
    print(f"Error exporting model: {e}")
    sys.exit(1)
EOF
    echo -e "${GREEN}✓ ONNX export complete${NC}"
fi

echo ""

# Step 2: Convert to QNN IR with W4A16 quantization
echo "Step 2: Converting to QNN IR with W4A16 quantization..."

${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-onnx-converter \
    --input_network ${ONNX_PATH}/decoder_model_merged.onnx \
    --output_path ${QNN_IR_PATH} \
    --input_list input_ids,attention_mask \
    --quantization_overrides "weights:4" \
    --act_bitwidth 16 \
    --float_fallback \
    --keep_quant_nodes \
    --log_level verbose

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ QNN IR conversion complete${NC}"
else
    echo -e "${RED}✗ QNN IR conversion failed${NC}"
    exit 1
fi

echo ""

# Step 3: Create HTP configuration
echo "Step 3: Creating HTP configuration..."

cat > htp_config.json <<EOF
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
EOF

echo -e "${GREEN}✓ HTP configuration created${NC}"
echo ""

# Step 4: Compile to HTP context binary
echo "Step 4: Compiling to HTP context binary..."
mkdir -p ${HTP_OUTPUT_DIR}

${QNN_SDK_ROOT}/bin/x86_64-linux-clang/qnn-context-binary-generator \
    --backend ${QNN_SDK_ROOT}/lib/x86_64-linux-clang/libQnnHtp.so \
    --model ${QNN_IR_PATH} \
    --output_dir ${HTP_OUTPUT_DIR} \
    --binary_file ${HTP_BINARY} \
    --log_level verbose \
    --config_file htp_config.json

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ HTP context binary generated${NC}"
else
    echo -e "${RED}✗ HTP context binary generation failed${NC}"
    exit 1
fi

echo ""

# Step 5: Verify output
echo "Step 5: Verifying output files..."

if [ -f "${HTP_OUTPUT_DIR}/${HTP_BINARY}" ]; then
    BINARY_SIZE=$(du -h "${HTP_OUTPUT_DIR}/${HTP_BINARY}" | cut -f1)
    echo -e "${GREEN}✓ Context binary: ${HTP_OUTPUT_DIR}/${HTP_BINARY} (${BINARY_SIZE})${NC}"
else
    echo -e "${RED}✗ Context binary not found${NC}"
    exit 1
fi

if [ -d "${ONNX_PATH}" ] && [ -f "${ONNX_PATH}/tokenizer_config.json" ]; then
    echo -e "${GREEN}✓ Tokenizer: ${ONNX_PATH}${NC}"
else
    echo -e "${RED}✗ Tokenizer not found${NC}"
    exit 1
fi

echo ""

# Summary
echo "============================================="
echo "Quantization Complete!"
echo "============================================="
echo ""
echo "Model artifacts created:"
echo "  1. ONNX Model: ${ONNX_PATH}"
echo "  2. QNN IR: ${QNN_IR_PATH}"
echo "  3. HTP Binary: ${HTP_OUTPUT_DIR}/${HTP_BINARY}"
echo "  4. Tokenizer: ${ONNX_PATH}"
echo ""
echo "Update your .env.qnn with:"
echo "  QNN_MODEL_CONTEXT=${HTP_OUTPUT_DIR}/${HTP_BINARY}"
echo "  TOKENIZER_PATH=${ONNX_PATH}"
echo ""
echo "Now you can start the Flask server with NPU acceleration!"
echo -e "${GREEN}Ready to go! 🚀${NC}"
