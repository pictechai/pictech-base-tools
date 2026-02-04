# pictech-base-tools
这是一个为您准备的 `README.md` 文件。它清晰地解释了项目结构、环境要求、配置方法以及如何运行这两个测试类。

```markdown
# PicTech API Java 客户端示例

本项目提供了一套轻量级的 Java 示例代码，用于演示如何对接和调用 PicTech 的图片处理 API（同步接口）。

示例涵盖了 **图片智能缩放 (Resize)**、**区域裁剪 (Crop)**、**高级水印合成 (Watermark)** 以及 **OCR 文字识别** 功能，并包含了完整的签名计算逻辑。

## 📋 项目特点

*   **零第三方依赖**：核心逻辑完全使用 Java 11+ 标准库 (`java.net.http`, `javax.crypto`) 编写，无需引入 Gson、Jackson 或 Apache HttpClient 即可运行。
*   **完整流程**：包含 参数排序 -> HMAC-SHA256 签名 -> JSON 构建 -> HTTP 请求 -> 结果解析（包括 Unicode 转中文）。
*   **场景丰富**：覆盖了基础图像处理、复杂水印合成及文字识别场景。

## 🛠 环境要求

*   **JDK 11** 或更高版本 (必须，因为使用了 `java.net.http.HttpClient`)。
*   **Maven** (用于构建项目，或者您可以直接在 IDE 中运行)。

## 📂 项目结构

```text
src/main/java/com/pictech/
├── PicTechApiTest.java       # [基础测试] 涵盖缩放、裁剪、基础水印及错误处理测试
├── PicTechWatermarkTest.java # [进阶测试] 专门用于测试所有水印模板效果及参数覆盖逻辑
└── PicTechOcrTest.java       # [OCR测试] 演示图片文字识别及结果解析
pom.xml                       # Maven 配置文件
README.md                     # 项目说明文档
```

## ⚙️ 配置说明

在运行代码之前，**必须**修改 Java 文件中的配置区域，填入您自己的 API 凭证。

请打开 `src/main/java/com/pictech/` 目录下的 `PicTechApiTest.java`、`PicTechWatermarkTest.java` 或 `PicTechOcrTest.java`，找到以下代码段并修改：

```java
// ================= 配置区域 =================
// ⚠️ 请务必替换为您自己的 AccountId 和 SecretKey
private static final String ACCOUNT_ID = "pic_YOUR_ID"; 
private static final String SECRET_KEY = "your_SECRET_KEY";
// ===========================================
```

> **注意**：`your_ACCOUNT_ID` 和 `your_SECRET_KEY` 请从 PicTech 开发者控制台获取。

## 🚀 如何运行

### 方法一：使用 IDE (IntelliJ IDEA / Eclipse) - 推荐
1.  将项目导入 IDE 作为 Maven 项目。
2.  找到对应的 Java 文件（如 `PicTechOcrTest.java`）。
3.  点击 `main` 方法旁边的运行按钮 (Run)。

### 方法二：使用 Maven 命令行
在项目根目录下（`pom.xml` 所在目录），执行以下命令：

**1. 运行基础功能测试 (PicTechApiTest):**
```bash
mvn clean compile exec:java -Dexec.mainClass="com.pictech.PicTechApiTest"
```

**2. 运行水印模板测试 (PicTechWatermarkTest):**
```bash
mvn clean compile exec:java -Dexec.mainClass="com.pictech.PicTechWatermarkTest"
```

**3. 运行 OCR 文字识别测试 (PicTechOcrTest):**
```bash
mvn clean compile exec:java -Dexec.mainClass="com.pictech.PicTechOcrTest"
```

## 🧪 测试用例说明

### 1. PicTechApiTest (基础功能)
运行后将在项目根目录生成一系列处理后的图片（如缩放、裁剪、基础水印等）：
*   `result_1_resize_50pct.jpg`: 按 0.5 比例缩放。
*   `result_2_watermark_pattern.jpg`: 全图斜向平铺防盗水印。
*   ... (以及其他裁剪和格式转换测试)

### 2. PicTechWatermarkTest (水印专项)
该测试会遍历系统支持的 10 种水印模板，生成如下文件：
*   `test_01_single_subtle_light.jpg`: 右下角通用版权
*   `test_07_pattern_diagonal_da.jpg`: 斜向防盗平铺
*   `test_09_special_confidential.jpg`: 绝密文件印章
*   ... (以及其他多种水印风格)

### 3. PicTechOcrTest (OCR 文字识别)
该测试不生成图片文件，而是将识别结果直接打印在控制台：
*   演示如何构建 JSON 请求体。
*   演示如何处理 API 返回的 Unicode 编码字符串。
*   **控制台输出**：格式化后的识别文字列表、RequestID 及原始 JSON 响应。

## 📝 注意事项

1.  **图片大小**：Base64 编码会增加约 33% 的数据体积，建议处理的源图不要过大（建议 10MB 以内），以免传输超时。
2.  **JSON 处理**：为了保持示例代码的独立性（Zero Dependency），代码中使用了简单的字符串拼接和解析来处理 JSON。**在生产环境中，强烈建议使用 `Jackson`、`Gson` 或 `Fastjson` 等成熟的 JSON 库。**
3.  **异常处理**：示例代码简单捕获了异常并打印堆栈，生产环境请根据业务需求进行完善的日志记录和重试机制。

---
© PicTech 2026
```