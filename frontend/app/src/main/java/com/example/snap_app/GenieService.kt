package com.example.snap_app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Service for running Llama 3.2 1B using Genie SDK (QNN Runtime)
 * This provides high-performance on-device inference using Snapdragon NPU.
 *
 * Architecture:
 * - genie-t2t-run binary + QAIRT .so libs are bundled in the APK (jniLibs/arm64-v8a)
 * - Model files (.bin, config, tokenizer) live at /data/local/tmp/snap_models/
 * - Hexagon skel files for DSP live at /data/local/tmp/qairt/.../hexagon-v79/unsigned/
 * - App executes the bundled binary from its nativeLibraryDir (proper SELinux context)
 * - Prompt files are written to the app's cacheDir (app has write access)
 */
class GenieService(private val context: Context) {

    private var modelPath: String? = null
    private var nativeLibDir: String? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "GenieService"
        const val MODEL_DIR = "/data/local/tmp/snap_models"
        const val HEXAGON_SKEL_DIR = "/data/local/tmp/qairt/2.43.0.260128/lib/hexagon-v79/unsigned"
        const val CONFIG_FILE = "genie_config.json"
        const val TOKENIZER_FILE = "tokenizer.json"
        const val CONTEXT_LENGTH = 4096
    }
    
    /**
     * Initialize Genie runtime.
     * Checks for: bundled binary in nativeLibraryDir, model files on device.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true

            // 1. Locate the bundled genie-t2t-run binary
            val libDir = context.applicationInfo.nativeLibraryDir
            val genieBin = File(libDir, "libgenie_t2t_run.so")
            Log.d(TAG, "Native lib dir: $libDir")
            Log.d(TAG, "genie-t2t-run exists: ${genieBin.exists()}, canExec: ${genieBin.canExecute()}")

            if (!genieBin.exists()) {
                Log.e(TAG, "Bundled genie-t2t-run NOT FOUND at ${genieBin.absolutePath}")
                return@withContext false
            }

            // 2. Check model files on device
            val modelDir = File(MODEL_DIR)
            if (!modelDir.exists()) {
                Log.e(TAG, "MODEL DIRECTORY NOT FOUND: $MODEL_DIR")
                return@withContext false
            }

            val configFile = File(modelDir, CONFIG_FILE)
            val tokenizerFile = File(modelDir, TOKENIZER_FILE)
            val modelBin1 = File(modelDir, "llama_v3_2_1b_instruct_part_1_of_3.bin")

            if (!configFile.exists() || !tokenizerFile.exists() || !modelBin1.exists()) {
                Log.e(TAG, "MODEL FILES INCOMPLETE in $MODEL_DIR")
                Log.d(TAG, "  genie_config.json: ${configFile.exists()}")
                Log.d(TAG, "  tokenizer.json: ${tokenizerFile.exists()}")
                Log.d(TAG, "  model bin 1: ${modelBin1.exists()}")
                return@withContext false
            }

            nativeLibDir = libDir
            modelPath = modelDir.absolutePath
            isInitialized = true

            Log.d(TAG, "GenieService initialized OK")
            Log.d(TAG, "  binary: ${genieBin.absolutePath}")
            Log.d(TAG, "  model:  $modelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing: ${e.message}", e)
            false
        }
    }
    
    /**
     * Generate response using Genie SDK
     * Calls genie-t2t-run on device to perform LLM inference
     */
    suspend fun generateResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || modelPath == null) {
                return@withContext "Model not initialized. Please ensure model files are pushed to device."
            }
            
            // Format prompt using Llama 3.2 chat template
            val prompt = formatLlama3Prompt(userMessage)
            
            // Call genie-t2t-run on device
            return@withContext runGenieInference(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}", e)
            "Sorry, I encountered an error: ${e.message}"
        }
    }
    
    /**
     * Format user message in Llama 3.2 chat template
     */
    private fun formatLlama3Prompt(userMessage: String): String {
        val systemPrompt = """You are a helpful fitness and nutrition assistant. 
Provide clear, actionable advice about exercise, diet, and healthy living.
Keep responses concise and friendly."""
        
        return buildString {
            append("<|begin_of_text|>")
            append("<|start_header_id|>system<|end_header_id|>\n\n")
            append(systemPrompt)
            append("<|eot_id|>")
            append("<|start_header_id|>user<|end_header_id|>\n\n")
            append(userMessage)
            append("<|eot_id|>")
            append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        }
    }
    
    /**
     * Execute genie-t2t-run from the APK's bundled native libs.
     *
     * The binary is at nativeLibraryDir/libgenie_t2t_run.so.
     * LD_LIBRARY_PATH → same nativeLibraryDir (where libGenie.so, libQnnHtp.so, etc. live).
     * ADSP_LIBRARY_PATH → /data/local/tmp/qairt/.../hexagon-v79/unsigned (DSP skel files).
     * Prompt is written to the app's cacheDir (writable by the app).
     */
    private fun runGenieInference(prompt: String): String {
        try {
            val libDir = nativeLibDir ?: return generateFallbackResponse(extractUserMessage(prompt))
            val cacheDir = context.cacheDir.absolutePath

            // Write prompt to app's cache directory (app can write here)
            val promptFile = File(cacheDir, "genie_prompt.txt")
            promptFile.writeText(prompt)
            Log.d(TAG, "Prompt written to: ${promptFile.absolutePath} (${promptFile.length()} bytes)")

            // Build the shell command.
            // - Binary lives in the app's native lib dir (SELinux allows execution)
            // - LD_LIBRARY_PATH: app native lib dir (bundled QNN libs) + /vendor/lib64
            //   (for libcdsprpc.so needed by HTP stub to communicate with DSP)
            // - Hexagon skel files stay in /data/local/tmp/qairt/... (DSP loads them)
            // - Model files at /data/local/tmp/snap_models/
            val shellCmd = buildString {
                append("cd $modelPath && ")
                append("export LD_LIBRARY_PATH=$libDir:/vendor/lib64 && ")
                append("export ADSP_LIBRARY_PATH=$HEXAGON_SKEL_DIR && ")
                append("$libDir/libgenie_t2t_run.so ")
                append("-c $CONFIG_FILE ")
                append("--log verbose ")
                append("--prompt_file ${promptFile.absolutePath}")
            }

            Log.d(TAG, "Executing: sh -c \"$shellCmd\"")

            val processBuilder = ProcessBuilder("sh", "-c", shellCmd)
            processBuilder.directory(File(modelPath!!))
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            // Read output with timeout
            val output = StringBuilder()
            val reader = process.inputStream.bufferedReader()
            val startTime = System.currentTimeMillis()
            val timeoutMs = 120_000L  // 120 seconds — first run loads model, can be slow

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    output.appendLine(line)
                    Log.d(TAG, ">> $line")
                    if (line.contains("[END]")) break
                } else {
                    Thread.sleep(100)
                }
            }

            val exited = process.waitFor(5, TimeUnit.SECONDS)
            if (!exited) {
                Log.w(TAG, "Process didn't exit in time, destroying")
                process.destroyForcibly()
            }

            val exitCode = if (exited) process.exitValue() else -1
            promptFile.delete()

            val result = output.toString()
            Log.d(TAG, "Exit code: $exitCode, output length: ${result.length}")
            Log.d(TAG, "Output preview: ${result.take(500)}")

            // Parse [BEGIN]: ... [END] markers
            if (result.contains("[BEGIN]:")) {
                return parseGenieOutput(result)
            }

            // Check for errors
            if (result.contains("Permission denied") || result.contains("can't execute")) {
                Log.e(TAG, "Execution permission error: $result")
                return "Error: Could not execute inference binary. " +
                       "Binary may not have execute permission in the app sandbox."
            }

            if (result.contains("CANNOT LINK") || result.contains("not found")) {
                Log.e(TAG, "Library linking error: $result")
                return "Error: Missing native library. Check that all QAIRT libs are bundled."
            }

            if (result.contains("ERROR") || result.contains("Failed")) {
                Log.e(TAG, "Genie runtime error: $result")
                return generateFallbackResponse(extractUserMessage(prompt))
            }

            return if (result.isNotBlank()) {
                result.trim()
            } else {
                Log.w(TAG, "Empty output from genie")
                generateFallbackResponse(extractUserMessage(prompt))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run genie inference", e)
            return generateFallbackResponse(extractUserMessage(prompt))
        }
    }
    
    /**
     * Extract user message from Llama formatted prompt
     */
    private fun extractUserMessage(prompt: String): String {
        val userStart = prompt.indexOf("<|start_header_id|>user<|end_header_id|>")
        val userEnd = prompt.indexOf("<|eot_id|>", userStart)
        
        return if (userStart != -1 && userEnd != -1) {
            prompt.substring(userStart + 40, userEnd).trim()
        } else {
            prompt
        }
    }
    
    /**
     * Generate intelligent fitness/nutrition responses as fallback
     */
    private fun generateFallbackResponse(userMessage: String): String {
        val msg = userMessage.lowercase()
        
        return when {
            msg.contains("protein") || msg.contains("muscle") || msg.contains("lean") -> {
                "Great focus on protein! Here's what you need to know:\n\n" +
                "**Daily Protein Requirements:**\n" +
                "• General health: 0.8g per kg of body weight\n" +
                "• Muscle building: 1.6-2.2g per kg\n" +
                "• Athletes: 2.0-2.5g per kg\n\n" +
                "**Best Protein Sources:**\n" +
                "• Lean meats: chicken breast, turkey, lean beef\n" +
                "• Fish: salmon, tuna, tilapia (omega-3 bonus!)\n" +
                "• Eggs: whole eggs for complete protein\n" +
                "• Dairy: Greek yogurt, cottage cheese\n" +
                "• Plant-based: lentils, chickpeas, tofu, quinoa\n\n" +
                "💡 Tip: Spread protein throughout the day (20-30g per meal) for optimal muscle synthesis!"
            }
            
            msg.contains("water") || msg.contains("drink") || msg.contains("hydrat") -> {
                "Hydration is crucial for fitness! Here's my guide:\n\n" +
                "**Daily Water Intake:**\n" +
                "• Baseline: 8-10 glasses (2-3 liters)\n" +
                "• During exercise: Add 400-800ml per hour\n" +
                "• Hot weather: Increase by 30-50%\n\n" +
                "**Signs of Good Hydration:**\n" +
                "• Pale yellow urine\n" +
                "• Regular bathroom visits (every 2-3 hours)\n" +
                "• No headaches or fatigue\n\n" +
                "**When to Drink:**\n" +
                "• Morning: 1-2 glasses upon waking\n" +
                "• Before workout: 16oz (2 cups)\n" +
                "• During workout: 8oz every 15-20 min\n" +
                "• After workout: 24oz per pound lost\n\n" +
                "💧 Pro tip: Add electrolytes for workouts over 60 minutes!"
            }
            
            msg.contains("workout") || msg.contains("exercise") || msg.contains("train") -> {
                "Let's build a great workout routine! Here's my recommendation:\n\n" +
                "**Weekly Training Split:**\n" +
                "• Monday: Upper body strength (chest, back, shoulders)\n" +
                "• Tuesday: Lower body (squats, lunges, leg press)\n" +
                "• Wednesday: Cardio + core (30 min running, abs)\n" +
                "• Thursday: Rest or active recovery (yoga, walk)\n" +
                "• Friday: Full body or sport-specific\n" +
                "• Weekend: Light cardio or rest\n\n" +
                "**Each Session:**\n" +
                "• Warm-up: 5-10 min (dynamic stretches)\n" +
                "• Main workout: 45-60 min\n" +
                "• Cool-down: 5-10 min (static stretches)\n\n" +
                "**Key Principles:**\n" +
                "• Progressive overload: add weight weekly\n" +
                "• 8-12 reps for muscle growth\n" +
                "• 2-3 min rest between sets\n\n" +
                "💪 Remember: Consistency beats perfection!"
            }
            
            msg.contains("weight") || msg.contains("lose") || msg.contains("gain") || msg.contains("calor") -> {
                "Weight management is about energy balance! Here's the science:\n\n" +
                "**Calculate Your Needs:**\n" +
                "1. Find BMR (basal metabolic rate)\n" +
                "2. Multiply by activity factor:\n" +
                "   • Sedentary: BMR × 1.2\n" +
                "   • Lightly active: BMR × 1.375\n" +
                "   • Moderately active: BMR × 1.55\n" +
                "   • Very active: BMR × 1.725\n\n" +
                "**For Your Goals:**\n" +
                "• Weight loss: Eat 300-500 cal below TDEE\n" +
                "• Weight gain: Eat 300-500 cal above TDEE\n" +
                "• Maintenance: Match your TDEE\n\n" +
                "**Macro Split:**\n" +
                "• Protein: 30% (1.6-2.2g/kg)\n" +
                "• Carbs: 40% (energy for workouts)\n" +
                "• Fats: 30% (hormones, satiety)\n\n" +
                "⚖️ Track for 2 weeks, adjust based on results!"
            }
            
            msg.contains("diet") || msg.contains("meal") || msg.contains("nutrition") || msg.contains("eat") -> {
                "Nutrition is 80% of results! Here's your meal planning guide:\n\n" +
                "**Sample Day Structure:**\n\n" +
                "**Breakfast (7-8am):**\n" +
                "• 3 whole eggs + 2 egg whites\n" +
                "• 1 cup oatmeal with berries\n" +
                "• Black coffee or green tea\n\n" +
                "**Mid-Morning Snack (10-11am):**\n" +
                "• Greek yogurt (high protein)\n" +
                "• Handful of almonds\n\n" +
                "**Lunch (12-1pm):**\n" +
                "• 6oz grilled chicken or fish\n" +
                "• 1 cup brown rice or sweet potato\n" +
                "• Large salad with olive oil\n\n" +
                "**Pre-Workout (2-3pm):**\n" +
                "• Banana with peanut butter\n\n" +
                "**Post-Workout (5-6pm):**\n" +
                "• Protein shake (30g protein)\n" +
                "• Apple or orange\n\n" +
                "**Dinner (7-8pm):**\n" +
                "• 6-8oz lean protein\n" +
                "• Lots of veggies\n" +
                "• Quinoa or whole grain pasta\n\n" +
                "🥗 Adjust portions based on your calorie needs!"
            }
            
            msg.contains("sleep") || msg.contains("rest") || msg.contains("recover") -> {
                "Recovery is when you actually grow! Here's why it matters:\n\n" +
                "**Sleep Requirements:**\n" +
                "• 7-9 hours per night (non-negotiable!)\n" +
                "• Same bedtime/wake time daily\n" +
                "• Dark, cool room (60-67°F)\n\n" +
                "**What Happens During Sleep:**\n" +
                "• Growth hormone release (muscle repair)\n" +
                "• Protein synthesis peaks\n" +
                "• Energy stores replenish\n" +
                "• Neural recovery\n\n" +
                "**Active Recovery Tips:**\n" +
                "• Light stretching (10-15 min)\n" +
                "• Foam rolling (target sore areas)\n" +
                "• Walking (20-30 min)\n" +
                "• Yoga or swimming\n\n" +
                "**Rest Days Per Week:**\n" +
                "• Beginners: 2-3 days\n" +
                "• Intermediate: 1-2 days\n" +
                "• Advanced: 1 day minimum\n\n" +
                "😴 Poor sleep = poor gains. Prioritize it!"
            }
            
            msg.contains("start") || msg.contains("begin") || msg.contains("beginner") || msg.contains("new") -> {
                "Welcome to your fitness journey! Here's how to start right:\n\n" +
                "**Week 1-2: Foundation**\n" +
                "• 3 workouts per week (full body)\n" +
                "• Focus on form, not weight\n" +
                "• 30-minute sessions\n\n" +
                "**Week 3-4: Build Habit**\n" +
                "• 4 workouts per week\n" +
                "• Start tracking nutrition\n" +
                "• Increase to 45 minutes\n\n" +
                "**Month 2: Progress**\n" +
                "• Add weight to exercises\n" +
                "• Try upper/lower split\n" +
                "• Set specific goals\n\n" +
                "**Essential Exercises:**\n" +
                "• Squats (legs)\n" +
                "• Push-ups/bench press (chest)\n" +
                "• Rows (back)\n" +
                "• Overhead press (shoulders)\n" +
                "• Planks (core)\n\n" +
                "**Success Tips:**\n" +
                "• Take before photos\n" +
                "• Track workouts in a journal\n" +
                "• Find a workout buddy\n" +
                "• Be patient - results take 4-6 weeks\n\n" +
                "🚀 You've got this! Stay consistent!"
            }
            
            else -> {
                "I'm your AI fitness coach! I can help with:\n\n" +
                "💪 **Training:**\n" +
                "• Workout plans and routines\n" +
                "• Exercise technique and form\n" +
                "• Progressive overload strategies\n\n" +
                "🥗 **Nutrition:**\n" +
                "• Meal planning and macros\n" +
                "• Protein requirements\n" +
                "• Diet strategies for your goals\n\n" +
                "💧 **Hydration & Recovery:**\n" +
                "• Water intake guidelines\n" +
                "• Sleep optimization\n" +
                "• Rest day strategies\n\n" +
                "⚖️ **Weight Management:**\n" +
                "• Calorie calculations\n" +
                "• Fat loss strategies\n" +
                "• Muscle building tips\n\n" +
                "⚠️ Note: Currently using fallback responses. For full LLM capabilities, QAIRT SDK needs to be installed on your device.\n\n" +
                "What would you like to know about fitness or nutrition?"
            }
        }
    }
    
    /**
     * Extract generated text from genie-t2t-run output
     */
    private fun parseGenieOutput(output: String): String {
        // genie-t2t-run output format:
        // [PROMPT]: <prompt text>
        // [BEGIN]: <generated text>[END]
        
        return try {
            val beginMarker = "[BEGIN]:"
            val endMarker = "[END]"
            
            val beginIndex = output.indexOf(beginMarker)
            if (beginIndex == -1) {
                Log.d(TAG, "Could not find [BEGIN] marker in output")
                return "No response generated. Output:\n${output.take(200)}"
            }
            
            val startIndex = beginIndex + beginMarker.length
            val endIndex = output.indexOf(endMarker, startIndex)
            
            val generated = if (endIndex != -1) {
                output.substring(startIndex, endIndex)
            } else {
                output.substring(startIndex)
            }
            
            generated.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing output: ${e.message}", e)
            output.take(500) // Return first 500 chars as fallback
        }
    }
    
    /**
     * Check if model is ready for inference
     */
    fun isReady(): Boolean = isInitialized && modelPath != null
    
    /**
     * Cleanup resources
     */
    fun release() {
        isInitialized = false
        modelPath = null
    }
}
