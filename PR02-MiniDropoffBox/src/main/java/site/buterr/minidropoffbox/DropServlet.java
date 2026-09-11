package site.buterr.minidropoffbox;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DropServlet
 * <p>
 * 处理上传、展示详情和下载：
 * - POST /drops    → 上传文件（表单提交）
 * - GET  /drops    → 展示上传表单和文件列表
 * - GET  /drops/{id}       → 文件详情
 * - GET  /drops/{id}/file  → 文件下载（支持 ETag 缓存）
 */
@WebServlet(urlPatterns = {"/drops", "/drops/*"})
@MultipartConfig(
        maxFileSize = 5 * 1024 * 1024,       // 单个文件最大 5MB
        maxRequestSize = 5 * 1024 * 1024     // 整个请求体最大 5MB（防止总量超限直接 500）
) // 限制单个文件最大 5MB
public class DropServlet extends HttpServlet {

    // 内存存储，id -> Drop
    private final Map<String, Drop> store = new ConcurrentHashMap<>();

    /**
     * 处理 GET 请求：列表页、详情页和下载
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // 请求路径
        resp.setCharacterEncoding("UTF-8");

        // 列表页：/drops
        if (path == null || "/".equals(path)) {
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("<h2>Mini Drop-off Box</h2>");
            out.println("<form method='post' enctype='multipart/form-data'>");
            out.println("描述: <input name='desc' maxlength='100'><br>");
            out.println("文件: <input type='file' name='file' required><br>");
            out.println("<button type='submit'>上传</button></form><hr>");
            String base = req.getContextPath() + "/drops/";
            // TODO 1: 遍历 store，把已有的 Drop 生成 <a href="...">链接</a>，展示文件名和描述
            for (Drop drop : store.values()) {
                out.printf("<a href='%s%s'>%s</a> - %s<br>", base, drop.getId(), esc(drop.getFilename()), esc(drop.getDesc()));
            }
            return;
        }

        // 去掉前导 "/"
        path = path.startsWith("/") ? path.substring(1) : path;

        // 下载：/drops/{id}/file
        if (path.endsWith("/file")) {
            String id = path.substring(0, path.length() - "/file".length());
            Drop d = store.get(id);
            if (d == null) {
                // TODO 2: 返回 404 错误（resp.sendError）
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            handleDownload(d, req, resp);
            return;
        }

        // 详情页：/drops/{id}
        String id = path;
        Drop d = store.get(id);
        if (d == null) {
            // TODO 3: 返回 404 错误
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String downloadUrl = req.getContextPath() + "/drops/" + id + "/file";
        // TODO 4: 输出文件详情（文件名、描述），并生成下载链接 <a href='downloadUrl'>下载</a>
        out.printf("<h2>文件详情</h2>");
        out.printf("<p>文件名: %s</p>", esc(d.getFilename()));
        out.printf("<p>描述: %s</p>", esc(d.getDesc()));
        out.printf("<p><a href='%s'>下载</a></p>", downloadUrl);
    }

    /**
     * 下载逻辑：支持 ETag 协商缓存
     */
    private void handleDownload(Drop d, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String etag = "\"" + d.getId() + "-" + d.getData().length + "\"";

        // 命中缓存，返回 304
        if (etag.equals(req.getHeader("If-None-Match"))) {
            // TODO 5: 设置状态码为 304 (resp.setStatus)
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // 正常下载，设置必要响应头
        // TODO 6: 设置 Content-Type、Content-Disposition、ETag、Content-Length
        resp.setContentType(d.getMime());
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + d.getFilename() + "\"");
        resp.setHeader("ETag", etag);
        resp.setContentLength(d.getData().length);

        // TODO 7: 把 d.getData() 写到 resp.getOutputStream()
        try (OutputStream out = resp.getOutputStream()) {
            out.write(d.getData());
        }
    }

    /**
     * 处理 POST 请求：上传文件
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        Part filePart;
        try {
            filePart = req.getPart("file");
        } catch (Exception e) {
            // 解析 multipart 超限时，Tomcat 会抛出异常
            // TODO 8: 返回 413 错误（resp.sendError），提示“文件过大”
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "文件过大");
            return;
        }
        if (filePart == null || filePart.getSize() == 0) {
            // TODO 9: 返回 400 错误，请求不合法（没有文件）
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "没有文件");
            return;
        }

        // 2) 校验大小
        final long MAX = 5L * 1024 * 1024;
        if (filePart.getSize() > MAX) {
            // TODO 10: 返回 413 错误
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "文件过大");
            return;
        }

        // 3) 校验 MIME 类型
        String mime = filePart.getContentType();
        if (!isAllowedMime(mime)) {
            // TODO 11: 返回 415 错误（不支持的媒体类型）
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, mime);
            return;
        }

        // 4) 读取文件内容
        String filename = sanitizeFilename(filePart.getSubmittedFileName());
        String desc = req.getParameter("desc");
        byte[] data;
        try (InputStream in = filePart.getInputStream();
             ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
            data = buf.toByteArray();
        }

        // 5) 保存到内存 Map
        String id = UUID.randomUUID().toString().substring(0, 6);
        Drop drop = new Drop(id, filename, mime, data, desc);
        store.put(id, drop);

        // 6) 返回结果
        String location = req.getContextPath() + "/drops/" + id;
        if ("application/json".equalsIgnoreCase(req.getHeader("Accept"))) {
            // API 调用：201 Created + Location + JSON
            // TODO 13: 设置 resp 状态码 201，设置 Location 头，返回 JSON {"id":..., "url":...}
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setHeader("Location", location);
            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.printf("{\"id\":\"%s\",\"url\":\"%s\"}", id, location);
        } else {
            // 浏览器：302 Found → 详情页
            // TODO 14: 设置 resp 状态码 302，resp.sendRedirect(location)
            resp.setStatus(HttpServletResponse.SC_FOUND);
            resp.sendRedirect(location);
        }
    }

    /**
     * MIME 白名单校验
     */
    private boolean isAllowedMime(String mime) {
        if (mime == null) return false;
        return mime.startsWith("image/")
                || mime.equals("text/plain")
                || mime.equals("application/pdf")
                || mime.equals("application/octet-stream");
    }

    /**
     * 文件名清洗，避免路径穿越与特殊字符
     */
    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        String base = name.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\r\\n]", "");
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.isEmpty()) base = "file";
        return base.length() > 100 ? base.substring(0, 100) : base;
    }

    /**
     * HTML 转义，防止 XSS
     */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
