# 第三章：Graph工作流原理

## 🎯 学习目标

- 理解Graph工作流的核心概念和设计思想
- 掌握StateGraph的基本原理和使用方法
- 了解异步执行和状态管理机制
- 为理解NL2SQL的工作流编排打下基础

## 📚 章节内容

### 3.1 什么是Graph工作流？

#### 传统工作流 vs Graph工作流

```
传统线性工作流：
A → B → C → D → 结束

Graph工作流：
     A
   ↙   ↘
  B     C
   ↘   ↙ ↘
     D   E
     ↓   ↓
    结束
```

#### 核心概念

**Graph工作流** 是一种基于图结构的工作流编排技术，它将复杂的业务流程建模为有向图，其中：

- **节点(Node)**：代表具体的处理步骤
- **边(Edge)**：代表数据流转和控制流
- **状态(State)**：在节点间传递的数据载体

#### 设计优势

1. **灵活性**：支持条件分支、并行执行、循环处理
2. **可靠性**：内置错误处理和恢复机制
3. **可观测性**：完整的执行链路追踪
4. **可扩展性**：模块化设计，易于扩展新节点

### 3.2 Spring AI Alibaba Graph架构

#### 核心组件架构

```
┌─────────────────────────────────────┐
│            应用层                    │
│      业务逻辑 + 工作流定义           │
├─────────────────────────────────────┤
│           Graph引擎层                │
│  StateGraph | CompiledGraph | ...  │
├─────────────────────────────────────┤
│           执行引擎层                 │
│ AsyncNodeGenerator | Dispatcher ... │
├─────────────────────────────────────┤
│           状态管理层                 │
│   OverAllState | KeyStrategy ...   │
└─────────────────────────────────────┘
```

#### 关键概念解析

1. **StateGraph（状态图）**
   - 工作流的设计蓝图
   - 定义节点、边和执行逻辑
   - 声明式编程模型

2. **CompiledGraph（编译图）**
   - StateGraph编译后的可执行版本
   - 实际的执行引擎
   - 负责运行工作流并管理状态转换

3. **OverAllState（全局状态）**
   - 工作流中的数据载体
   - 存储和传递所有节点间的数据
   - 支持状态合并和历史追踪

### 3.3 StateGraph详解

#### 3.3.1 基本结构

```java
/**
 * StateGraph基本结构示例
 */
public class BasicGraphExample {
    
    public StateGraph createSimpleGraph() {
        // 1. 创建状态图
        StateGraph graph = new StateGraph("simple-workflow", keyStrategyFactory)
            // 2. 添加节点
            .addNode("nodeA", new NodeA())
            .addNode("nodeB", new NodeB())
            .addNode("nodeC", new NodeC())
            
            // 3. 添加边（定义流转路径）
            .addEdge(StateGraph.START, "nodeA")
            .addEdge("nodeA", "nodeB")
            .addEdge("nodeB", "nodeC")
            .addEdge("nodeC", StateGraph.END);
            
        return graph;
    }
}
```

#### 3.3.2 节点实现

```java
/**
 * 自定义节点实现
 */
public class NodeA implements NodeAction {
    
    @Override
    public Map<String, Object> execute(
            OverAllState state, 
            RunnableConfig config) {
        
        // 1. 获取输入数据
        String input = (String) state.value("input").orElse("");
        
        // 2. 执行业务逻辑
        String result = processInput(input);
        
        // 3. 返回输出数据
        return Map.of("nodeA_output", result);
    }
    
    private String processInput(String input) {
        // 具体的业务处理逻辑
        return "Processed: " + input;
    }
}
```

#### 3.3.3 条件分支

```java
/**
 * 条件分支示例
 */
public class ConditionalGraphExample {
    
    public StateGraph createConditionalGraph() {
        return new StateGraph("conditional-workflow", keyStrategyFactory)
            .addNode("decision", new DecisionNode())
            .addNode("pathA", new PathANode())
            .addNode("pathB", new PathBNode())
            .addNode("merge", new MergeNode())
            
            // 起始边
            .addEdge(StateGraph.START, "decision")
            
            // 条件边
            .addConditionalEdges(
                "decision", 
                new DecisionDispatcher(),
                Map.of(
                    "PATH_A", "pathA",
                    "PATH_B", "pathB"
                )
            )
            
            // 汇聚边
            .addEdge("pathA", "merge")
            .addEdge("pathB", "merge")
            .addEdge("merge", StateGraph.END);
    }
}

/**
 * 决策调度器
 */
public class DecisionDispatcher implements EdgeAction {
    
    @Override
    public String execute(OverAllState state, RunnableConfig config) {
        // 根据状态数据决定下一步路径
        Integer value = (Integer) state.value("decision_value").orElse(0);
        return value > 50 ? "PATH_A" : "PATH_B";
    }
}
```

### 3.4 异步执行机制

#### 3.4.1 异步节点

```java
/**
 * 异步节点实现
 */
public class AsyncNodeExample {
    
    /**
     * 创建异步节点
     */
    public static AsyncNodeAction createAsyncNode() {
        return node_async(new BusinessLogicNode());
    }
    
    /**
     * 业务逻辑节点
     */
    static class BusinessLogicNode implements NodeAction {
        
        @Override
        public Map<String, Object> execute(
                OverAllState state, 
                RunnableConfig config) {
            
            // 模拟异步处理
            return CompletableFuture.supplyAsync(() -> {
                // 耗时的业务处理
                processBusinessLogic();
                return Map.of("result", "async_processed");
            }).join();
        }
        
        private void processBusinessLogic() {
            // 具体的业务逻辑
            try {
                Thread.sleep(1000); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

#### 3.4.2 流式处理

```java
/**
 * 流式处理示例
 */
@Service
public class StreamingGraphService {
    
    private final CompiledGraph compiledGraph;
    
    /**
     * 流式执行工作流
     */
    public Flux<OverAllState> streamExecute(OverAllState initialState) {
        return Flux.create(sink -> {
            try {
                // 异步执行工作流
                AsyncNodeGenerator generator = compiledGraph.stream(
                    initialState, 
                    RunnableConfig.builder().build()
                );
                
                // 监听状态变化
                generator.subscribe(
                    state -> sink.next(state),      // 状态更新
                    error -> sink.error(error),     // 错误处理
                    () -> sink.complete()           // 完成信号
                );
                
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
}
```

### 3.5 状态管理机制

#### 3.5.1 OverAllState详解

```java
/**
 * 状态管理示例
 */
public class StateManagementExample {
    
    /**
     * 状态操作示例
     */
    public void demonstrateStateOperations() {
        // 1. 创建初始状态
        OverAllState state = new OverAllState();
        
        // 2. 设置初始数据
        state.updateState(Map.of(
            "user_query", "查询上月销售数据",
            "timestamp", System.currentTimeMillis()
        ));
        
        // 3. 读取状态数据
        String query = (String) state.value("user_query").orElse("");
        
        // 4. 更新状态
        state.updateState(Map.of(
            "processed_query", "SELECT * FROM sales WHERE month = 'last_month'"
        ));
        
        // 5. 获取执行历史
        List<String> history = state.getNodeHistory();
        System.out.println("执行路径：" + String.join(" → ", history));
    }
}
```

#### 3.5.2 状态合并策略

```java
/**
 * 状态合并策略
 */
public class StateStrategyExample {
    
    /**
     * 创建键策略工厂
     */
    public KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            
            // 替换策略：新值覆盖旧值
            strategies.put("user_query", new ReplaceStrategy());
            strategies.put("sql_result", new ReplaceStrategy());
            
            // 追加策略：保留历史记录
            strategies.put("execution_log", new AppendStrategy());
            
            // 合并策略：合并对象属性
            strategies.put("metadata", new MergeStrategy());
            
            return strategies;
        };
    }
}

/**
 * 自定义追加策略
 */
public class AppendStrategy implements KeyStrategy {
    
    @Override
    public Object merge(Object existing, Object update) {
        if (existing instanceof List && update instanceof List) {
            List<Object> merged = new ArrayList<>((List<?>) existing);
            merged.addAll((List<?>) update);
            return merged;
        }
        return update;
    }
}
```

### 3.6 错误处理与恢复

#### 3.6.1 错误处理节点

```java
/**
 * 错误处理节点
 */
public class ErrorHandlingNode implements NodeAction {
    
    @Override
    public Map<String, Object> execute(
            OverAllState state, 
            RunnableConfig config) {
        
        try {
            // 尝试执行业务逻辑
            return executeBusinessLogic(state);
            
        } catch (BusinessException e) {
            // 业务异常处理
            return handleBusinessError(e, state);
            
        } catch (Exception e) {
            // 系统异常处理
            return handleSystemError(e, state);
        }
    }
    
    private Map<String, Object> executeBusinessLogic(OverAllState state) {
        // 具体业务逻辑
        return Map.of("success", true, "result", "processed");
    }
    
    private Map<String, Object> handleBusinessError(
            BusinessException e, OverAllState state) {
        return Map.of(
            "error", true,
            "error_type", "business",
            "error_message", e.getMessage(),
            "retry_count", getRetryCount(state) + 1
        );
    }
    
    private Map<String, Object> handleSystemError(
            Exception e, OverAllState state) {
        return Map.of(
            "error", true,
            "error_type", "system",
            "error_message", e.getMessage(),
            "need_manual_intervention", true
        );
    }
    
    private int getRetryCount(OverAllState state) {
        return (Integer) state.value("retry_count").orElse(0);
    }
}
```

#### 3.6.2 重试机制

```java
/**
 * 重试调度器
 */
public class RetryDispatcher implements EdgeAction {
    
    private static final int MAX_RETRY_COUNT = 3;
    
    @Override
    public String execute(OverAllState state, RunnableConfig config) {
        Boolean hasError = (Boolean) state.value("error").orElse(false);
        
        if (!hasError) {
            return "SUCCESS";
        }
        
        Integer retryCount = (Integer) state.value("retry_count").orElse(0);
        String errorType = (String) state.value("error_type").orElse("");
        
        // 系统错误不重试
        if ("system".equals(errorType)) {
            return "FAILED";
        }
        
        // 超过最大重试次数
        if (retryCount >= MAX_RETRY_COUNT) {
            return "FAILED";
        }
        
        // 继续重试
        return "RETRY";
    }
}
```

### 3.7 NL2SQL中的Graph应用

#### 3.7.1 NL2SQL工作流结构

```java
/**
 * NL2SQL Graph工作流示例
 */
public class NL2SQLGraphExample {
    
    public StateGraph createNL2SQLGraph() {
        return new StateGraph("nl2sql-workflow", createKeyStrategyFactory())
            // 问题理解阶段
            .addNode("query_rewrite", node_async(new QueryRewriteNode()))
            .addNode("keyword_extract", node_async(new KeywordExtractNode()))
            
            // Schema检索阶段
            .addNode("schema_recall", node_async(new SchemaRecallNode()))
            .addNode("table_relation", node_async(new TableRelationNode()))
            
            // SQL生成阶段
            .addNode("sql_generate", node_async(new SqlGenerateNode()))
            .addNode("sql_validate", node_async(new SqlValidateNode()))
            
            // 执行阶段
            .addNode("sql_execute", node_async(new SqlExecuteNode()))
            
            // 定义执行流程
            .addEdge(StateGraph.START, "query_rewrite")
            .addEdge("query_rewrite", "keyword_extract")
            .addEdge("keyword_extract", "schema_recall")
            .addEdge("schema_recall", "table_relation")
            .addEdge("table_relation", "sql_generate")
            
            // 条件分支：SQL验证
            .addConditionalEdges(
                "sql_generate",
                edge_async(new SqlValidationDispatcher()),
                Map.of(
                    "VALID", "sql_execute",
                    "INVALID", "sql_generate",  // 重新生成
                    "ERROR", StateGraph.END
                )
            )
            
            .addEdge("sql_execute", StateGraph.END);
    }
}
```

#### 3.7.2 关键节点实现

```java
/**
 * 查询重写节点
 */
public class QueryRewriteNode implements NodeAction {
    
    private final ChatClient chatClient;
    
    @Override
    public Map<String, Object> execute(
            OverAllState state, 
            RunnableConfig config) {
        
        // 获取用户原始查询
        String originalQuery = (String) state.value("user_query").orElse("");
        
        // 使用LLM重写查询
        String rewrittenQuery = chatClient.prompt()
            .system("将用户查询重写为更清晰、更具体的表达")
            .user(originalQuery)
            .call()
            .content();
        
        return Map.of(
            "rewritten_query", rewrittenQuery,
            "original_query", originalQuery
        );
    }
}

/**
 * SQL生成节点
 */
public class SqlGenerateNode implements NodeAction {
    
    private final ChatClient chatClient;
    
    @Override
    public Map<String, Object> execute(
            OverAllState state, 
            RunnableConfig config) {
        
        // 获取处理后的查询和Schema信息
        String query = (String) state.value("rewritten_query").orElse("");
        String schema = (String) state.value("relevant_schema").orElse("");
        
        // 构建SQL生成提示
        String prompt = buildSqlGenerationPrompt(query, schema);
        
        // 生成SQL
        String sql = chatClient.prompt()
            .system("你是一个专业的SQL专家，根据用户查询和数据库Schema生成准确的SQL语句")
            .user(prompt)
            .call()
            .content();
        
        return Map.of(
            "generated_sql", sql,
            "generation_timestamp", System.currentTimeMillis()
        );
    }
    
    private String buildSqlGenerationPrompt(String query, String schema) {
        return String.format(
            "用户查询：%s\n\n数据库Schema：\n%s\n\n请生成对应的SQL语句：",
            query, schema
        );
    }
}
```

### 3.8 Graph工作流最佳实践

#### 3.8.1 节点设计原则

1. **单一职责**：每个节点只负责一个明确的功能
2. **无状态设计**：节点不应保存内部状态
3. **幂等性**：相同输入应产生相同输出
4. **错误处理**：每个节点都应处理可能的异常

#### 3.8.2 性能优化

```java
/**
 * 性能优化示例
 */
@Component
public class OptimizedGraphService {
    
    /**
     * 并行执行多个独立节点
     */
    public StateGraph createParallelGraph() {
        return new StateGraph("parallel-workflow", keyStrategyFactory)
            .addNode("parallel_a", node_async(new ParallelNodeA()))
            .addNode("parallel_b", node_async(new ParallelNodeB()))
            .addNode("parallel_c", node_async(new ParallelNodeC()))
            .addNode("merge", node_async(new MergeNode()))
            
            // 并行执行
            .addEdge(StateGraph.START, "parallel_a")
            .addEdge(StateGraph.START, "parallel_b")
            .addEdge(StateGraph.START, "parallel_c")
            
            // 等待所有并行节点完成
            .addEdge("parallel_a", "merge")
            .addEdge("parallel_b", "merge")
            .addEdge("parallel_c", "merge")
            
            .addEdge("merge", StateGraph.END);
    }
}
```

#### 3.8.3 监控和调试

```java
/**
 * 监控节点
 */
public class MonitoringNode implements NodeAction {
    
    private final MeterRegistry meterRegistry;
    private final Logger logger;
    
    @Override
    public Map<String, Object> execute(
            OverAllState state, 
            RunnableConfig config) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // 执行业务逻辑
            Map<String, Object> result = executeBusinessLogic(state);
            
            // 记录成功指标
            meterRegistry.counter("node.execution.success").increment();
            
            return result;
            
        } catch (Exception e) {
            // 记录失败指标
            meterRegistry.counter("node.execution.failure").increment();
            logger.error("节点执行失败", e);
            throw e;
            
        } finally {
            // 记录执行时间
            sample.stop(Timer.builder("node.execution.time")
                .register(meterRegistry));
        }
    }
    
    private Map<String, Object> executeBusinessLogic(OverAllState state) {
        // 具体业务逻辑
        return Map.of("result", "processed");
    }
}
```

## 🎯 本章小结

通过本章学习，您应该已经：

✅ **理解了Graph工作流的核心概念和优势**
✅ **掌握了StateGraph的基本使用方法**
✅ **了解了异步执行和状态管理机制**
✅ **学会了错误处理和重试策略**
✅ **理解了Graph在NL2SQL中的应用**

## 🚀 下一步学习

接下来，我们将深入学习向量检索与RAG技术，这是NL2SQL中Schema理解的核心技术。

👉 [第四章：向量检索与RAG](../04-向量检索RAG/README.md)

## 📝 实践练习

### 练习1：简单工作流
创建一个包含3个节点的简单工作流，实现数据的顺序处理。

### 练习2：条件分支
实现一个带条件分支的工作流，根据输入数据选择不同的处理路径。

### 练习3：错误处理
为工作流添加错误处理和重试机制。

## 📚 延伸阅读

- [工作流引擎设计原理](https://martinfowler.com/articles/workflow-engines.html)
- [有向无环图(DAG)在工作流中的应用](https://en.wikipedia.org/wiki/Directed_acyclic_graph)
- [异步编程最佳实践](https://docs.oracle.com/javase/tutorial/essential/concurrency/)

---

**恭喜您完成第三章的学习！** 🎉