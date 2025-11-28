# Minecraft Kubernetes 管理服务

基于 YAML 模板的 Spring Boot 服务，用于管理 Kubernetes 上的 Minecraft 服务器。

## 项目结构

```
├── domain/model/              # 领域模型（充血）
│   └── MinecraftServer.java  # 包含业务逻辑的聚合根
├── service/                   # 服务层
│   ├── MinecraftServerService.java  # 业务服务
│   └── K8sCommandExecutor.java      # kubectl 命令执行
├── dto/                       # 数据传输对象
│   ├── CreateServerRequest.java
│   └── UpdateServerRequest.java
├── infrastructure/exception/  # 异常处理
│   └── GlobalExceptionHandler.java
├── controller/                # 控制器
│   └── MinecraftServerController.java
└── resources/
    └── k8s-template.yaml      # K8s YAML 模板
```

## 核心特点

✅ **YAML 模板**：直接使用你提供的 YAML 作为模板，参数替换即可  
✅ **充血模型**：`MinecraftServer` 包含业务方法（scaleReplicas、updateMaxPlayers、updateMemory）  
✅ **kubectl 命令**：通过 `kubectl apply -f -` 直接应用配置  
✅ **MySQL 持久化**：配置保存到数据库，支持查询和管理  
✅ **自动分配**：端口号自动递增（31001-32000），命名空间为 minecraft{端口号}（如 minecraft31001）  
✅ **简单直接**：无需复杂的 K8s Java 客户端  

## 快速开始

### 开发环境（H2 内存数据库）
```bash
# 方式 1：使用 Maven
mvn spring-boot:run

# 方式 2：打包后运行
mvn clean package
java -jar target/gamesmanager-1.0.0.jar
```

### 快速测试
```bash
# Linux/Mac
chmod +x test-api.sh
./test-api.sh

# Windows
test-api.bat
```

### 生产环境（MySQL）
```bash
# 1. 创建 MySQL 数据库
mysql -u root -p
CREATE DATABASE minecraft_manager;

# 2. 启动应用（使用 prod 配置）
java -jar target/k8s-manager-1.0.0.jar --spring.profiles.active=prod
```

## API 示例

### 创建服务器（配置分为 K8s 和 Minecraft 两部分）
```bash
curl -X POST http://localhost:8080/api/minecraft/servers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-server",
    "k8sConfig": {
      "memoryLimit": "20Gi",
      "cpuLimit": "4",
      "storageSize": "20Gi"
    },
    "minecraftConfig": {
      "serverType": "PAPER",
      "maxPlayers": 1000,
      "onlineMode": false,
      "jvmOptions": "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200",
      "version": "latest",
      "difficulty": "normal",
      "pvp": true,
      "viewDistance": 10
    }
  }'
```

系统会自动：
- 分配端口号（31001, 31002...）
- 生成命名空间（minecraft31001）
- 设置默认值（replicas=1, storageClassName=longhorn）
- 计算初始资源（内存/CPU 请求为限制的 25%）
- 计算 JVM 内存（为内存限制的 80%）
- 配置以 JSON 格式存储到数据库

**用户只需配置：**
- 资源上限（内存、CPU、存储）
- Minecraft 游戏配置（玩家数、难度等）

**系统自动配置：**
- 副本数（replicas）
- 存储类名（storageClassName）
- 端口号（nodePort）
- 命名空间（namespace）

### 更新服务器（基于 namespace，只能更新资源上限和游戏配置）
```bash
# 使用 namespace 更新服务器（例如：mc-30001）
curl -X PUT http://localhost:8080/api/minecraft/servers/mc-30001 \
  -H "Content-Type: application/json" \
  -d '{
    "k8sConfig": {
      "memoryLimit": "32Gi",
      "cpuLimit": "8",
      "storageSize": "40Gi"
    },
    "minecraftConfig": {
      "maxPlayers": 2000,
      "difficulty": "hard",
      "pvp": false
    }
  }'
```

**注意：** 
- 接口操作基于 **namespace**，因为每个 namespace 代表一个唯一的游戏服务器
- replicas 和 storageClassName 不允许用户修改，由系统管理

### 查询/删除（基于 namespace）
```bash
# 查询单个服务器（使用 namespace）
curl http://localhost:8080/api/minecraft/servers/mc-30001

# 列出所有服务器
curl http://localhost:8080/api/minecraft/servers

# 删除服务器（使用 namespace）
curl -X DELETE http://localhost:8080/api/minecraft/servers/mc-30001
```

## 工作原理

1. 用户提交创建请求（只需提供服务器名称）
2. 系统自动分配端口号（从 31001 开始递增）
3. 根据端口号生成命名空间（minecraft31001、minecraft31002...）
4. 保存配置到 MySQL 数据库
5. 读取 `k8s-template.yaml` 模板并替换参数
6. 通过 `kubectl apply -f -` 应用到 K8s 集群
7. 更新数据库状态（CREATING → RUNNING）

## 命名规则

- **命名空间**：`mc-{端口号}`（如 mc-30001）- **作为服务器的唯一标识**
- **服务器名称**：用户自定义（如 my-server）
- **端口号**：自动分配（30001-32000）

## API 设计说明

所有的查询、更新、删除操作都基于 **namespace** 而不是服务器名称，原因：
- 每个 namespace 在 Kubernetes 中是唯一的，代表一个独立的游戏服务器实例
- namespace 与端口号一一对应，便于管理和定位
- 符合 Kubernetes 的资源隔离设计理念

## 服务器类型说明

支持的 Minecraft 服务器类型：
- **PAPER**（推荐）：最稳定、性能最好的服务器，适合大多数场景
- **FOLIA**：实验性多线程服务器，可能存在稳定性问题，仅适合高并发场景
- **VANILLA**：原版服务器
- **SPIGOT**：经典插件服务器
- **PURPUR**：Paper 的增强版

**默认使用 PAPER**，如需使用其他类型，在创建时指定 `serverType` 参数。

## 依赖

- Java 17+
- Maven 3.6+
- Kubernetes 集群
- kubectl 命令行工具（已配置好）

## 数据库

### 开发环境（默认）
使用 H2 内存数据库，无需安装配置：
- 访问 H2 控制台：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:minecraft_manager`
- 用户名: `sa`
- 密码: （留空）

**注意：** H2 是内存数据库，重启后数据会丢失。

### 生产环境
使用 MySQL 数据库，需要：
1. 安装 MySQL 并创建数据库 `minecraft_manager`
2. 修改 `pom.xml` 添加 MySQL 依赖
3. 使用 `--spring.profiles.active=prod` 启动

详见 [TESTING.md](TESTING.md)
