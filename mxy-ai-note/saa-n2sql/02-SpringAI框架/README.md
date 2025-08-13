# 第二章：Spring AI框架入门

## 🎯 学习目标

- 理解Spring AI框架的核心概念和设计理念
- 掌握Spring AI的基本组件和使用方法
- 了解Spring AI Alibaba的扩展功能
- 为后续的NL2SQL学习打下框架基础

## 📚 章节内容

### 2.1 Spring AI框架概述

#### 什么是Spring AI？

**Spring AI** 是Spring生态系统中专门用于AI应用开发的框架，它提供了构建AI应用所需的底层抽象和工具。

```
Spring AI = Spring Boot + AI能力抽象
```

#### 核心设计理念

1. **抽象化**：屏蔽不同AI服务提供商的差异
2. **标准化**：提供统一的编程接口
3. **集成化**：深度集成Spring生态系统
4. **可扩展**：支持自定义扩展和插件

#### 架构层次

```
┌─────────────────────────────────────┐
│        应用层 (Your Application)    │
├─────────────────────────────────────┤
│        Spring AI 抽象层             │
│  ChatClient | EmbeddingModel | ... │
├─────────────────────────────────────┤
│        AI服务提供商层                │
│  OpenAI | Azure | Alibaba | ...    │
└─────────────────────────────────────┘
```

### 2.2 Spring AI核心组件

#### 2.2.1 ChatClient - 对话客户端

**ChatClient** 是Spring AI中用于与大语言模型交互的核心组件。

##### 基本用法

```java
@Service
public class ChatService {
    
    private final ChatClient chatClient;
    
    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * 简单对话示例
     */
    public String simpleChat(String userMessage) {
        return chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
    }
    
    /**
     * 带系统提示的对话
     */
    public String chatWithSystem(String userMessage) {
        return chatClient.prompt()
            .system("你是一个专业的数据分析师")
            .user(userMessage)
            .call()
            .content();
    }
}
```

##### 高级特性

1. **流式响应**
```java
/**
 * 流式对话，实时返回结果
 */
public Flux<String> streamChat(String userMessage) {
    return chatClient.prompt()
        .user(userMessage)
        .stream()
        .content();
}
```

2. **结构化输出**
```java
/**
 * 返回结构化数据
 */
public Person extractPerson(String text) {
    return chatClient.prompt()
        .user("从以下文本中提取人员信息：" + text)
        .call()
        .entity(Person.class);
}
```

#### 2.2.2 EmbeddingModel - 向量化模型

**EmbeddingModel** 用于将文本转换为向量表示，是RAG技术的基础。

```java
@Service
public class EmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    
    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    /**
     * 文本向量化
     */
    public List<Double> embedText(String text) {
        EmbeddingResponse response = embeddingModel.embedForResponse(
            List.of(text)
        );
        return response.getResults().get(0).getOutput();
    }
    
    /**
     * 批量向量化
     */
    public List<List<Double>> embedTexts(List<String> texts) {
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        return response.getResults().stream()
            .map(result -> result.getOutput())
            .collect(Collectors.toList());
    }
}
```

#### 2.2.3 VectorStore - 向量存储

**VectorStore** 提供向量数据的存储和检索功能。

```java
@Service
public class VectorStoreService {
    
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    
    /**
     * 添加文档到向量库
     */
    public void addDocuments(List<String> texts) {
        List<Document> documents = texts.stream()
            .map(text -> new Document(text))
            .collect(Collectors.toList());
        
        vectorStore.add(documents);
    }
    
    /**
     * 相似度搜索
     */
    public List<Document> similaritySearch(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
    }
}
```

#### 2.2.4 Document - 文档抽象

**Document** 是Spring AI中的文档抽象，包含内容和元数据。

```java
// 创建文档
Document document = new Document(
    "这是文档内容",
    Map.of(
        "source", "database_schema",
        "table", "users",
        "type", "table_description"
    )
);

// 访问文档信息
String content = document.getContent();
Map<String, Object> metadata = document.getMetadata();
String source = (String) metadata.get("source");
```

### 2.3 Spring AI Alibaba扩展

#### 2.3.1 DashScope集成

**Spring AI Alibaba** 提供了与阿里云DashScope服务的深度集成。

##### 配置示例

```yaml
# application.yml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        model: qwen-max
        options:
          temperature: 0.7
          max-tokens: 2000
      embedding:
        model: text-embedding-v3
```

##### 使用示例

```java
@Configuration
public class AIConfig {
    
    /**
     * 配置DashScope聊天客户端
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("你是一个专业的SQL专家")
            .build();
    }
}
```

#### 2.3.2 AnalyticDB向量存储

```java
@Configuration
public class VectorStoreConfig {
    
    /**
     * 配置AnalyticDB向量存储
     */
    @Bean
    public VectorStore vectorStore(
            AnalyticDbVectorStoreProperties properties,
            EmbeddingModel embeddingModel) {
        return new AnalyticDbVectorStore(properties, embeddingModel);
    }
}
```

### 2.4 实践示例：构建简单的RAG应用

#### 2.4.1 项目结构

```
src/main/java/
├── config/
│   ├── AIConfig.java          # AI配置
│   └── VectorStoreConfig.java # 向量存储配置
├── service/
│   ├── DocumentService.java   # 文档服务
│   ├── ChatService.java       # 对话服务
│   └── RAGService.java        # RAG服务
└── controller/
    └── ChatController.java     # REST控制器
```

#### 2.4.2 核心实现

```java
@Service
public class RAGService {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    public RAGService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }
    
    /**
     * RAG问答实现
     */
    public String ragQuery(String question) {
        // 1. 检索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(3)
        );
        
        // 2. 构建上下文
        String context = relevantDocs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));
        
        // 3. 生成回答
        return chatClient.prompt()
            .system("基于以下上下文回答用户问题：\n" + context)
            .user(question)
            .call()
            .content();
    }
}
```

#### 2.4.3 REST接口

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final RAGService ragService;
    
    public ChatController(RAGService ragService) {
        this.ragService = ragService;
    }
    
    /**
     * RAG问答接口
     */
    @PostMapping("/rag")
    public ResponseEntity<String> ragChat(@RequestBody ChatRequest request) {
        try {
            String response = ragService.ragQuery(request.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("处理请求时发生错误：" + e.getMessage());
        }
    }
    
    /**
     * 流式RAG问答接口
     */
    @PostMapping(value = "/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ragChatStream(@RequestBody ChatRequest request) {
        return ragService.ragQueryStream(request.getMessage());
    }
}
```

### 2.5 Spring AI最佳实践

#### 2.5.1 配置管理

```java
@ConfigurationProperties(prefix = "app.ai")
@Data
public class AIProperties {
    
    /**
     * 模型配置
     */
    private ModelConfig model = new ModelConfig();
    
    /**
     * 向量存储配置
     */
    private VectorConfig vector = new VectorConfig();
    
    @Data
    public static class ModelConfig {
        private String defaultModel = "qwen-max";
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
    }
    
    @Data
    public static class VectorConfig {
        private Integer topK = 5;
        private Double similarityThreshold = 0.7;
    }
}
```

#### 2.5.2 错误处理

```java
@Service
public class RobustChatService {
    
    private final ChatClient chatClient;
    private final RetryTemplate retryTemplate;
    
    /**
     * 带重试机制的聊天服务
     */
    public String chatWithRetry(String message) {
        return retryTemplate.execute(context -> {
            try {
                return chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            } catch (Exception e) {
                log.warn("聊天请求失败，重试次数：{}", context.getRetryCount());
                throw e;
            }
        });
    }
}
```

#### 2.5.3 性能优化

```java
@Service
public class OptimizedChatService {
    
    private final ChatClient chatClient;
    private final CacheManager cacheManager;
    
    /**
     * 带缓存的聊天服务
     */
    @Cacheable(value = "chat-responses", key = "#message")
    public String cachedChat(String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
    
    /**
     * 异步聊天服务
     */
    @Async
    public CompletableFuture<String> asyncChat(String message) {
        String response = chatClient.prompt()
            .user(message)
            .call()
            .content();
        return CompletableFuture.completedFuture(response);
    }
}
```

### 2.6 与传统Spring Boot的对比

#### 传统Spring Boot应用

```java
// 传统的数据库查询
@Service
public class TraditionalDataService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> findActiveUsers() {
        return userRepository.findByStatus("ACTIVE");
    }
}
```

#### Spring AI增强应用

```java
// AI增强的智能查询
@Service
public class AIEnhancedDataService {
    
    private final ChatClient chatClient;
    private final UserRepository userRepository;
    
    /**
     * 自然语言查询用户
     */
    public String queryUsers(String naturalLanguageQuery) {
        // 1. 将自然语言转换为查询逻辑
        String queryPlan = chatClient.prompt()
            .system("将用户的自然语言查询转换为数据库查询逻辑")
            .user(naturalLanguageQuery)
            .call()
            .content();
        
        // 2. 执行查询并返回结果
        return executeQueryPlan(queryPlan);
    }
}
```

### 2.7 Spring AI在NL2SQL中的作用

#### 核心价值

1. **统一抽象**：屏蔽不同AI服务的差异
2. **简化开发**：提供开箱即用的组件
3. **Spring集成**：无缝集成Spring生态
4. **企业级特性**：支持缓存、重试、监控等

#### 在NL2SQL项目中的应用

```
NL2SQL工作流中的Spring AI组件：

用户问题 → ChatClient(问题理解) → EmbeddingModel(向量化)
    ↓                                        ↓
VectorStore(Schema检索) ← Document(Schema文档)
    ↓
ChatClient(SQL生成) → 最终SQL
```

## 🎯 本章小结

通过本章学习，您应该已经：

✅ **理解了Spring AI框架的核心概念和组件**
✅ **掌握了ChatClient、EmbeddingModel、VectorStore的基本用法**
✅ **了解了Spring AI Alibaba的扩展功能**
✅ **能够构建简单的RAG应用**
✅ **理解了Spring AI在NL2SQL中的作用**

## 🚀 下一步学习

接下来，我们将深入学习Graph工作流的原理，这是Spring AI Alibaba NL2SQL项目的核心创新。

👉 [第三章：Graph工作流原理](../03-Graph工作流/README.md)

## 📝 实践练习

### 练习1：基础聊天应用
创建一个简单的Spring Boot应用，集成Spring AI，实现基本的聊天功能。

### 练习2：文档向量化
实现一个文档向量化服务，能够将文本文档转换为向量并存储到向量数据库。

### 练习3：简单RAG系统
基于本章的示例，构建一个完整的RAG问答系统。

## 📚 延伸阅读

- [Spring AI官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba GitHub](https://github.com/alibaba/spring-ai-alibaba)
- [RAG技术详解](https://arxiv.org/abs/2005.11401)

---

**恭喜您完成第二章的学习！** 🎉