package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.FundacionApi;
import edu.dosw.proyecto.style_radar.mapper.FundacionMapper;
import edu.dosw.proyecto.style_radar.model.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.request.FundacionRequestDTO;
import edu.dosw.proyecto.style_radar.model.dto.response.FundacionResponseDTO;
import edu.dosw.proyecto.style_radar.service.IFundacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FundacionController implements FundacionApi {

    private final IFundacionService fundacionService;
    private final FundacionMapper fundacionMapper;

    @Override
    public ResponseEntity<FundacionResponseDTO> registrar(@Valid @RequestBody FundacionRequestDTO request) {
        var registrada = fundacionService.registrar(fundacionMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(fundacionMapper.toResponse(registrada));
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(fundacionService.listarTodas().stream().map(fundacionMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> editar(@PathVariable Long id, @Valid @RequestBody FundacionRequestDTO request) {
        return ResponseEntity.ok(fundacionMapper.toResponse(fundacionService.editar(id, fundacionMapper.toDomain(request))));
    }

    @Override
    public ResponseEntity<FundacionResponseDTO> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(fundacionMapper.toResponse(fundacionService.desactivar(id)));
    }

    @Override
    public ResponseEntity<List<FundacionResponseDTO>> consultarCercanas(
            @Valid @ModelAttribute ConsultaFundacionesRequestDTO request) {
        return ResponseEntity.ok(fundacionService
                .consultarCercanas(request.getLatitud(), request.getLongitud(), request.getTipo()).stream()
                .map(fundacionMapper::toResponse)
                .toList());
    }
}
