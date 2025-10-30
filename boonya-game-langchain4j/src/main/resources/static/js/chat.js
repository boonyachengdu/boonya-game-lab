// 聊天功能
document.addEventListener('DOMContentLoaded', function() {
    const chatForm = document.getElementById('chatForm');
    const questionInput = document.getElementById('questionInput');
    const chatMessages = document.getElementById('chatMessages');
    const sendButton = document.getElementById('sendButton');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const sessionId = document.getElementById('sessionId').value;

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

    // 处理表单提交
    chatForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const question = questionInput.value.trim();
        if (!question) return;

        // 禁用输入和按钮
        questionInput.disabled = true;
        sendButton.disabled = true;

        // 添加用户消息
        addMessage(question, true);

        // 清空输入框
        questionInput.value = '';

        // 显示加载动画
        loadingSpinner.style.display = 'block';

        try {
            // 发送请求到后端
            const response = await fetch('/api/rag/ask', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `sessionId=${encodeURIComponent(sessionId)}&question=${encodeURIComponent(question)}`
            });

            const data = await response.json();

            if (data.status === 'success') {
                addMessage(data.answer, false);
            } else {
                addMessage(`抱歉，出现错误: ${data.message || '未知错误'}`, false);
            }

        } catch (error) {
            console.error('请求失败:', error);
            addMessage('抱歉，网络连接出现问题，请稍后重试。', false);
        } finally {
            // 恢复输入和按钮
            questionInput.disabled = false;
            sendButton.disabled = false;
            loadingSpinner.style.display = 'none';

            // 聚焦输入框
            questionInput.focus();

            // 刷新页面以更新消息计数（简单实现）
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        }
    });

    // 输入框回车发送
    questionInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            chatForm.dispatchEvent(new Event('submit'));
        }
    });

    // 初始聚焦输入框
    questionInput.focus();
});

// 会话管理功能
function confirmEndSession(sessionId, isCurrent) {
    if (isCurrent) {
        alert('无法结束当前正在使用的会话。请先切换到其他会话。');
        return false;
    }

    return confirm('确定要结束这个会话吗？会话历史将被清除。');
}