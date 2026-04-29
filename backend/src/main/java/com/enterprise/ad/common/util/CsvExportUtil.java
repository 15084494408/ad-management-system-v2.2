package com.enterprise.ad.common.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * CSV 导出公共工具类
 * ★ 抽取自 FinanceController 中重复的导出逻辑
 */
public final class CsvExportUtil {

    private CsvExportUtil() {}

    /**
     * 设置 CSV 下载响应头
     *
     * @param response  HttpServletResponse
     * @param fileName  文件名（不含扩展名）
     */
    public static void setExportHeaders(HttpServletResponse response, String fileName) throws IOException {
        String encodedName = URLEncoder.encode(fileName + ".csv", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    /**
     * CSV 字段转义（处理逗号、引号、换行）
     */
    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
