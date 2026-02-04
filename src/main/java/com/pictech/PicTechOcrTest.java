package com.pictech;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class PicTechOcrTest {
    // 替换为您的实际 API 地址
    private static final String API_URL = "https://www.pictech.top/pictech/commonapi/image_ocr_sync";
    private static final String ACCOUNT_ID = "pic_YOUR_ID";
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";

    public static void main(String[] args) throws Exception {
        String imagePath = "/Users/Downloads/aa.jpeg"; // 待识别图片路径

        // 执行OCR识别
        String result = performOcr(imagePath);

        // 输出美化后的结果
        printFormattedResult(result);
    }

    /**
     * 执行OCR识别
     */
    private static String performOcr(String imagePath) throws Exception {
        // 1. 准备参数
        Map<String, Object> params = new TreeMap<>();
        params.put("AccountId", ACCOUNT_ID);

        // 获取 CST (UTC+8) 时间戳
        long cstTimestamp = Instant.now().atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        params.put("Timestamp", String.valueOf(cstTimestamp));

        // 读取图片并转 Base64
        params.put("ImageBase64", readFileAsBase64(imagePath));

        // 2. 生成签名
        String signature = generateSignature(params, SECRET_KEY);
        params.put("Signature", signature);

        // 3. 构建JSON请求体（使用更可靠的方法）
        String jsonBody = buildJsonRequest(params);

        // 4. 发送请求
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        System.out.println("正在发送OCR请求...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 5. 返回响应
        System.out.println("响应状态码: " + response.statusCode());
        return response.body();
    }

    /**
     * 读取文件并转换为带 MIME 头的 Base64 字符串
     */
    private static String readFileAsBase64(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(path));
        String lowerPath = path.toLowerCase();

        // 根据文件扩展名确定MIME类型
        String mimeType = "image/jpeg"; // 默认
        if (lowerPath.endsWith(".png")) {
            mimeType = "image/png";
        } else if (lowerPath.endsWith(".bmp")) {
            mimeType = "image/bmp";
        } else if (lowerPath.endsWith(".gif")) {
            mimeType = "image/gif";
        } else if (lowerPath.endsWith(".tiff") || lowerPath.endsWith(".tif")) {
            mimeType = "image/tiff";
        }

        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * 生成 API 请求签名 (HMAC-SHA256)
     */
    private static String generateSignature(Map<String, Object> params, String secretKey) throws Exception {
        // 1. 过滤空值并按Key排序
        String paramString = params.entrySet().stream()
                .filter(entry -> {
                    Object value = entry.getValue();
                    return value != null && !value.toString().isEmpty();
                })
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
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
     * 构建JSON请求体（使用更简单可靠的方法）
     */
    private static String buildJsonRequest(Map<String, Object> params) {
        // 使用StringBuilder构建JSON
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            String key = entry.getKey();
            Object value = entry.getValue();

            json.append("\"").append(key).append("\":");

            if (value instanceof String) {
                // 转义特殊字符
                String escaped = ((String) value)
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                json.append("\"").append(escaped).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else {
                // 其他类型都转为字符串处理
                json.append("\"").append(value.toString()).append("\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 美化并打印OCR结果
     */
    private static void printFormattedResult(String jsonResponse) {
        try {
            // 将Unicode转义序列转换为中文
            String decodedResponse = jsonResponse
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            // 简单解析JSON以提取数据
            if (jsonResponse.contains("\"Code\":200")) {
                System.out.println("\n========== OCR识别成功 ==========");
                System.out.println("状态: 识别完成");

                // 提取识别结果
                if (jsonResponse.contains("TemplateJson")) {
                    System.out.println("\n识别到的文字区域:");
                    System.out.println("----------------------------------------");

                    // 查找并提取每个文字区域
                    int startIndex = jsonResponse.indexOf("\"TemplateJson\":");
                    if (startIndex > 0) {
                        // 简化处理，提取所有"text"字段
                        String[] parts = jsonResponse.split("\"text\":\"");
                        for (int i = 1; i < parts.length; i++) {
                            String part = parts[i];
                            int endIndex = part.indexOf("\"");
                            if (endIndex > 0) {
                                String text = part.substring(0, endIndex);
                                // 将Unicode转义序列转换为中文
                                String chineseText = convertUnicodeToChinese(text);
                                System.out.println("区域 " + i + ": " + chineseText);
                            }
                        }
                    }
                }

                // 提取RequestId
                if (jsonResponse.contains("\"RequestId\":")) {
                    int start = jsonResponse.indexOf("\"RequestId\":\"") + 13;
                    int end = jsonResponse.indexOf("\"", start);
                    if (start > 13 && end > start) {
                        String requestId = jsonResponse.substring(start, end);
                        System.out.println("\n请求ID: " + requestId);
                    }
                }

                System.out.println("========================================\n");
                System.out.println("完整响应:");
                System.out.println(jsonResponse);

            } else {
                System.out.println("\n========== OCR识别失败 ==========");
                System.out.println("响应内容: " + jsonResponse);
            }

        } catch (Exception e) {
            System.out.println("解析响应时出错: " + e.getMessage());
            System.out.println("原始响应: " + jsonResponse);
        }
    }

    /**
     * 将Unicode转义序列转换为中文
     */
    private static String convertUnicodeToChinese(String unicodeText) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < unicodeText.length()) {
            if (unicodeText.charAt(i) == '\\' && i + 1 < unicodeText.length() && unicodeText.charAt(i + 1) == 'u') {
                // 处理Unicode转义序列
                String hex = unicodeText.substring(i + 2, i + 6);
                try {
                    int codePoint = Integer.parseInt(hex, 16);
                    result.append((char) codePoint);
                    i += 6;
                } catch (Exception e) {
                    result.append(unicodeText.charAt(i));
                    i++;
                }
            } else {
                result.append(unicodeText.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}