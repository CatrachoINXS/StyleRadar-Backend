package edu.dosw.proyecto.style_radar.service.impl;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.proyecto.style_radar.mapper.FundacionEntityMapper;
import edu.dosw.proyecto.style_radar.model.domain.Fundacion;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.TipoPrenda;
import edu.dosw.proyecto.style_radar.model.entity.FundacionEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.repository.FundacionRepository;
import edu.dosw.proyecto.style_radar.service.DistanciaCalculator;
import edu.dosw.proyecto.style_radar.service.IFundacionService;
import edu.dosw.proyecto.style_radar.validator.FundacionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundacionServiceImpl implements IFundacionService {

    private final FundacionRepository fundacionRepository;
    private final FundacionEntityMapper fundacionEntityMapper;
    private final FundacionValidator fundacionValidator;
    private final DistanciaCalculator distanciaCalculator;
    private final Clock clock;

    @Override
    @Transactional
    public Fundacion registrar(Fundacion fundacion) {
        log.info("Registrando fundación aliada '{}'", fundacion.getNombre());
        fundacionValidator.validarNombreUnico(fundacion.getNombre(), null);
        UsuarioEntity representante = fundacionValidator.validarRepresentante(fundacion.getRepresentanteId(), null);

        FundacionEntity entity = FundacionEntity.builder()
                .representante(representante)
                .activa(true)
                .fechaRegistro(clock.instant())
                .build();
        copiarDatos(fundacion, entity);
        asignarRolRepresentante(representante);

        FundacionEntity guardada = fundacionRepository.save(entity);
        log.info("Fundación registrada con ID: {} y representante ID: {}", guardada.getId(), representante.getId());
        return fundacionEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public Fundacion editar(Long fundacionId, Fundacion cambios) {
        log.info("Editando fundación ID: {}", fundacionId);
        FundacionEntity entity = fundacionValidator.validarYObtener(fundacionId);
        fundacionValidator.validarNombreUnico(cambios.getNombre(), fundacionId);

        UsuarioEntity anterior = entity.getRepresentante();
        if (!anterior.getId().equals(cambios.getRepresentanteId())) {
            UsuarioEntity nuevo = fundacionValidator.validarRepresentante(cambios.getRepresentanteId(), fundacionId);
            anterior.getRoles().remove(Rol.REPRESENTANTE_FUNDACION);
            asignarRolRepresentante(nuevo);
            entity.setRepresentante(nuevo);
            log.info("Representante de la fundación ID: {} cambiado de {} a {}", fundacionId, anterior.getId(), nuevo.getId());
        }
        copiarDatos(cambios, entity);

        FundacionEntity guardada = fundacionRepository.save(entity);
        log.info("Fundación ID: {} editada exitosamente", fundacionId);
        return fundacionEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional
    public Fundacion desactivar(Long fundacionId) {
        log.info("Desactivando fundación ID: {}", fundacionId);
        FundacionEntity entity = fundacionValidator.validarYObtener(fundacionId);
        fundacionValidator.validarActiva(entity);
        // Se conserva el registro: el historial de donaciones sigue referenciándola.
        entity.setActiva(false);
        FundacionEntity guardada = fundacionRepository.save(entity);
        log.info("Fundación ID: {} desactivada", fundacionId);
        return fundacionEntityMapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fundacion> listarTodas() {
        log.info("Listando todas las fundaciones aliadas");
        List<Fundacion> fundaciones = fundacionRepository.findAllByOrderByNombreAsc().stream()
                .map(fundacionEntityMapper::toDomain)
                .toList();
        log.info("Fundaciones encontradas: {}", fundaciones.size());
        return fundaciones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fundacion> consultarCercanas(double latitud, double longitud, TipoPrenda tipo) {
        log.info("Consultando fundaciones activas cercanas. Tipo de prenda: {}", tipo);
        // shortcut: distancia calculada en memoria sobre las activas; con miles de fundaciones usar consulta espacial (PostGIS).
        List<Fundacion> cercanas = fundacionRepository.findByActivaTrue().stream()
                .filter(f -> tipo == null || f.getCategoriasAceptadas().contains(tipo))
                .map(f -> {
                    Fundacion fundacion = fundacionEntityMapper.toDomain(f);
                    fundacion.setDistanciaKm(distanciaCalculator.calcularKm(latitud, longitud, f.getLatitud(), f.getLongitud()));
                    return fundacion;
                })
                .sorted(Comparator.comparingDouble(Fundacion::getDistanciaKm).thenComparing(Fundacion::getId))
                .toList();
        log.info("Fundaciones cercanas encontradas: {}", cercanas.size());
        return cercanas;
    }

    private void copiarDatos(Fundacion origen, FundacionEntity destino) {
        destino.setNombre(origen.getNombre().trim());
        destino.setDescripcion(origen.getDescripcion());
        destino.setDireccion(origen.getDireccion());
        destino.setTelefono(origen.getTelefono());
        destino.setEmail(origen.getEmail());
        destino.setLatitud(origen.getLatitud());
        destino.setLongitud(origen.getLongitud());
        destino.setCategoriasAceptadas(new HashSet<>(origen.getCategoriasAceptadas()));
    }

    private void asignarRolRepresentante(UsuarioEntity usuario) {
        if (usuario.getRoles().isEmpty()) {
            // Un set vacío equivale a COMPRADOR; se conserva al añadir el nuevo rol.
            usuario.getRoles().add(Rol.COMPRADOR);
        }
        usuario.getRoles().add(Rol.REPRESENTANTE_FUNDACION);
    }
}
