package site.buterr.pr04anotherchatroom.servlet;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * ChatServlet
 * 展示聊天室页面并处理消息发送；消息上限由 context-param 控制。
 */
public class ChatServlet extends HttpServlet {

    @SuppressWarnings("unchecked")
    private List<String> getMsgs() {
        return (List<String>) getServletContext().getAttribute("msgs");
    }

    @SuppressWarnings("unchecked")
    private List<String> getUsers() {
        return (List<String>) getServletContext().getAttribute("users");
    }

    private int getMaxMsgCount() {
        String v = getServletContext().getInitParameter("maxMessageCount");
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return 50;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        HttpSession session = req.getSession(false);
        String user = (session == null) ? "" : (String) session.getAttribute("user");
        long lastActive = (session == null || session.getAttribute("lastActiveAt") == null)
                ? 0L : (long) session.getAttribute("lastActiveAt");

        List<String> users = getUsers();
        List<String> msgs = getMsgs();

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html><html><head><meta charset='UTF-8'><title>聊天室</title></head><body>");

            out.printf("<p>当前用户：%s | 在线会话数：%d | 最近活跃：%s</p>",
                    user, (int) getServletContext().getAttribute("onlineCount"),
                    (lastActive == 0 ? "-" : new Date(lastActive).toString()));

            out.println("<h3>消息列表</h3><ul>");
            synchronized (msgs) {
                for (String m : msgs) {
                    out.printf("<li>%s</li>%n", m);
                }
            }
            out.println("</ul>");

            out.println("<form method='post' action='chat'>");
            out.println("<textarea name='msg' rows='3'></textarea><br/>");
            out.println("<button type='submit'>发送</button>");
            out.println("</form>");

            out.println("<h3>在线用户</h3><ul>");
            synchronized (users) {
                for (String u : users) {
                    out.printf("<li>%s</li>%n", u);
                }
            }
            out.println("</ul>");

            out.println("<form method='post' action='logout'><button>退出登录</button></form>");

            out.println("</body></html>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String user = (String) session.getAttribute("user");

        // 优先使用 EncodingFilter 过滤后的文本
        String filtered = (String) req.getAttribute("filteredMsg");
        String raw = req.getParameter("msg");
        String msg = filtered != null ? filtered : (raw == null ? "" : raw);

        if (!msg.isBlank()) {
            List<String> msgs = getMsgs();
            int max = getMaxMsgCount();
            synchronized (msgs) {
                msgs.add("<b>" + user + "</b>: " + msg);
                while (msgs.size() > max) msgs.remove(0);
            }
        }
        resp.sendRedirect(req.getContextPath() + "/chat");
    }
}
