// RAGPromptManager.java
package com.metaforge.ai.langchain4j.dashscope.prompt;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG 提示词管理器
 * 负责管理和优化 RAG 系统的提示词模板
 */
@Component
public class RAGPromptManager {

    // Prompt 模板管理
    private final Map<String, String> promptTemplates = new HashMap<>();

    public RAGPromptManager() {
        initializePromptTemplates();
    }

    /**
     * 初始化 Prompt 模板
     */
    private void initializePromptTemplates() {
        // 默认 RAG prompt - 严格基于文档
        promptTemplates.put("default", """
            你是一个专业的文档问答助手。请严格按照以下要求回答问题：

            ## 核心原则
            1. **基于文档**：只使用检索到的文档内容回答问题，不要使用外部知识或常识
            2. **准确完整**：如果文档中有相关信息，请全面、详细地整合所有信息
            3. **诚实可信**：如果文档中没有相关信息，请明确说"在提供的文档中没有找到相关信息"
            4. **拒绝编造**：严禁编造、推断或添加文档中不存在的内容
            5. **引用提示**：可以提及"根据文档内容"或"文档中提到"

            ## 回答格式要求
            - 首先给出直接、准确的答案摘要
            - 然后提供详细的支撑信息、具体数据和例子
            - 使用清晰的段落结构，必要时使用列表
            - 保持专业且易于理解的语气
            - 确保回答内容丰富且全面

            ## 当前上下文
            - 用户问题：{{question}}
            - 当前时间：%s
            - 信息来源：用户上传的文档内容

            请基于提供的文档内容详细回答：
            """.formatted(LocalDateTime.now()));

        // 详细回答 prompt
        promptTemplates.put("detailed", """
            你是一个详细的信息提取助手。请基于文档内容提供全面、详细的回答。

            ## 回答要求
            - **完整性**：整合文档中所有相关信息，不要遗漏重要细节
            - **准确性**：严格基于文档内容，不添加外部知识
            - **结构化**：使用清晰的标题、段落和列表组织信息
            - **详细性**：提供具体的例子、数据、步骤或引用
            - **可读性**：让回答易于阅读和理解

            ## 回答结构
            1. 首先给出总结性答案
            2. 然后按主题或重要性组织详细信息
            3. 提供具体的支撑内容
            4. 说明不同信息之间的关系

            ## 如果文档中没有相关信息：
            明确告知："在已上传的文档中没有找到与您问题相关的具体信息。"

            问题：{{question}}
            """);

        // 事实核查 prompt
        promptTemplates.put("strict", """
            你是一个事实核查助手。请严格基于文档内容回答问题。

            ## 严格约束
            - 只使用检索到的文档片段中的明确信息
            - 如果文档中没有明确提到，就说"文档中没有相关信息"
            - 严禁推断、猜测或使用常识
            - 如果信息不完整，如实说明"文档中只有部分信息"

            ## 回答模板
            基于文档内容：[直接答案]
            
            详细信息：
            [逐条列出相关事实]
            
            信息来源说明：[提及信息来自文档]

            问题：{{question}}
            """);

        // 简洁回答 prompt
        promptTemplates.put("concise", """
            请基于文档内容提供简洁、直接的答案。
            
            要求：
            - 只使用文档中的信息
            - 如果文档中没有相关信息，直接说"没有找到"
            - 回答要简短精炼
            - 不要添加额外解释

            问题：{{question}}
            """);
    }

    /**
     * 获取优化的问题
     */
    public String getOptimizedQuestion(String question, String promptType) {
        String template = promptTemplates.getOrDefault(promptType, promptTemplates.get("default"));
        String systemPrompt = template.replace("{{question}}", question);
        return systemPrompt + "\n\n用户问题：" + question;
    }

    /**
     * 获取默认优化问题
     */
    public String getDefaultOptimizedQuestion(String question) {
        return getOptimizedQuestion(question, "default");
    }

    /**
     * 获取所有可用的 prompt 类型
     */
    public String[] getAvailablePromptTypes() {
        return promptTemplates.keySet().toArray(new String[0]);
    }

    /**
     * 添加自定义 prompt 模板
     */
    public void addPromptTemplate(String name, String template) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt 模板名称不能为空");
        }
        if (template == null || template.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt 模板内容不能为空");
        }

        promptTemplates.put(name, template);
    }

    /**
     * 移除 prompt 模板
     */
    public boolean removePromptTemplate(String name) {
        return promptTemplates.remove(name) != null;
    }

    /**
     * 获取 prompt 模板内容
     */
    public String getPromptTemplate(String name) {
        return promptTemplates.get(name);
    }

    /**
     * 检查 prompt 类型是否存在
     */
    public boolean containsPromptType(String promptType) {
        return promptTemplates.containsKey(promptType);
    }

    /**
     * 重新加载默认模板（重置所有自定义修改）
     */
    public void reloadDefaultTemplates() {
        promptTemplates.clear();
        initializePromptTemplates();
    }
}