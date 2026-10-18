# 神秘动力学（Mystery Dynamics）

为「神秘时代」移植版添加联动 **机械动力（Create）** 的设备。NeoForge 1.21.1 / Create 6.0.x。

## 当前内容

**傀儡动力核心**：把傀儡装进机壳的应力源，32 RPM / 512 SU，正面朝向放置者，输出轴与正面垂直。

| 项目 | 数值 |
|---|---|
| 额定转速 | **32 RPM** |
| 应力容量 | **512 SU**（按每 RPM 登记：16 SU/RPM × 32 RPM） |
| 应力消耗 | 0（纯应力源） |
| 输出端 | `facing=north/south` 时为东、西两面；`facing=east/west` 时为南、北两面 |

## 操作

| 操作 | 效果 |
|---|---|
| 扳手右键**顶面/底面** | 旋转整机朝向 90°，应力输出端跟着转到另外两面 |
| 扳手右键**侧面** | 翻转输出转向（顺时针 ⇄ 逆时针） |
| **空手**右键顶面/底面 | 同样翻转输出转向，动作栏提示当前方向 |
| 潜行 + 扳手 | 拆除 |

## 下载

见仓库的 [Releases](../../releases) 页面。

## 构建

需要 JDK 21，以及 Create 6.0.10 的 4 个编译依赖（放在 `libs/`，见 [libs/README.md](libs/README.md)）。

```powershell
.\gradlew.bat build
```

产物：`build/libs/神秘动力学-<version>.jar`。

> 如果访问不了 Mojang / NeoForge 官方源，`gradle/mojang-mirror.gradle` 会把仓库切到镜像；网络正常时可以删掉 `build.gradle` 里的 `apply from` 一行。

## 项目结构

```
src/main/java/com/mysterydynamics/
  MysteryDynamics.java              模组入口、方块/物品/方块实体/创造标签注册、应力值注册
  block/GolemDynamoBlock.java       朝向、旋转轴、输出面、扳手交互
  block/GolemDynamoBlockEntity.java 32 RPM / 16 SU per RPM 的应力源与转向翻转
src/main/resources/
  assets/mystery_dynamics/...       blockstate、方块模型、物品模型、lang、贴图
  data/mystery_dynamics/...         掉落表
  data/minecraft/tags/block/mineable/pickaxe.json
model/
  golem_body.bbmodel                Blockbench 源文件
  textures/                         8 种傀儡材质 + 安山机壳贴图
tools/
  convert_model.py                  .bbmodel -> 方块模型 JSON（inflate、UV/8、display）
  preview_block.py                  软件渲染预览
  rcon_test.py                      开发服务器 RCON 冒烟测试
```

## 已做的验证

开发服务器 + RCON 实测：

- 放置后 NBT：`Speed: 32.0f`、`Network: {Capacity: 512.0f, AddedCapacity: 16.0f}`；
- 东侧放 `create:shaft[axis=x]`：轴获得 `Speed: 32.0f`、`Source` 指向核心，网络 `Size: 2`；
- `facing=east` 时南侧放 `create:shaft[axis=z]` 同样连通 —— 输出轴确实垂直于正面。

## 说明

- 模型转换会读取 bbmodel 里元素的 `inflate`（负值内缩）和 `display`（手持/GUI 变换），这两项如果漏掉会出现“傀儡撑满机壳 / 手持显示过大”的问题。
- 目前没有为输出轴注册旋转渲染（BER），模型本身是静态方块；相邻的 Create 传动轴会自己渲染到方块表面。
- 配方暂未添加。
