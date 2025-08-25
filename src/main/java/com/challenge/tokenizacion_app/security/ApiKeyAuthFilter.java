package com.challenge.tokenizacion_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    public static final String HEADER = "X-API-KEY";

    private final String expectedApiKey;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public ApiKeyAuthFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean skip = matcher.match("/api/ping", path)
                || matcher.match("/actuator/health", path)
                || matcher.match("/v3/api-docs/**", path)
                || matcher.match("/swagger-ui/**", path)
                || matcher.match("/error", path);
        if (skip) log.debug("ApiKeyAuthFilter: skipping {}", path);
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        log.debug("ApiKeyAuthFilter: filtering {}", req.getRequestURI());
        String provided = req.getHeader(HEADER);

        if (provided == null || !constantTimeEquals(provided, expectedApiKey)) {
            log.debug("ApiKeyAuthFilter: missing/invalid API key");
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Missing or invalid X-API-KEY\"}");
            return;
        }

        // ✅ Marcar la petición como autenticada (ROLE_API)
        var auth = new UsernamePasswordAuthenticationToken(
                "api-client",            // principal
                null,                    // credentials
                List.of(new SimpleGrantedAuthority("ROLE_API"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.debug("ApiKeyAuthFilter: API key OK, authentication set as ROLE_API");
        chain.doFilter(req, res);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0; for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
}
