package com.example.agente.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            
            try {
                if (jwtUtil.validarToken(token)) {
                    String email = jwtUtil.obtenerEmail(token);
                    UUID empresaId = jwtUtil.obtenerEmpresaId(token);
                    
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email, null, Collections.emptyList()
                        );
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // Guardar la autenticación en el contexto
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // Guardar el empresaId en un atributo de request para fácil lectura en controladores
                        request.setAttribute("empresaId", empresaId);
                    }
                }
            } catch (Exception e) {
                System.err.println("[JwtFilter] Error al procesar el token JWT: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
