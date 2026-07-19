# RAG 核心概念详解

## 1. 什么是 RAG？

### 📖 定义
**RAG（Retrieval-Augmented Generation，检索增强生成）** 是一种结合了信息检索和生成式 AI 的技术架构。

### 🎯 为什么需要 RAG？

**问题 1：LLM 的知识盲区**
- GPT-4 的训练数据截止到 2023 年 9 月
- 无法回答训练数据之后的事件
- 无法访问私有数据（公司内部文档）

**问题 2：幻觉（Hallucination）**
- LLM 可能"编造"不存在的答案
- 缺乏事实依据，回答不可靠

**问题 3：上下文窗口限制**
- GPT-4 上下文窗口 128K tokens
- 无法一次性处理大量文档

### ✅ RAG 如何解决？

```
传统 LLM：
用户问题 → LLM → 回答（可能不准确/产生幻觉）

RAG：
用户问题 → 检索相关文档 → LLM 基于文档生成 → 回答（准确、有依据）
```

**RAG 的优势：**
- ✅ **准确性高**：基于真实文档，减少幻觉
- ✅ **可溯源**：可以标注引用来源
- ✅ **可更新**：文档更新，答案自动更新
- ✅ **成本低**：不需要微调模型
- ✅ **隐私安全**：数据无需上传到模型提供商

---

## 2. 向量（Embedding）核心概念

### 🧠 什么是向量？

**直观理解：**
- 文本 → 数字列表 → 机器能理解的"意义"

**举例：**
```
文本："苹果公司发布了新iPhone"
向量：[0.023, -0.456, 0.789, ..., 0.123]  (1536 维数字)
```

**为什么能表示语义？**
- OpenAI 的 Embedding 模型在大量文本上训练
- 学会了将"语义相似"的文本映射到"空间相近"的向量
- 通过向量距离衡量语义相似度

### 📏 相似度计算

**余弦相似度（Cosine Similarity）：**
```
similarity = cos(θ) = (A · B) / (||A|| × ||B||)

其中：
- A, B 是两个向量
- A · B 是点积
- ||A|| 是向量 A 的模（长度）

结果范围：[-1, 1]
- 1：完全同向（最相似）
- 0：正交（不相关）
- -1：完全相反
```

**实际应用：**
```
问题向量：Q = [0.1, 0.2, 0.3]
文档向量：D1 = [0.1, 0.2, 0.4]  → 相似度 0.99（很相似）
文档向量：D2 = [0.8, -0.1, 0.2] → 相似度 0.15（不相关）
```

### 🔢 向量维度选择

**常用模型：**
- **text-embedding-ada-002**：1536 维（OpenAI 推荐）
- **text-embedding-3-small**：1536 维
- **text-embedding-3-large**：3072 维（更高精度，更慢）

**维度影响：**
- 维度越高 → 表达能力越强，但计算和存储成本越高
- 1536 维是性价比最优的选择

---

## 3. 文档分块（Chunking）

### 📄 为什么需要分块？

**问题：**
- PDF 文档可能有 100 页
- 每页 500-1000 字符
- 总共 50K-100K 字符
- 超过 LLM 上下文窗口限制

**解决方案：**
- 将长文档分割成小块
- 每块 500-1000 字符
- 检索时只返回相关的块

### ✂️ 分块策略

#### 策略 1：固定字符分块
```java
// 每 500 字符一块
TextSplitter splitter = new CharacterTextSplitter(500, 0);
```

**优点：** 简单
**缺点：** 可能在句子中间切断

---

#### 策略 2：按段落分块
```java
// 按段落分割，每段不超过 500 字符
TextSplitter splitter = new ParagraphTextSplitter(500, 100);
```

**优点：** 保持段落完整性
**缺点：** 段落可能过长

---

#### 策略 3：智能语义分块（推荐）
```java
// 按句子分割，智能合并相关句子
TextSplitter splitter = new SentenceTextSplitter(500, 100);
```

**优点：**
- 保持句子完整
- 语义连贯
- 检索精度高

**缺点：** 计算稍慢

---

### 🔄 重叠（Overlap）

**什么是重叠？**
```
文档：[A B C D E F G H I J]

不重叠分块（500 字符，无重叠）：
块 1: [A B C D E]
块 2: [F G H I J]

重叠分块（500 字符，重叠 100）：
块 1: [A B C D E F G H I J]
块 2: [C D E F G H I J K L]
```

**为什么需要重叠？**
- 避免重要信息在边界被切断
- 提高检索召回率
- 保持上下文连贯性

**推荐参数：**
- 块大小：500-1000 字符
- 重叠大小：100-200 字符

---

## 4. 向量数据库

### 🗄️ 为什么不用传统数据库？

**传统数据库的问题：**
```
SELECT * FROM documents WHERE content LIKE '%苹果公司%'
```
- 只能匹配关键词
- 无法理解语义
- 搜索"苹果公司"找不到"Apple Inc."

**向量数据库的优势：**
```
query_vector = embed("苹果公司")
SELECT * FROM documents ORDER BY cosine_similarity(vector, query_vector) LIMIT 5
```
- 语义搜索：能理解"意思相近"
- 相似度排序：返回最相关的结果
- 高性能：使用索引加速检索

---

### 🚀 主流向量数据库对比

| 数据库 | 优点 | 缺点 | 适用场景 |
|--------|------|------|----------|
| **Milvus** | 高性能、可扩展、开源 | 部署复杂 | 大规模生产环境 |
| **ChromaDB** | 简单易用、轻量级 | 性能相对较低 | 开发测试、中小项目 |
| **Pinecone** | 托管服务、无需运维 | 付费、数据在云端 | 快速原型、小团队 |
| **Weaviate** | 语义搜索强大 | 学习曲线陡峭 | 需要复杂查询的场景 |
| **Qdrant** | Rust 实现、高性能 | 生态相对小 | 追求性能的项目 |

---

### 📊 向量索引原理

**问题：**
- 100 万个向量，每个 1536 维
- 计算相似度需要 100 万 × 1536 = 15.36 亿次运算
- 太慢！

**解决方案：索引**
```
无索引：线性搜索 O(n)
有索引：近似搜索 O(log n)
```

**常用索引算法：**

#### IVF（Inverted File Index）
- 将向量空间分成多个"桶"（聚类）
- 搜索时只查询最近的几个桶
- 速度提升 10-100 倍

#### HNSW（Hierarchical Navigable Small World）
- 构建多层图结构
- 类似高速公路网络，快速定位区域
- 速度提升 100-1000 倍

**推荐：**
- 小数据（< 10 万）：FLAT（精确搜索）
- 中等数据（10 万-100 万）：IVF
- 大数据（> 100 万）：HNSW

---

## 5. Prompt Engineering

### 📝 什么是 Prompt Engineering？

**定义：** 设计和优化输入给 LLM 的提示词，以获得期望的输出。

**重要性：**
- 好的 Prompt = 好的回答
- 坏的 Prompt = 糟糕的回答
- 这是 RAG 系统的核心技能

---

### 🎯 Prompt 设计原则

#### 原则 1：清晰定义角色
```
❌ 不好：回答问题
✅ 好：你是一个专业的技术文档助手，擅长解释复杂概念。
```

#### 原则 2：提供具体上下文
```
❌ 不好：根据文档回答
✅ 好：请根据以下技术文档片段回答用户问题。如果文档中没有答案，请诚实说明。
```

#### 原则 3：指定输出格式
```
❌ 不好：回答问题
✅ 好：请用中文回答，包含以下部分：
1. 简要回答（1-2 句话）
2. 详细解释
3. 引用来源（文档名 + 页码）
```

#### 原则 4：添加示例
```
✅ 好：
示例：
用户：什么是 RAG？
回答：RAG（检索增强生成）是一种结合信息检索和生成式 AI 的技术...

现在请回答用户的问题。
```

---

### 📋 RAG 系统 Prompt 模板

**系统提示词（System Prompt）：**
```markdown
你是一个智能问答助手，擅长基于技术文档回答问题。

# 规则
1. 仅根据提供的参考文档回答问题
2. 如果文档中没有答案，请诚实说明"文档中未找到相关信息"
3. 回答要准确、简洁、易懂
4. 必须标注引用来源（文档名 + 片段序号）
5. 保持客观中立的语气

# 输出格式
## 回答
[你的回答]

## 引用来源
- 文档：[文档名]
- 相关片段：[片段序号]
```

**用户 Prompt 构建：**
```markdown
# 参考文档
{{检索到的文档块，编号1, 2, 3...}}

# 用户问题
{{用户的问题}}

# 历史对话（可选）
{{最近几轮对话}}

请根据以上信息回答用户的问题。
```

---

### 🛠️ Prompt 优化技巧

#### 技巧 1：少样本学习（Few-Shot Learning）
```
# 示例
Q: 什么是 Spring Boot？
A: Spring Boot 是一个基于 Spring 框架的快速开发框架，简化了 Spring 应用的配置和部署...

Q: 什么是 LangChain4j？
A: LangChain4j 是 Java 版本的 LangChain 框架，用于构建 LLM 应用...

Q: {{用户问题}}
A:
```

#### 技巧 2：思维链（Chain-of-Thought）
```
请按以下步骤思考：
1. 理解用户问题
2. 在参考文档中查找相关信息
3. 综合多个片段的信息
4. 组织语言回答

参考文档：
{{文档块}}

用户问题：{{用户问题}}
```

#### 技巧 3：反思和修正
```
请先给出初步回答，然后反思：
1. 回答是否基于文档？
2. 是否有遗漏的信息？
3. 语言是否清晰？

如果需要修正，请给出改进后的回答。

参考文档：{{文档块}}
用户问题：{{用户问题}}
```

---

## 6. 对话历史管理

### 💾 为什么需要对话历史？

**场景 1：多轮对话**
```
用户：什么是 Spring Boot？
AI：Spring Boot 是一个快速开发框架...

用户：它有什么优点？（"它"指 Spring Boot）
AI：[需要知道上文在讨论 Spring Boot]
```

**场景 2：追问**
```
用户：如何配置数据库连接？
AI：可以在 application.yml 中配置...

用户：那 PostgreSQL 呢？（在追问具体数据库）
AI：[需要知道上下文是数据库配置]
```

---

### 🔄 历史管理策略

#### 策略 1：全部历史
```java
List<Message> history = getAllHistory(); // 可能很长
```

**问题：** 容易超出上下文窗口

---

#### 策略 2：固定窗口（推荐）
```java
List<Message> history = getLastNMessages(5); // 最近 5 轮
```

**优点：** 控制长度
**缺点：** 可能丢失早期重要信息

---

#### 策略 3：智能摘要
```java
String summary = summarizeOldHistory(); // 摘要早期对话
List<Message> history = List.of(summary, recentMessages);
```

**优点：** 保留关键信息
**缺点：** 需要额外的 LLM 调用

---

### 📦 历史存储格式

**JSON 格式：**
```json
{
  "session_id": "abc123",
  "messages": [
    {
      "role": "user",
      "content": "什么是 Spring Boot？",
      "timestamp": "2024-01-01T10:00:00Z"
    },
    {
      "role": "assistant",
      "content": "Spring Boot 是...",
      "timestamp": "2024-01-01T10:00:01Z"
    }
  ]
}
```

**数据库表设计：**
```sql
CREATE TABLE chat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL,
    role ENUM('user', 'assistant') NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_created (created_at)
);
```

---

## 7. 流式输出

### 🌊 什么是流式输出？

**传统输出：**
```
用户提问 → 等待 10 秒 → 一次性返回完整回答
```

**流式输出：**
```
用户提问 → 逐字返回（打字机效果）
"我" → "们" → "来" → "学" → "习" → ...
```

---

### ✅ 流式输出的优势

1. **更好的用户体验**
   - 立即看到响应
   - 类似 ChatGPT 的体验

2. **降低感知延迟**
   - 即使总时间相同，感觉更快

3. **提前终止**
   - 用户可以随时停止生成

---

### 🔧 实现方式

**Server-Sent Events (SSE)：**
```java
@GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chat(@RequestBody String question) {
    return chatService.streamResponse(question);
}
```

**前端接收：**
```javascript
const eventSource = new EventSource('/api/chat?question=' + question);
eventSource.onmessage = (event) => {
    appendToChat(event.data); // 逐字追加
};
```

---

## 8. 性能优化

### ⚡ 关键优化点

#### 优化 1：缓存向量化结果
```java
@Cacheable("embeddings")
public float[] embed(String text) {
    return embeddingModel.embed(text);
}
```

#### 优化 2：批量向量化
```java
List<float[]> embeddings = embeddingModel.embedAll(texts); // 一次处理多个
```

#### 优化 3：并行检索
```java
List<Document> results = vectorStore.similaritySearchWithScore(query, 5, true);
```

#### 优化 4：使用更快的 Embedding 模型
- text-embedding-3-small（更快、更便宜）
- 本地模型（Sentence Transformers）

#### 优化 5：向量数据库优化
- 选择合适的索引（HNSW）
- 调整索引参数（ef_search）
- 分片和复制

---

## 9. 错误处理

### 🚨 常见错误及处理

#### 错误 1：OpenAI API 调用失败
```java
try {
    Response response = openAiService.chat(messages);
} catch (RateLimitException e) {
    // 重试或返回友好提示
    return "请求过于频繁，请稍后再试";
} catch (AuthenticationException e) {
    // API Key 错误
    return "配置错误，请联系管理员";
}
```

#### 错误 2：向量检索无结果
```java
List<Document> docs = vectorStore.similaritySearch(query, 5);
if (docs.isEmpty()) {
    // 返回默认回答
    return "未找到相关文档，请尝试其他问题";
}
```

#### 错误 3：文档解析失败
```java
try {
    String text = pdfParser.parse(file);
} catch (IOException e) {
    // 记录日志，返回错误
    log.error("PDF 解析失败", e);
    throw new DocumentParseException("无法解析文档，请检查文件格式");
}
```

---

## 10. 安全性

### 🔒 安全注意事项

#### 1. API Key 保护
```yaml
# 不要硬编码在代码中！
openai:
  api-key: ${OPENAI_API_KEY} # 从环境变量读取
```

#### 2. 输入验证
```java
public ResponseEntity<?> chat(@RequestBody @Valid ChatRequest request) {
    // 验证输入长度、格式等
    if (request.getQuestion().length() > 1000) {
        return ResponseEntity.badRequest().body("问题过长");
    }
}
```

#### 3. 输出过滤
```java
String response = openAiService.chat(messages);
// 过滤敏感信息
response = filterSensitiveContent(response);
```

#### 4. 访问控制
```java
@PreAuthorize("hasRole('USER')")
public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
    // 只有登录用户可以访问
}
```

---

## 📚 总结

RAG 系统的核心概念：
1. **检索增强生成**：结合检索和生成
2. **向量化**：文本转数字，语义搜索
3. **文档分块**：长文档切分成小块
4. **向量数据库**：高效语义检索
5. **Prompt Engineering**：设计好的提示词
6. **对话历史**：支持多轮对话
7. **流式输出**：更好的用户体验
8. **性能优化**：缓存、批量、并行
9. **错误处理**：优雅降级
10. **安全性**：保护 API Key，验证输入

掌握这些概念，你就能构建一个强大的 RAG 系统！🚀