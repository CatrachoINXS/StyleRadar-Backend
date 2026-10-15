package edu.dosw.proyecto.style_radar.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.dosw.proyecto.style_radar.controller.docs.ProbadorVirtualApi;
import edu.dosw.proyecto.style_radar.model.domain.FotoProbador;
import edu.dosw.proyecto.style_radar.model.dto.request.PruebaVirtualRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.FotoProbadorResponseDTO;
import edu.dosw.proyecto.style_radar.security.UsuarioPrincipal;
import edu.dosw.proyecto.style_radar.service.IProbadorVirtualService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProbadorVirtualController implements ProbadorVirtualApi {

    private final IProbadorVirtualService probadorService;

    @Override
    public ResponseEntity<FotoProbadorResponseDTO> cargarFoto(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestPart("foto") MultipartFile foto) {
        try {
            FotoProbador cargada = probadorService.cargarFoto(principal.getUsuarioId(), foto.getContentType(), foto.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(cargada));
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible procesar la fotografía recibida", exception);
        }
    }

    @Override
    public ResponseEntity<FotoProbadorResponseDTO> obtenerFoto(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(toResponse(probadorService.obtenerFoto(principal.getUsuarioId())));
    }

    @Override
    public ResponseEntity<byte[]> probar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PruebaVirtualRequestDTO request) {
        byte[] png = probadorService.probar(principal.getUsuarioId(), request.getItemCatalogoId());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @Override
    public ResponseEntity<Void> eliminarFoto(@AuthenticationPrincipal UsuarioPrincipal principal) {
        probadorService.eliminarFoto(principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    private FotoProbadorResponseDTO toResponse(FotoProbador foto) {
        return FotoProbadorResponseDTO.builder().contentType(foto.getContentType())
                .ancho(foto.getAncho()).alto(foto.getAlto()).fechaCarga(foto.getFechaCarga()).build();
    }
}
