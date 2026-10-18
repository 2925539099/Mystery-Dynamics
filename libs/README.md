# libs —— 编译期第三方依赖（不提交）

本工程把机械动力（Create）当作 **compileOnly** 依赖，不会打进产物；构建前需要自行准备以下 4 个 jar 放进本目录：

| 文件 | 来源 |
|---|---|
| `create-6.0.10.jar` | Create 6.0.10（1.21.1 / NeoForge） |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | Create 的 `META-INF/jarjar/` 内 |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | 同上 |
| `Registrate-MC1.21-1.3.0+67.jar` | 同上 |

拿到 Create 的 mod jar 后，可以用压缩软件直接抽出 `META-INF/jarjar/` 下的三个嵌套 jar。

这些小文件被 `.gitignore` 排除，原因：体积大、且属于第三方分发物。
