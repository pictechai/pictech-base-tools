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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 独立的图片修复 (Inpaint) 测试类
 * 输入：原图 URL, 蒙版 URL
 * 输出：本地修复后的图片文件
 */
public class PicTechInpaintTest {

    // ================= 配置区域 =================
    // 请替换为您真实的 AccountId 和 SecretKey
    private static final String ACCOUNT_ID = "pic_YOUR_ID";
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";
    // API 地址 (根据您的实际环境调整，通常是 image_inpaint_sync 或 inpaint_image_sync)
    private static final String API_URL = "https://www.pictech.top/pictech/commonapi/inpaint_image_sync";
    // ===========================================

    public static void main(String[] args) {
        // 1. 定义输入 URL (示例)
        String sourceImageUrl = "https://pictech.top/pctccloud/pictechcc-translate/1770220800000/atlas81e209b7068e43e6bc9ef58e6f0289cc/423cc781e46d48fdaedb5b5e7ee43785_source_Chinese.jpeg"; // 原图 URL
        String maskImageUrl = "https://pictech.top/pctccloud/pictechcc-translate/1770220800000/atlas81e209b7068e43e6bc9ef58e6f0289cc/aaf07885586a4ab29583da1a091df515_mask.webp";     // 蒙版 URL (白色为修复区域)

        // 2. 定义输出路径
        String saveDir = "./output_images";
        String saveFileName = "inpaint_result"; // 不需要后缀，程序会自动添加 .png

        try {
            System.out.println("开始执行图片修复任务...");
            System.out.println("原图: " + sourceImageUrl);
            System.out.println("蒙版: " + maskImageUrl);

            // 3. 执行 iopaint 逻辑
            String savedFileName = iopaint(sourceImageUrl, maskImageUrl, saveDir, saveFileName);

            System.out.println("✅ 任务成功！图片已保存至: " + saveDir + "/" + savedFileName);

        } catch (Exception e) {
            System.err.println("❌ 任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 【业务包装方法】
     * 下载URL图片 -> 转换Base64 -> 调用API -> 保存文件
     */
    public static String iopaint(String sourceUrl, String maskUrl, String savePath, String imageName) throws Exception {
        // 1. 将 URL 图片下载并转为 Base64
        String sourceBase64 = downloadUrlAsBase64(sourceUrl);
        String maskBase64 = downloadUrlAsBase64(maskUrl);

        if (sourceBase64 == null || maskBase64 == null) {
            throw new IOException("无法下载图片或蒙版，请检查 URL 是否有效。");
        }

        // 2. 调用 API 获取修复后的图片字节流
        byte[] responseBytes = inpaintImageSync(sourceBase64, maskBase64);

        if (responseBytes == null || responseBytes.length == 0) {
            throw new RuntimeException("API 返回的图片数据为空");
        }

        // 3. 保存到本地
        String fileExt = "png"; // API 返回的通常是 PNG
        String fullFileName = imageName + "." + fileExt;

        // 确保存储目录存在
        Path dirPath = Paths.get(savePath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path finalPath = dirPath.resolve(fullFileName);
        try (FileOutputStream fos = new FileOutputStream(finalPath.toFile())) {
            fos.write(responseBytes);
        }

        return fullFileName;
    }

    /**
     * 【核心 API 调用方法】
     * 构造请求 -> 签名 -> 发送 -> 返回字节数组
     */
    public static byte[] inpaintImageSync(String sourceImageBase64, String maskImageBase64) throws Exception {
        // 1. 准备请求参数
        Map<String, String> params = new TreeMap<>();
        params.put("AccountId", ACCOUNT_ID);
        // 获取 CST 时间戳
        long cstTimestamp = Instant.now().atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        params.put("Timestamp", String.valueOf(cstTimestamp));

        // 注意：这里传入的是纯 Base64，不需要 data:image/png;base64, 前缀
        params.put("image", sourceImageBase64);
        params.put("mask", maskImageBase64);

        // 2. 生成签名
        String signature = generateSignature(params, SECRET_KEY);
        params.put("Signature", signature);

        // 3. 构建 JSON
        String jsonBody = buildJsonRequest(params);

        // 4. 发送 HTTP 请求
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "*/*") // 接受二进制流
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        // 5. 处理响应
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            // 如果出错，尝试将 byte[] 转为字符串打印错误信息
            String errorMsg = new String(response.body(), StandardCharsets.UTF_8);
            throw new RuntimeException("API 请求失败 [HTTP " + response.statusCode() + "]: " + errorMsg);
        }
    }

    /**
     * 工具方法：下载 URL 资源并转为纯 Base64 字符串 (不带 data 前缀)
     */
    private static String downloadUrlAsBase64(String urlStr) {
        try {
            System.out.println("正在下载: " + urlStr);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                System.err.println("下载失败，状态码: " + response.statusCode());
                return null;
            }

            return Base64.getEncoder().encodeToString(response.body());
        } catch (Exception e) {
            System.err.println("下载图片异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 工具方法：生成签名
     */
    private static String generateSignature(Map<String, String> params, String secretKey) throws Exception {
        // 过滤空值并排序
        String paramString = params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        String signString = paramString + "&SecretKey=" + secretKey;

        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        hmacSha256.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = hmacSha256.doFinal(signString.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 工具方法：手动构建简单的 JSON 字符串
     * (避免引入 Jackson/Gson 依赖，确保独立运行)
     */
    private static String buildJsonRequest(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"");
            // 简单的转义处理
            sb.append(entry.getValue().replace("\"", "\\\"").replace("\n", ""));
            sb.append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}