package com.pictech;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class PicTechWatermarkTest {

    // ================= 配置区域 =================
    private static final String API_HOST = "https://www.pictech.top/pictech/commonapi";
    // 测试图片 URL
    private static final String TEST_IMAGE_URL = "https://pictech.top/pctccloud/pictechcc-translate/1769184000000/atlas50696a9ea89e4456898a0c6d3393792f/581f3966ef924b13b1c486d6d4d00197_source_Chinese.webp";

    // 请替换为你的真实 ID 和 Key
    private static final String ACCOUNT_ID = "your_ACCOUNT_ID";
    private static final String SECRET_KEY = "your_SECRET_KEY";
    // ===========================================
    // 全局 HTTP 客户端
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        // 1. 执行批量模板测试
        testAllTemplates();

        // 2. 执行覆盖参数测试 (如需测试请取消注释)
        // testMultipleOverride();
    }

    /**
     * 定义所有测试用例并批量执行
     */
    public static void testAllTemplates() {
        List<TestCase> testCases = new ArrayList<>();
        testCases.add(new TestCase("single_subtle_light", "右下角 (白字半透明)", "PicTech © 2026"));
        testCases.add(new TestCase("special_multiline", "左下角 (多行文字)", "内部资料\n仅供参考"));
        testCases.add(new TestCase("single_strong_red", "中心 (红色警示)", "DANGER"));
        testCases.add(new TestCase("multiple_corners", "四角 (多点水印)", "UID: 9527"));
        testCases.add(new TestCase("multiple_edges", "四边 (边缘居中)", "Edge Mark"));
        testCases.add(new TestCase("pattern_subtle", "平铺 (正向排列)", "PicTech"));
        testCases.add(new TestCase("pattern_diagonal_da", "平铺 (斜向防盗图)", "禁止盗图 @PicTech"));
        testCases.add(new TestCase("special_photography", "摄影参数 (等宽字体)", "ISO 200  f/1.8  1/500s"));
        testCases.add(new TestCase("special_confidential", "绝密文件 (默认文字)", null)); // null 表示使用默认
        testCases.add(new TestCase("special_approved", "审核通过 (绿色印章)", null));

        System.out.println("🚀 开始批量测试 10 个水印模板...");
        System.out.println("API: " + API_HOST);

        for (int i = 0; i < testCases.size(); i++) {
            TestCase output = testCases.get(i);
            runSingleTest(i + 1, output.key, output.text, output.desc);

            // 稍微暂停一下，避免请求太快
            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    /**
     * 执行单个水印测试
     */
    public static void runSingleTest(int caseIndex, String templateKey, String textContent, String description) {
        System.out.printf("\n--- Case %d: %s [%s] ---%n", caseIndex, description, templateKey);
        String url = API_HOST + "/tools/watermark";

        // 使用 TreeMap 自动按 Key 排序，方便后续签名（虽然生成签名时会再次处理，但保持有序是个好习惯）
        Map<String, Object> params = new TreeMap<>();
        params.put("AccountId", ACCOUNT_ID);
        params.put("Image", TEST_IMAGE_URL);
        params.put("TemplateKey", templateKey);
        params.put("OutputFormat", "JPEG");
        params.put("Quality", 90);
        params.put("Timestamp", String.valueOf(System.currentTimeMillis()));

        if (textContent != null) {
            params.put("Text", textContent);
        }

        try {
            // 1. 生成签名
            String signature = generateSignature(params, SECRET_KEY);
            params.put("Signature", signature);

            // 2. 发送请求
            long startTime = System.currentTimeMillis();
            String responseBody = sendPostRequest(url, params);
            long cost = System.currentTimeMillis() - startTime;

            // 3. 解析结果 (简单解析 Status Code，实际应用建议检查 JSON 中的 Code)
            // 注意：这里为了简化没有完全解析 JSON 对象，而是直接处理字符串
            if (responseBody != null && responseBody.contains("\"Code\":200") || responseBody.contains("\"Code\": 200")) {
                System.out.printf("   耗时: %dms | 状态码: 200 (Success)%n", cost);
                String filename = String.format("test_%02d_%s.jpg", caseIndex, templateKey);
                saveResultImage(responseBody, filename);
            } else {
                System.out.println("❌ 请求失败: " + responseBody);
            }

        } catch (Exception e) {
            System.out.println("❌ 网络或系统异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试用例: 强制覆盖颜色和透明度
     */
    public static void testMultipleOverride() {
        System.out.println("\n--- 测试用例: 强制覆盖颜色和透明度 (修复看不见的问题) ---");
        String url = API_HOST + "/tools/watermark";

        try {
            // 测试 A: 四角 + 红色 + 100%不透明 + 字体加大
            Map<String, Object> paramsCorners = new TreeMap<>();
            paramsCorners.put("AccountId", ACCOUNT_ID);
            paramsCorners.put("Image", TEST_IMAGE_URL);
            paramsCorners.put("TemplateKey", "multiple_corners");
            paramsCorners.put("Text", "CORNER-TEST");
            paramsCorners.put("Color", "#FF0000"); // 【覆盖】红色
            paramsCorners.put("Opacity", 100);     // 【覆盖】完全不透明
            paramsCorners.put("Size", 30);         // 【覆盖】字体30
            paramsCorners.put("OutputFormat", "JPEG");
            paramsCorners.put("Timestamp", String.valueOf(System.currentTimeMillis()));

            paramsCorners.put("Signature", generateSignature(paramsCorners, SECRET_KEY));

            String respA = sendPostRequest(url, paramsCorners);
            if (respA.contains("\"Code\":200") || respA.contains("\"Code\": 200")) {
                saveResultImage(respA, "debug_multiple_corners_RED.jpg");
            } else {
                System.out.println("Corners 失败: " + respA);
            }

            // 测试 B: 四边 + 蓝色 + 100%不透明
            Map<String, Object> paramsEdges = new TreeMap<>();
            paramsEdges.put("AccountId", ACCOUNT_ID);
            paramsEdges.put("Image", TEST_IMAGE_URL);
            paramsEdges.put("TemplateKey", "multiple_edges");
            paramsEdges.put("Text", "EDGE-TEST");
            paramsEdges.put("Color", "#0000FF"); // 【覆盖】蓝色
            paramsEdges.put("Opacity", 100);     // 【覆盖】完全不透明
            paramsEdges.put("OutputFormat", "JPEG");

            paramsEdges.put("Signature", generateSignature(paramsEdges, SECRET_KEY));

            String respB = sendPostRequest(url, paramsEdges);
            if (respB.contains("\"Code\":200") || respB.contains("\"Code\": 200")) {
                saveResultImage(respB, "debug_multiple_edges_BLUE.jpg");
            } else {
                System.out.println("Edges 失败: " + respB);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= 辅助工具方法 =================

    /**
     * 生成 API 请求签名 (HMAC-SHA256)
     */
    private static String generateSignature(Map<String, Object> params, String secretKey) throws Exception {
        // 1. 将参数按 Key 字典序排序，并拼接为 k=v&k=v 格式
        // TreeMap 默认按 Key 升序排列
        Map<String, Object> sortedParams = new TreeMap<>(params);

        String paramString = sortedParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().toString().isEmpty())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // 2. 拼接 SecretKey
        String signString = paramString + "&SecretKey=" + secretKey;

        // 3. 计算 HMAC-SHA256
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = hmacSha256.doFinal(signString.getBytes(StandardCharsets.UTF_8));

        // 4. 返回 Base64 字符串
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 发送 POST 请求
     */
    private static String sendPostRequest(String url, Map<String, Object> params) throws Exception {
        String jsonBody = buildJson(params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 简单的 JSON 构建器 (为了不依赖 Gson/Jackson)
     * 注意：这里只处理了简单的 String/Int/Float 类型，复杂的嵌套对象需要额外处理
     */
    private static String buildJson(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            sb.append("\"").append(key).append("\":");

            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                // 简单的转义处理
                String valStr = value.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
                sb.append("\"").append(valStr).append("\"");
            }
            sb.append(",");
        }
        // 移除最后一个逗号
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 解析 JSON 响应并保存 Base64 图片
     * 注意：这里使用了简单的字符串查找来提取 Base64，生产环境请使用 JSON 库
     */
    private static void saveResultImage(String jsonResponse, String filename) {
        try {
            // 1. 简单提取 Base64 字符串 (寻找 "Base64":"...")
            String marker = "\"Base64\"";
            int startIndex = jsonResponse.indexOf(marker);
            if (startIndex == -1) {
                System.out.println("❌ [" + filename + "] 保存失败: 返回数据中没有Base64");
                return;
            }

            // 定位到值的开始引号
            int valueStart = jsonResponse.indexOf("\"", startIndex + marker.length() + 1) + 1;
            // 定位到值的结束引号
            int valueEnd = jsonResponse.indexOf("\"", valueStart);

            if (valueStart <= 0 || valueEnd <= 0) {
                System.out.println("❌ [" + filename + "] 保存失败: 无法解析 Base64 字符串");
                return;
            }

            String b64Str = jsonResponse.substring(valueStart, valueEnd);

            // 2. 去掉 data:image/jpeg;base64, 前缀 (如果存在)
            if (b64Str.contains(",")) {
                b64Str = b64Str.split(",")[1];
            }

            // 3. 解码并保存
            byte[] imgData = Base64.getDecoder().decode(b64Str);
            try (FileOutputStream fos = new FileOutputStream(filename)) {
                fos.write(imgData);
            }

            // 尝试获取宽高 (仅供显示，不影响保存)
            String width = "unknown";
            String height = "unknown";
            if(jsonResponse.contains("\"Width\"")) {
                // 极其简陋的提取，仅演示用
                // 实际请务必使用 JSON 库
            }

            System.out.println("✅ [" + filename + "] 图片已保存");

        } catch (IOException | IllegalArgumentException e) {
            System.out.println("❌ [" + filename + "] 保存异常: " + e.getMessage());
        }
    }

    /**
     * 内部类：用于存储测试用例数据
     */
    static class TestCase {
        String key;
        String desc;
        String text;

        public TestCase(String key, String desc, String text) {
            this.key = key;
            this.desc = desc;
            this.text = text;
        }
    }
}