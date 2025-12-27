package vn.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Development helper: when `frontend.proxy.enabled=true` this controller redirects
 * requests for the root page(s) to the frontend dev server (e.g., http://localhost:3001).
 * This avoids needing to remove the packaged static/index.html during development.
 */
@Controller
@ConditionalOnProperty(name = "frontend.proxy.enabled", havingValue = "true")
public class DevForwardController {

    @Value("${frontend.url:http://localhost:3001}")
    private String frontendUrl;

    @GetMapping({"/", "/index.html"})
    public RedirectView forwardToFrontend(HttpServletRequest req) {
        String qs = req.getQueryString();
        String target = frontendUrl + (qs != null ? "?" + qs : "");
        RedirectView rv = new RedirectView(target);
        rv.setExposeModelAttributes(false);
        return rv;
    }
}
