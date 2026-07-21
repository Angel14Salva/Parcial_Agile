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

    private static final List<Enums.EstadoSesionCaja> ESTADOS_ACTIVOS = List.of(
        Enums.EstadoSesionCaja.PENDIENTE_APERTURA,
        Enums.EstadoSesionCaja.ABIERTA,
        Enums.EstadoSesionCaja.PENDIENTE_CIERRE);

    private static final List<Enums.EstadoSesionCaja> ESTADOS_PENDIENTES = List.of(
        Enums.EstadoSesionCaja.PENDIENTE_APERTURA,
        Enums.EstadoSesionCaja.PENDIENTE_CIERRE);

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

    /** Sesión en curso del cajero (solicitud de apertura, abierta, o solicitud de cierre). */
    public Optional<CajaSesion> obtenerSesionActual(Usuario cajero) {
        return cajaSesionRepo.findFirstByCajeroAndEstadoInOrderByFechaAperturaDesc(cajero, ESTADOS_ACTIVOS);
    }

    public boolean tieneCajaAbierta(Usuario cajero) {
        return obtenerSesionActual(cajero)
            .map(s -> s.getEstado() == Enums.EstadoSesionCaja.ABIERTA)
            .orElse(false);
    }

    @Transactional
    public CajaSesion solicitarApertura(Usuario cajero) {
        if (obtenerSesionActual(cajero).isPresent())
            throw new IllegalStateException("Ya tienes una caja abierta o una solicitud en curso.");

        CajaSesion sesion = cajaSesionRepo.save(CajaSesion.builder().cajero(cajero).build());

        for (Usuario admin : usuarioRepo.findByRol(Enums.Rol.ADMIN)) {
            notificacionService.crear(admin,
                "Solicitud de apertura de caja",
                "El cajero " + cajero.getNombreCompleto() + " solicita abrir caja. Debes asignar el monto inicial en efectivo.",
                "/admin/cajas/pendientes");
        }
        return sesion;
    }

    /** Monto inicial + efectivo cobrado hasta ahora en esta sesion (para mostrar en vivo en el panel). */
    public BigDecimal calcularMontoEsperado(CajaSesion sesion) {
        BigDecimal efectivo = facturaRepo.sumarPorCajeroYMetodo(
            sesion.getCajero(), Enums.MetodoPago.EFECTIVO, Enums.EstadoFactura.PAGADA,
            sesion.getFechaApertura(), LocalDateTime.now());
        return sesion.getMontoApertura().add(efectivo);
    }

    @Transactional
    public CajaSesion solicitarCierre(Usuario cajero, BigDecimal montoContado, String observaciones) {
        if (montoContado == null || montoContado.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("El monto contado no puede ser negativo.");
        CajaSesion sesion = obtenerSesionActual(cajero)
            .orElseThrow(() -> new IllegalStateException("No tienes ninguna caja abierta."));
        if (sesion.getEstado() != Enums.EstadoSesionCaja.ABIERTA)
            throw new IllegalStateException("Tu caja todavía no ha sido aprobada o ya tiene un cierre en revisión.");

        BigDecimal montoEsperado = calcularMontoEsperado(sesion);
        BigDecimal diferencia = montoContado.subtract(montoEsperado);
        sesion.setMontoCierreContado(montoContado);
        sesion.setMontoCierreEsperado(montoEsperado);
        sesion.setDiferencia(diferencia);
        sesion.setObservacionesCajero(observaciones);
        sesion.setEstado(Enums.EstadoSesionCaja.PENDIENTE_CIERRE);
        sesion = cajaSesionRepo.save(sesion);

        String detalle = diferencia.compareTo(BigDecimal.ZERO) == 0
            ? "El monto contado coincide con lo esperado (S/ " + montoEsperado + ")."
            : "Hay una diferencia de S/ " + diferencia.abs() + " (esperado S/ " + montoEsperado +
                ", contado S/ " + montoContado + ").";
        for (Usuario admin : usuarioRepo.findByRol(Enums.Rol.ADMIN)) {
            notificacionService.crear(admin,
                "Solicitud de cierre de caja",
                "El cajero " + cajero.getNombreCompleto() + " solicita cerrar caja. " + detalle,
                "/admin/cajas/pendientes");
        }
        return sesion;
    }

    public List<CajaSesion> obtenerPendientesAprobacion() {
        return cajaSesionRepo.findByEstadoInOrderByFechaAperturaAsc(ESTADOS_PENDIENTES);
    }

    private CajaSesion obtenerPendiente(Long id) {
        CajaSesion sesion = cajaSesionRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sesión de caja no encontrada: " + id));
        if (!ESTADOS_PENDIENTES.contains(sesion.getEstado()))
            throw new IllegalStateException("Esta solicitud ya fue resuelta.");
        return sesion;
    }

    @Transactional
    public void aprobar(Long id, Usuario admin, BigDecimal montoApertura, String comentario) {
        CajaSesion sesion = obtenerPendiente(id);
        sesion.setRevisadoPor(admin);
        sesion.setFechaRevision(LocalDateTime.now());
        sesion.setComentarioAdmin(comentario);

        if (sesion.getEstado() == Enums.EstadoSesionCaja.PENDIENTE_APERTURA) {
            if (montoApertura == null || montoApertura.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Debes indicar el monto en efectivo que se le entrega al cajero.");
            sesion.setMontoApertura(montoApertura);
            sesion.setEstado(Enums.EstadoSesionCaja.ABIERTA);
            sesion.setFechaApertura(LocalDateTime.now());
            cajaSesionRepo.save(sesion);
            notificacionService.crear(sesion.getCajero(),
                "Apertura de caja aprobada",
                "El administrador aprobó la apertura de tu caja con S/ " + montoApertura + ". Ya puedes operar." +
                    comentarioSuffix(comentario));
        } else {
            sesion.setEstado(Enums.EstadoSesionCaja.CERRADA);
            sesion.setFechaCierre(LocalDateTime.now());
            cajaSesionRepo.save(sesion);
            notificacionService.crear(sesion.getCajero(),
                "Cierre de caja aprobado",
                "El administrador aprobó el cierre de tu caja." + comentarioSuffix(comentario));
        }
    }

    @Transactional
    public void rechazar(Long id, Usuario admin, String comentario) {
        CajaSesion sesion = obtenerPendiente(id);
        sesion.setRevisadoPor(admin);
        sesion.setFechaRevision(LocalDateTime.now());
        sesion.setComentarioAdmin(comentario);

        if (sesion.getEstado() == Enums.EstadoSesionCaja.PENDIENTE_APERTURA) {
            sesion.setEstado(Enums.EstadoSesionCaja.RECHAZADA);
            cajaSesionRepo.save(sesion);
            notificacionService.crear(sesion.getCajero(),
                "Apertura de caja rechazada",
                "El administrador rechazó tu solicitud de apertura de caja. Puedes enviar una nueva solicitud." +
                    comentarioSuffix(comentario));
        } else {
            sesion.setEstado(Enums.EstadoSesionCaja.ABIERTA);
            sesion.setMontoCierreContado(null);
            sesion.setMontoCierreEsperado(null);
            sesion.setDiferencia(null);
            cajaSesionRepo.save(sesion);
            notificacionService.crear(sesion.getCajero(),
                "Cierre de caja rechazado",
                "El administrador rechazó tu solicitud de cierre. Vuelve a contar el efectivo e intenta de nuevo." +
                    comentarioSuffix(comentario));
        }
    }

    private String comentarioSuffix(String comentario) {
        return comentario != null && !comentario.isBlank() ? " Comentario: " + comentario : "";
    }
}
