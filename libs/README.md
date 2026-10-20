# libs —— 编译期第三方依赖（不提交、不发布）

本工程只发布「神秘动力学」本体。下列前置 jar 仅用于编译，不会进入构建产物、仓库或 Release：

| 文件 | 来源 |
|---|---|
| `create-6.0.10.jar` | Create 6.0.10（1.21.1 / NeoForge） |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | Create 的 `META-INF/jarjar/` 内 |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | 同上 |
| `Registrate-MC1.21-1.3.0+67.jar` | 同上 |
| `thaumcraft-0.2.2.95-port.265.jar` | 神秘时代移植版（必需前置，用于奥术工作台配方） |

## 放置位置

- 默认读取项目的 `libs/`；
- 也可以放在项目外，用 `-PdepsDir=<路径>` 指定；
- 本机放在 `../.tools/deps`（`build.gradle` 会优先找这个目录，找不到再退回 `libs/`）。

`libs/*.jar` 已被 `.gitignore` 排除；仓库和 Release 里只有「神秘动力学」自己的 jar。