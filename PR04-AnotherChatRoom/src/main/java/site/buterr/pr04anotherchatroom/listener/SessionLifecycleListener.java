package site.buterr.pr04anotherchatroom.listener;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.util.List;

/**
 * SessionLifecycleListener
 * 监听会话创建/销毁；统计在线会话数，销毁时从在线用户列表移除。
 */
public class SessionLifecycleListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // TODO
        HttpSession session = se.getSession();
        ServletContext context = session.getServletContext();
        synchronized (context) {
            Integer onlineCount = (Integer) context.getAttribute("onlineCount");
            if (onlineCount == null) {
                onlineCount = 0;
            }
            context.setAttribute("onlineCount", onlineCount + 1);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // TODO
        HttpSession session = se.getSession();
        ServletContext context = session.getServletContext();
        synchronized (context) {
            Integer onlineCount = (Integer) context.getAttribute("onlineCount");
            if (onlineCount == null) {
                onlineCount = 0;
            }
            context.setAttribute("onlineCount", onlineCount - 1);
        }
    }
}
