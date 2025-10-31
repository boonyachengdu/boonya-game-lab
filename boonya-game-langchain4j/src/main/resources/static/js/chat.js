// chat.js - 添加认证支持
document.addEventListener('DOMContentLoaded', function() {
    const chatForm = document.getElementById('chatForm');
    const questionInput = document.getElementById('questionInput');
    const chatMessages = document.getElementById('chatMessages');
    const sendButton = document.getElementById('sendButton');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const sessionId = document.getElementById('sessionId').value;

    console.log('Chat initialized with sessionId:', sessionId);

    // 自动滚动到底部
    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    // 添加消息到聊天界面
    function addMessage(content, isUser, timestamp = new Date()) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isUser ? 'user-message' : 'ai-message'}`;

        const timeStr = timestamp.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
        });

        messageDiv.innerHTML = `
            <div class="message-content">
                <div class="message-header">${isUser ? '您' : 'AI助手'}</div>
                <div class="message-text">${content}</div>
                <div class="message-time">${timeStr}</div>
            </div>
        `;

        // 移除欢迎消息（如果是第一条用户消息）
        const welcomeMessage = chatMessages.querySelector('.welcome-message');
        if (welcomeMessage && isUser) {
            welcomeMessage.remove();
        }

        chatMessages.appendChild(messageDiv);
        scrollToBottom();
    }

    // 显示错误消息
    function showError(message, details = '') {
        console.error('Error:', message, details);

        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger alert-dismissible fade show';

        let errorHtml = `<strong>错误:</strong> ${message}`;
        if (details) {
            errorHtml += `<br><small>详情: ${details}</small>`;
        }
        errorHtml += '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';

        errorDiv.innerHTML = errorHtml;
        chatMessages.appendChild(errorDiv);
        scrollToBottom();

        // 5秒后自动隐藏错误
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove();
            }
        }, 5000);
    }

    // 检查认证状态
    async function checkAuthStatus() {
        try {
            const response = await fetch('/api/rag/health');
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const data = await response.json();
            console.log('Auth status:', data);
            return data.authenticated === true;
        } catch (error) {
            console.error('Auth check failed:', error);
            return false;
        }
    }

    // 处理表单提交
    chatForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const question = questionInput.value.trim();
        if (!question) {
            showError('请输入问题内容');
            return;
        }

        console.log('Sending question:', question);

        // 禁用输入和按钮
        questionInput.disabled = true;
        sendButton.disabled = true;
        sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 发送中...';

        // 添加用户消息
        addMessage(question, true);

        // 清空输入框
        questionInput.value = '';

        // 显示加载动画
        loadingSpinner.style.display = 'block';

        try {
            // 检查认证状态
            const isAuthenticated = await checkAuthStatus();
            if (!isAuthenticated) {
                throw new Error('用户未认证，请重新登录');
            }

            // 构建请求参数
            const requestBody = new URLSearchParams();
            requestBody.append('sessionId', sessionId);
            requestBody.append('question', question);

            console.log('Making request to /api/rag/ask with:', {
                sessionId: sessionId,
                question: question
            });

            // 发送请求到后端
            const response = await fetch('/api/rag/ask', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: requestBody,
                credentials: 'include' // 包含认证cookie
            });

            console.log('Response status:', response.status, response.statusText);

            if (!response.ok) {
                let errorDetails = `HTTP ${response.status}: ${response.statusText}`;
                try {
                    const errorText = await response.text();
                    if (errorText) {
                        errorDetails += ` - ${errorText}`;
                    }
                } catch (textError) {
                    console.warn('Could not read error response body:', textError);
                }
                throw new Error(errorDetails);
            }

            const data = await response.json();
            console.log('Response data:', data);

            if (data.status === 'success') {
                addMessage(data.answer, false);
            } else {
                const errorMsg = data.message || '未知错误';
                showError('服务器返回错误', errorMsg);
                addMessage(`抱歉，服务器处理时出现错误: ${errorMsg}`, false);
            }

        } catch (error) {
            console.error('请求失败:', error);

            let userFriendlyError = '网络连接出现问题，请稍后重试';
            let technicalDetails = error.message;

            // 提供更友好的错误信息
            if (error.message.includes('Failed to fetch')) {
                userFriendlyError = '无法连接到服务器，请检查：\n1. 后端服务是否启动\n2. 网络连接是否正常';
            } else if (error.message.includes('HTTP 401')) {
                userFriendlyError = '用户未认证，请重新登录';
                // 重定向到登录页面
                setTimeout(() => {
                    window.location.href = '/login';
                }, 2000);
            } else if (error.message.includes('HTTP 403')) {
                userFriendlyError = '权限不足，无法访问此资源';
            } else if (error.message.includes('HTTP 404')) {
                userFriendlyError = '请求的接口不存在 (404)';
            } else if (error.message.includes('HTTP 500')) {
                userFriendlyError = '服务器内部错误 (500)';
            }

            showError(userFriendlyError, technicalDetails);
            addMessage(`抱歉，${userFriendlyError}`, false);
        } finally {
            // 恢复输入和按钮
            questionInput.disabled = false;
            sendButton.disabled = false;
            sendButton.innerHTML = '<i class="fas fa-paper-plane"></i> 发送';
            loadingSpinner.style.display = 'none';

            // 聚焦输入框
            questionInput.focus();
        }
    });

    // 输入框回车发送
    questionInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            chatForm.dispatchEvent(new Event('submit'));
        }
    });

    // 初始认证检查
    checkAuthStatus().then(authenticated => {
        if (!authenticated) {
            showError('用户未认证', '请确保您已登录系统');
        }
    });

    // 初始聚焦输入框
    questionInput.focus();
});