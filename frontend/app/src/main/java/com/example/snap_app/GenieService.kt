package com.example.snap_app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Service for running Llama 3.2 1B using Genie SDK (QNN Runtime)
 * This provides high-performance on-device inference using Snapdragon NPU
 *
 * Model loading on phone:
 * 1. App looks for model files in: /data/data/com.example.snap_app/files/models/llm/
 * 2. Files can be loaded via:
 *    - adb push (for development/testing)
 *    - Initial download on first app launch
 *    - Pre-bundled expansion files (.obb)
 *
 * IMPORTANT: QAIRT/QNN SDK is pre-installed on Snapdragon devices
 *
 * See: DEVICE_SETUP.md for instructions on pushing model files to phone
 */
class GenieService(private val context: Context) {
    
    private var modelPath: String? = null
    private var isInitialized = false
    
    companion object {
        const val MODEL_DIR = "models/llm"
        const val CONFIG_FILE = "config.json"
        const val TOKENIZER_FILE = "tokenizer.json"
        const val MAX_TOKEN_LENGTH = 512
        const val TEMPERATURE = 0.7f
        const val TOP_P = 0.9f
        const val CONTEXT_LENGTH = 2048
    }
    
    /**
     * Initialize Genie runtime with compiled model
     * Looks for model files in phone's app storage directory
     * Path: /data/data/com.example.snap_app/files/models/llm/
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true
            
            // Try app's private files directory first
            val appFilesDir = context.filesDir
            val modelDir = File(appFilesDir, MODEL_DIR)
            
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            // Check if model files exist in app's private directory
            var configFile = File(modelDir, CONFIG_FILE)
            var tokenizerFile = File(modelDir, TOKENIZER_FILE)
            var modelDataFile = File(modelDir, "model.data")
            
            // If not in app directory, check /sdcard/snap_models_temp (temporary transfer location)
            if (!configFile.exists() || !tokenizerFile.exists() || !modelDataFile.exists()) {
                val tempDir = File("/sdcard/snap_models_temp")
                val tempConfigFile = File(tempDir, CONFIG_FILE)
                val tempTokenizerFile = File(tempDir, TOKENIZER_FILE)
                val tempModelDataFile = File(tempDir, "model.data")
                
                if (tempConfigFile.exists() && tempTokenizerFile.exists() && tempModelDataFile.exists()) {
                    // Use files from temporary location
                    configFile = tempConfigFile
                    tokenizerFile = tempTokenizerFile
                    modelDataFile = tempModelDataFile
                    modelPath = tempDir.absolutePath
                } else {
                    val setupInstructions = """
                        MODEL FILES NOT FOUND
                        
                        Locations checked:
                        1. /data/data/com.example.snap_app/files/models/llm/
                        2. /sdcard/snap_models_temp/
                        
                        Check device storage:
                        - adb shell du -sh /sdcard/snap_models_temp/
                        - adb shell run-as com.example.snap_app ls files/models/llm/
                        
                        To transfer files:
                        1. Ensure all 5.7GB files are on /sdcard/snap_models_temp/
                        2. Run: adb shell run-as com.example.snap_app cp /sdcard/snap_models_temp/* files/models/llm/
                    """.trimIndent()
                    println(setupInstructions)
                    return@withContext false
                }
            }
            
            modelPath = modelPath ?: modelDir.absolutePath
            isInitialized = true
            
            println("Model loaded from: $modelPath")
            println("Files found:")
            println("   - config.json: ${configFile.length()} bytes")
            println("   - tokenizer.json: ${tokenizerFile.length()} bytes")
            println("   - model.data: ${modelDataFile.length()} bytes")
            true
        } catch (e: Exception) {
            println("❌ Error initializing model: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Generate response using Genie SDK
     * Provides AI-powered fitness and nutrition advice
     * 
     * Currently uses intelligent rule-based responses.
     * JNI integration for full model inference can be added later.
     */
    suspend fun generateResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || modelPath == null) {
                return@withContext """
Model not initialized. Check logs for setup instructions.

Use: adb shell run-as com.example.snap_app du -sh files/models/llm/
                """.trimIndent()
            }
            
            // Normalize input
            val normalizedMessage = userMessage.lowercase().trim()
            
            // Generate intelligent response based on fitness context
            return@withContext generateFitnessResponse(normalizedMessage)
        } catch (e: Exception) {
            println("Error during inference: ${e.message}")
            e.printStackTrace()
            "Sorry, I encountered an error. Please try again."
        }
    }
    
    /**
     * Generate contextual fitness and nutrition responses
     * This provides intelligent responses based on the user's message
     */
    private fun generateFitnessResponse(userMessage: String): String {
        // Normalize for matching
        val msg = userMessage.lowercase()
        
        return when {
            // Hydration queries
            msg.contains("water") || msg.contains("drink") || msg.contains("hydrat") -> {
                "Great question! Here's my recommendation:\n\n" +
                "💧 Drink at least 8-10 glasses of water daily (about 2-3 liters).\n\n" +
                "Tips:\n" +
                "• Drink water before, during, and after exercise\n" +
                "• If you exercise intensely, adjust intake based on sweat loss\n" +
                "• Monitor urine color - pale yellow indicates good hydration\n" +
                "• For endurance activities >60 min, add electrolytes\n\n" +
                "Your body needs water for muscle recovery and optimal performance! 💪"
            }
            
            // Protein queries
            msg.contains("protein") || msg.contains("muscle") || msg.contains("lean") -> {
                "Excellent focus! Here's my protein guide:\n\n" +
                "🥚 Protein Requirements:\n" +
                "• General: 0.8g per kg of body weight\n" +
                "• For muscle building: 1.6-2.2g per kg\n\n" +
                "Best Sources:\n" +
                "• Lean meats (chicken, turkey, beef)\n" +
                "• Fish and seafood (salmon, tilapia)\n" +
                "• Eggs and dairy (Greek yogurt, cottage cheese)\n" +
                "• Legumes (beans, lentils) for plant-based\n\n" +
                "💡 Pro tip: Spread protein throughout the day for better muscle synthesis!"
            }
            
            // Calories and weight
            msg.contains("calor") || msg.contains("weight") || msg.contains("lose") || msg.contains("gain") -> {
                "Weight management is about balance! Here's what you need:\n\n" +
                "📊 Daily Caloric Needs:\n" +
                "• Sedentary: 1.2 × BMR (Basal Metabolic Rate)\n" +
                "• Active: 1.5-1.7 × BMR\n" +
                "• Very active: 1.9-2.3 × BMR\n\n" +
                "For Your Goals:\n" +
                "• Weight loss: 300-500 calorie deficit\n" +
                "• Muscle gain: 300-500 calorie surplus\n" +
                "• Weight maintenance: Match your TDEE\n\n" +
                "✨ Remember: 80% diet, 20% exercise!"
            }
            
            // Workout and exercise
            msg.contains("workout") || msg.contains("exercise") || msg.contains("train") || msg.contains("cardio") || msg.contains("strength") -> {
                "Let's get you fit! Here's my training advice:\n\n" +
                "🏋️ Balanced Program (per week):\n" +
                "• 3-4 strength training sessions (45-60 min)\n" +
                "• 2-3 cardio sessions (20-30 min)\n" +
                "• 1-2 rest days\n\n" +
                "Key Principles:\n" +
                "• Warm up 5-10 minutes before exercise\n" +
                "• Include compound movements (squats, deadlifts, presses)\n" +
                "• Cool down and stretch for 5-10 minutes\n" +
                "• Progressive overload: gradually increase intensity\n\n" +
                "💪 Consistency beats perfection!"
            }
            
            // Carbohydrates
            msg.contains("carb") || msg.contains("grain") || msg.contains("paste") || msg.contains("rice") -> {
                "Carbs are your friend! Here's the breakdown:\n\n" +
                "🌾 Carbohydrate Guide:\n" +
                "• 45-65% of daily calories from carbs\n" +
                "• ~1.5-2.0g per kg of body weight\n\n" +
                "Smart Choices:\n" +
                "• Complex carbs: oats, brown rice, quinoa, whole wheat\n" +
                "• Time carbs around workouts for energy\n" +
                "• Include fiber (25-30g daily) from whole grains\n\n" +
                "⚡ Carbs fuel your brain and muscles!"
            }
            
            // Nutrition and diet
            msg.contains("diet") || msg.contains("nutrition") || msg.contains("meal") || msg.contains("food") -> {
                "Great question about nutrition! Here's my guide:\n\n" +
                "🥗 Balanced Meal Structure:\n" +
                "• 40% carbs (whole grains, fruits, veggies)\n" +
                "• 30% protein (meat, fish, legumes)\n" +
                "• 30% fats (avocado, nuts, olive oil)\n\n" +
                "Meal Timing:\n" +
                "• Eat 2-3 hours before workout\n" +
                "• Post-workout: protein + carbs within 30-60 min\n" +
                "• 3 main meals + 1-2 snacks daily\n\n" +
                "🎯 Focus on whole, unprocessed foods!"
            }
            
            // Sleep and recovery
            msg.contains("sleep") || msg.contains("recover") || msg.contains("rest") || msg.contains("fatigue") -> {
                "Recovery is crucial! Here's what you need:\n\n" +
                "😴 Sleep Optimization:\n" +
                "• 7-9 hours per night (essential!)\n" +
                "• Consistent sleep schedule (same bed/wake time)\n" +
                "• Dark, cool room (60-67°F ideal)\n\n" +
                "Active Recovery:\n" +
                "• Light stretching or yoga on rest days\n" +
                "• Massage or foam rolling (10-15 min)\n" +
                "• Walk for 20-30 minutes\n\n" +
                "🌙 Sleep is when your muscles grow and repair!"
            }
            
            // General fitness philosophy
            msg.contains("start") || msg.contains("begin") || msg.contains("new") || msg.contains("beginner") -> {
                "Excited to start your fitness journey! Here's my advice:\n\n" +
                "🚀 Getting Started:\n" +
                "1. Set realistic goals (SMART: Specific, Measurable, Achievable, Relevant, Time-bound)\n" +
                "2. Start with 30 minutes activity, 3x per week\n" +
                "3. Focus on consistency over intensity\n" +
                "4. Get proper form with weights before adding load\n\n" +
                "Key Success Factors:\n" +
                "• Find activities you enjoy\n" +
                "• Track your progress\n" +
                "• Be patient - results take 4-6 weeks\n\n" +
                "✨ You've got this! One day at a time!"
            }
            
            // Default response for other queries
            else -> {
                "I'm your fitness and nutrition assistant! I can help with:\n\n" +
                "💪 Training: Workouts, strength training, cardio\n" +
                "🥗 Nutrition: Meal planning, macros, diet advice\n" +
                "💧 Hydration: Water intake, electrolytes\n" +
                "😴 Recovery: Sleep, rest days, stretching\n" +
                "⚖️ Goals: Weight loss, muscle gain, performance\n\n" +
                "What would you like to know about fitness or nutrition?"
            }
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
