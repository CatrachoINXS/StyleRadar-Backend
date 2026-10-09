package edu.dosw.proyecto.style_radar.validator;

import org.springframework.stereotype.Component;
import edu.dosw.proyecto.style_radar.exception.ReglaDeNegocioException;
import edu.dosw.proyecto.style_radar.model.domain.Almacen;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AlmacenValidator {
    public static boolean coordenadasValidas(Double latitud, Double longitud) {
        return latitud != null && longitud != null
                && Double.isFinite(latitud) && Double.isFinite(longitud)
                && latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180;
    }

    public void validarCoordenadas(Almacen almacen) {
        if (!coordenadasValidas(almacen.getLatitud(), almacen.getLongitud())) {
            log.warn("Almacén sin coordenadas geográficas válidas: {}", almacen.getNit());
            throw new ReglaDeNegocioException("El almacén no dispone de coordenadas geográficas válidas y completas");
        }
    }
}
