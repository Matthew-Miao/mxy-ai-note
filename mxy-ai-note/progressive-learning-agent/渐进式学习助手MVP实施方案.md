# 渐进式学习助手MVP实施方案

## 目录

1. [MVP概述](#1-mvp概述)
2. [核心功能简化](#2-核心功能简化)
3. [技术架构设计](#3-技术架构设计)
4. [MVP实现路线图](#4-mvp实现路线图)
5. [核心代码结构](#5-核心代码结构)
6. [扩展性设计](#6-扩展性设计)
7. [快速启动指南](#7-快速启动指南)

---

## 1. MVP概述

### 1.1 MVP目标

基于SpringAI Alibaba Graph框架，快速构建一个**最小可行产品(MVP)**，实现渐进式学习助手的核心功能，为后续迭代奠定坚实基础。

### 1.2 MVP核心价值

- **快速验证**：2-3周内完成核心功能开发
- **技术验证**：验证SpringAI Graph在教育场景的可行性
- **用户反馈**：获取真实用户使用反馈
- **迭代基础**：为后续功能扩展提供稳定架构

### 1.3 MVP功能范围

**包含功能**：
- ✅ 基础学习流程（问答式学习）
- ✅ 简单内容生成（基于LLM）
- ✅ 基础评估机制（正确率统计）
- ✅ 学习进度跟踪
- ✅ 简单的自适应调整

**暂不包含**：
- ❌ 复杂的学习者画像
- ❌ 多维度评估体系
- ❌ 人工干预机制
- ❌ 社交学习功能
- ❌ 复杂的UI界面

---

## 2. 核心功能简化

### 2.1 简化的学习流程

```
简化学习流程：

[开始] → [知识点介绍] → [理解检测] → [结果评估] → [下一步决策]
   ↓           ↓            ↓           ↓           ↓
[初始化]   [内容生成]   [问答交互]   [评分计算]   [路径调整]
```

### 2.2 核心节点简化设计

#### 2.2.1 内容生成节点 (SimpleContentNode)
**功能**：基于知识点生成学习内容和测试问题
```java
// 输入：知识点名称、难度级别
// 输出：学习内容、测试问题
```

#### 2.2.2 理解检测节点 (ComprehensionTestNode)
**功能**：通过问答形式检测学习者理解程度
```java
// 输入：测试问题、学习者答案
// 输出：正确性判断、理解度评分
```

#### 2.2.3 进度管理节点 (ProgressManagerNode)
**功能**：管理学习进度和路径调整
```java
// 输入：当前进度、评估结果
// 输出：下一个学习节点、难度调整建议
```

### 2.3 简化的状态模型

```java
/**
 * 简化学习状态实体类
 * 使用MyBatis Plus注解
 */
@Data
@TableName("learning_state")
public class SimpleLearningState {
    
    @TableId(type = IdType.AUTO)
    private Long id;                       // 主键ID
    
    private String learnerId;              // 学习者ID
    private String currentTopic;           // 当前学习主题
    private Integer currentLevel;          // 当前难度等级(1-5)
    private Double comprehensionScore;     // 理解度评分(0-1)
    private Integer correctAnswers;        // 正确答案数
    private Integer totalQuestions;        // 总问题数
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> completedTopics;  // 已完成主题(JSON存储)
    
    private String nextAction;             // 下一步行动
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;  // 扩展元数据
    
    @TableLogic
    private Integer deleted;               // 逻辑删除标记
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;       // 创建时间
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;       // 更新时间
}
```

---

## 3. 技术架构设计

### 3.1 MVP架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    MVP系统架构                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   接口层 (API Layer)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐     │
│  │ REST API    │  │ WebSocket   │  │  健康检查       │     │
│  └─────────────┘  └─────────────┘  └─────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                 业务逻辑层 (Service Layer)                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐     │
│  │ 学习服务     │  │ 内容服务     │  │  评估服务       │     │
│  └─────────────┘  └─────────────┘  └─────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│               Graph工作流层 (Graph Layer)                   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              简化学习流程 StateGraph                │   │
│  │                                                     │   │
│  │  START → 内容生成 → 理解检测 → 进度管理 → END       │   │
│  │            ↓         ↓         ↓                  │   │
│  │      [ContentNode][TestNode][ProgressNode]        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────┐
│                 数据层 (Data Layer)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐     │
│  │ H2数据库     │  │ Redis缓存   │  │  通义千问API    │     │
│  │ (开发测试)   │  │ (会话状态)   │  │  (内容生成)     │     │
│  └─────────────┘  └─────────────┘  └─────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 技术栈选择

**后端技术**：
- Spring Boot 3.x
- SpringAI Alibaba Graph 1.0.0.3-SNAPSHOT
- MySQL 8.0+ (数据库)
- MyBatis + MyBatis Plus (ORM框架)
- Redis (可选，用于缓存)
- Maven

**AI服务**：
- 通义千问API (内容生成)
- Spring AI ChatClient

**前端技术**：
- 简单的HTML + JavaScript (MVP阶段)
- Bootstrap (快速UI)
- WebSocket (实时交互)

---

## 4. MVP实现路线图

### 4.1 第一周：基础架构搭建

**Day 1-2: 项目初始化**
- [ ] 创建Spring Boot项目
- [ ] 集成SpringAI Alibaba Graph依赖
- [ ] 配置通义千问API
- [ ] 设置H2数据库

**Day 3-4: 核心模型设计**
- [ ] 定义SimpleLearningState
- [ ] 创建基础实体类
- [ ] 设计数据库表结构
- [ ] 实现基础Repository

**Day 5-7: Graph节点实现**
- [ ] 实现SimpleContentNode
- [ ] 实现ComprehensionTestNode
- [ ] 实现ProgressManagerNode
- [ ] 配置StateGraph工作流

### 4.2 第二周：核心功能开发

**Day 8-10: 服务层开发**
- [ ] 实现LearningService
- [ ] 实现ContentService
- [ ] 实现AssessmentService
- [ ] 集成LLM内容生成

**Day 11-12: API接口开发**
- [ ] 实现REST API接口
- [ ] 添加WebSocket支持
- [ ] 实现错误处理
- [ ] 添加日志记录

**Day 13-14: 前端界面**
- [ ] 创建简单的学习界面
- [ ] 实现问答交互
- [ ] 添加进度显示
- [ ] 集成WebSocket通信

### 4.3 第三周：测试与优化

**Day 15-17: 测试验证**
- [ ] 单元测试编写
- [ ] 集成测试
- [ ] 端到端测试
- [ ] 性能测试

**Day 18-21: 优化部署**
- [ ] 代码优化
- [ ] 文档完善
- [ ] 部署配置
- [ ] 用户测试

---

## 5. 核心代码结构

### 5.1 项目结构

```
src/main/java/com/example/learning/
├── LearningAssistantApplication.java
├── config/
│   ├── GraphConfiguration.java
│   ├── ChatClientConfiguration.java
│   ├── WebSocketConfiguration.java
│   └── MyBatisPlusConfiguration.java
├── model/
│   ├── SimpleLearningState.java
│   ├── LearningTopic.java
│   ├── Question.java
│   └── Answer.java
├── node/
│   ├── SimpleContentNode.java
│   ├── ComprehensionTestNode.java
│   └── ProgressManagerNode.java
├── service/
│   ├── LearningService.java
│   ├── ContentService.java
│   └── AssessmentService.java
├── controller/
│   ├── LearningController.java
│   └── WebSocketController.java
├── mapper/
│   ├── LearningStateMapper.java
│   ├── TopicMapper.java
│   └── LearningSessionMapper.java
└── dto/
    ├── LearningRequest.java
    └── LearningResponse.java

src/main/resources/
├── mapper/
│   ├── LearningStateMapper.xml
│   ├── TopicMapper.xml
│   └── LearningSessionMapper.xml
├── db/
│   └── migration/
│       └── V1__init_tables.sql
└── application.yml
```

### 5.2 MyBatis Plus配置类

```java
/**
 * MyBatis Plus配置类
 */
@Configuration
@MapperScan("com.example.learning.mapper")
public class MyBatisPlusConfiguration {
    
    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    /**
     * 自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
            
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

### 5.3 Mapper接口实现

```java
/**
 * 学习状态Mapper接口
 */
@Mapper
public interface LearningStateMapper extends BaseMapper<SimpleLearningState> {
    
    /**
     * 根据学习者ID查询最新学习状态
     */
    @Select("SELECT * FROM learning_state WHERE learner_id = #{learnerId} AND deleted = 0 ORDER BY updated_at DESC LIMIT 1")
    SimpleLearningState findLatestByLearnerId(@Param("learnerId") String learnerId);
    
    /**
     * 更新学习进度
     */
    @Update("UPDATE learning_state SET current_topic = #{currentTopic}, current_level = #{currentLevel}, " +
            "comprehension_score = #{comprehensionScore}, correct_answers = #{correctAnswers}, " +
            "total_questions = #{totalQuestions}, next_action = #{nextAction} " +
            "WHERE learner_id = #{learnerId} AND deleted = 0")
    int updateProgress(SimpleLearningState state);
    
    /**
     * 查询学习统计信息
     */
    @Select("SELECT learner_id, COUNT(*) as total_sessions, AVG(comprehension_score) as avg_score " +
            "FROM learning_state WHERE deleted = 0 GROUP BY learner_id")
    List<Map<String, Object>> getLearningStatistics();
}
```

```java
/**
 * 学习主题Mapper接口
 */
@Mapper
public interface TopicMapper extends BaseMapper<LearningTopic> {
    
    /**
     * 查询所有激活的主题
     */
    @Select("SELECT * FROM learning_topics WHERE is_active = 1 AND deleted = 0 ORDER BY created_at")
    List<LearningTopic> findActiveTopics();
    
    /**
     * 根据主题名称查询
     */
    @Select("SELECT * FROM learning_topics WHERE topic_name = #{topicName} AND deleted = 0")
    LearningTopic findByTopicName(@Param("topicName") String topicName);
}
```

```java
/**
 * 学习会话Mapper接口
 */
@Mapper
public interface LearningSessionMapper extends BaseMapper<LearningSession> {
    
    /**
     * 查询学习者的会话历史
     */
    @Select("SELECT * FROM learning_sessions WHERE learner_id = #{learnerId} AND deleted = 0 " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<LearningSession> findSessionHistory(@Param("learnerId") String learnerId, 
                                           @Param("limit") int limit);
    
    /**
     * 统计主题的学习情况
     */
    @Select("SELECT topic, COUNT(*) as total_questions, " +
            "SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct_count, " +
            "AVG(comprehension_score) as avg_score " +
            "FROM learning_sessions WHERE learner_id = #{learnerId} AND deleted = 0 " +
            "GROUP BY topic")
    List<Map<String, Object>> getTopicStatistics(@Param("learnerId") String learnerId);
}
```

### 5.4 核心配置类

```java
@Configuration
public class GraphConfiguration {
    
    @Bean
    public StateGraph simpleLearningGraph(
            ChatClient.Builder chatClientBuilder,
            ContentService contentService,
            AssessmentService assessmentService) {
        
        // 状态合并策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("learningState", new ReplaceStrategy());
            strategies.put("currentContent", new ReplaceStrategy());
            strategies.put("testResults", new AppendStrategy());
            return strategies;
        };
        
        return new StateGraph(keyStrategyFactory)
            // 添加核心节点
            .addNode("contentGenerator", 
                nodeasync(new SimpleContentNode(chatClientBuilder)))
            .addNode("comprehensionTest", 
                nodeasync(new ComprehensionTestNode(chatClientBuilder)))
            .addNode("progressManager", 
                nodeasync(new ProgressManagerNode(assessmentService)))
            
            // 定义流程路径
            .addEdge(StateGraph.START, "contentGenerator")
            .addEdge("contentGenerator", "comprehensionTest")
            .addEdge("comprehensionTest", "progressManager")
            
            // 条件分支：根据理解度决定下一步
            .addConditionalEdges(
                "progressManager",
                this::routeBasedOnProgress,
                Map.of(
                    "continue", "contentGenerator",
                    "complete", StateGraph.END
                )
            );
    }
    
    /**
     * 根据学习进度决定路由
     */
    private String routeBasedOnProgress(OverAllState state) {
        SimpleLearningState learningState = state.value("learningState");
        
        // 如果理解度达标且还有未完成主题，继续学习
        if (learningState.getComprehensionScore() >= 0.7 && 
            hasMoreTopics(learningState)) {
            return "continue";
        }
        
        // 否则完成学习
        return "complete";
    }
    
    private boolean hasMoreTopics(SimpleLearningState state) {
        // 检查是否还有未完成的学习主题
        return state.getCompletedTopics().size() < getTotalTopics();
    }
}
```

### 5.3 核心节点实现

```java
/**
 * 简化内容生成节点
 */
public class SimpleContentNode implements NodeAction {
    
    private final ChatClient chatClient;
    
    public SimpleContentNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        SimpleLearningState learningState = state.value("learningState");
        String currentTopic = learningState.getCurrentTopic();
        Integer level = learningState.getCurrentLevel();
        
        // 生成学习内容
        String content = generateContent(currentTopic, level);
        
        // 生成测试问题
        String question = generateQuestion(currentTopic, level);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("question", question);
        result.put("topic", currentTopic);
        
        return result;
    }
    
    /**
     * 生成学习内容
     */
    private String generateContent(String topic, Integer level) {
        String prompt = String.format(
            "请为主题'%s'生成适合难度级别%d的学习内容。" +
            "内容应该简洁明了，包含核心概念和简单示例。" +
            "字数控制在200字以内。",
            topic, level
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    /**
     * 生成测试问题
     */
    private String generateQuestion(String topic, Integer level) {
        String prompt = String.format(
            "基于主题'%s'和难度级别%d，生成一个测试问题。" +
            "问题应该能够检验学习者对核心概念的理解。" +
            "只返回问题，不要包含答案。",
            topic, level
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
}
```

```java
/**
 * 理解检测节点
 */
public class ComprehensionTestNode implements NodeAction {
    
    private final ChatClient chatClient;
    
    public ComprehensionTestNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String question = state.value("question");
        String userAnswer = state.value("userAnswer");
        String topic = state.value("topic");
        
        // 评估答案正确性
        boolean isCorrect = evaluateAnswer(question, userAnswer, topic);
        
        // 计算理解度评分
        double comprehensionScore = calculateComprehensionScore(isCorrect, state);
        
        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("comprehensionScore", comprehensionScore);
        result.put("feedback", generateFeedback(isCorrect, userAnswer));
        
        return result;
    }
    
    /**
     * 评估答案正确性
     */
    private boolean evaluateAnswer(String question, String userAnswer, String topic) {
        String prompt = String.format(
            "问题：%s\n" +
            "学习者答案：%s\n" +
            "主题：%s\n\n" +
            "请评估这个答案是否正确。只回答'正确'或'错误'。",
            question, userAnswer, topic
        );
        
        String evaluation = chatClient.prompt()
            .user(prompt)
            .call()
            .content();
        
        return evaluation.contains("正确");
    }
    
    /**
     * 计算理解度评分
     */
    private double calculateComprehensionScore(boolean isCorrect, OverAllState state) {
        SimpleLearningState learningState = state.value("learningState");
        
        int correct = learningState.getCorrectAnswers();
        int total = learningState.getTotalQuestions();
        
        if (isCorrect) {
            correct++;
        }
        total++;
        
        return (double) correct / total;
    }
    
    /**
     * 生成反馈信息
     */
    private String generateFeedback(boolean isCorrect, String userAnswer) {
        if (isCorrect) {
            return "回答正确！继续保持。";
        } else {
            return "回答需要改进，建议重新学习相关内容。";
        }
    }
}
```

### 5.5 服务层实现

```java
/**
 * 学习服务实现类
 */
@Service
@Transactional
public class LearningServiceImpl implements LearningService {
    
    private final LearningStateMapper learningStateMapper;
    private final TopicMapper topicMapper;
    private final LearningSessionMapper sessionMapper;
    private final CompiledGraph learningGraph;
    
    public LearningServiceImpl(LearningStateMapper learningStateMapper,
                              TopicMapper topicMapper,
                              LearningSessionMapper sessionMapper,
                              @Qualifier("simpleLearningGraph") StateGraph stateGraph) throws GraphStateException {
        this.learningStateMapper = learningStateMapper;
        this.topicMapper = topicMapper;
        this.sessionMapper = sessionMapper;
        this.learningGraph = stateGraph.compile();
    }
    
    /**
     * 开始学习会话
     */
    @Override
    public Map<String, Object> startLearning(String learnerId, String topic) {
        // 查询或创建学习状态
        SimpleLearningState learningState = learningStateMapper.findLatestByLearnerId(learnerId);
        if (learningState == null) {
            learningState = createNewLearningState(learnerId, topic);
            learningStateMapper.insert(learningState);
        } else {
            learningState.setCurrentTopic(topic);
            learningState.setCurrentLevel(1);
            learningStateMapper.updateById(learningState);
        }
        
        // 执行Graph工作流
        Map<String, Object> input = new HashMap<>();
        input.put("learningState", learningState);
        
        RunnableConfig config = RunnableConfig.builder()
            .threadId(learnerId)
            .build();
        
        Optional<OverAllState> result = learningGraph.invoke(input, config);
        
        if (result.isPresent()) {
            return result.get().data();
        } else {
            throw new RuntimeException("学习流程执行失败");
        }
    }
    
    /**
     * 提交答案
     */
    @Override
    public Map<String, Object> submitAnswer(String learnerId, String answer) {
        SimpleLearningState learningState = learningStateMapper.findLatestByLearnerId(learnerId);
        if (learningState == null) {
            throw new RuntimeException("未找到学习状态");
        }
        
        // 执行答案评估流程
        Map<String, Object> input = new HashMap<>();
        input.put("learningState", learningState);
        input.put("userAnswer", answer);
        
        RunnableConfig config = RunnableConfig.builder()
            .threadId(learnerId)
            .build();
        
        Optional<OverAllState> result = learningGraph.invoke(input, config);
        
        if (result.isPresent()) {
            // 保存学习记录
            saveLearningSession(learningState, answer, result.get());
            return result.get().data();
        } else {
            throw new RuntimeException("答案评估失败");
        }
    }
    
    /**
     * 创建新的学习状态
     */
    private SimpleLearningState createNewLearningState(String learnerId, String topic) {
        SimpleLearningState state = new SimpleLearningState();
        state.setLearnerId(learnerId);
        state.setCurrentTopic(topic);
        state.setCurrentLevel(1);
        state.setComprehensionScore(0.0);
        state.setCorrectAnswers(0);
        state.setTotalQuestions(0);
        state.setCompletedTopics(new ArrayList<>());
        state.setNextAction("start");
        state.setMetadata(new HashMap<>());
        return state;
    }
    
    /**
     * 保存学习会话记录
     */
    private void saveLearningSession(SimpleLearningState learningState, String answer, OverAllState result) {
        LearningSession session = new LearningSession();
        session.setLearnerId(learningState.getLearnerId());
        session.setTopic(learningState.getCurrentTopic());
        session.setQuestion(result.value("question"));
        session.setUserAnswer(answer);
        session.setIsCorrect(result.value("isCorrect"));
        session.setComprehensionScore(result.value("comprehensionScore"));
        
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("level", learningState.getCurrentLevel());
        sessionData.put("feedback", result.value("feedback"));
        session.setSessionData(sessionData);
        
        sessionMapper.insert(session);
    }
}
```

### 5.6 REST API控制器

```java
@RestController
@RequestMapping("/api/learning")
public class LearningController {
    
    private final LearningService learningService;
    
    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }
    
    /**
     * 开始学习会话
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startLearning(
            @RequestParam String learnerId,
            @RequestParam String topic) {
        
        Map<String, Object> result = learningService.startLearning(learnerId, topic);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @RequestParam String learnerId,
            @RequestParam String answer) {
        
        Map<String, Object> result = learningService.submitAnswer(learnerId, answer);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取学习进度
     */
    @GetMapping("/progress/{learnerId}")
    public ResponseEntity<SimpleLearningState> getProgress(
            @PathVariable String learnerId) {
        
        SimpleLearningState progress = learningService.getProgress(learnerId);
        return ResponseEntity.ok(progress);
    }
    
    /**
     * 获取可用主题列表
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getAvailableTopics() {
        List<String> topics = learningService.getAvailableTopics();
        return ResponseEntity.ok(topics);
    }
}
```

---

## 6. 扩展性设计

### 6.1 架构扩展点

**节点扩展**：
```java
// 预留接口，便于后续添加新节点
public interface ExtendedNodeAction extends NodeAction {
    String getNodeType();
    Map<String, Object> getNodeMetadata();
}
```

**状态扩展**：
```java
// 状态模型支持动态扩展
public class ExtendableLearningState extends SimpleLearningState {
    private Map<String, Object> extensions = new HashMap<>();
    
    public void addExtension(String key, Object value) {
        extensions.put(key, value);
    }
}
```

**服务扩展**：
```java
// 插件化服务架构
public interface LearningPlugin {
    String getPluginName();
    void initialize(Map<String, Object> config);
    Map<String, Object> process(OverAllState state);
}
```

### 6.2 配置扩展

```yaml
# application.yml - 支持配置化扩展
learning:
  assistant:
    # 基础配置
    default-difficulty: 3
    max-questions-per-topic: 5
    passing-score: 0.7
    
    # 扩展配置
    plugins:
      enabled: true
      scan-packages: 
        - com.example.learning.plugins
    
    # AI模型配置
    ai:
      model: qwen-max
      temperature: 0.7
      max-tokens: 500
    
    # 未来扩展预留
    features:
      advanced-assessment: false
      social-learning: false
      gamification: false
```

### 6.3 数据库扩展设计

```sql
-- V1__init_tables.sql
-- 核心表设计，支持后续扩展
CREATE TABLE learning_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    learner_id VARCHAR(100) NOT NULL COMMENT '学习者ID',
    current_topic VARCHAR(200) COMMENT '当前学习主题',
    current_level INTEGER DEFAULT 1 COMMENT '当前难度等级',
    comprehension_score DECIMAL(3,2) DEFAULT 0.0 COMMENT '理解度评分',
    correct_answers INTEGER DEFAULT 0 COMMENT '正确答案数',
    total_questions INTEGER DEFAULT 0 COMMENT '总问题数',
    completed_topics TEXT COMMENT '已完成主题列表',
    next_action VARCHAR(100) COMMENT '下一步行动',
    
    -- 扩展字段
    metadata JSON COMMENT '扩展元数据',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_learner_id (learner_id),
    INDEX idx_topic (current_topic),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习状态表';

-- 主题配置表
CREATE TABLE learning_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    topic_name VARCHAR(200) NOT NULL UNIQUE COMMENT '主题名称',
    description TEXT COMMENT '主题描述',
    difficulty_levels INTEGER DEFAULT 5 COMMENT '难度级别数',
    prerequisites TEXT COMMENT '前置条件',
    
    -- 扩展配置
    config JSON COMMENT '主题配置',
    is_active TINYINT DEFAULT 1 COMMENT '是否激活',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_topic_name (topic_name),
    INDEX idx_active (is_active),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习主题表';

-- 学习记录表
CREATE TABLE learning_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    learner_id VARCHAR(100) NOT NULL COMMENT '学习者ID',
    topic VARCHAR(200) COMMENT '学习主题',
    question TEXT COMMENT '问题内容',
    user_answer TEXT COMMENT '用户答案',
    is_correct TINYINT COMMENT '是否正确',
    comprehension_score DECIMAL(3,2) COMMENT '理解度评分',
    
    -- 扩展数据
    session_data JSON COMMENT '会话数据',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_learner_topic (learner_id, topic),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习会话记录表';

-- 初始化主题数据
INSERT INTO learning_topics (topic_name, description, difficulty_levels, is_active) VALUES
('Spring Boot基础', 'Spring Boot框架基础知识学习', 5, 1),
('Java集合框架', 'Java集合框架的使用和原理', 5, 1),
('数据库设计', '关系型数据库设计原理和实践', 5, 1),
('算法与数据结构', '基础算法和数据结构学习', 5, 1),
('设计模式', '常用设计模式的理解和应用', 5, 1);
```

---

## 7. 快速启动指南

### 7.1 环境准备

**必需环境**：
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- 通义千问API密钥

**可选环境**：
- Redis (用于缓存)

### 7.2 项目初始化

```bash
# 1. 创建项目目录
mkdir progressive-learning-mvp
cd progressive-learning-mvp

# 2. 初始化Maven项目
mvn archetype:generate \
  -DgroupId=com.example.learning \
  -DartifactId=learning-assistant-mvp \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false

# 3. 进入项目目录
cd learning-assistant-mvp
```

### 7.3 依赖配置 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example.learning</groupId>
    <artifactId>learning-assistant-mvp</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    
    <name>Progressive Learning Assistant MVP</name>
    <description>MVP implementation of progressive learning assistant</description>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.0</spring-boot.version>
        <spring-ai-alibaba.version>1.0.0.3-SNAPSHOT</spring-ai-alibaba.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- 移除JPA，使用MyBatis Plus -->
        <!-- <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency> -->
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        
        <!-- SpringAI Alibaba Graph -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-graph-core</artifactId>
            <version>${spring-ai-alibaba.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
            <version>${spring-ai-alibaba.version}</version>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.4</version>
        </dependency>
        
        <!-- MyBatis Plus Generator (可选，用于代码生成) -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-generator</artifactId>
            <version>3.5.4</version>
            <scope>test</scope>
        </dependency>
        
        <!-- Optional: Redis for caching -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### 7.4 配置文件 (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: learning-assistant-mvp
  
  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/learning_assistant?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
    
# MyBatis Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.example.learning.model
  
  # AI配置
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
      chat:
        options:
          model: qwen-max
          temperature: 0.7
          max-tokens: 500

# 学习助手配置
learning:
  assistant:
    default-difficulty: 3
    max-questions-per-topic: 5
    passing-score: 0.7
    
    # 预设主题
    topics:
      - "Spring Boot基础"
      - "Java集合框架"
      - "数据库设计"
      - "算法与数据结构"
      - "设计模式"

# 日志配置
logging:
  level:
    com.example.learning: DEBUG
    org.springframework.ai: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### 7.5 启动步骤

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE learning_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 执行数据库初始化脚本
mysql -u root -p learning_assistant < src/main/resources/db/migration/V1__init_tables.sql

# 3. 设置环境变量
set DASHSCOPE_API_KEY=your-actual-api-key
set DB_USERNAME=root
set DB_PASSWORD=your-mysql-password

# 4. 编译项目
mvn clean compile

# 5. 运行应用
mvn spring-boot:run

# 6. 访问应用
# Web界面: http://localhost:8080
# API文档: http://localhost:8080/swagger-ui.html (如果集成了Swagger)
# 数据库管理: 可使用Navicat、DBeaver等工具连接MySQL
```

### 7.6 快速测试

```bash
# 1. 开始学习
curl -X POST "http://localhost:8080/api/learning/start" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "learnerId=test001&topic=Spring Boot基础"

# 2. 提交答案
curl -X POST "http://localhost:8080/api/learning/answer" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "learnerId=test001&answer=Spring Boot是一个Java框架"

# 3. 查看进度
curl "http://localhost:8080/api/learning/progress/test001"

# 4. 获取主题列表
curl "http://localhost:8080/api/learning/topics"

# 5. 查看学习统计
curl "http://localhost:8080/api/learning/statistics/test001"

# 6. 查看数据库数据
mysql -u root -p learning_assistant -e "SELECT * FROM learning_state;"
mysql -u root -p learning_assistant -e "SELECT * FROM learning_sessions ORDER BY created_at DESC LIMIT 10;"
```

---

## 总结

这个MVP方案将复杂的渐进式学习助手简化为核心功能，重点关注：

1. **快速验证**：2-3周内可完成开发和测试
2. **技术验证**：验证SpringAI Graph在教育场景的可行性
3. **扩展性**：预留了充分的扩展接口和配置
4. **实用性**：提供了完整的开发和部署指南

通过这个MVP，您可以：
- 快速搭建基础架构
- 验证核心技术方案
- 收集用户反馈
- 为后续迭代奠定基础

后续可以基于用户反馈和业务需求，逐步添加更复杂的功能，如多维度评估、学习者画像、社交学习等。

---

**文档信息**
- 文档版本：v1.0
- 创建时间：2024年12月
- 适用阶段：MVP开发
- 预计开发周期：2-3周
- 技术栈：SpringAI Alibaba Graph + Spring Boot + H2