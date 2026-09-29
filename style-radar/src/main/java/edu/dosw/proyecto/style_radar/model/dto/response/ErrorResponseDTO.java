package edu.dosw.proyecto.style_radar.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponseDTO {

    LocalDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
    Map<String, String> validationErrors;
}
