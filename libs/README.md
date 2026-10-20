# libs —— 编译期第三方依赖（不提交）

本工程把机械动力（Create）当作 **compileOnly** 依赖，不会打进产物；构建前需要自行准备以下 5 个 jar 放进本目录：

| 文件 | 来源 |
|---|---|
| `create-6.0.10.jar` | Create 6.0.10（1.21.1 / NeoForge） |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | Create 的 `META-INF/jarjar/` 内 |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | 同上 |
| `Registrate-MC1.21-1.3.0+67.jar` | 同上 |
| `thaumcraft-0.2.2.95-port.265.jar` | 神秘时代移植版（仅用于奥术工作台配方编译；可选） |

拿到 Create 的 mod jar 后，可以用压缩软件直接抽出 `META-INF/jarjar/` 下的三个嵌套 jar。

神秘时代 jar 只用于编译 `ThaumcraftRecipes`；运行时未安装神秘时代也不会加载该类。

这些小文件被 `.gitignore` 排除，原因：体积大、且属于第三方分发物。
