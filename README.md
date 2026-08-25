# 敏捷项目管理系统

一个面向团队协作的敏捷项目管理系统，提供产品、项目、迭代、任务、缺陷、测试用例和系统管理等功能。

## 功能概览

- 工作台：查看项目进度、待办任务和团队动态
- 产品管理：维护产品、需求、计划和版本
- 项目管理：管理项目、迭代、任务和项目成员
- 质量中心：管理缺陷、测试用例、测试套件和测试执行
- 统计报表：查看项目数据和迭代燃尽趋势
- 系统管理：管理用户、角色、权限、部门、操作日志和系统配置

## 技术栈

### 后端

- Java 17+
- Spring Boot 3.5
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- JWT

### 前端

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

## 项目结构

```text
backend/     Spring Boot 后端源代码和数据库脚本
frontend/    Vue 3 前端源代码
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- Redis 6+

## 配置说明

后端本地环境变量示例位于 `backend/.env.example`。启动前请根据本地环境配置数据库账号、数据库密码和 JWT 密钥。

JWT 密钥建议使用不少于 32 个字符的随机字符串，不要将真实生产密钥提交到代码仓库。

## 运行项目

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8081/api`

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 数据库初始化

请先创建名为 `pms` 的 MySQL 数据库，再按顺序执行 `backend/sql/` 目录中的数据库脚本。

## 账号说明

系统初始账号由后端初始化逻辑创建。实际账号和密码以本地数据库初始化结果为准，请在首次使用后及时修改密码。

## 许可证

本项目仅用于学习和毕业设计用途。