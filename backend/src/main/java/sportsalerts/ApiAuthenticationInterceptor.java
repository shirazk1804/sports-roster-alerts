package sportsalerts;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthenticationInterceptor
        implements HandlerInterceptor {

    private final AppUserService
            appUserService;

    public ApiAuthenticationInterceptor(
            AppUserService appUserService) {

        this.appUserService =
                appUserService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        /*
         * Allow browser CORS preflight requests.
         */
        if ("OPTIONS".equalsIgnoreCase(
                request.getMethod())) {

            return true;
        }

        String authorizationHeader =
                request.getHeader(
                        "Authorization");

        /*
         * requireAuthenticatedUser() already
         * handles:
         *
         * - missing bearer token
         * - invalid token
         * - expired token
         */
        appUserService
                .requireAuthenticatedUser(
                        authorizationHeader);

        return true;
    }
}