package com.webox.webox.interceptor;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(SessionKeys.LOGIN_USER) != null) return true;
        // skip redirect for JSON API: return 401
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            res.setStatus(401); res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
            return false;
        }
        res.sendRedirect(req.getContextPath() + "/login");
        return false;
    }
}
