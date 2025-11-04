package com.boonya.game.service;

import com.boonya.game.datasource.annotation.Master;
import com.boonya.game.datasource.annotation.ReadOnly;
import com.boonya.game.cache.AbstractMultiLevelCache;
import com.boonya.game.component.ApplicationContextHolder;
import com.boonya.game.dao.UserRepository;
import com.boonya.game.model.User;
import jakarta.annotation.Resource;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

@Service
public class UserService extends AbstractMultiLevelCache<User, Long> {
    private final ConcurrentHashMap<Long, User> userCache = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Resource
    private UserRepository userRepository;

    @Autowired
    public UserService(CacheManager localCache, RedisTemplate<String, Object> redisCache) {
        super(localCache, redisCache);
    }

    @Override
    public JpaRepository<User, Long> database() {
        return ApplicationContextHolder.getBean(UserRepository.class);
    }


    /**
     * 读操作 - 自动使用从库
     */
    @ReadOnly
    public User findById(Long id) {
        // 这个方法会自动使用从库
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 写操作 - 明确指定使用主库
     */
    @Master
    public User save(User user) {
        // 这个方法会使用主库
        return userRepository.save(user);
    }

    /**
     * 异步获取用户
     */
    @Async("taskExecutor")
    public CompletableFuture<User> getUserAsync(Long id) {
        return CompletableFuture.completedFuture(getUser(id));
    }

    /**
     * 缓存
     */
    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) {
        // 模拟数据库查询延迟
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return userCache.get(id);
    }

    public boolean cacheContains(Long id) {
        return userCache.containsKey(id);
    }

    public List<Long> getHotUserIds() {
        // 模拟数据库查询延迟
        return List.of(1L, 2L, 3L);
    }

    public List<Long> predictHotUserIds() {
        // 与查询用户热点数据进行预测
        return List.of();
    }

    public boolean deleteUser(Long id) {
        return userCache.remove(id) != null;
    }

    public int getUserCount() {
        return userCache.size();
    }

    /**
     * 多级缓存
     */
    @Cacheable(value = "users", key = "#id")
    public User getUserMultiLevel(Long id) {
        return super.findById(User.class, "users", id);
    }

    /**
     * 无锁结构
     */

    // 使用并发性能更好的数据结构
    private final ConcurrentHashMap<Long, User> lockFreeUserCache = new ConcurrentHashMap<>(10000, 0.75f, 64);

    // 使用LongAdder替代AtomicLong
    private final LongAdder queryCounter = new LongAdder();
    private final LongAdder hitCounter = new LongAdder();

    // 使用StampedLock提高读性能
    private final StampedLock lock = new StampedLock();

    public User getUserLockFree(Long id) {
        queryCounter.increment();

        // 乐观读
        long stamp = lock.tryOptimisticRead();
        User user = lockFreeUserCache.get(id);
        if (!lock.validate(stamp)) {
            // 乐观读失败，升级为悲观读
            stamp = lock.readLock();
            try {
                user = lockFreeUserCache.get(id);
            } finally {
                lock.unlockRead(stamp);
            }
        }

        if (user != null) {
            hitCounter.increment();
        }
        return user;
    }

    /**
     * 对象池
     */

    private final ConcurrentHashMap<Long, String> cachedNames = new ConcurrentHashMap<>();

    public class UserPoolFactory extends BasePooledObjectFactory<User> {
        @Override
        public User create() {
            return new User();
        }

        @Override
        public PooledObject<User> wrap(User user) {
            return null;
        }
    }

    // 使用对象池减少GC
    private final GenericObjectPool<User> userPool = new GenericObjectPool<>(new UserPoolFactory());

    public User getUserOptimized(Long id) {
        User user = null;
        try {
            // 复用对象而不是创建新对象
            user = userPool.borrowObject();
            // 直接设置字段而不是创建新对象
            user.setId(id);
            user.setName(cachedNames.get(id)); // 使用缓存
            return user;
        } catch (Exception e) {
            // 如果获取对象失败，确保已获取的对象被正确释放
            if (user != null) {
                try {
                    userPool.returnObject(user);
                } catch (Exception returnException) {
                    // 记录返回对象时的异常，但不影响主异常
                    // 可以使用日志记录
                }
            }
            // 处理对象池异常
            throw new RuntimeException("Failed to borrow object from pool", e);
        } finally {
            if (user != null) {
                boolean borrowedSuccessfully = false;
                try {
                    // 检查是否已经因为异常而归还了对象
                    userPool.returnObject(user);
                } catch (Exception e) {
                    // 不应该忽略返回对象时的异常，至少要记录日志
                    throw new RuntimeException("Failed to return object to pool", e);
                }
            }
        }
    }

}