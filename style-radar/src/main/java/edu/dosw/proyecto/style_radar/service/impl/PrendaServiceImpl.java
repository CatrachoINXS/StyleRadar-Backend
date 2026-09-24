package edu.dosw.proyecto.style_radar.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import edu.dosw.proyecto.style_radar.model.domain.Prenda;
import edu.dosw.proyecto.style_radar.service.IPrendaService;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PrendaServiceImpl implements IPrendaService {

    private final Map<Long, Prenda> prendas = new HashMap<>();

	@Override
	public List<Prenda> obtenerPrendas() {
		return prendas.values().stream().toList();
	}
    
}
