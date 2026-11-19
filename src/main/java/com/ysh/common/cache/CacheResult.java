package com.ysh.common.cache;

public record CacheResult<T> (
        T value, boolean isHit
) {
    public static <T> CacheResult<T> hit(T value) {
        return new CacheResult<>(value, true);
    }
    public static <T> CacheResult<T> miss(T value) {
        return new CacheResult<>(value, false);
    }
}
