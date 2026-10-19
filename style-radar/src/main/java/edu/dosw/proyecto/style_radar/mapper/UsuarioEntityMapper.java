package edu.dosw.proyecto.style_radar.mapper;

import java.util.HashSet;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import edu.dosw.proyecto.style_radar.model.domain.EstadoCuenta;
import edu.dosw.proyecto.style_radar.model.domain.Rol;
import edu.dosw.proyecto.style_radar.model.domain.Talla;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioEntity;
import edu.dosw.proyecto.style_radar.model.entity.UsuarioTallaHabitualEntity;

@Mapper(componentModel = "spring")
public interface UsuarioEntityMapper {

    @Mapping(target = "busquedasGuardadas", ignore = true)
    @Mapping(target = "fechaCambioEstado", ignore = true)
    @Mapping(target = "administradorCambioEstadoId", ignore = true)
    @Mapping(target = "motivoCambioEstado", ignore = true)
    @Mapping(target = "tallasHabitualesPorTipo", ignore = true)
    UsuarioEntity toEntity(Usuario usuario);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesCompatibles")
    @Mapping(target = "estadoCuenta", source = "estadoCuenta", qualifiedByName = "estadoCompatible")
    @Mapping(target = "tallasHabituales", source = "tallasHabitualesPorTipo", qualifiedByName = "tallasHabitualesDeEntidad")
    Usuario toDomain(UsuarioEntity usuarioEntity);

    // Compatibilidad de lectura, sin alterar la entidad ni migrar datos implícitamente.
    @Named("rolesCompatibles")
    default Set<Rol> rolesCompatibles(Set<Rol> roles) {
        return roles == null || roles.isEmpty()
                ? new HashSet<>(Set.of(Rol.COMPRADOR)) : new HashSet<>(roles);
    }

    @Named("estadoCompatible")
    default EstadoCuenta estadoCompatible(EstadoCuenta estado) {
        return estado == null ? EstadoCuenta.ACTIVA : estado;
    }

    @Named("tallasHabitualesDeEntidad")
    default Set<Talla> tallasHabitualesDeEntidad(Set<UsuarioTallaHabitualEntity> tallasPorTipo) {
        if (tallasPorTipo == null || tallasPorTipo.isEmpty()) {
            return new HashSet<>();
        }
        return tallasPorTipo.stream()
                .map(UsuarioTallaHabitualEntity::getTalla)
                .collect(java.util.stream.Collectors.toSet());
    }
}
