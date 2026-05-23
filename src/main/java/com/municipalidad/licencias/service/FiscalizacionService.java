package com.municipalidad.licencias.service;

import com.municipalidad.licencias.dto.InspeccionOficioDto;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.InspeccionRepository;
import com.municipalidad.licencias.repository.LicenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class FiscalizacionService {

    private final InspeccionRepository inspeccionRepo;
    private final LicenciaRepository   licenciaRepo;
    private final LicenciaService      licenciaService;

    public FiscalizacionService(InspeccionRepository inspeccionRepo,
                                LicenciaRepository licenciaRepo,
                                LicenciaService licenciaService) {
        this.inspeccionRepo  = inspeccionRepo;
        this.licenciaRepo    = licenciaRepo;
        this.licenciaService = licenciaService;
    }

    @Transactional
    public Inspeccion registrarInspeccionOficio(Long licenciaId,
                                                InspeccionOficioDto dto,
                                                Usuario inspector) {
        Licencia licencia = licenciaService.obtenerPorId(licenciaId);
        if (licencia.getEstado() == Enums.EstadoLicencia.REVOCADA)
            throw new IllegalStateException("La licencia ya está revocada.");

        String notas = dto.getNotas() != null ? dto.getNotas() : "";
        if (!dto.isLicenciaVisible())
            notas = "[ALERTA: Licencia no visible en el local] " + notas;
        if (dto.getResultado() == Enums.ResultadoInspeccion.CON_OBSERVACIONES
            && dto.getDescripcionInfraccion() != null)
            notas = notas + " | MULTA: " + dto.getDescripcionInfraccion();

        Inspeccion inspeccion = Inspeccion.builder()
            .licencia(licencia).inspector(inspector)
            .tipo(Enums.TipoInspeccion.OFICIO)
            .fechaProgramada(LocalDate.now())
            .fechaRealizada(LocalDateTime.now())
            .resultado(dto.getResultado())
            .notasInspector(notas)
            .licenciaVisible(dto.isLicenciaVisible())
            .build();
        return inspeccionRepo.save(inspeccion);
    }

    @Transactional
    public Licencia revocarLicencia(Long licenciaId, String motivo) {
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("Debe especificar el motivo de la revocación.");
        return licenciaService.revocar(licenciaId, motivo);
    }
}
