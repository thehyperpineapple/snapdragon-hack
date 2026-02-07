package com.example.snap_app

/**
 * Llama 3.2 Tokenizer for encoding text to token IDs and decoding back
 * 
 * NOTE: This is a SIMPLIFIED tokenizer for development.
 * For production, use the official HuggingFace tokenizer with the 128k vocabulary.
 * 
 * When you export the model from Qualcomm AI Hub, you'll get tokenizer.json
 * which includes the full vocabulary - use that instead of this simplified version.
 */
class LlamaTokenizer {
    
    companion object {
        // Special tokens for Llama 3.2
        const val BOS_TOKEN = 128000      // Beginning of sequence
        const val EOS_TOKEN = 128001      // End of sequence
        const val PAD_TOKEN = 0           // Padding
        
        // Simplified vocabulary size (actual is 128k for Llama 3.2)
        const val VOCAB_SIZE = 128256
    }
    
    /**
     * Encode text to token IDs
     * This is simplified - use proper tokenizer.json for production
     */
    fun encode(text: String): List<Int> {
        val tokens = mutableListOf<Int>()
        
        // Add BOS token
        tokens.add(BOS_TOKEN)
        
        // Split by spaces and special characters
        val words = text.split(Regex("[\\s\\p{P}]+"))
        
        for (word in words) {
            if (word.isNotBlank()) {
                // Hash word to token ID (simplified - use real tokenizer in production)
                val tokenId = (word.hashCode() % (VOCAB_SIZE - 1000)) + 1000
                tokens.add(tokenId)
            }
        }
        
        // Add EOS token
        tokens.add(EOS_TOKEN)
        
        return tokens
    }
    
    /**
     * Decode token IDs back to text
     * This is simplified - use proper tokenizer.json for production
     */
    fun decode(tokens: List<Int>): String {
        val words = mutableListOf<String>()
        
        for (token in tokens) {
            when (token) {
                BOS_TOKEN -> continue  // Skip special tokens
                EOS_TOKEN -> break
                PAD_TOKEN -> continue
                else -> {
                    // Simplified: map token back to text
                    // In production, use lookup table from tokenizer.json
                    words.add("token_$token")
                }
            }
        }
        
        return words.joinToString(" ")
    }
    
    /**
     * Format message in Llama 3.2 chat template
     */
    fun formatChatMessage(role: String, content: String): String {
        return when (role) {
            "system" -> "<|start_header_id|>system<|end_header_id|>\n\n$content<|eot_id|>"
            "user" -> "<|start_header_id|>user<|end_header_id|>\n\n$content<|eot_id|>"
            "assistant" -> "<|start_header_id|>assistant<|end_header_id|>\n\n$content<|eot_id|>"
            else -> content
        }
    }
}
