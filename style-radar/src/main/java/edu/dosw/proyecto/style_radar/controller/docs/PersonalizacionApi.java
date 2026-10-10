package edu.dosw.proyecto.style_radar.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import edu.dosw.proyecto.style_radar.model.dto.request.PersonalizacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.RecomendacionesResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Personalización", description = "Feed y recomendaciones del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public interface PersonalizacionApi {
    @Operation(summary = "Obtener mi feed personalizado", description = "Prioriza estilo y talla, después coincidencias "
            + "exactas con búsquedas guardadas propias. Desempata por publicación descendente e ID ascendente. "
            + "Sin señales retorna el feed general disponible. page=0 y size=20 por defecto; page>=0, size entre 1 y 100.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feed paginado, incluyendo páginas vacías"),
        @ApiResponse(responseCode = "400", description = "Paginación inválida"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta no activa"),
        @ApiResponse(responseCode = "404", description = "Usuario inexistente durante una consulta autenticada válida"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/feed")
    ResponseEntity<PageResponseDTO<CatalogoItemResponseDTO>> feed(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ParameterObject @ModelAttribute PersonalizacionRequestDTO request);

    @Operation(summary = "Obtener mis recomendaciones de prendas", description = "Exige todas las dimensiones "
            + "configuradas: alguno de los estilos y alguna talla con stock positivo. Sin coincidencias devuelve vacío. "
            + "Sin perfil ofrece selección reciente con generalSinPreferencias=true. Orden publicación DESC, ID ASC. "
            + "page=0 y size=20 por defecto; page>=0, size entre 1 y 100.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recomendaciones paginadas o selección general explícita"),
        @ApiResponse(responseCode = "400", description = "Paginación inválida"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o cuenta no activa"),
        @ApiResponse(responseCode = "404", description = "Usuario inexistente durante una consulta autenticada válida"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/recomendaciones")
    ResponseEntity<RecomendacionesResponseDTO> recomendaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @ParameterObject @ModelAttribute PersonalizacionRequestDTO request);
}
