package edu.dosw.proyecto.style_radar.controller.docs;

import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.dosw.proyecto.style_radar.model.dto.request.*;
import edu.dosw.proyecto.style_radar.model.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Descubrimiento de almacenes", description = "Lectura pública de almacenes y catálogo")
public interface AlmacenApi {
    @Operation(summary = "Consultar almacenes geolocalizables",
            description = "Coordenadas completas y válidas. Categoría comercial opcional; page desde 0, "
                    + "size de 1 a 100 (20 por defecto). Orden nombreComercial ASC, nit ASC. "
                    + "Actualmente no existe verificación de aliados; RF62 deberá filtrar los aprobados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de almacenes, incluso vacía"),
        @ApiResponse(responseCode = "400", description = "Categoría o paginación inválida"),
        @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    @GetMapping
    ResponseEntity<PageResponseDTO<AlmacenResponseDTO>> consultar(
            @Valid @ParameterObject @ModelAttribute ConsultaAlmacenRequestDTO request);

    @Operation(summary = "Consultar resumen público del catálogo",
            description = "Muestra provisional de publicaciones disponibles más recientes, sin selección editorial. "
                    + "Orden fechaPublicacion DESC, itemId ASC. limite de 1 a 20, por defecto 5. "
                    + "Excluye ítems retirados y stock efectivo cero.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Muestra de catálogo, incluso vacía"),
        @ApiResponse(responseCode = "400", description = "Límite inválido"),
        @ApiResponse(responseCode = "404", description = "Almacén inexistente"),
        @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    @GetMapping("/{nit}/catalogo/resumen")
    ResponseEntity<List<CatalogoItemResponseDTO>> resumen(@PathVariable String nit,
            @Valid @ParameterObject @ModelAttribute ResumenAlmacenRequestDTO request);

    @Operation(summary = "Consultar novedades semanales",
            description = "Ventana móvil UTC [Clock.instant() - 7 días, Clock.instant()], extremos inclusivos. "
                    + "Filtra fechaPublicacion y stock efectivo, sin depender de NUEVA_PRENDA. "
                    + "Orden fechaPublicacion DESC, itemId ASC. page desde 0; size de 1 a 100, por defecto 20.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de novedades, incluso vacía"),
        @ApiResponse(responseCode = "400", description = "Paginación inválida"),
        @ApiResponse(responseCode = "404", description = "Almacén inexistente"),
        @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    @GetMapping("/{nit}/catalogo/novedades")
    ResponseEntity<PageResponseDTO<CatalogoItemResponseDTO>> novedades(@PathVariable String nit,
            @Valid @ParameterObject @ModelAttribute PaginacionAlmacenRequestDTO request);

    @Operation(summary = "Estimar distancia geográfica al almacén",
            description = "Haversine en kilómetros en línea recta, presentado con dos decimales. "
                    + "Ambas coordenadas del usuario son obligatorias y finitas: latitud [-90,90], "
                    + "longitud [-180,180].")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distancia estimada en línea recta"),
        @ApiResponse(responseCode = "400", description = "Coordenadas del usuario inválidas o ausentes"),
        @ApiResponse(responseCode = "404", description = "Almacén inexistente"),
        @ApiResponse(responseCode = "422", description = "Almacén sin coordenadas válidas y completas"),
        @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    @GetMapping("/{nit}/distancia")
    ResponseEntity<DistanciaAlmacenResponseDTO> distancia(@PathVariable String nit,
            @Valid @ParameterObject @ModelAttribute DistanciaAlmacenRequestDTO request);
}
