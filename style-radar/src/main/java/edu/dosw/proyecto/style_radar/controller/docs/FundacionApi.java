package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import edu.dosw.proyecto.style_radar.model.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.FundacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.FundacionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Fundaciones", description = "Fundaciones aliadas: administración (admin) y consulta por cercanía (donantes)")
@SecurityRequirement(name = "bearerAuth")
public interface FundacionApi {

    @Operation(summary = "Registrar fundación aliada (RF-63)",
            description = "ROLE_ADMIN_STYLERADAR. El usuario indicado en representanteId recibe el rol REPRESENTANTE_FUNDACION.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fundación registrada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Representante inexistente"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o representante ya asignado"),
            @ApiResponse(responseCode = "422", description = "Representante con cuenta no activa"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('ADMIN_STYLERADAR')")
    @PostMapping("/api/v1/admin/fundaciones")
    ResponseEntity<FundacionResponseDTO> registrar(@Valid @RequestBody FundacionRequestDTO request);

    @Operation(summary = "Listar todas las fundaciones", description = "ROLE_ADMIN_STYLERADAR. Incluye desactivadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundaciones consultadas"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('ADMIN_STYLERADAR')")
    @GetMapping("/api/v1/admin/fundaciones")
    ResponseEntity<List<FundacionResponseDTO>> listarTodas();

    @Operation(summary = "Editar fundación aliada (RF-64)",
            description = "ROLE_ADMIN_STYLERADAR. Si cambia el representante, el rol pasa del anterior al nuevo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundación actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Fundación o representante inexistente"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o representante ya asignado"),
            @ApiResponse(responseCode = "422", description = "Representante con cuenta no activa"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('ADMIN_STYLERADAR')")
    @PutMapping("/api/v1/admin/fundaciones/{id}")
    ResponseEntity<FundacionResponseDTO> editar(@PathVariable Long id, @Valid @RequestBody FundacionRequestDTO request);

    @Operation(summary = "Desactivar fundación aliada (RF-65)",
            description = "ROLE_ADMIN_STYLERADAR. Impide nuevas donaciones y conserva el historial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundación desactivada"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "404", description = "Fundación inexistente"),
            @ApiResponse(responseCode = "422", description = "La fundación ya estaba desactivada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('ADMIN_STYLERADAR')")
    @PatchMapping("/api/v1/admin/fundaciones/{id}/desactivar")
    ResponseEntity<FundacionResponseDTO> desactivar(@PathVariable Long id);

    @Operation(summary = "Consultar fundaciones cercanas (RF-43)",
            description = "ROLE_COMPRADOR. Solo activas, ordenadas por distancia a la ubicación indicada; filtro opcional por tipo de prenda aceptado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fundaciones activas ordenadas por cercanía"),
            @ApiResponse(responseCode = "400", description = "Coordenadas ausentes o fuera de rango"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Rol sin permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/api/v1/fundaciones")
    ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ParameterObject @ModelAttribute ConsultaFundacionesRequestDTO request);
}
