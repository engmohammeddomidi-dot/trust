package com.trust.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** يقرأ Authorization: Bearer <jwt> ويضبط سياق الأمان إذا كان صالحًا */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                AuthenticatedUser principal = new AuthenticatedUser(
                        Long.valueOf(claims.get("userId").toString()),
                        claims.getSubject(),
                        claims.get("role", String.class),
                        claims.get("organizationId") != null ? Long.valueOf(claims.get("organizationId").toString()) : null,
                        claims.get("branchId") != null ? Long.valueOf(claims.get("branchId").toString()) : null
                );
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ignored) {
                // رمز غير صالح أو منتهي - يبقى الطلب بدون مصادقة، سيُرفض لاحقًا إن كان المسار محميًا
            }
        }
        filterChain.doFilter(request, response);
    }
}
