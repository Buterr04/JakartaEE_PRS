package site.buterr.board;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
// TODO: 选择合适的数据结构（提示：并发场景）
import java.util.concurrent.CopyOnWriteArrayList;
// import java.util.ArrayList;

@WebServlet("/board")
public class BoardServlet extends HttpServlet {
    // TODO 1: 用合适的线程安全集合替换下面这一行（当前仅为占位）
    // private final List<Message> messages = java.util.Collections.emptyList();
    private final List<Message> messages = new CopyOnWriteArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Mini Board</title></head>
            <body>
            <h2>Mini Message Board</h2>
            <form method="post" action="board">
                Nickname: <input name="nickname" maxlength="20" required>
                <br>Message: <input name="content" maxlength="140" required style="width:320px">
                <br><button type="submit">Post</button>
            </form><hr>
            <h3>Messages</h3>
            """);

        // TODO 2: 按“最新在上”的顺序输出 messages
        // 需要对 nickname 与 content 做 HTML 转义（调用你在 TODO 4 实现的方法）
        // 示例占位：
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            out.println("<p><b>" + esc(message.getNickname()) + "</b>: "
                    + esc(message.getContent()) + "</p>");
        }

        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // TODO 3: 从请求中取 nickname 与 content，做基本校验（非空、长度限制）
        // 通过你的 Message 类创建对象并加入 messages
        // 完成后重定向回 GET /board
        String nickname = req.getParameter("nickname");
        String content = req.getParameter("content");

        if (nickname == null || content == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nickname and content are required");
            return;
        }

        nickname = nickname.trim();
        content = content.trim();

        if (nickname.isEmpty() || content.isEmpty()
                || nickname.length() > 20 || content.length() > 140) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
            return;
        }

        messages.add(new Message(nickname, content));
        resp.sendRedirect("board"); // 这行保留
    }

    // TODO 4: 实现一个最小的 HTML 转义方法，防止 XSS
    // 要至少处理 &, <, >
    private String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");// TODO: 替换为正确实现
    }
}
