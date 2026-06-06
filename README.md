# Create Heads Up Display

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219-brightgreen)](https://neoforged.net/)
[![Create](https://img.shields.io/badge/Create-6.0.10-important)](https://modrinth.com/mod/create)

**Create Heads Up Display** 是 [Create](https://modrinth.com/mod/create) 模组的一个附属，允许玩家通过头戴显示器（头盔）接收来自显示连接器网络的数据，并在 HUD 上自定义显示布局。

## 功能特性

- 📺 **终端方块**：作为数据接收与配置中心，可连接任意显示连接器数据源。
- 🎮 **头戴显示器**：装备后根据绑定终端显示自定义 HUD，支持文本和进度条。
- 🎨 **高度自定义**：每个槽位可独立调整位置、缩放、旋转、颜色和透明度。
- 📝 **静态文本槽位**：不依赖数据源，直接添加自定义文本。
- 🔗 **绑定机制**：手持头盔右键点击终端即可绑定，HUD 自动显示对应数据。

## 依赖

- Minecraft 1.21.1
- NeoForge 21.1.219 或更高
- Create 6.0.10-280 或更高

## 安装

1. 确保已安装 NeoForge 和 Create 模组。
2. 将本模组的 JAR 文件放入 `.minecraft/mods` 文件夹。
3. 启动游戏。

## 使用指南

### 终端方块
- 放置终端，使用显示连接器（Display Link）连接到任意数据源（如应力表、物品观察器等）。
- 右键终端打开配置界面，可拖拽、缩放、旋转每个槽位，并调整颜色/透明度。
- 配置自动按玩家 UUID 保存。

### 头戴显示器
- 合成头盔，右键终端绑定。
- 装备后，HUD 上会显示已配置的槽位数据。
- 头盔支持原版盔甲拖拽装备（实现 `Equipable` 接口）。

### 静态文本
- 在终端配置界面中，可添加自定义文本作为独立槽位。

## 配置与同步

- 服务端终端实体存储所有槽位配置和最新数据。
- 通过 `CustomPayload` 同步给所有佩戴头盔且绑定了该终端的客户端。

## 开发状态

- 目前这个模组属于早期开发状态，并没有完成全部内容
- 这个介绍界面我懒得经常更新，所以有些新内容不在这里

## 致谢

- [Create 团队](https://github.com/Creators-of-Create) 提供的强大 API 和示例，及其部分纹理资源。
- [Create-Radar-Sable](https://github.com/DXENDBucket/Create-Radar-Sable) 提供的显示器方块模型与贴图资源。
- 所有测试者与贡献者。

## 许可证

MIT © 2026 HAOYIYU994
