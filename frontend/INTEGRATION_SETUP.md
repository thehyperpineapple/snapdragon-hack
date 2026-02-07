# Llama 3.2 1B Instruct - Android Integration Guide

## Status: Model Export in Progress ⏳

Your Qualcomm API is currently exporting the Llama 3.2 1B model for Snapdragon 8 Elite.

**Compilation Jobs Submitted:**
- Prompt Processing (3 jobs): jgnerxzkg, jp0rekm9p, j5q29wln5
- Token Generation (3 jobs): jpev426o5, jg9498o85, jg9498o85+

**Track Progress:**
https://workbench.aihub.qualcomm.com/jobs/jgnerxzkg/

Expected completion: **30-60 minutes** from submission

---

## What's Being Created

### Model Output Structure
```
llama_model/
├── genie_bundle/
│   ├── genie_config.json              # Genie runtime config
│   ├── *.serialized.bin               # QNN context binaries (compiled model)
│   └── metadata.json
├── tokenizer.json                      # Token vocabulary (128k tokens)
├── Llama_3_2_1B_AIMETOnnx_w4a16.onnx # ONNX fallback
└── README.md
```

### What Each File Does

| File | Purpose |
|------|---------|
| `*.serialized.bin` | Optimized model weights compiled for Snapdragon NPU |
| `genie_config.json` | Configuration for Genie runtime (context length, batch size, etc.) |
| `tokenizer.json` | Maps words ↔ token IDs (128k vocabulary) |
| `*_AIMETOnnx_w4a16.onnx` | ONNX version with 4-bit quantization (fallback) |

---

## Integration Steps (After Export Completes)

### Step 1: Copy Model to Android App

When export completes, you'll have `llama_model/` folder. Copy files to your app:

```bash
# Create assets directory if needed
mkdir -p frontend/app/src/main/assets/models/llm

# Copy compiled binaries
cp llama_model/genie_bundle/*.bin frontend/app/src/main/assets/models/llm/

# Copy configuration and tokenizer
cp llama_model/genie_bundle/genie_config.json frontend/app/src/main/assets/models/llm/
cp llama_model/tokenizer.json frontend/app/src/main/assets/models/llm/
```

### Step 2: Add QAIRT SDK Dependencies

In `build.gradle.kts`, add:

```kotlin
dependencies {
    // QAIRT SDK for Genie Runtime (QNN inference)
    implementation("com.qualcomm.qti.qairt:core:2.29.0")  // Adjust version as needed
    
    // For JSON loading
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

// Add QAIRT SDK repository
repositories {
    maven {
        url = uri("https://qualcomm-artifactory.qti.qualcomm.com/artifactory/api/gradle/qairt-releases")
    }
}
```

### Step 3: Add Permissions

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

### Step 4: Integrate GenieService into ChatScreen

Update your ChatScreen.kt:

```kotlin
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Genie service
    val genieService = remember { GenieService(LocalContext.current) }
    var modelReady by remember { mutableStateOf(false) }
    var loadingError by remember { mutableStateOf("") }
    
    // Load model on startup
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val initialized = genieService.initialize()
            if (initialized) {
                modelReady = true
                messages = listOf(
                    ChatMessage(
                        content = "👋 Llama 3.2 1B is ready! Ask me anything about fitness and nutrition.",
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                loadingError = "Failed to load model. Check logcat for details."
            }
        }
    }
    
    // ... rest of your ChatScreen code ...
    
    // Send message handler
    if (!modelReady) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (loadingError.isNotEmpty()) {
                Text(loadingError, color = Color.Red)
            } else {
                Text("Loading LLM model...")
            }
        }
    } else {
        // Generate AI response
        val onSendMessage = { text: String ->
            if (text.isNotBlank()) {
                messages = messages + ChatMessage(content = text, isUser = true)
                messageText = ""
                isTyping = true
                
                coroutineScope.launch {
                    val response = genieService.generateResponse(text)
                    messages = messages + ChatMessage(content = response, isUser = false)
                    isTyping = false
                }
            }
        }
        
        // Your existing UI code...
    }
}
```

---

## Performance Expectations

### Compiled Model Performance (Snapdragon 8 Elite)
- **First Token Latency**: ~500-800ms
- **Token Generation Speed**: 2-3 tokens/second
- **Full Response Time**: ~10-20 seconds (for 20 token output)
- **Memory Usage**: 800MB-1.2GB peak
- **Model Size**: 500-600MB (4-bit quantized)

### ONNX Fallback Performance (if NPU unavailable)
- Same model, CPU execution
- ~2-3x slower (10x slower than NPU)
- Still achievable on Snapdragon 8/8+ devices

---

## Troubleshooting

### Model Not Found
```
Error: Failed to load model assets
```
**Solution**: Verify files are in `app/src/main/assets/models/llm/`:
```bash
ls -la app/src/main/assets/models/llm/
# Should show: genie_config.json, tokenizer.json, *.bin files
```

### Out of Memory (OOM)
```
fatal signal 9 (SIGKILL), code -1 (UNKNOWN) in tid 12345

"Unable to allocate 1.2GB"
```
**Solutions**:
1. Run on physical device (not emulator)
2. Close other apps before test
3. Reduce context length in genie_config.json from 4096 to 2048

### Genie SDK Version Mismatch
```
libGenie.so: cannot load library
```
**Solution**: Ensure QAIRT SDK version matches what was used for compilation. Check job page:
- Job: https://workbench.aihub.qualcomm.com/jobs/jgnerxzkg/
- Look for "QAIRT SDK Version: x.x.x"
- Download matching QAIRT SDK from Qualcomm

### Model too Large for Device
If device has < 4GB RAM, reduce model context length:

```bash
# Re-export with smaller context (30-45 minutes)
python -m qai_hub_models.models.llama_v3_2_1b_instruct.export \
  --device "Snapdragon 8 Elite QRD" \
  --context-length 2048 \  # Reduced from 4096
  --skip-profiling \
  --output-dir llama_model_2k
```

---

## Next Steps When Export Completes

1. ✅ **Monitor Compilation**: Check job status pages (links above)
2. ✅ **Download Artifacts**: Go to job page, download `genie_bundle/`
3. ✅ **Copy to App**: Run commands from Step 1 in "Integration Steps"
4. ✅ **Update build.gradle.kts**: Add QAIRT SDK dependencies
5. ✅ **Build APK**: `./gradlew.bat clean build`
6. ✅ **Test on Device**: Install and test chat inference

---

## Device Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **Android Version** | 15 | 15+ |
| **RAM** | 8GB | 12GB+ |
| **Storage** | 1.5GB free | 2GB free |
| **Processor** | Snapdragon 8 Gen 3 | Snapdragon 8 Elite |
| **Architecture** | arm64-v8a | arm64-v8a |

---

## Model Information

- **Model**: Llama 3.2 1B Instruct
- **Parameters**: ~1.2 billion
- **Context Length**: 4096 tokens
- **Quantization**: w4a16 (4-bit weights, 16-bit activations)
- **Size**: ~500-600MB (compressed)
- **Tokenizer**: BPE with 128k token vocabulary
- **Chat Template**: Official Llama 3.2 format

---

## References

- [Llama 3.2 Model Card](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct)
- [Qualcomm AI Hub](https://aihub.qualcomm.com/)
- [QAIRT SDK Documentation](https://qpm.qualcomm.com/#/main/tools/details/Qualcomm_AI_Runtime_SDK)
- [Genie SDK Guide](https://github.com/quic/ai-hub-apps/tree/main/tutorials/llm_on_genie)

---

## Support

- **Compilation Issues**: Check job page logs in Qualcomm AI Hub
- **Integration Issues**: Check Android logcat: `adb logcat | grep -i genie`
- **Model Issues**: See troubleshooting section above
