package sportsalerts;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiSecurityHeadersFilter
        extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/")) {

            /*
             * Prevent browsers from guessing a
             * different content type.
             */
            response.setHeader(
                    "X-Content-Type-Options",
                    "nosniff");

            /*
             * This backend API should never be
             * embedded inside another webpage.
             */
            response.setHeader(
                    "X-Frame-Options",
                    "DENY");

            /*
             * Avoid leaking request URLs through
             * the browser's Referer header.
             */
            response.setHeader(
                    "Referrer-Policy",
                    "no-referrer");

            /*
             * The API does not need access to
             * browser hardware/features.
             */
            response.setHeader(
                    "Permissions-Policy",
                    "camera=(), microphone=(), geolocation=()");

            /*
             * Protected API responses may contain
             * user-specific information and should
             * not be cached by browsers/proxies.
             *
             * /api/teams is public, so we leave it
             * eligible for normal caching.
             */
            if (!request.getRequestURI().equals("/api/teams")) {

                response.setHeader(
                        "Cache-Control",
                        "no-store");

                response.setHeader(
                        "Pragma",
                        "no-cache");
            }
        }

        filterChain.doFilter(
                request,
                response);
    }
}