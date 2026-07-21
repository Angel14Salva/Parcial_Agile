package com.municipalidad.licencias.service;

import com.municipalidad.licencias.model.CajaSesion;
import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Usuario;
import com.municipalidad.licencias.repository.CajaSesionRepository;
import com.municipalidad.licencias.repository.FacturaCajaRepository;
import com.municipalidad.licencias.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaSesionService {

    private static final List<Enums.EstadoSesionCaja> ESTADOS_ACTIVOS =
        List.of(Enums.EstadoSesionCaja.ABIERTA, Enums.EstadoSesionCaja.PENDIENTE_APROBACION);

    private final CajaSesionRepository cajaSesionRepo;
    private final FacturaCajaRepository facturaRepo;
    private final UsuarioRepository usuarioRepo;
    private final NotificacionService notificacionService;

    public CajaSesionService(CajaSesionRepository cajaSesionRepo, FacturaCajaRepository facturaRepo,
                              UsuarioRepository usuarioRepo, NotificacionService notificacionService) {
        this.cajaSesionRepo = cajaSesionRepo;
        this.facturaRepo = facturaRepo;
        this.usuarioRepo = usuarioRepo;
        this.notificacionService = notificacionService;
    }

    public Optional<CajaSesion> obtenerSesionActual(Usuario cajero) {
        return cajaSesionRepo.findFirstByCajeroAndEstadoInOrderByFechaAperturaDesc(cajero, ESTADOS_ACTIVOS);
    }

    @Transactional
    public CajaSesion abrir(Usuario cajero, BigDecimal montoApertura) {
        if (montoApertura == null || montoApertura.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("El monto de apertura no puede ser negativo.");
        if (obtenerSesionActual(cajero).isPresent())
            throw new IllegalStateException("Ya tienes una caja abierta o pendiente de revisión.");
        return cajaSesionRepo.save(CajaSesion.builder().cajero(cajero).montoApertura(montoApertura).build());
    }

    private BigDecimal calcularMontoEsperado(CajaSesion sesion) {
        BigDecimal efectivo = facturaRepo.sumarPorCajeroYMetodo(
            sesion.getCajero(), Enums.MetodoPago.EFECTIVO, Enums.EstadoFactura.PAGADA,
            sesion.getFechaApertura(), LocalDateTime.now());
        return sesion.getMontoApertura().add(efectivo);
    }

    @Transactional
    public CajaSesion cerrar(Usuario cajero, BigDecimal montoContado, String observaciones) {
        if (montoContado == null || montoContado.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("El monto contado no puede ser negativo.");
        CajaSesion sesion = obtenerSesionActual(cajero)
            .orElseThrow(() -> new IllegalStateException("No tienes ninguna caja abierta."));
        if (sesion.getEstado() == Enums.EstadoSesionCaja.PENDIENTE_APROBACION)
            throw new IllegalStateException("El cierre de esta caja ya está en revisión por el administrador.");

        BigDecimal montoEsperado = calcularMontoEsperado(sesion);
        BigDecimal diferencia = montoContado.subtract(montoEsperado);
        sesion.setMontoCierreContado(montoContado);
        sesion.setMontoCierreEsperado(montoEsperado);
        sesion.setDiferencia(diferencia);
        sesion.setObservacionesCajero(observaciones);

        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            sesion.setEstado(Enums.EstadoSesionCaja.CERRADA);
            sesion.setFechaCierre(LocalDateTime.now());
        } else {
            sesion.setEstado(Enums.EstadoSesionCaja.PENDIENTE_APROBACION);
            String signo = diferencia.compareTo(BigDecimal.ZERO) > 0 ? "sobrante" : "faltante";
            for (Usuario admin : usuarioRepo.findByRol(Enums.Rol.ADMIN)) {
                notificacionService.crear(admin,
                    "Cierre de caja con inconsistencia",
                    "El cajero " + cajero.getNombreCompleto() + " intentó cerrar caja con un " + signo +
                        " de S/ " + diferencia.abs() + " (esperado S/ " + montoEsperado + ", contado S/ " + montoContado + ").",
                    "/admin/cajas/pendientes");
            }
        }
        return cajaSesionRepo.save(sesion);
    }

    public List<CajaSesion> obtenerPendientesAprobacion() {
        return cajaSesionRepo.findByEstadoOrderByFechaAperturaAsc(Enums.EstadoSesionCaja.PENDIENTE_APROBACION);
    }

    private CajaSesion obtenerPendiente(Long id) {
        CajaSesion sesion = cajaSesionRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sesión de caja no encontrada: " + id));
        if (sesion.getEstado() != Enums.EstadoSesionCaja.PENDIENTE_APROBACION)
            throw new IllegalStateException("Esta sesión de caja ya fue resuelta.");
        return sesion;
    }

    @Transactional
    public void aprobar(Long id, Usuario admin, String comentario) {
        CajaSesion sesion = obtenerPendiente(id);
        sesion.setEstado(Enums.EstadoSesionCaja.CERRADA);
        sesion.setFechaCierre(LocalDateTime.now());
        sesion.setRevisadoPor(admin);
        sesion.setFechaRevision(LocalDateTime.now());
        sesion.setComentarioAdmin(comentario);
        cajaSesionRepo.save(sesion);
        notificacionService.crear(sesion.getCajero(),
            "Cierre de caja aprobado",
            "El administrador aprobó el cierre de tu caja con una diferencia de S/ " + sesion.getDiferencia() + "." +
                (comentario != null && !comentario.isBlank() ? " Comentario: " + comentario : ""));
    }

    @Transactional
    public void rechazar(Long id, Usuario admin, String comentario) {
        CajaSesion sesion = obtenerPendiente(id);
        sesion.setEstado(Enums.EstadoSesionCaja.ABIERTA);
        sesion.setMontoCierreContado(null);
        sesion.setMontoCierreEsperado(null);
        sesion.setDiferencia(null);
        sesion.setRevisadoPor(admin);
        sesion.setFechaRevision(LocalDateTime.now());
        sesion.setComentarioAdmin(comentario);
        cajaSesionRepo.save(sesion);
        notificacionService.crear(sesion.getCajero(),
            "Cierre de caja rechazado",
            "El administrador rechazó el cierre de tu caja. Vuelve a contar el efectivo e intenta cerrar de nuevo." +
                (comentario != null && !comentario.isBlank() ? " Motivo: " + comentario : ""));
    }
}
