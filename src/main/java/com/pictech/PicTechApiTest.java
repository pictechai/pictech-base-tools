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
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PicTechApiTest {

    // ================= 配置区域 =================
    private static final String API_HOST = "https://www.pictech.top/pictech/commonapi";
    // 使用一张公网图片作为测试源
    private static final String TEST_IMAGE_URL = "https://pictech.top/pctccloud/pictechcc-translate/1769184000000/atlas50696a9ea89e4456898a0c6d3393792f/bac35d1fe8904961a0c7478d36fa8eb6_source_Chinese.jpeg";

    // 请替换为您自己的 AccountId 和 SecretKey
    private static final String ACCOUNT_ID = "your_ACCOUNT_ID";
    private static final String SECRET_KEY = "your_SECRET_KEY";
    // ===========================================
    // 全局 HTTP 客户端
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public static void main(String[] args) {
        System.out.println("开始执行 Java 版 API 测试用例...\n");

        testResizeScale();        // 用例 1: 按比例缩放
        testWatermarkPattern();   // 用例 2: 平铺防盗水印
        testWatermarkStamp();     // 用例 3: 审核通过印章
        testResizeFixedPng();     // 用例 4: 强制拉伸 + PNG
        testCropCenter();         // 用例 5: 裁剪图片
        testWatermarkCorners();   // 用例 6: 四角水印
        testResizeWidthOnly();    // 用例 7: 仅指定宽度缩放
    }

    // ==========================================
    // 测试用例 1: 按比例缩放 (Scale Resize)
    // ==========================================
    public static void testResizeScale() {
        System.out.println("\n--- 测试用例 1: 按比例缩小 50% ---");
        String url = API_HOST + "/tools/resize";

        // 使用 TreeMap 自动按 Key 进行字典序排序，方便后续签名
        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("Scale", 0.5); // 核心参数：缩放比例
        payload.put("OutputFormat", "JPEG");
        payload.put("Quality", 90);
        payload.put("Timestamp", String.valueOf(System.currentTimeMillis()));

        sendRequestAndSave(url, payload, "result_1_resize_50pct.jpg");
    }

    // ==========================================
    // 测试用例 2: 平铺防盗水印 (Pattern Watermark)
    // ==========================================
    public static void testWatermarkPattern() {
        System.out.println("\n--- 测试用例 2: 平铺斜向防盗水印 ---");
        String url = API_HOST + "/tools/watermark";

        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("TemplateKey", "pattern_diagonal_da"); // 核心参数
        payload.put("Text", "绝密资料 禁止外传");
        payload.put("OutputFormat", "JPEG");
        payload.put("Quality", 95);

        sendRequestAndSave(url, payload, "result_2_watermark_pattern.jpg");
    }

    // ==========================================
    // 测试用例 3: 审核通过印章 (Stamp Watermark)
    // ==========================================
    public static void testWatermarkStamp() {
        System.out.println("\n--- 测试用例 3: 审核通过大印章 (中心) ---");
        String url = API_HOST + "/tools/watermark";

        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("TemplateKey", "special_approved"); // 核心参数
        // payload.put("Text", "已审核"); // 注释掉则使用模板默认文字
        payload.put("OutputFormat", "JPEG");
        payload.put("Quality", 90);

        sendRequestAndSave(url, payload, "result_3_watermark_approved.jpg");
    }

    // ==========================================
    // 测试用例 4: 强制拉伸 + 格式转换 (Fixed Resize -> PNG)
    // ==========================================
    public static void testResizeFixedPng() {
        System.out.println("\n--- 测试用例 4: 强制拉伸 200x200 并转为 PNG ---");
        String url = API_HOST + "/tools/resize";

        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("Width", 200);
        payload.put("Height", 300);
        payload.put("Mode", "fixed"); // 核心参数：强制拉伸
        payload.put("OutputFormat", "PNG"); // 核心参数：PNG格式
        payload.put("Quality", 100);

        sendRequestAndSave(url, payload, "result_4_fixed_200x200.png");
    }

    // ==========================================
    // 测试用例 5: 裁剪图片 (Crop)
    // ==========================================
    public static void testCropCenter() {
        System.out.println("\n--- 测试用例 5: 裁剪中间区域 ---");
        String url = API_HOST + "/tools/crop";

        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("X", 100);
        payload.put("Y", 50);
        payload.put("Width", 300);
        payload.put("Height", 150);
        payload.put("OutputFormat", "JPEG");

        sendRequestAndSave(url, payload, "result_5_crop_300x150.jpg");
    }

    // ==========================================
    // 测试用例 6: 四角水印 (Multiple Corners)
    // ==========================================
    public static void testWatermarkCorners() {
        System.out.println("\n--- 测试用例 6: 四角多点水印 ---");
        String url = API_HOST + "/tools/watermark";

        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("TemplateKey", "multiple_corners");
        payload.put("Text", "Company Logo");
        payload.put("OutputFormat", "JPEG");

        sendRequestAndSave(url, payload, "result_6_corners.jpg");
    }

    // ==========================================
    // 测试用例 7: 仅指定宽度缩放 (Auto Height)
    // ==========================================
    public static void testResizeWidthOnly() {
        System.out.println("\n--- 测试用例 7: 仅指定宽度 300px (高度自适应) ---");
        String url = API_HOST + "/tools/resize";

        Map<String, Object> payload = new TreeMap<>();
        payload.put("AccountId", ACCOUNT_ID);
        payload.put("Image", TEST_IMAGE_URL);
        payload.put("Width", 300);
        payload.put("Mode", "lfit");
        // 不传 Height，让其自动计算

        sendRequestAndSave(url, payload, "result_7_width_300.jpg");
    }


    // =================================================================
    // 核心辅助方法
    // =================================================================

    /**
     * 封装流程：签名 -> 发送请求 -> 解析并保存图片
     */
    private static void sendRequestAndSave(String url, Map<String, Object> payload, String filename) {
        try {
            // 1. 生成签名
            String signature = generateSignature(payload, SECRET_KEY);
            payload.put("Signature", signature);

            // 2. 发送 POST 请求
            HttpResponse<String> response = postJsonWithResponse(url, payload);
            System.out.println("状态码: " + response.statusCode());

            if (response.statusCode() == 200) {
                // 3. 保存结果
                saveResultImage(response.body(), filename);
            } else {
                System.out.println("错误: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成 API 请求签名 (HMAC-SHA256)
     * 逻辑对应 Python 的 generate_signature
     */
    private static String generateSignature(Map<String, Object> params, String secretKey) throws Exception {
        // 1. 过滤空值 (Python代码中: if v is not None and v != '')
        // TreeMap 已自动按 Key 升序排列
        String paramString = params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().toString().isEmpty())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // 2. 拼接 SecretKey
        String signString = paramString + "&SecretKey=" + secretKey;

        // 3. 计算 HMAC-SHA256
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = hmacSha256.doFinal(signString.getBytes(StandardCharsets.UTF_8));

        // 4. Base64 编码
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 将返回的 Base64 保存为图片
     * 逻辑对应 Python 的 save_result_image
     */
    private static void saveResultImage(String jsonResponse, String filename) {
        try {
            // 简易解析 JSON 获取 Base64 字段 (为了不依赖 Gson/Jackson)
            // 查找 "Base64":"..."
            String searchKey = "\"Base64\"";
            int idx = jsonResponse.indexOf(searchKey);

            if (idx == -1) {
                System.out.println("❌ [" + filename + "] 保存失败: 返回数据中没有Base64");
                return;
            }

            // 定位 value 的引号位置
            int startQuote = jsonResponse.indexOf("\"", idx + searchKey.length() + 1);
            int endQuote = jsonResponse.indexOf("\"", startQuote + 1);

            if (startQuote == -1 || endQuote == -1) {
                System.out.println("❌ [" + filename + "] 保存失败: 解析错误");
                return;
            }

            String b64Str = jsonResponse.substring(startQuote + 1, endQuote);

            // 去掉 data:image/jpeg;base64, 前缀 (如果存在)
            if (b64Str.contains(",")) {
                b64Str = b64Str.split(",")[1];
            }

            // 解码并保存文件
            byte[] imgData = Base64.getDecoder().decode(b64Str);
            try (FileOutputStream fos = new FileOutputStream(filename)) {
                fos.write(imgData);
            }

            // 尝试获取宽高用于打印 (简单字符串截取)
            String info = "(尺寸未知)";
            if (jsonResponse.contains("\"Width\"") && jsonResponse.contains("\"Height\"")) {
                // 仅作演示，实际请用 JSON 库解析
                info = "(图片已生成)";
            }

            System.out.println("✅ [" + filename + "] 图片已保存 " + info);

        } catch (Exception e) {
            System.out.println("❌ [" + filename + "] 保存异常: " + e.getMessage());
        }
    }

    /**
     * 发送 POST 请求并返回响应体字符串
     */
    private static String postJson(String url, Map<String, Object> payload) throws IOException, InterruptedException {
        return postJsonWithResponse(url, payload).body();
    }

    /**
     * 发送 POST 请求并返回完整响应对象
     */
    private static HttpResponse<String> postJsonWithResponse(String url, Map<String, Object> payload) throws IOException, InterruptedException {
        String jsonBody = buildJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 手动构建 JSON 字符串 (替代第三方 JSON 库)
     */
    private static String buildJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            sb.append("\"").append(key).append("\":");

            if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                // 字符串需要加引号，并处理简单转义
                String strVal = String.valueOf(val)
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                sb.append("\"").append(strVal).append("\"");
            }
            sb.append(",");
        }
        // 移除最后一个逗号
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}