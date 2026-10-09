package edu.dosw.proyecto.style_radar.model.dto.request;

import edu.dosw.proyecto.style_radar.model.domain.CategoriaAlmacen;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultaAlmacenRequestDTO extends PaginacionAlmacenRequestDTO {
    private CategoriaAlmacen categoria;
}
