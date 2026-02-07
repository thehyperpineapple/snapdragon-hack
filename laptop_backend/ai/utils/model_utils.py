"""
Utility functions for AI layer operations
"""

import json
import logging
from typing import Dict, Any, Optional, Tuple

logger = logging.getLogger(__name__)


def validate_generation_params(
    max_new_tokens: Optional[int] = None,
    temperature: Optional[float] = None,
    top_p: Optional[float] = None
) -> Tuple[bool, Optional[str]]:
    """
    Validate generation parameters.

    Args:
        max_new_tokens: Maximum tokens to generate
        temperature: Sampling temperature
        top_p: Nucleus sampling threshold

    Returns:
        Tuple of (is_valid, error_message)
    """
    if max_new_tokens is not None:
        if not isinstance(max_new_tokens, int) or max_new_tokens <= 0:
            return False, "max_new_tokens must be a positive integer"
        if max_new_tokens > 2048:
            return False, "max_new_tokens cannot exceed 2048"

    if temperature is not None:
        if not isinstance(temperature, (int, float)) or temperature <= 0:
            return False, "temperature must be a positive number"
        if temperature > 2.0:
            return False, "temperature should not exceed 2.0 for stable generation"

    if top_p is not None:
        if not isinstance(top_p, (int, float)):
            return False, "top_p must be a number"
        if not 0 < top_p <= 1.0:
            return False, "top_p must be between 0 and 1"

    return True, None


def format_response(
    raw_output: str,
    expected_format: str = "json"
) -> Dict[str, Any]:
    """
    Format and parse LLM output.

    Args:
        raw_output: Raw output from LLM
        expected_format: Expected output format ('json', 'text')

    Returns:
        Formatted response dictionary
    """
    if expected_format == "json":
        try:
            # Try to extract JSON from the output
            json_start = raw_output.find('{')
            json_end = raw_output.rfind('}') + 1

            if json_start != -1 and json_end > json_start:
                json_str = raw_output[json_start:json_end]
                parsed = json.loads(json_str)
                return {
                    "success": True,
                    "data": parsed,
                    "raw_output": raw_output
                }
            else:
                logger.warning("No JSON object found in output")
                return {
                    "success": False,
                    "error": "No JSON object found in output",
                    "raw_output": raw_output
                }

        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON: {e}")
            return {
                "success": False,
                "error": f"Invalid JSON: {str(e)}",
                "raw_output": raw_output
            }

    # Text format
    return {
        "success": True,
        "data": {"text": raw_output.strip()},
        "raw_output": raw_output
    }


def extract_json_from_text(text: str) -> Optional[Dict]:
    """
    Extract JSON object from text that may contain additional content.

    Args:
        text: Text potentially containing JSON

    Returns:
        Parsed JSON dict or None
    """
    try:
        # Find the first { and last }
        start = text.find('{')
        end = text.rfind('}') + 1

        if start != -1 and end > start:
            json_str = text[start:end]
            return json.loads(json_str)

        return None

    except (json.JSONDecodeError, ValueError) as e:
        logger.error(f"Failed to extract JSON: {e}")
        return None


def sanitize_user_input(user_input: str, max_length: int = 5000) -> str:
    """
    Sanitize user input before passing to LLM.

    Args:
        user_input: Raw user input
        max_length: Maximum allowed length

    Returns:
        Sanitized input
    """
    # Truncate if too long
    if len(user_input) > max_length:
        logger.warning(f"Input truncated from {len(user_input)} to {max_length} chars")
        user_input = user_input[:max_length]

    # Remove potentially problematic characters
    user_input = user_input.strip()

    # Remove multiple consecutive whitespaces
    user_input = ' '.join(user_input.split())

    return user_input


def calculate_token_estimate(text: str) -> int:
    """
    Rough estimation of token count for text.

    Args:
        text: Input text

    Returns:
        Estimated token count
    """
    # Rough estimate: 1 token ≈ 4 characters (English)
    return len(text) // 4


def merge_user_context(
    health_data: Dict[str, Any],
    nutrition_data: Dict[str, Any],
    plan_data: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """
    Merge user data from different sources into unified context.

    Args:
        health_data: Health profile data
        nutrition_data: Nutrition preferences
        plan_data: Optional current plan data

    Returns:
        Merged context dictionary
    """
    context = {
        "profile": health_data,
        "nutrition": nutrition_data
    }

    if plan_data:
        context["current_plan"] = plan_data

    return context


def format_error_response(error: Exception, context: str = "") -> Dict[str, Any]:
    """
    Format error response for API consumption.

    Args:
        error: Exception that occurred
        context: Additional context about where error occurred

    Returns:
        Error response dictionary
    """
    logger.error(f"Error in {context}: {str(error)}", exc_info=True)

    return {
        "success": False,
        "error": str(error),
        "context": context,
        "type": type(error).__name__
    }


def validate_json_structure(data: Dict, required_keys: list) -> Tuple[bool, Optional[str]]:
    """
    Validate that JSON response has required keys.

    Args:
        data: JSON data to validate
        required_keys: List of required key names

    Returns:
        Tuple of (is_valid, error_message)
    """
    missing_keys = [key for key in required_keys if key not in data]

    if missing_keys:
        return False, f"Missing required keys: {', '.join(missing_keys)}"

    return True, None
