package edu.dosw.proyecto.style_radar.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class RecomendacionesResponseDTO extends PageResponseDTO<CatalogoItemResponseDTO> {
    @Schema(description = "true indica una selección general reciente porque no hay estilos ni tallas configurados")
    private final boolean generalSinPreferencias;

    public RecomendacionesResponseDTO(PageResponseDTO<CatalogoItemResponseDTO> pagina, boolean generalSinPreferencias) {
        super(pagina.getContent(), pagina.getPage(), pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages(), pagina.isFirst(), pagina.isLast());
        this.generalSinPreferencias = generalSinPreferencias;
    }
}
