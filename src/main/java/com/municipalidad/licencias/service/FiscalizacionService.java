package com.municipalidad.licencias.service;

import com.municipalidad.licencias.dto.InspeccionOficioDto;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.Repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FiscalizacionService {

    private final InspeccionRepository inspeccionRepo;
    private final LicenciaRepository licenciaRepo;
    private final LicenciaService licenciaService;

    // ── E6-US01: Registrar inspección de oficio ───────────────────────────────
    @Transactional
    public Inspeccion registrarInspeccionOficio(Long licenciaId,
                                                 InspeccionOficioDto dto,
                                                 Usuario inspector) {
        Licencia licencia = licenciaService.obtenerPorId(licenciaId);

        if (licencia.getEstado() == Enums.EstadoLicencia.REVOCADA) {
            throw new IllegalStateException("La licencia ya está revocada.");
        }

        Inspeccion inspeccion = Inspeccion.builder()
            .licencia(licencia)
            .inspector(inspector)
            .tipo(Enums.TipoInspeccion.OFICIO)
            .fechaProgramada(LocalDate.now())
            .fechaRealizada(LocalDateTime.now())
            .resultado(dto.getResultado())
            .notasInspector(dto.getNotas())
            .licenciaVisible(dto.isLicenciaVisible())
            .build();

        // E6-US04: si la licencia no está visible, queda como alerta en las notas
        if (!dto.isLicenciaVisible()) {
            String nota = "[ALERTA: Licencia no visible en el local] ";
            inspeccion.setNotasInspector(nota + (dto.getNotas() != null ? dto.getNotas() : ""));
        }

        // E6-US02: si hay infracción → registrar motivo de multa en las notas
        if (dto.getResultado() == Enums.ResultadoInspeccion.CON_OBSERVACIONES
            && dto.getDescripcionInfraccion() != null) {
            inspeccion.setNotasInspector(
                inspeccion.getNotasInspector() + " | MULTA: " + dto.getDescripcionInfraccion());
        }

        return inspeccionRepo.save(inspeccion);
    }

    // ── E6-US03: Revocar licencia ─────────────────────────────────────────────
    @Transactional
    public Licencia revocarLicencia(Long licenciaId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Debe especificar el motivo de la revocación.");
        }
        return licenciaService.revocar(licenciaId, motivo);
    }
}
