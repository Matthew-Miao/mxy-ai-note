# 渐进式学习助手 (MXY Learning Agent)

基于SpringAI Alibaba Graph的智能学习系统，采用DDD架构模式构建。

## 项目概述

本项目是一个渐进式学习助手系统，利用SpringAI Alibaba Graph框架实现智能化的学习流程编排，为学习者提供个性化的学习体验。

### 技术栈

- **框架**: Spring Boot 3.2.0
- **AI引擎**: SpringAI Alibaba Graph 1.0.0.3-SNAPSHOT
- **数据库**: MySQL 8.0+ 
- **ORM**: MyBatis Plus 3.5.4
- **构建工具**: Maven
- **JDK版本**: 17+

## 架构设计

### DDD分层架构

本项目严格按照领域驱动设计(DDD)架构模式进行组织，包含以下分层：

```
src/main/java/com/mxy/platform/ai/learning/agent/
├── trigger/           # 触发器层（用户接口层）
│   ├── http/          # HTTP接口
│   └── websocket/     # WebSocket接口
├── validator/         # 校验层（参数校验）
│   ├── chain/         # 责任链管理
│   ├── rule/          # 校验规则定义
│   ├── impl/          # 具体校验器实现
│   └── annotation/    # 自定义校验注解
├── converter/         # 数据转换层（DTO转换）
│   ├── dto/           # DTO转换器
│   ├── entity/        # Entity转换器
│   ├── page/          # 分页转换器
│   └── factory/       # 转换器工厂
├── application/       # 应用服务层（编排层）
│   ├── service/       # 应用服务接口
│   └── impl/          # 应用服务实现
├── domain/           # 领域层（核心业务逻辑）
│   ├── learning/      # 学习领域
│   │   ├── model/     # 领域模型
│   │   │   ├── entity/    # 实体类
│   │   │   ├── aggregate/ # 聚合根
│   │   │   └── vo/        # 值对象
│   │   ├── service/   # 领域服务
│   │   └── adapter/   # 适配器接口
│   ├── graph/         # Graph工作流领域
│   │   ├── node/      # Graph节点
│   │   ├── config/    # Graph配置
│   │   └── service/   # Graph服务
│   └── shared/        # 共享领域
├── infrastructure/   # 基础设施层（技术实现）
│   ├── adapter/       # 适配器实现
│   │   ├── repository/    # 仓储实现
│   │   └── sdk/          # 外部服务适配器
│   ├── dao/           # 数据访问对象
│   │   ├── mapper/    # MyBatis Mapper
│   │   └── po/        # 持久化对象
│   ├── redis/         # 缓存服务
│   └── sdk/           # 第三方SDK封装
├── types/            # 通用类型定义
│   ├── enums/        # 枚举定义
│   ├── constants/    # 常量定义
│   ├── exception/    # 异常定义
│   └── utils/        # 工具类
└── config/           # 配置层
    ├── GraphConfiguration.java      # Graph配置
    ├── MyBatisPlusConfiguration.java # MyBatis Plus配置
    ├── WebSocketConfiguration.java  # WebSocket配置
    └── SwaggerConfiguration.java    # Swagger配置
```

### 分层调用关系

```
Trigger Layer (触发器层)
    ↓
Validator Layer (校验层) ← 新增：入参校验
    ↓
Converter Layer (数据转换层) ← 新增：DTO ↔ Entity 转换
    ↓
Application Layer (应用服务层)
    ↓
Domain Layer (领域层)
    ↓
Converter Layer (数据转换层) ← 新增：Entity ↔ PO 转换
    ↓
Infrastructure Layer (基础设施层)
```

## 核心功能模块

### 1. 学习流程管理
- 个性化学习路径规划
- 学习进度跟踪
- 自适应难度调整

### 2. Graph工作流引擎
- 基于SpringAI Alibaba Graph的流程编排
- 支持复杂的学习决策逻辑
- 状态管理和检查点机制

### 3. 智能内容生成
- 基于LLM的个性化内容生成
- 多模态学习资源支持
- 实时内容优化

### 4. 多维度评估
- 知识掌握度评估
- 学习行为分析
- 情感状态监测

## 开发规范

### 命名规范

| 层次 | 类型 | 命名规范 | 示例 |
|------|------|----------|------|
| Trigger | Controller | `XxxController` | `LearningController` |
| Trigger | DTO | `XxxRequestDTO`/`XxxResponseDTO` | `StartLearningRequestDTO` |
| Validator | 校验器接口 | `IXxxValidator` | `ILearningRequestValidator` |
| Validator | 校验器实现 | `XxxValidator` | `LearningRequestValidator` |
| Converter | DTO转换器 | `XxxDtoConverter` | `LearningDtoConverter` |
| Application | 应用服务接口 | `IXxxServiceCase` | `ILearningServiceCase` |
| Application | 应用服务实现 | `XxxServiceCaseImpl` | `LearningServiceCaseImpl` |
| Domain | 实体 | `XxxEntity` | `LearningSessionEntity` |
| Domain | 领域服务接口 | `IXxxService` | `ILearningService` |
| Domain | 领域服务实现 | `XxxService` | `LearningService` |
| Infrastructure | 仓储实现 | `XxxRepository` | `LearningRepository` |
| Infrastructure | PO | `XxxPO` | `LearningSessionPO` |
| Infrastructure | Mapper | `XxxMapper` | `LearningSessionMapper` |

### 注释规范

```java
/**
 * 类功能描述
 *
 * @author 作者名
 * @date 创建日期
 */
public class ExampleClass {
    
    /**
     * 方法功能描述
     *
     * @param paramName 参数描述
     * @return 返回值描述
     */
    public String exampleMethod(String paramName) {
        // 实现逻辑
        return "result";
    }
}
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis (可选)

### 配置步骤

1. **数据库配置**
   ```bash
   # 创建数据库
   mysql -u root -p
   CREATE DATABASE learning_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **环境变量配置**
   ```bash
   # Windows
   set DASHSCOPE_API_KEY=your-dashscope-api-key
   set DB_USERNAME=root
   set DB_PASSWORD=your-mysql-password
   
   # Linux/Mac
   export DASHSCOPE_API_KEY=your-dashscope-api-key
   export DB_USERNAME=root
   export DB_PASSWORD=your-mysql-password
   ```

3. **启动应用**
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

4. **访问应用**
   - 应用地址: http://localhost:8080
   - API文档: http://localhost:8080/swagger-ui.html
   - 健康检查: http://localhost:8080/actuator/health

## API接口

### 学习管理接口

- `POST /api/learning/start` - 开始学习会话
- `POST /api/learning/answer` - 提交学习答案
- `GET /api/learning/progress/{learnerId}` - 获取学习进度
- `GET /api/learning/topics` - 获取可用主题列表

### WebSocket接口

- `/ws/learning` - 实时学习交互

## 扩展指南

### 添加新的学习节点

1. 在 `domain/graph/node/` 下创建新的节点类
2. 实现 `NodeAction` 接口
3. 在 `GraphConfiguration` 中注册节点

### 添加新的校验规则

1. 在 `validator/rule/` 下定义校验规则
2. 在 `validator/impl/` 下实现校验器
3. 在校验链中注册新的校验器

### 添加新的数据转换器

1. 在 `converter/dto/` 或 `converter/entity/` 下创建转换器
2. 实现相应的转换接口
3. 在需要的地方注入使用

## 监控和运维

### 日志配置

- 日志文件位置: `logs/learning-agent.log`
- 日志级别: DEBUG (开发环境)
- 日志轮转: 100MB/文件，保留30天

### 监控端点

- `/actuator/health` - 健康检查
- `/actuator/info` - 应用信息
- `/actuator/metrics` - 应用指标
- `/actuator/prometheus` - Prometheus指标

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目维护者: MXY Team
- 邮箱: support@mxy.com
- 项目地址: [GitHub Repository](https://github.com/mxy/learning-agent)

---

**注意**: 这是一个代码框架模板，具体的业务逻辑实现需要根据实际需求进行开发。