package com.dominik.todolist.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String userEmail = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                userEmail = jwtUtil.extractUsername(jwt);
                log.debug("Extracted userEmail: {} from JWT", userEmail);
            } catch (Exception e) {
                // JwtUtil already logs specific errors for expired, malformed, signature issues, etc.
                // This catch is more for unexpected issues during extraction itself.
                log.warn("Could not extract username from JWT or token is invalid early: {}", e.getMessage());
            }
        }

        if (StringUtils.hasText(userEmail) && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("User email {} found in token, attempting to load UserDetails.", userEmail);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail); // Can throw UsernameNotFoundException

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                log.info("JWT Token is valid for user {}. Setting authentication context.", userEmail);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("JWT token validation failed for user {}.", userEmail);
            }
        } else if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ") && !StringUtils.hasText(userEmail)) {
            // This case means token was present but username extraction failed (e.g., token was invalid/expired)
            // JwtUtil's extractAllClaims would have logged the specific reason.
            log.debug("JWT was present but userEmail could not be extracted or was invalid.");
        }


        filterChain.doFilter(request, response);
    }
}