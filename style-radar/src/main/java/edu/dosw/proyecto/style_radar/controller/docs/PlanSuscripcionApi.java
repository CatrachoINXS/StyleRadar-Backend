package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import edu.dosw.proyecto.style_radar.model.dto.response.PlanSuscripcionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Planes de suscripción")
public interface PlanSuscripcionApi {
    @Operation(summary = "Consultar planes configurados y habilitados", description = "Consulta pública. "
            + "id, nombre, periodo y unidad MESES_CALENDARIO; sin precios, beneficios ni pagos incluidos. "
            + "Si no hay planes devuelve []. No se siembran planes al iniciar.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Planes habilitados; puede ser lista vacía"),
            @ApiResponse(responseCode = "500", description = "Fallo técnico")})
    @GetMapping
    ResponseEntity<List<PlanSuscripcionResponseDTO>> consultar();
}
