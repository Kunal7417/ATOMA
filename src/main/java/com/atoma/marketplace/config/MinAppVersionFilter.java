package com.atoma.marketplace.config;

import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MinAppVersionFilter extends OncePerRequestFilter {

    private static final Pattern VERSION = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private final AppVersionProperties appVersionProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!appVersionProperties.isEnforceVersionHeader()) {
            filterChain.doFilter(request, response);
            return;
        }
        var clientVersion = request.getHeader("X-App-Version");
        if (clientVersion == null || clientVersion.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!VERSION.matcher(clientVersion.trim()).matches()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (compareSemver(clientVersion.trim(), appVersionProperties.getMinMerchantVersion()) < 0) {
            writeUpgradeRequired(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUpgradeRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UPGRADE_REQUIRED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = ErrorResponse.builder()
                .code(ErrorCodes.APP_UPDATE_REQUIRED)
                .message("App update required")
                .details(Map.of("minVersion", appVersionProperties.getMinMerchantVersion()))
                .build();
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /** @return negative if a < b, zero if equal, positive if a > b */
    static int compareSemver(String a, String b) {
        var pa = a.split("\\.");
        var pb = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int va = Integer.parseInt(pa[i]);
            int vb = Integer.parseInt(pb[i]);
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }
}
