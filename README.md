# 企业协同日历 H5 系统

本项目面向 PC Web 与移动端 H5，包含用户端日历能力、管理端维护能力、Spring Boot API 服务、PostgreSQL 数据库脚本和单机容器化部署配置。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus
- 后端：Spring Boot 3、JDK 17
- 数据库：PostgreSQL
- 部署：Nginx、Spring Boot、PostgreSQL、服务器附件目录

## 目录

```text
backend/    后端 API 服务
frontend/   前端用户端与管理端
database/   PostgreSQL 表结构与初始化数据
deploy/     容器化部署配置
docs/       系统设计、功能架构文档、编译验证记录
需求清单/   产品需求文档
设计稿/     参考界面
```

## 本期边界

本期不包含登录认证、消息通知、会议室预约、外部联系人、第三方日历同步和完整权限体系。系统通过当前用户上下文支撑多用户视角，全员日历共享成员用于该日历自身的协同管理与编辑。

## 文档

- [功能架构文档](docs/functional-architecture.md) — 完整的功能架构、API 清单、数据库设计、部署架构
- [系统设计文档](docs/system_design.md) — 上一版本的系统架构设计
- [PRD 最小可用版本](docs/PRD-minimum-viable-version.md) — 产品需求文档

## 本地启动

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 http://localhost:3007

容器化：

```bash
cd deploy
docker compose up -d --build
```
