package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import edu.dosw.proyecto.style_radar.model.dto.response.PrendaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public interface PrendaApi {

    @Operation(summary = "Obtener prendas", description = "Retorna las prendas registradas en el catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prendas consultadas correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    ResponseEntity<List<PrendaResponseDTO>> obtenerPrendas();
}
