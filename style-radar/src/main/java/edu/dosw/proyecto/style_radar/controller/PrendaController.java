package edu.dosw.proyecto.style_radar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.proyecto.style_radar.controller.docs.PrendaApi;
import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.model.dto.response.PrendaResponseDTO;
import edu.dosw.proyecto.style_radar.service.IPrendaService;
import edu.dosw.proyecto.style_radar.mapper.PrendaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/api/v1/prendas")
@RequiredArgsConstructor 
@Slf4j 

public class PrendaController implements PrendaApi {

    private final IPrendaService prendaService;
    private final PrendaMapper prendaMapper;
    
    @Override
    public ResponseEntity<List<PrendaResponseDTO>> obtenerPrendas() {
        log.info("GET /api/v1/prendas");
        List<Prenda> prendas = prendaService.obtenerPrendas();
        
        return ResponseEntity.ok(
            prendas.stream().map(prendaMapper::toResponse).toList()
        );
    }
}
