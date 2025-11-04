package com.metaforge.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 用户认证方法
     *
     * @param username 用户名
     * @param password 密码（前端传过来的明文）
     * @return 认证结果
     */
    public boolean authenticate(String username, String password) {
        try {
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            // 使用AuthenticationManager进行认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 认证成功
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication.isAuthenticated();

        } catch (BadCredentialsException e) {
            // 用户名或密码错误
            System.out.println("认证失败: 用户名或密码错误");
            return false;
        } catch (AuthenticationException e) {
            // 其他认证异常
            System.out.println("认证失败: " + e.getMessage());
            return false;
        }
    }
}