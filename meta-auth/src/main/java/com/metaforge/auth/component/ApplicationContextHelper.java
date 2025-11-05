package com.metaforge.auth.component;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring ApplicationContext 工具类
 * 用于在非Spring管理的类中获取Spring容器中的Bean
 */
@Component
public class ApplicationContextHelper implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHelper.applicationContext = applicationContext;
    }

    /**
     * 获取 ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过名称获取Bean
     */
    public static Object getBean(String name) {
        checkApplicationContext();
        return applicationContext.getBean(name);
    }

    /**
     * 通过类型获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        checkApplicationContext();
        return applicationContext.getBean(clazz);
    }

    /**
     * 通过名称和类型获取Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        checkApplicationContext();
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 获取指定类型的所有Bean
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        checkApplicationContext();
        return applicationContext.getBeansOfType(clazz);
    }

    /**
     * 检查是否包含指定名称的Bean
     */
    public static boolean containsBean(String name) {
        checkApplicationContext();
        return applicationContext.containsBean(name);
    }

    /**
     * 判断指定名称的Bean是否为单例
     */
    public static boolean isSingleton(String name) {
        checkApplicationContext();
        return applicationContext.isSingleton(name);
    }

    /**
     * 获取Bean的类型
     */
    public static Class<?> getType(String name) {
        checkApplicationContext();
        return applicationContext.getType(name);
    }

    /**
     * 获取环境配置
     */
    public static Environment getEnvironment() {
        checkApplicationContext();
        return applicationContext.getEnvironment();
    }

    /**
     * 获取配置属性
     */
    public static String getProperty(String key) {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty(key);
    }

    /**
     * 获取配置属性，带默认值
     */
    public static String getProperty(String key, String defaultValue) {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty(key, defaultValue);
    }

    /**
     * 获取配置属性（指定类型）
     */
    public static <T> T getProperty(String key, Class<T> targetType) {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty(key, targetType);
    }

    /**
     * 获取配置属性（指定类型，带默认值）
     */
    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty(key, targetType, defaultValue);
    }

    /**
     * 发布应用事件
     */
    public static void publishEvent(Object event) {
        checkApplicationContext();
        applicationContext.publishEvent(event);
    }

    /**
     * 检查ApplicationContext是否已初始化
     */
    private static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext 未初始化，请确保该类在Spring容器启动后使用");
        }
    }

    /**
     * 获取当前激活的Profile
     */
    public static String[] getActiveProfiles() {
        checkApplicationContext();
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    /**
     * 获取默认的Profile
     */
    public static String[] getDefaultProfiles() {
        checkApplicationContext();
        return applicationContext.getEnvironment().getDefaultProfiles();
    }

    /**
     * 检查当前是否激活了指定的Profile
     */
    public static boolean isProfileActive(String profile) {
        checkApplicationContext();
        Environment env = applicationContext.getEnvironment();
        for (String activeProfile : env.getActiveProfiles()) {
            if (activeProfile.equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取应用名称
     */
    public static String getApplicationName() {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty("spring.application.name", "Unknown");
    }

    /**
     * 获取服务器端口
     */
    public static String getServerPort() {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty("server.port", "8080");
    }

    /**
     * 获取上下文路径
     */
    public static String getContextPath() {
        checkApplicationContext();
        return applicationContext.getEnvironment().getProperty("server.servlet.context-path", "");
    }
}