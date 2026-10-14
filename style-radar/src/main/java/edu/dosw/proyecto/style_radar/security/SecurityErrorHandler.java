package edu.dosw.proyecto.style_radar.security;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import edu.dosw.proyecto.style_radar.model.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    public static final String UNAUTHORIZED_MESSAGE = "Autenticación inválida";
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(request, response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "Acceso denegado");
    }

    public void serverError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente nuevamente más tarde.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now(clock)).status(status.value()).error(status.getReasonPhrase())
                .message(message).path(request.getRequestURI()).validationErrors(Map.of()).build());
    }
}
