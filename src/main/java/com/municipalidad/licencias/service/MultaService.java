package com.municipalidad.licencias.service;

import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.MultaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class MultaService {

    private final MultaRepository multaRepo;
    private final LicenciaService licenciaService;
    private final NotificacionService notificacionService;

    public MultaService(MultaRepository multaRepo,
                        LicenciaService licenciaService,
                        NotificacionService notificacionService) {
        this.multaRepo           = multaRepo;
        this.licenciaService     = licenciaService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Multa registrar(Long licenciaId, String descripcion,
                           BigDecimal monto, Usuario inspector) {
        if (descripcion == null || descripcion.isBlank())
            throw new IllegalArgumentException("La descripción es obligatoria.");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El monto debe ser mayor a 0.");

        Licencia licencia = licenciaService.obtenerPorId(licenciaId);

        Multa multa = Multa.builder()
            .licencia(licencia)
            .inspector(inspector)
            .descripcion(descripcion)
            .monto(monto)
            .build();
        multaRepo.save(multa);

        // Notificar al negocio (verificar que la solicitud y usuario estén cargados)
        try {
            com.municipalidad.licencias.model.Solicitud sol = licencia.getSolicitud();
            if (sol != null && sol.getUsuario() != null) {
                notificacionService.crear(sol.getUsuario(),
                    "Se registró una multa en tu establecimiento",
                    "El inspector " + inspector.getNombreCompleto() +
                    " registró una multa de S/ " + monto +
                    ". Descripción: " + descripcion,
                    "/multas/" + multa.getId() + "/detalle");
            }
        } catch (Exception e) {
            // log pero no fallar el registro de la multa
        }

        return multa;
    }

    @org.springframework.transaction.annotation.Transactional
    public Multa pagarMulta(Long multaId) {
        Multa multa = multaRepo.findById(multaId)
            .orElseThrow(() -> new IllegalArgumentException("Multa no encontrada"));
        if (multa.getEstado() == Multa.EstadoMulta.PAGADA)
            throw new IllegalStateException("Esta multa ya fue pagada.");
        multa.setEstado(Multa.EstadoMulta.PAGADA);
        multaRepo.save(multa);
        // Notificar al negocio
        try {
            com.municipalidad.licencias.model.Solicitud sol = multa.getLicencia().getSolicitud();
            if (sol != null && sol.getUsuario() != null) {
                notificacionService.crear(sol.getUsuario(),
                    "Multa pagada",
                    "Tu multa de S/ " + multa.getMonto() + " ha sido registrada como pagada.");
            }
        } catch (Exception ignored) {}
        return multa;
    }

    public Multa obtenerPorId(Long id) {
        return multaRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Multa no encontrada"));
    }

    public List<Multa> obtenerPorLicencia(Long licenciaId) {
        Licencia licencia = licenciaService.obtenerPorId(licenciaId);
        return multaRepo.findByLicenciaOrderByCreadoEnDesc(licencia);
    }

    public List<Multa> obtenerTodas() {
        return multaRepo.findAllByOrderByCreadoEnDesc();
    }
}
