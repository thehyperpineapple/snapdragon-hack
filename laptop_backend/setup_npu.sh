#!/bin/bash
###############################################################################
# NPU Setup Script for Snapdragon AI Integration
# This script sets up the environment for running 8B LLM on Qualcomm NPU
###############################################################################

set -e  # Exit on error

echo "============================================="
echo "Snapdragon NPU Setup Script"
echo "============================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running on ARM64
ARCH=$(uname -m)
if [[ "$ARCH" != "aarch64" && "$ARCH" != "arm64" ]]; then
    echo -e "${YELLOW}Warning: Not running on ARM64 architecture (detected: $ARCH)${NC}"
    echo -e "${YELLOW}NPU acceleration will not be available. Continuing in simulation mode...${NC}"
    echo ""
fi

# Step 1: Check Python version
echo "Step 1: Checking Python version..."
PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
REQUIRED_VERSION="3.10"

if [[ $(echo -e "$PYTHON_VERSION\n$REQUIRED_VERSION" | sort -V | head -n1) == "$REQUIRED_VERSION" ]]; then
    echo -e "${GREEN}✓ Python $PYTHON_VERSION found${NC}"
else
    echo -e "${RED}✗ Python 3.10+ required (found $PYTHON_VERSION)${NC}"
    exit 1
fi

# Step 2: Create virtual environment
echo ""
echo "Step 2: Creating virtual environment..."
if [ ! -d "venv_qnn" ]; then
    python3 -m venv venv_qnn
    echo -e "${GREEN}✓ Virtual environment created${NC}"
else
    echo -e "${YELLOW}⚠ Virtual environment already exists${NC}"
fi

# Activate virtual environment
source venv_qnn/bin/activate
echo -e "${GREEN}✓ Virtual environment activated${NC}"

# Step 3: Install Python dependencies
echo ""
echo "Step 3: Installing Python dependencies..."
pip install --upgrade pip > /dev/null 2>&1
pip install -r requirements.txt

echo -e "${GREEN}✓ Dependencies installed${NC}"

# Step 4: Check for QNN SDK
echo ""
echo "Step 4: Checking Qualcomm QNN SDK..."
if [ -z "$QNN_SDK_ROOT" ]; then
    echo -e "${YELLOW}⚠ QNN_SDK_ROOT not set${NC}"
    echo "Please download QAIRT from: https://developer.qualcomm.com/software/qualcomm-ai-stack"
    echo "Then set QNN_SDK_ROOT in .env.qnn"
else
    if [ -d "$QNN_SDK_ROOT" ]; then
        echo -e "${GREEN}✓ QNN SDK found at: $QNN_SDK_ROOT${NC}"
    else
        echo -e "${RED}✗ QNN SDK not found at: $QNN_SDK_ROOT${NC}"
    fi
fi

# Step 5: Create directories
echo ""
echo "Step 5: Creating model directories..."
mkdir -p models/llama3_8b_onnx
mkdir -p models/llama3_htp_context
mkdir -p qnn_logs
echo -e "${GREEN}✓ Directories created${NC}"

# Step 6: Setup environment file
echo ""
echo "Step 6: Setting up environment configuration..."
if [ ! -f ".env.qnn" ]; then
    cp .env.qnn.example .env.qnn
    echo -e "${GREEN}✓ Created .env.qnn from template${NC}"
    echo -e "${YELLOW}⚠ Please edit .env.qnn with your specific paths${NC}"
else
    echo -e "${YELLOW}⚠ .env.qnn already exists${NC}"
fi

# Step 7: Test AI layer import
echo ""
echo "Step 7: Testing AI layer..."
python3 - <<EOF
try:
    from ai import get_gemini_engine
    print("${GREEN}✓ AI layer imports successfully${NC}")
except ImportError as e:
    print("${RED}✗ AI layer import failed: {e}${NC}")
    exit(1)
EOF

# Step 8: Summary
echo ""
echo "============================================="
echo "Setup Complete!"
echo "============================================="
echo ""
echo "Next steps:"
echo "1. Edit .env.qnn with your QNN SDK paths"
echo "2. Download Llama 3 8B model (see NPU_INTEGRATION_GUIDE.md)"
echo "3. Run model quantization script (see guide)"
echo "4. Start Flask server: python app.py"
echo ""
echo "For detailed instructions, see:"
echo "  - NPU_INTEGRATION_GUIDE.md"
echo ""
echo "To activate the virtual environment later:"
echo "  source venv_qnn/bin/activate"
echo ""
echo -e "${GREEN}Happy coding!${NC}"
