# NL2SQL Graph节点调试断点指南

## 🎯 调试目标

通过在关键位置设置断点，观察Spring AI Alibaba NL2SQL项目中Graph工作流的节点执行流程、状态流转和数据传递机制。

## 🔍 核心调试位置

### 1. 流式接口入口断点

#### 位置1: streamSearch接口入口
**文件**: `Nl2sqlForGraphController.java`  
**方法**: `streamSearch`  
**行号**: 约160行

```java
@GetMapping(value = "/stream/search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamSearch(
    @RequestParam String query, 
    @RequestParam String agentId,
    HttpServletResponse response) throws Exception {
    
    // 🔴 断点1: 观察输入参数
    logger.info("Starting stream search for query: {} with agentId: {}", query, agentId);
```

**观察要点**:
- 用户输入的query内容
- agentId的值
- HTTP请求头信息

#### 位置2: Graph工作流启动
**文件**: `Nl2sqlForGraphController.java`  
**方法**: `streamSearch`  
**行号**: 约177行

```java
// 🔴 断点2: Graph工作流启动点
AsyncGenerator<NodeOutput> generator = compiledGraph
    .stream(Map.of(INPUT_KEY, query, Constant.AGENT_ID, agentId));
```

**观察要点**:
- 传递给Graph的初始状态Map
- CompiledGraph的配置信息
- AsyncGenerator的创建过程

### 2. Graph配置与编译断点

#### 位置3: Graph配置类
**文件**: `Nl2sqlConfiguration.java`  
**方法**: `nl2sqlGraph`  
**行号**: 约149行

```java
@Bean
public StateGraph nl2sqlGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
    
    // 🔴 断点3: Graph构建开始
    StateGraph stateGraph = new StateGraph(NL2SQL_GRAPH_NAME, keyStrategyFactory)
        // 自然语言理解阶段
        .addNode(QUERY_REWRITE_NODE, node_async(new QueryRewriteNode(nl2SqlService)))
        .addNode(KEYWORD_EXTRACT_NODE, node_async(new KeywordExtractNode(nl2SqlService)))
```

**观察要点**:
- 13个核心节点的注册过程
- 节点间边的连接关系
- KeyStrategyFactory的配置

#### 位置4: Graph编译
**文件**: `Nl2sqlForGraphController.java`  
**构造函数**: 约62行

```java
public Nl2sqlForGraphController(
    @Qualifier("nl2sqlGraph") StateGraph stateGraph,
    SimpleVectorStoreService simpleVectorStoreService, 
    DatasourceService datasourceService
) throws GraphStateException {
    
    // 🔴 断点4: Graph编译过程
    this.compiledGraph = stateGraph.compile();
    this.compiledGraph.setMaxIterations(100);
```

**观察要点**:
- StateGraph到CompiledGraph的编译过程
- 最大迭代次数设置
- 编译后的Graph结构

### 3. 核心节点执行断点

#### 位置5: 问题重写节点
**文件**: `QueryRewriteNode.java`  
**方法**: `execute`

```java
@Override
public Map<String, Object> execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点5: 问题重写节点执行
    String originalQuery = (String) state.value("input").orElse("");
    
    // 使用LLM重写查询
    String rewrittenQuery = chatClient.prompt()
        .system("将用户查询重写为更清晰、更具体的表达")
        .user(originalQuery)
        .call()
        .content();
```

**观察要点**:
- 输入状态中的原始查询
- LLM重写后的查询结果
- 节点执行时间

#### 位置6: 关键词提取节点
**文件**: `KeywordExtractNode.java`  
**方法**: `execute`

```java
@Override
public Map<String, Object> execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点6: 关键词提取节点
    String query = (String) state.value("rewritten_query").orElse("");
    List<String> evidences = (List<String>) state.value("evidences").orElse(new ArrayList<>());
    
    // 提取关键词
    List<String> keywords = nl2SqlService.extractKeywords(query, evidences);
```

**观察要点**:
- 重写后的查询内容
- 证据列表的内容
- 提取出的关键词列表

#### 位置7: Schema召回节点
**文件**: `SchemaRecallNode.java`  
**方法**: `execute`

```java
@Override
public Map<String, Object> execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点7: Schema召回节点
    String query = (String) state.value("rewritten_query").orElse("");
    List<String> keywords = (List<String>) state.value("keywords").orElse(new ArrayList<>());
    
    // 向量检索相关Schema
    List<Document> schemaDocuments = schemaService.recallSchema(query, keywords);
```

**观察要点**:
- 向量检索的查询条件
- 召回的Schema文档数量和内容
- 相似度分数

#### 位置8: SQL生成节点
**文件**: `SqlGenerateNode.java`  
**方法**: `execute`

```java
@Override
public Map<String, Object> execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点8: SQL生成节点
    String query = (String) state.value("rewritten_query").orElse("");
    String schema = (String) state.value("relevant_schema").orElse("");
    
    // 生成SQL
    String sql = chatClient.prompt()
        .system("你是一个专业的SQL专家，根据用户查询和数据库Schema生成准确的SQL语句")
        .user(buildSqlGenerationPrompt(query, schema))
        .call()
        .content();
```

**观察要点**:
- Schema信息的完整性
- SQL生成的Prompt内容
- 生成的SQL语句

#### 位置9: SQL执行节点
**文件**: `SqlExecuteNode.java`  
**方法**: `execute`

```java
@Override
public Map<String, Object> execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点9: SQL执行节点
    String sql = (String) state.value("generated_sql").orElse("");
    
    // 执行SQL
    ResultSetBO result = dbAccessor.query(new DbQueryParameter(sql));
```

**观察要点**:
- 待执行的SQL语句
- 数据库连接配置
- 查询结果集

### 4. 调度器断点

#### 位置10: 查询重写调度器
**文件**: `QueryRewriteDispatcher.java`  
**方法**: `execute`

```java
@Override
public String execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点10: 查询重写调度器
    String rewrittenQuery = (String) state.value("rewritten_query").orElse("");
    
    // 判断是否需要继续处理
    if (isValidQuery(rewrittenQuery)) {
        return "KEYWORD_EXTRACT_NODE";
    } else {
        return "END";
    }
```

**观察要点**:
- 调度决策的判断条件
- 下一个节点的选择逻辑
- 状态数据的完整性

#### 位置11: SQL生成调度器
**文件**: `SqlGenerateDispatcher.java`  
**方法**: `execute`

```java
@Override
public String execute(OverAllState state, RunnableConfig config) {
    
    // 🔴 断点11: SQL生成调度器
    String sql = (String) state.value("generated_sql").orElse("");
    Integer generateCount = (Integer) state.value("sql_generate_count").orElse(0);
    
    // 判断SQL质量和重试次数
    if (isValidSql(sql)) {
        return "SQL_EXECUTE_NODE";
    } else if (generateCount < MAX_RETRY_COUNT) {
        return "KEYWORD_EXTRACT_NODE";  // 重新生成
    } else {
        return "END";  // 超过重试次数
    }
```

**观察要点**:
- SQL验证逻辑
- 重试机制的触发条件
- 路由决策过程

### 5. 状态管理断点

#### 位置12: 状态更新
**文件**: `OverAllState.java`  
**方法**: `updateState`

```java
public Map<String, Object> updateState(Map<String, Object> updates) {
    
    // 🔴 断点12: 状态更新
    for (Map.Entry<String, Object> entry : updates.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        
        // 应用键策略
        KeyStrategy strategy = keyStrategyFactory.getStrategy(key);
        Object mergedValue = strategy.merge(data.get(key), value);
        data.put(key, mergedValue);
    }
```

**观察要点**:
- 状态键值对的变化
- 键策略的应用过程
- 状态合并逻辑

#### 位置13: 状态读取
**文件**: `OverAllState.java`  
**方法**: `value`

```java
public Optional<Object> value(String key) {
    
    // 🔴 断点13: 状态读取
    Object value = data.get(key);
    return Optional.ofNullable(value);
```

**观察要点**:
- 请求的状态键
- 返回的状态值
- 状态数据的完整性

### 6. 流式输出断点

#### 位置14: 流式输出处理
**文件**: `Nl2sqlForGraphController.java`  
**方法**: `streamSearch`  
**行号**: 约185行

```java
generator.forEachAsync(output -> {
    try {
        // 🔴 断点14: 流式输出处理
        logger.debug("Received output: {}", output.getClass().getSimpleName());
        
        if (output instanceof StreamingOutput) {
            StreamingOutput streamingOutput = (StreamingOutput) output;
            String chunk = streamingOutput.chunk();
            
            if (chunk != null && !chunk.trim().isEmpty()) {
                // 🔴 断点15: 数据块发送
                logger.debug("Emitting chunk: {}", chunk);
                ServerSentEvent<String> event = ServerSentEvent
                    .builder(JSON.toJSONString(chunk))
                    .build();
                sink.tryEmitNext(event);
```

**观察要点**:
- 输出对象的类型
- StreamingOutput的数据块内容
- SSE事件的构建过程

## 🛠️ 调试配置

### IDE断点设置

#### IntelliJ IDEA
1. 在指定行号左侧点击设置断点
2. 右键断点选择"More"配置条件断点
3. 设置日志断点记录变量值

#### 条件断点示例
```java
// 只在特定查询时触发
query.contains("销售") || query.contains("用户")

// 只在特定节点时触发
state.getCurrentNode().equals("SQL_GENERATE_NODE")

// 只在错误时触发
exception != null
```

### 日志配置

#### application.yml
```yaml
logging:
  level:
    com.alibaba.cloud.ai: DEBUG
    com.alibaba.cloud.ai.node: TRACE
    com.alibaba.cloud.ai.dispatcher: TRACE
    com.alibaba.cloud.ai.graph: DEBUG
```

#### 自定义日志
```java
// 在关键位置添加详细日志
logger.debug("Node: {}, State: {}, Input: {}", 
    nodeName, state.getData(), input);
    
logger.trace("State transition: {} -> {}", 
    currentNode, nextNode);
```

## 📊 调试数据收集

### 状态快照
```java
// 在每个节点执行前后记录状态
public void captureStateSnapshot(String nodeName, OverAllState state) {
    Map<String, Object> snapshot = new HashMap<>(state.getData());
    logger.info("State snapshot at {}: {}", nodeName, snapshot);
}
```

### 执行时间统计
```java
// 记录节点执行时间
long startTime = System.currentTimeMillis();
// 节点执行逻辑
long endTime = System.currentTimeMillis();
logger.info("Node {} execution time: {}ms", nodeName, endTime - startTime);
```

### 内存使用监控
```java
// 监控内存使用情况
Runtime runtime = Runtime.getRuntime();
long usedMemory = runtime.totalMemory() - runtime.freeMemory();
logger.debug("Memory usage at {}: {} MB", nodeName, usedMemory / 1024 / 1024);
```

## 🎯 调试场景

### 场景1: 追踪完整执行流程
**目标**: 观察从用户输入到最终结果的完整流程

**断点设置**:
- 位置1: 接口入口
- 位置5: 问题重写
- 位置6: 关键词提取
- 位置7: Schema召回
- 位置8: SQL生成
- 位置9: SQL执行
- 位置14: 流式输出

### 场景2: 调试SQL生成问题
**目标**: 分析SQL生成不准确的原因

**断点设置**:
- 位置7: Schema召回（检查Schema质量）
- 位置8: SQL生成（检查Prompt和结果）
- 位置11: SQL生成调度器（检查验证逻辑）

### 场景3: 性能分析
**目标**: 找出性能瓶颈

**断点设置**:
- 所有节点的入口和出口
- 添加时间统计代码
- 监控内存使用

### 场景4: 状态流转分析
**目标**: 理解状态在节点间的传递

**断点设置**:
- 位置12: 状态更新
- 位置13: 状态读取
- 所有调度器的决策点

## 🔧 调试技巧

### 1. 使用条件断点
```java
// 只在特定条件下暂停
if (query.contains("debug") && agentId.equals("1")) {
    // 断点会在这里触发
}
```

### 2. 表达式求值
在断点暂停时，使用IDE的表达式求值功能：
```java
// 查看状态内容
state.getData()

// 检查特定键值
state.value("generated_sql")

// 调用方法
nl2SqlService.extractKeywords(query, evidences)
```

### 3. 变量监视
添加关键变量到监视窗口：
- `state.getData()`
- `query`
- `agentId`
- `generatedSql`
- `executionResult`

### 4. 调用栈分析
观察方法调用栈，理解执行路径：
- Graph编译过程
- 节点执行顺序
- 调度器决策链

## 📝 调试检查清单

### 启动前检查
- [ ] 设置合适的日志级别
- [ ] 配置断点位置
- [ ] 准备测试查询
- [ ] 确认数据源配置

### 执行中观察
- [ ] 输入参数是否正确
- [ ] 状态传递是否完整
- [ ] 节点执行顺序是否符合预期
- [ ] 调度器决策是否合理
- [ ] 错误处理是否生效

### 结果分析
- [ ] 最终结果是否正确
- [ ] 执行时间是否合理
- [ ] 内存使用是否正常
- [ ] 日志信息是否完整

## 🚀 高级调试技巧

### 1. 自定义调试节点
```java
@Component
public class DebugNode implements NodeAction {
    
    @Override
    public Map<String, Object> execute(OverAllState state, RunnableConfig config) {
        // 输出完整状态信息
        logger.info("Debug Node - Current State: {}", state.getData());
        logger.info("Debug Node - Node History: {}", state.getNodeHistory());
        
        // 不修改状态，直接返回
        return Map.of();
    }
}
```

### 2. 状态变化监听器
```java
@Component
public class StateChangeListener {
    
    @EventListener
    public void onStateChange(StateChangeEvent event) {
        logger.info("State changed in node: {}, changes: {}", 
            event.getNodeName(), event.getChanges());
    }
}
```

### 3. 性能监控切面
```java
@Aspect
@Component
public class NodePerformanceAspect {
    
    @Around("execution(* com.alibaba.cloud.ai.node.*.execute(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        
        logger.info("Node {} executed in {}ms", 
            joinPoint.getTarget().getClass().getSimpleName(), 
            endTime - startTime);
            
        return result;
    }
}
```

通过这些断点和调试技巧，您可以深入理解Spring AI Alibaba NL2SQL项目中Graph工作流的执行机制，观察节点间的状态流转，并快速定位和解决问题。