"""
KV Cache Manager for efficient autoregressive generation
Implements sliding window cache with memory-aware eviction
"""

import logging
from typing import Dict, Optional
import numpy as np

logger = logging.getLogger('ai')


class KVCacheManager:
    """
    Manages Key-Value cache for efficient autoregressive generation.
    Implements sliding window cache with eviction policy.
    """

    def __init__(self, max_cache_size_mb: int = 512):
        """
        Initialize KV cache manager.

        Args:
            max_cache_size_mb: Maximum cache size in megabytes
        """
        self.max_cache_size_mb = max_cache_size_mb
        self.cache: Dict[str, np.ndarray] = {}
        self.cache_hits = 0
        self.cache_misses = 0
        logger.info(f"AI_CACHE: KV Cache initialized with {max_cache_size_mb}MB limit")

    def get(self, key: str) -> Optional[np.ndarray]:
        """
        Retrieve cached KV pairs.

        Args:
            key: Cache key

        Returns:
            Cached numpy array or None if not found
        """
        if key in self.cache:
            self.cache_hits += 1
            logger.debug(f"AI_CACHE: Cache HIT for key: {key[:50]}...")
            return self.cache[key]

        self.cache_misses += 1
        logger.debug(f"AI_CACHE: Cache MISS for key: {key[:50]}...")
        return None

    def put(self, key: str, value: np.ndarray):
        """
        Store KV pairs with memory-aware eviction.

        Args:
            key: Cache key
            value: Numpy array to cache
        """
        current_size_mb = sum(v.nbytes for v in self.cache.values()) / (1024 * 1024)

        # Evict oldest entries if exceeding limit (FIFO policy)
        while current_size_mb > self.max_cache_size_mb and self.cache:
            oldest_key = next(iter(self.cache))
            evicted_size = self.cache[oldest_key].nbytes / (1024 * 1024)
            del self.cache[oldest_key]
            current_size_mb -= evicted_size
            logger.debug(f"AI_CACHE: Evicted cache entry: {oldest_key[:50]}... ({evicted_size:.2f}MB)")

        self.cache[key] = value
        value_size_mb = value.nbytes / (1024 * 1024)
        logger.debug(f"AI_CACHE: Stored cache entry: {key[:50]}... ({value_size_mb:.2f}MB), total_cache={current_size_mb + value_size_mb:.2f}MB")

    def clear(self):
        """Clear all cache entries."""
        self.cache.clear()
        self.cache_hits = 0
        self.cache_misses = 0
        logger.info("AI_CACHE: KV cache cleared")

    def get_stats(self) -> Dict:
        """
        Return cache performance metrics.

        Returns:
            Dictionary with cache statistics
        """
        total = self.cache_hits + self.cache_misses
        hit_rate = self.cache_hits / total if total > 0 else 0.0

        return {
            "cache_size_mb": round(
                sum(v.nbytes for v in self.cache.values()) / (1024 * 1024), 2
            ),
            "num_entries": len(self.cache),
            "hit_rate": f"{hit_rate:.2%}",
            "hits": self.cache_hits,
            "misses": self.cache_misses
        }

    def __repr__(self) -> str:
        stats = self.get_stats()
        return (
            f"KVCacheManager(size={stats['cache_size_mb']}MB, "
            f"entries={stats['num_entries']}, hit_rate={stats['hit_rate']})"
        )
