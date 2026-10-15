package edu.dosw.proyecto.style_radar.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.mapper.PrendaEntityMapper;
import edu.dosw.proyecto.style_radar.repository.PrendaRepository;
import edu.dosw.proyecto.style_radar.service.IPrendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j
public class PrendaServiceImpl implements IPrendaService {

	private final PrendaRepository prendaRepository;
	private final PrendaEntityMapper prendaEntityMapper;

	@Override
	public List<Prenda> obtenerPrendas() {
		log.info("Consultando todas las prendas del catálogo");
		List<Prenda> prendas = prendaRepository.findPublicas().stream()
				.map(prendaEntityMapper::toDomain)
				.toList();
		log.info("Consulta de prendas completada. Total encontrado: {}", prendas.size());
		return prendas;
	}
    
}
