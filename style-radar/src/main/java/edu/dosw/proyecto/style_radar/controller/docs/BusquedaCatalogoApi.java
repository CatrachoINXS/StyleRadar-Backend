package edu.dosw.proyecto.style_radar.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import edu.dosw.proyecto.style_radar.model.dto.request.BusquedaCatalogoRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.CatalogoItemResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.PageResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Búsqueda de catálogo", description = "Búsqueda global y filtros combinables del catálogo")
public interface BusquedaCatalogoApi {

    @Operation(
            summary = "Buscar en el catálogo global",
            description = "Busca ítems disponibles mediante criterios combinables con AND. "
                    + "Primero realiza la búsqueda textual directa. Si q no obtiene coincidencias directas, "
                    + "retorna prendas similares que siguen cumpliendo todos los demás filtros. "
                    + "En el fallback la similitud es prioritaria y DISTANCIA o REPUTACION solo desempatan; "
                    + "el orden se aplica globalmente antes de paginar.")
    @Parameters({
            @Parameter(name = "q", description = "Texto parcial para nombre, descripción, marca o color. "
                    + "Si no hay coincidencias directas, activa similares conservando los demás filtros."),
            @Parameter(name = "tipo", description = "Tipo exacto de prenda"),
            @Parameter(name = "color", description = "Color exacto sin distinguir mayúsculas/minúsculas"),
            @Parameter(name = "talla", description = "Talla con unidades disponibles"),
            @Parameter(name = "precioMin", description = "Precio mínimo inclusivo"),
            @Parameter(name = "precioMax", description = "Precio máximo inclusivo"),
            @Parameter(name = "marca", description = "Marca exacta sin distinguir mayúsculas/minúsculas"),
            @Parameter(name = "estilo", description = "Estilo exacto"),
            @Parameter(
                    name = "orden",
                    description = "Orden opcional de los resultados",
                    schema = @Schema(allowableValues = { "DISTANCIA", "REPUTACION" })),
            @Parameter(
                    name = "latitudUsuario",
                    description = "Latitud entre -90 y 90; DISTANCIA requiere ambas coordenadas"),
            @Parameter(
                    name = "longitudUsuario",
                    description = "Longitud entre -180 y 180; DISTANCIA requiere ambas coordenadas"),
            @Parameter(name = "page", description = "Página desde cero; por defecto 0"),
            @Parameter(name = "size", description = "Tamaño de página entre 1 y 100; por defecto 20")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsqueda procesada correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    @GetMapping("/buscar")
    ResponseEntity<PageResponseDTO<CatalogoItemResponseDTO>> buscar(
            @Valid @ParameterObject @ModelAttribute BusquedaCatalogoRequestDTO request);
}
