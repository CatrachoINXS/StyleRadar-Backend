package edu.dosw.proyecto.style_radar.model.domain;

import org.springframework.data.domain.Page;

public record ResultadoRecomendaciones(Page<ItemCatalogo> pagina, boolean generalSinPreferencias) {
}
