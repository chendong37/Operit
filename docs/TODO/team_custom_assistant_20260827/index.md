---
title: Team Custom Assistant
status: source-ready-build-blocked
fork: pending-user-repository
branch: feat/team-custom-assistant
---

# 团队专属助手

## 原状

- Operit 已提供 Android Agent、角色卡、记忆、Skill、MCP、ToolPkg、工作流和手机自动化能力。
- 上游应用面向通用用户，没有用户团队的领域角色、记忆边界、权限档位和品牌配置。
- 团队手机的系统版本与授权条件不同，不能把 Root 作为基础运行条件。

## 意图

- 建立一套团队内部使用的专属助手，并保持角色、Skill、记忆规则和 App 品牌层相互独立。
- 以普通 Android 权限与无障碍作为基础档位，把 Shizuku 建模为用户主动启用的增强能力。
- 助手包与独立品牌 App 使用同一套版本化定义。

## 已确定方案

- 正式名称：知行 AI
- 应用标识：`com.zhixing.ai`
- 首批模型：DeepSeek API，推荐模型 `deepseek-v4-flash`
- 首台设备：小米 / 澎湃OS
- 权限基线：普通 Android 权限 + 无障碍；Shizuku 仅作可选增强
- Root 策略：不要求、不配置，不作为团队运行或验收条件
- 分发范围：本人和团队内部

## 预期结果

- 普通权限与无障碍环境可以完成对话、知识库、研发协作、教学资料和允许范围内的手机自动化。
- Shizuku 获得明确授权后，只开放与该能力对应的工具。
- 角色、Skill、记忆规则可导入、导出和版本化，官方应用与独立 App 使用同一份定义。
- 独立 App 使用单独的应用标识、名称、图标、签名和团队内部更新通道。

## 作用域

- 显式权限能力模型与授权引导
- 一个中枢角色和领域专员角色
- 团队宪章、任务路由、记忆策略、安全执行和证据门禁 Skill
- 软件研发、教学资料、手机操作和个人知识库首批工作流
- 独立品牌配置、内部构建配置和安装迁移说明

## 非目标

- 首版不公开上架应用商店。
- 首版不允许自动付款、自动发送对外消息、生产数据库写入或无人确认的系统级操作。
- 首版不把 API Key、Token、学生信息或客户隐私写入角色卡和记忆模板。
- 首版不要求团队设备具备 Root。

## 步骤

1. [权限能力档位](01_capability_profiles.md)
2. [助手包与记忆边界](02_assistant_bundle.md)
3. [独立品牌与内部发布](03_brand_and_distribution.md)
4. [验证与交付](04_verification_and_delivery.md)

## 当前状态

- 品牌、独立应用标识、公共目录、端口、对外 Intent、快捷方式和更新入口已完成源码隔离。
- 助手包 0.2.0、六张角色卡、五个 Skill 和澎湃OS配置说明已完成。
- 当前执行环境缺少 Android SDK/NDK、完整原生依赖、JDK 21 与团队签名材料，因此不能在这里生成或安装 APK。
- 下一步需要在用户的 GitHub fork CI 或具备依赖的 Android 构建机执行 `assembleInternal`，再在真机完成验收。
