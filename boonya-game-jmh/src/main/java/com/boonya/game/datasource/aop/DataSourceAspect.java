package com.boonya.game.datasource.aop;

import com.boonya.game.datasource.DynamicDataSourceHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(1) // 在事务注解之前执行
public class DataSourceAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceAspect.class);

    /**
     * 拦截带有@ReadOnly注解的方法，使用从库
     */
    @Around("@annotation(com.boonya.game.datasource.annotation.ReadOnly)")
    public Object aroundReadOnly(ProceedingJoinPoint point) throws Throwable {
        return proceedWithSlaveDataSource(point, "ReadOnly");
    }

    /**
     * 拦截带有@Master注解的方法，使用主库
     */
    @Around("@annotation(com.boonya.game.datasource.annotation.Master)")
    public Object aroundMaster(ProceedingJoinPoint point) throws Throwable {
        return proceedWithMasterDataSource(point, "Master");
    }

    /**
     * 拦截@Service类中非写操作的方法，自动使用从库
     */
    @Around("execution(* com.boonya.game..service..*.*(..)) && " +
            "!@annotation(com.boonya.game.datasource.annotation.Master) && " +
            "!@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object aroundServiceMethod(ProceedingJoinPoint point) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String methodName = method.getName();

        // 根据方法名判断是否使用从库
        if (isReadOperation(methodName)) {
            return proceedWithSlaveDataSource(point, "Auto-Read");
        }

        // 默认使用主库
        return proceedWithMasterDataSource(point, "Auto-Write");
    }

    /**
     * 使用从库执行
     */
    private Object proceedWithSlaveDataSource(ProceedingJoinPoint point, String source) throws Throwable {
        String oldDataSource = DynamicDataSourceHolder.getDataSource();

        try {
            DynamicDataSourceHolder.useSlave();
            logger.debug("{} - Switching to slave datasource: {}", source,
                    DynamicDataSourceHolder.getDataSourceInfo());

            return point.proceed();

        } finally {
            // 恢复原来的数据源
            if (oldDataSource != null) {
                DynamicDataSourceHolder.setDataSource(oldDataSource);
            } else {
                DynamicDataSourceHolder.clearDataSource();
            }
            logger.debug("{} - Restored datasource: {}", source, oldDataSource);
        }
    }

    /**
     * 使用主库执行
     */
    private Object proceedWithMasterDataSource(ProceedingJoinPoint point, String source) throws Throwable {
        String oldDataSource = DynamicDataSourceHolder.getDataSource();

        try {
            DynamicDataSourceHolder.useMaster();
            logger.debug("{} - Switching to master datasource: {}", source,
                    DynamicDataSourceHolder.getDataSourceInfo());

            return point.proceed();

        } finally {
            // 恢复原来的数据源
            if (oldDataSource != null) {
                DynamicDataSourceHolder.setDataSource(oldDataSource);
            } else {
                DynamicDataSourceHolder.clearDataSource();
            }
            logger.debug("{} - Restored datasource: {}", source, oldDataSource);
        }
    }

    /**
     * 判断是否为读操作
     */
    private boolean isReadOperation(String methodName) {
        if (methodName == null) {
            return false;
        }

        String lowerMethodName = methodName.toLowerCase();
        return lowerMethodName.startsWith("get") ||
                lowerMethodName.startsWith("find") ||
                lowerMethodName.startsWith("query") ||
                lowerMethodName.startsWith("select") ||
                lowerMethodName.startsWith("list") ||
                lowerMethodName.startsWith("count") ||
                lowerMethodName.startsWith("exists") ||
                lowerMethodName.startsWith("search") ||
                lowerMethodName.startsWith("read") ||
                lowerMethodName.startsWith("load") ||
                lowerMethodName.startsWith("fetch");
    }
}
