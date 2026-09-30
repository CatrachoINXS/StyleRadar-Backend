package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.UsuarioApi;
import edu.dosw.proyecto.style_radar.mapper.BusquedaGuardadaMapper;
import edu.dosw.proyecto.style_radar.mapper.UsuarioMapper;
import edu.dosw.proyecto.style_radar.model.domain.BusquedaGuardada;
import edu.dosw.proyecto.style_radar.model.domain.Usuario;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarPreferenciasEstiloRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.ActualizarTallasHabitualesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.GuardarBusquedaRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.RegistrarUsuarioRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.BusquedaGuardadaResponseDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.UsuarioResponseDTO;
import edu.dosw.proyecto.style_radar.service.IUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController implements UsuarioApi {

    private final IUsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;
    private final BusquedaGuardadaMapper busquedaGuardadaMapper;

    @Override
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistrarUsuarioRequestDTO request) {
        Usuario usuario = usuarioMapper.toDomain(request);
        Usuario registrado = usuarioService.registrarUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioMapper.toResponse(registrado));
    }

    @Override
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
    }

    @Override
    public ResponseEntity<UsuarioResponseDTO> actualizarPreferenciasEstilo(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPreferenciasEstiloRequestDTO request) {
        Usuario actualizado = usuarioService.actualizarPreferenciasEstilo(id, request.getPreferencias());
        return ResponseEntity.ok(usuarioMapper.toResponse(actualizado));
    }

    @Override
    public ResponseEntity<UsuarioResponseDTO> actualizarTallasHabituales(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarTallasHabitualesRequestDTO request) {
        Usuario actualizado = usuarioService.actualizarTallasHabituales(id, request.getTallas());
        return ResponseEntity.ok(usuarioMapper.toResponse(actualizado));
    }

    @Override
    public ResponseEntity<BusquedaGuardadaResponseDTO> guardarBusqueda(
            @PathVariable Long id,
            @Valid @RequestBody GuardarBusquedaRequestDTO request) {
        BusquedaGuardada busqueda = busquedaGuardadaMapper.toDomain(request);
        BusquedaGuardada guardada = usuarioService.guardarBusqueda(id, busqueda);
        return ResponseEntity.status(HttpStatus.CREATED).body(busquedaGuardadaMapper.toResponse(guardada));
    }

    @Override
    public ResponseEntity<List<BusquedaGuardadaResponseDTO>> obtenerBusquedasGuardadas(@PathVariable Long id) {
        List<BusquedaGuardadaResponseDTO> busquedas = usuarioService.obtenerBusquedasGuardadas(id).stream()
                .map(busquedaGuardadaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(busquedas);
    }

    @Override
    public ResponseEntity<Void> eliminarBusquedaGuardada(
            @PathVariable Long id,
            @PathVariable Long busquedaId) {
        usuarioService.eliminarBusquedaGuardada(id, busquedaId);
        return ResponseEntity.noContent().build();
    }
}
