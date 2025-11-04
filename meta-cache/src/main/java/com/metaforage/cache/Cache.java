package com.metaforage.cache;

import com.metaforage.cache.mode.CacheStats;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存接口
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface Cache<K, V> {

    /**
     * 获取缓存值
     */
    V get(K key);

    /**
     * 获取缓存值，如果不存在则返回默认值
     */
    V get(K key, V defaultValue);

    /**
     * 设置缓存值
     */
    void put(K key, V value);

    /**
     * 设置缓存值并指定过期时间
     */
    void put(K key, V value, long duration, TimeUnit timeUnit);

    /**
     * 如果键不存在则设置缓存值
     * @return 如果设置成功返回true，如果键已存在返回false
     */
    boolean putIfAbsent(K key, V value);

    /**
     * 如果键不存在则设置缓存值并指定过期时间
     */
    boolean putIfAbsent(K key, V value, long duration, TimeUnit timeUnit);

    /**
     * 删除缓存
     */
    boolean evict(K key);

    /**
     * 批量删除缓存
     */
    void evictAll(Iterable<K> keys);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 判断缓存是否存在
     */
    boolean containsKey(K key);

    /**
     * 获取缓存大小
     */
    long size();

    /**
     * 获取所有键
     */
    Set<K> keys();

    /**
     * 获取所有值
     */
    Collection<V> values();

    /**
     * 设置过期时间
     */
    boolean expire(K key, long duration, TimeUnit timeUnit);

    /**
     * 获取剩余过期时间
     */
    long getExpire(K key, TimeUnit timeUnit);

    /**
     * 原子递增
     */
    long increment(K key, long delta);

    /**
     * 原子递减
     */
    long decrement(K key, long delta);

    /**
     * 获取缓存统计信息
     */
    CacheStats getStats();
}