package com.municipalidad.licencias.controller;

import com.municipalidad.licencias.dto.*;
import com.municipalidad.licencias.model.*;
import com.municipalidad.licencias.repository.UsuarioRepository;
import com.municipalidad.licencias.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

// ── Auth ──────────────────────────────────────────────────────────────────────
@Controller
@RequestMapping("/auth")
class AuthController {

    @GetMapping("/login")
    String loginPage(@RequestParam(required = false) String error,
                     @RequestParam(required = false) String logout,
                     Model model) {
        if (error  != null) model.addAttribute("error", "Usuario o contraseña incorrectos.");
        if (logout != null) model.addAttribute("msg",   "Sesión cerrada correctamente.");
        return "auth/login";
    }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
@Controller
class DashboardController {

    private final UsuarioRepository usuarioRepo;
    private final SolicitudService solicitudService;
    private final InspeccionService inspeccionService;
    private final com.municipalidad.licencias.service.NotificacionService notificacionService;
    private final LicenciaService licenciaService;

    DashboardController(UsuarioRepository usuarioRepo,
                        SolicitudService solicitudService,
                        InspeccionService inspeccionService,
                        com.municipalidad.licencias.service.NotificacionService notificacionService,
                        LicenciaService licenciaService) {
        this.usuarioRepo         = usuarioRepo;
        this.solicitudService    = solicitudService;
        this.inspeccionService   = inspeccionService;
        this.notificacionService = notificacionService;
        this.licenciaService     = licenciaService;
    }

    @GetMapping("/dashboard")
    String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        Usuario usuario = getUsuario(ud);
        model.addAttribute("usuario", usuario);
        if (usuario.getRol() == Enums.Rol.SUBGERENTE) {
            return "redirect:/subgerente/dashboard";
        }
        if (usuario.getRol() == Enums.Rol.GERENTE_DISTRITAL) {
            return "redirect:/gerente/distrital";
        }
        if (usuario.getRol() == Enums.Rol.GERENTE_MUNICIPAL) {
            return "redirect:/gerente-municipal/dashboard";
        }
        if (usuario.getRol() == Enums.Rol.FISCALIZADOR) {
            return "redirect:/inspector/dashboard";
        }
        if (usuario.getRol() == Enums.Rol.INSPECTOR) {
            model.addAttribute("inspeccionesPendientes",
                inspeccionService.obtenerPendientesPorInspector(usuario));
            model.addAttribute("licenciasVigentes",
                licenciaService.obtenerLicenciasVigentes());
            model.addAttribute("licenciasRevocadas",
                licenciaService.obtenerLicenciasRevocadas());
            return "inspector/dashboard-inspector";
        } else {
            model.addAttribute("tramitesActivos", solicitudService.obtenerTramitesActivos());
            return "admin/dashboard-admin";
        }
    }

    Usuario getUsuario(UserDetails ud) {
        return usuarioRepo.findByUsername(ud.getUsername())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}

// ── Solicitud ─────────────────────────────────────────────────────────────────
@Controller
@RequestMapping("/solicitud")
class SolicitudController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SolicitudController.class);
    private final SolicitudService solicitudService;
    private final LicenciaService  licenciaService;
    private final UsuarioRepository usuarioRepo;
    private final com.municipalidad.licencias.service.FlowService flowService;
    private final com.municipalidad.licencias.service.MultaService multaServiceDet;

    SolicitudController(SolicitudService solicitudService,
                        LicenciaService licenciaService,
                        UsuarioRepository usuarioRepo,
                        com.municipalidad.licencias.service.FlowService flowService,
                        com.municipalidad.licencias.service.MultaService multaServiceDet) {
        this.solicitudService = solicitudService;
        this.licenciaService  = licenciaService;
        this.usuarioRepo      = usuarioRepo;
        this.flowService      = flowService;
        this.multaServiceDet  = multaServiceDet;
    }

    @GetMapping("/nueva")
    String nuevaForm(Model model) {
        model.addAttribute("solicitudDto", new SolicitudDto());
        model.addAttribute("rubros", Rubros.LISTA);
        return "solicitud/nueva";
    }

    @PostMapping("/nueva")
    String crearBorrador(@Valid @ModelAttribute SolicitudDto dto,
                         BindingResult errors,
                         @RequestParam(required = false) org.springframework.web.multipart.MultipartFile plano,
                         @RequestParam(required = false) org.springframework.web.multipart.MultipartFile firma,
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("rubros", Rubros.LISTA);
            return "solicitud/nueva";
        }
        if (plano == null || plano.isEmpty()) {
            model.addAttribute("rubros", Rubros.LISTA);
            model.addAttribute("errorPlano", "El plano del local es obligatorio.");
            return "solicitud/nueva";
        }
        if (firma == null || firma.isEmpty()) {
            model.addAttribute("rubros", Rubros.LISTA);
            model.addAttribute("errorFirma", "La firma del solicitante es obligatoria.");
            return "solicitud/nueva";
        }
        try {
            Usuario usuario = usuarioRepo.findByUsername("publico").orElseThrow();
            Solicitud s = solicitudService.crearBorrador(dto, usuario);
            // Guardar plano
            solicitudService.cargarPlano(s.getId(), plano);
            // Guardar firma
            solicitudService.cargarFirma(s.getId(), firma);
            return "redirect:/solicitud/" + s.getId() + "/pago";
        } catch (IllegalStateException e) {
            model.addAttribute("rubros", Rubros.LISTA);
            model.addAttribute("errorLicencia", e.getMessage());
            return "solicitud/nueva";
        } catch (Exception e) {
            model.addAttribute("rubros", Rubros.LISTA);
            model.addAttribute("errorGeneral", e.getMessage());
            return "solicitud/nueva";
        }
    }

    @GetMapping("/{id}/plano")
    String planoForm(@PathVariable Long id, Model model) {
        model.addAttribute("solicitud", solicitudService.obtenerPorId(id));
        return "solicitud/cargar-plano";
    }

    @PostMapping("/{id}/plano")
    String cargarPlano(@PathVariable Long id,
                       @RequestParam MultipartFile plano,
                       RedirectAttributes ra) {
        try {
            solicitudService.cargarPlano(id, plano);
            return "redirect:/solicitud/" + id + "/pago";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitud/" + id + "/plano";
        }
    }

    @GetMapping("/{id}/pago")
    String pagoForm(@PathVariable Long id, Model model) {
        model.addAttribute("solicitud", solicitudService.obtenerPorId(id));
        return "solicitud/pago";
    }

    @PostMapping("/{id}/pago/flow")
    String iniciarPagoFlow(@PathVariable Long id,
                           jakarta.servlet.http.HttpServletRequest request,
                           RedirectAttributes ra) {
        try {
            com.municipalidad.licencias.model.Solicitud s = solicitudService.obtenerPorId(id);
            String scheme = request.getHeader("X-Forwarded-Proto") != null ?
                request.getHeader("X-Forwarded-Proto") : request.getScheme();
            String host = request.getHeader("X-Forwarded-Host") != null ?
                request.getHeader("X-Forwarded-Host") : request.getServerName();
            String baseUrl = scheme + "://" + host;
            String urlRetorno      = baseUrl + "/pago/retorno/" + id;
            String urlConfirmacion = baseUrl + "/pago/confirmar/" + id;
            String email = s.getCorreoElectronico() != null && !s.getCorreoElectronico().isBlank() ?
                s.getCorreoElectronico() : "publico@licencias.gob.pe";
            String nombre = s.getNombreRepresentante() != null ? s.getNombreRepresentante() : "Ciudadano";
            com.municipalidad.licencias.service.FlowService.OrdenFlow orden =
                flowService.crearOrden(id, email, nombre, 2.0, urlRetorno, urlConfirmacion);
            solicitudService.guardarReferencia(id, orden.token());
            return "redirect:" + orden.url();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al conectar con Flow: " + e.getMessage());
            return "redirect:/solicitud/" + id + "/pago";
        }
    }

    @GetMapping("/{id}/pago/retorno")
    String retornoPago(@PathVariable Long id,
                       @RequestParam(required = false) String token,
                       org.springframework.ui.Model model,
                       RedirectAttributes ra) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null) {
                    int statusCode = estado.path("status").asInt();
                    if (statusCode == 2) {
                        com.municipalidad.licencias.model.Solicitud s = solicitudService.enviarConPago(id, token);
                        model.addAttribute("solicitud", s);
                        model.addAttribute("codigoSeguimiento", s.getCodigoSeguimiento());
                        return "solicitud/comprobante";
                    } else if (statusCode == 3) {
                        ra.addFlashAttribute("error", "El pago fue rechazado. Intenta nuevamente.");
                    } else {
                        ra.addFlashAttribute("error", "El pago está pendiente de confirmación.");
                    }
                }
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error verificando el pago: " + e.getMessage());
        }
        return "redirect:/solicitud/" + id + "/pago";
    }

    @PostMapping("/{id}/pago/confirmar")
    @org.springframework.web.bind.annotation.ResponseBody
    String confirmarPagoWebhook(@PathVariable Long id,
                                 @RequestParam(required = false) String token) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null && estado.path("status").asInt() == 2) {
                    solicitudService.enviarConPago(id, token);
                    log.info("Pago confirmado via webhook para solicitud {}", id);
                }
            }
        } catch (Exception e) {
            log.error("Error en webhook Flow solicitud {}: {}", id, e.getMessage());
        }
        return "OK";
    }


    @PostMapping("/{id}/pago")
    String procesarPago(@PathVariable Long id,
                        @RequestParam String referenciaPago,
                        RedirectAttributes ra) {
        try {
            solicitudService.enviarConPago(id, referenciaPago);
            ra.addFlashAttribute("exito", "Solicitud enviada. Se programó la inspección técnica.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitud/" + id + "/pago";
        }
    }

    @GetMapping("/{id}/detalle")
    String detalle(@PathVariable Long id, Model model) {
        Solicitud s = solicitudService.obtenerPorId(id);
        com.municipalidad.licencias.model.Licencia lic = licenciaService.obtenerPorSolicitud(s);
        model.addAttribute("solicitud", s);
        model.addAttribute("licencia", lic);
        if (lic != null) {
            model.addAttribute("multasLicencia",
                multaServiceDet.obtenerPorLicencia(lic.getId()));
        } else {
            model.addAttribute("multasLicencia", java.util.List.of());
        }
        return "solicitud/detalle";
    }
}

// ── Inspector ─────────────────────────────────────────────────────────────────
@Controller
@RequestMapping("/inspector")
class InspectorController {

    private final InspeccionService inspeccionService;
    private final LicenciaService   licenciaService;
    private final UsuarioRepository usuarioRepo;

    InspectorController(InspeccionService inspeccionService,
                        LicenciaService licenciaService,
                        UsuarioRepository usuarioRepo) {
        this.inspeccionService = inspeccionService;
        this.licenciaService   = licenciaService;
        this.usuarioRepo       = usuarioRepo;
    }

    @GetMapping("/inspeccion/{id}")
    String verInspeccion(@PathVariable Long id, Model model) {
        model.addAttribute("inspeccion", inspeccionService.obtenerPorId(id));
        model.addAttribute("dto", new ResultadoInspeccionDto());
        return "inspector/registrar-resultado";
    }

    @PostMapping("/inspeccion/{id}/resultado")
    String registrarResultado(@PathVariable Long id,
                              @Valid @ModelAttribute("dto") ResultadoInspeccionDto dto,
                              BindingResult errors,
                              RedirectAttributes ra, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("inspeccion", inspeccionService.obtenerPorId(id));
            return "inspector/registrar-resultado";
        }
        try {
            Inspeccion inspeccion = inspeccionService.registrarResultado(id, dto);
            if (inspeccion.getResultado() == Enums.ResultadoInspeccion.CONFORME) {
                licenciaService.emitirLicencia(inspeccion.getSolicitud());
                ra.addFlashAttribute("exito", "Trámite aprobado y licencia emitida.");
            } else {
                ra.addFlashAttribute("exito", "Observaciones registradas. Segunda inspección programada.");
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/inspector/inspeccion/" + id;
        }
    }
}

// ── Licencia ──────────────────────────────────────────────────────────────────
@Controller
@RequestMapping("/licencia")
class LicenciaController {

    private final LicenciaService licenciaService;

    LicenciaController(LicenciaService licenciaService) {
        this.licenciaService = licenciaService;
    }

    @GetMapping("/{id}/descargar")
    ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        try {
            byte[] pdf = licenciaService.generarPdf(id);
            Licencia licencia = licenciaService.obtenerPorId(id);
            String filename = "licencia-" + licencia.getNumeroLicencia() + ".pdf";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/renovar")
    String renovarForm(@PathVariable Long id, Model model) {
        model.addAttribute("licencia", licenciaService.obtenerPorId(id));
        return "licencia/renovar";
    }

    @PostMapping("/{id}/renovar")
    String procesarRenovacion(@PathVariable Long id,
                              @RequestParam String referenciaPago,
                              RedirectAttributes ra) {
        try {
            licenciaService.renovar(id, referenciaPago);
            ra.addFlashAttribute("exito", "Licencia renovada exitosamente por un año más.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }
}

// ── Fiscalización ─────────────────────────────────────────────────────────────
@Controller
@RequestMapping("/fiscalizacion")
class FiscalizacionController {

    private final FiscalizacionService fiscalizacionService;
    private final LicenciaService      licenciaService;
    private final UsuarioRepository    usuarioRepo;

    FiscalizacionController(FiscalizacionService fiscalizacionService,
                             LicenciaService licenciaService,
                             UsuarioRepository usuarioRepo) {
        this.fiscalizacionService = fiscalizacionService;
        this.licenciaService      = licenciaService;
        this.usuarioRepo          = usuarioRepo;
    }

    @GetMapping("/licencia/{id}/oficio")
    String inspeccionOficioForm(@PathVariable Long id, Model model) {
        model.addAttribute("licencia", licenciaService.obtenerPorId(id));
        model.addAttribute("dto", new InspeccionOficioDto());
        return "fiscalizacion/inspeccion-oficio";
    }

    @PostMapping("/licencia/{id}/oficio")
    String registrarOficio(@PathVariable Long id,
                           @Valid @ModelAttribute("dto") InspeccionOficioDto dto,
                           @AuthenticationPrincipal UserDetails ud,
                           RedirectAttributes ra) {
        try {
            Usuario inspector = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            fiscalizacionService.registrarInspeccionOficio(id, dto, inspector);
            ra.addFlashAttribute("exito", "Inspección de oficio registrada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/licencia/{id}/revocar")
    String revocar(@PathVariable Long id,
                   @RequestParam String motivo,
                   RedirectAttributes ra) {
        try {
            fiscalizacionService.revocarLicencia(id, motivo);
            ra.addFlashAttribute("exito", "Licencia revocada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }
}

// ── Catálogo de rubros ────────────────────────────────────────────────────────
class Rubros {
    static final List<String> LISTA = List.of(
        "Bodega / Abarrotes", "Restaurante / Comida", "Farmacia / Botica",
        "Ferretería", "Salón de belleza / Peluquería", "Taller mecánico",
        "Tienda de ropa / Calzado", "Librería / Papelería",
        "Consultorio médico / Dental", "Gimnasio / Centro deportivo",
        "Hotel / Hospedaje", "Panadería / Pastelería", "Lavandería",
        "Agencia de viajes", "Otro"
    );
}

@org.springframework.stereotype.Controller
class RootController {
    @org.springframework.web.bind.annotation.GetMapping("/")
    String root() {
        return "redirect:/publico";
    }
}

// ── Subsanación de observaciones ──────────────────────────────────────────────
@org.springframework.stereotype.Controller
class ObservacionController {

    private final com.municipalidad.licencias.repository.ObservacionRepository observacionRepo;
    private final com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo;
    private final com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo;

    private final com.municipalidad.licencias.service.CloudinaryService cloudinaryService;
    ObservacionController(
        com.municipalidad.licencias.repository.ObservacionRepository observacionRepo,
        com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo,
        com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo,
        com.municipalidad.licencias.service.CloudinaryService cloudinaryService) {
        this.observacionRepo = observacionRepo;
        this.solicitudRepo = solicitudRepo;
        this.inspeccionRepo = inspeccionRepo;
        this.cloudinaryService = cloudinaryService;
    }
    @org.springframework.web.bind.annotation.GetMapping("/solicitud/{id}/observaciones")
    String verObservaciones(@org.springframework.web.bind.annotation.PathVariable Long id,
                            org.springframework.ui.Model model) {
        try {
            com.municipalidad.licencias.model.Solicitud s = solicitudRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
            java.util.List<com.municipalidad.licencias.model.Inspeccion> inspecciones =
                inspeccionRepo.findBySolicitud(s);
            com.municipalidad.licencias.model.Inspeccion ultimaConObs = inspecciones.stream()
                .filter(i -> i.getResultado() ==
                    com.municipalidad.licencias.model.Enums.ResultadoInspeccion.CON_OBSERVACIONES)
                .reduce((a, b) -> b)
                .orElse(null);
            java.util.List<com.municipalidad.licencias.model.Observacion> observaciones =
                ultimaConObs != null
                    ? observacionRepo.findByInspeccion(ultimaConObs)
                    : java.util.List.of();
            String fechaLimite = "-";
            if (ultimaConObs != null) {
                java.time.LocalDate limite = ultimaConObs.getFechaProgramada().plusDays(42);
                fechaLimite = limite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            model.addAttribute("solicitud", s);
            model.addAttribute("observaciones", observaciones);
            model.addAttribute("fechaLimite", fechaLimite);
            model.addAttribute("error", null);
        } catch (Exception e) {
            model.addAttribute("error", "Error cargando observaciones: " + e.getMessage());
            model.addAttribute("observaciones", java.util.List.of());
            model.addAttribute("fechaLimite", "-");
        }
        return "solicitud/observaciones";
    }

    @org.springframework.web.bind.annotation.PostMapping("/solicitud/{solicitudId}/observacion/{obsId}/subsanar")
    String subsanarObservacion(
        @org.springframework.web.bind.annotation.PathVariable Long solicitudId,
        @org.springframework.web.bind.annotation.PathVariable Long obsId,
        @org.springframework.web.bind.annotation.RequestParam(required = false)
            org.springframework.web.multipart.MultipartFile archivo,
        org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {

        com.municipalidad.licencias.model.Observacion obs = observacionRepo.findById(obsId)
            .orElseThrow(() -> new IllegalArgumentException("Observación no encontrada"));

        if (obs.getTipo() == com.municipalidad.licencias.model.Enums.TipoObservacion.DOCUMENTAL
            && archivo != null && !archivo.isEmpty()) {
            try {
                String url = cloudinaryService.subirArchivo(archivo, "observaciones");
                obs.setDocumentoCorregidoUrl(url);
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Error al subir archivo: " + e.getMessage());
                return "redirect:/solicitud/" + solicitudId + "/observaciones";
            }
        }

        obs.setSubsanada(true);
        obs.setFechaSubsanacion(java.time.LocalDateTime.now());
        observacionRepo.save(obs);

        // Verificar si todas las observaciones están subsanadas
        com.municipalidad.licencias.model.Solicitud sol = solicitudRepo.findById(solicitudId).orElse(null);
        if (sol != null) {
            java.util.List<com.municipalidad.licencias.model.Observacion> pendientes =
                observacionRepo.findObservacionesPendientes(sol);
            if (pendientes.isEmpty()) {
                // Todas subsanadas - quitar la alerta del dashboard
                if (sol.getEstado() == com.municipalidad.licencias.model.Enums.EstadoTramite.OBSERVADO ||
                    sol.getEstado() == com.municipalidad.licencias.model.Enums.EstadoTramite.SEGUNDA_INSPECCION_PROGRAMADA) {
                    sol.setEstado(com.municipalidad.licencias.model.Enums.EstadoTramite.SEGUNDA_INSPECCION_PROGRAMADA);
                    solicitudRepo.save(sol);
                }
                ra.addFlashAttribute("exito", "¡Todas las observaciones subsanadas! Espera la segunda inspección.");
            } else {
                ra.addFlashAttribute("exito", "Observación marcada como subsanada. Quedan " + pendientes.size() + " pendientes.");
            }
        }
        return "redirect:/solicitud/" + solicitudId + "/observaciones";
    }
}

// ── Notificaciones ────────────────────────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/notificaciones")
class NotificacionController {

    private final com.municipalidad.licencias.service.NotificacionService notificacionService;
    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;

    NotificacionController(
        com.municipalidad.licencias.service.NotificacionService notificacionService,
        com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo) {
        this.notificacionService = notificacionService;
        this.usuarioRepo = usuarioRepo;
    }

    @org.springframework.web.bind.annotation.GetMapping
    String verNotificaciones(
        @org.springframework.security.core.annotation.AuthenticationPrincipal
        org.springframework.security.core.userdetails.UserDetails ud,
        org.springframework.ui.Model model) {
        com.municipalidad.licencias.model.Usuario usuario =
            usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        // Contar ANTES de marcar como leídas para mostrar en navbar
        long noLeidas = notificacionService.contarNoLeidas(usuario);
        model.addAttribute("notificaciones",
            notificacionService.obtenerPorUsuario(usuario));
        model.addAttribute("notifCount", noLeidas);
        notificacionService.marcarTodasLeidas(usuario);
        return "notificaciones/lista";
    }
}

// ── Multas ────────────────────────────────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/multas")
class MultaController {

    private final com.municipalidad.licencias.service.MultaService multaService;
    private final com.municipalidad.licencias.service.LicenciaService licenciaService;
    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;
    private final com.municipalidad.licencias.service.FlowService flowService;
    MultaController(com.municipalidad.licencias.service.MultaService multaService,
                    com.municipalidad.licencias.service.LicenciaService licenciaService,
                    com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo,
                    com.municipalidad.licencias.service.FlowService flowService) {
        this.multaService    = multaService;
        this.licenciaService = licenciaService;
        this.usuarioRepo     = usuarioRepo;
        this.flowService      = flowService;
    }

    // Ver historial de multas de una licencia
    @org.springframework.web.bind.annotation.GetMapping("/licencia/{id}")
    String historial(@org.springframework.web.bind.annotation.PathVariable Long id,
                     org.springframework.ui.Model model) {
        model.addAttribute("licencia", licenciaService.obtenerPorId(id));
        model.addAttribute("multas", multaService.obtenerPorLicencia(id));
        return "multas/historial";
    }

    // Formulario registrar multa
    @org.springframework.web.bind.annotation.GetMapping("/licencia/{id}/nueva")
    String nuevaForm(@org.springframework.web.bind.annotation.PathVariable Long id,
                     org.springframework.ui.Model model) {
        model.addAttribute("licencia", licenciaService.obtenerPorId(id));
        return "multas/nueva";
    }

    // Registrar multa
    @org.springframework.web.bind.annotation.PostMapping("/licencia/{id}/nueva")
    String registrar(@org.springframework.web.bind.annotation.PathVariable Long id,
                     @org.springframework.web.bind.annotation.RequestParam String descripcion,
                     @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal monto,
                     @org.springframework.security.core.annotation.AuthenticationPrincipal
                         org.springframework.security.core.userdetails.UserDetails ud,
                     org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            com.municipalidad.licencias.model.Usuario inspector =
                usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
            multaService.registrar(id, descripcion, monto, inspector);
            ra.addFlashAttribute("exito", "Multa registrada y negocio notificado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/multas/licencia/" + id;
    }

    // Panel admin: todas las multas
    @org.springframework.web.bind.annotation.GetMapping
    String todas(org.springframework.ui.Model model) {
        model.addAttribute("multas", multaService.obtenerTodas());
        return "multas/todas";
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}/detalle")
    String detalle(@org.springframework.web.bind.annotation.PathVariable Long id,
                   org.springframework.ui.Model model) {
        model.addAttribute("multa", multaService.obtenerPorId(id));
        return "multas/detalle";
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/pagar")
    String iniciarPago(@org.springframework.web.bind.annotation.PathVariable Long id,
                       @org.springframework.security.core.annotation.AuthenticationPrincipal
                           org.springframework.security.core.userdetails.UserDetails ud,
                       jakarta.servlet.http.HttpServletRequest request,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            com.municipalidad.licencias.model.Multa multa = multaService.obtenerPorId(id);
            com.municipalidad.licencias.model.Usuario usuario =
                usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();

            String scheme = request.getHeader("X-Forwarded-Proto") != null ?
                request.getHeader("X-Forwarded-Proto") : request.getScheme();
            String host   = request.getHeader("X-Forwarded-Host") != null ?
                request.getHeader("X-Forwarded-Host") : request.getServerName();
            String baseUrl = scheme + "://" + host;

            String email  = multa.getLicencia().getSolicitud().getCorreoElectronico() != null ?
                multa.getLicencia().getSolicitud().getCorreoElectronico() :
                usuario.getUsername() + "@licencias.gob.pe";

            String urlRetorno = baseUrl + "/pago/multa/retorno/" + id +
                "?u=" + java.net.URLEncoder.encode(usuario.getUsername(), java.nio.charset.StandardCharsets.UTF_8) +
                "&lid=" + multa.getLicencia().getId();
            String urlConfirmar = baseUrl + "/pago/multa/confirmar/" + id;

            com.municipalidad.licencias.service.FlowService.OrdenFlow orden =
                flowService.crearOrden(id, email, usuario.getNombreCompleto(),
                    multa.getMonto().doubleValue(), urlRetorno, urlConfirmar);

            return "redirect:" + orden.url();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al iniciar pago: " + e.getMessage());
            return "redirect:/multas/" + id + "/detalle";
        }
    }

}

// ── Subgerente ───────────────────────────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/subgerente")
class SubgerenteController {

    private final com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo;
    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;

    SubgerenteController(com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo,
                         com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo) {
        this.solicitudRepo = solicitudRepo;
        this.usuarioRepo   = usuarioRepo;
    }

    @org.springframework.web.bind.annotation.GetMapping("/dashboard")
    String dashboard(@org.springframework.security.core.annotation.AuthenticationPrincipal
                         org.springframework.security.core.userdetails.UserDetails ud,
                     org.springframework.ui.Model model) {
        com.municipalidad.licencias.model.Usuario usuario =
            usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        com.municipalidad.licencias.model.Enums.Distrito distrito = usuario.getDistrito();
        model.addAttribute("usuario", usuario);

        java.util.List<com.municipalidad.licencias.model.Solicitud> todas =
            distrito != null ? solicitudRepo.findByDistrito(distrito) : java.util.List.of();

        model.addAttribute("todasSolicitudes", todas);
        model.addAttribute("solicitudesPendientes", todas.stream()
            .filter(s -> s.getEstado() == com.municipalidad.licencias.model.Enums.EstadoTramite.ADMITIDO)
            .toList());
        model.addAttribute("fiscalizadores",
            distrito != null ?
            usuarioRepo.findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR, distrito) :
            java.util.List.of());
        return "subgerente/dashboard";
    }

    @org.springframework.web.bind.annotation.PostMapping("/solicitud/{id}/asignar")
    String asignarInspector(@org.springframework.web.bind.annotation.PathVariable Long id,
                             @org.springframework.web.bind.annotation.RequestParam Long inspectorId,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            com.municipalidad.licencias.model.Solicitud s = solicitudRepo.findById(id).orElseThrow();
            com.municipalidad.licencias.model.Usuario inspector = usuarioRepo.findById(inspectorId).orElseThrow();
            s.setInspector(inspector);
            solicitudRepo.save(s);
            ra.addFlashAttribute("exito", "Fiscalizador " + inspector.getNombreCompleto() + " asignado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al asignar: " + e.getMessage());
        }
        return "redirect:/subgerente/dashboard";
    }
}

// ── Gerente Distrital ─────────────────────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/gerente")
class GerenteDistritController {

    private final com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo;
    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;

    GerenteDistritController(com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo,
                              com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo) {
        this.solicitudRepo = solicitudRepo;
        this.usuarioRepo   = usuarioRepo;
    }

    @org.springframework.web.bind.annotation.GetMapping("/distrital")
    String dashboard(@org.springframework.security.core.annotation.AuthenticationPrincipal
                         org.springframework.security.core.userdetails.UserDetails ud,
                     org.springframework.ui.Model model) {
        com.municipalidad.licencias.model.Usuario usuario =
            usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        com.municipalidad.licencias.model.Enums.Distrito distrito = usuario.getDistrito();
        model.addAttribute("usuario", usuario);

        java.util.List<com.municipalidad.licencias.model.Solicitud> solicitudes =
            distrito != null ? solicitudRepo.findByDistrito(distrito) : java.util.List.of();
        java.util.List<com.municipalidad.licencias.model.Usuario> fiscalizadores =
            distrito != null ?
            usuarioRepo.findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR, distrito) :
            java.util.List.of();

        model.addAttribute("totalSolicitudes", solicitudes.size());
        model.addAttribute("aprobadas", solicitudes.stream()
            .filter(s -> s.getEstado() == com.municipalidad.licencias.model.Enums.EstadoTramite.APROBADO).count());
        model.addAttribute("pendientes", solicitudes.stream()
            .filter(s -> s.getEstado() != com.municipalidad.licencias.model.Enums.EstadoTramite.APROBADO
                      && s.getEstado() != com.municipalidad.licencias.model.Enums.EstadoTramite.DENEGADO).count());
        model.addAttribute("totalFiscalizadores", fiscalizadores.size());
        model.addAttribute("fiscalizadores", fiscalizadores);
        model.addAttribute("subgerente",
            distrito != null ?
            usuarioRepo.findByRolAndDistritoAndActivo(
                com.municipalidad.licencias.model.Enums.Rol.SUBGERENTE, distrito, true).orElse(null) : null);
        return "gerente/dashboard-distrital";
    }
}

// ── Gerente Municipal ─────────────────────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/gerente-municipal")
class GerenteMunicipalController {

    private final com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo;
    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;

    GerenteMunicipalController(com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo,
                                com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo) {
        this.solicitudRepo = solicitudRepo;
        this.usuarioRepo   = usuarioRepo;
    }

    @org.springframework.web.bind.annotation.GetMapping("/dashboard")
    String dashboard(org.springframework.ui.Model model,
                     @org.springframework.security.core.annotation.AuthenticationPrincipal
                         org.springframework.security.core.userdetails.UserDetails ud) {
        com.municipalidad.licencias.model.Usuario usuario =
            usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        model.addAttribute("usuario", usuario);

        java.util.List<com.municipalidad.licencias.model.Solicitud> todas = solicitudRepo.findAll();
        model.addAttribute("totalSolicitudes", todas.size());
        model.addAttribute("totalAprobadas", todas.stream()
            .filter(s -> s.getEstado() == com.municipalidad.licencias.model.Enums.EstadoTramite.APROBADO).count());
        model.addAttribute("totalPendientes", todas.stream()
            .filter(s -> s.getEstado() != com.municipalidad.licencias.model.Enums.EstadoTramite.APROBADO
                      && s.getEstado() != com.municipalidad.licencias.model.Enums.EstadoTramite.DENEGADO).count());
        model.addAttribute("totalFuncionarios",
            usuarioRepo.findByRol(com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR).size() +
            usuarioRepo.findByRol(com.municipalidad.licencias.model.Enums.Rol.SUBGERENTE).size() +
            usuarioRepo.findByRol(com.municipalidad.licencias.model.Enums.Rol.GERENTE_DISTRITAL).size());

        // Resumen por distrito
        java.util.List<java.util.Map<String,Object>> distritos = new java.util.ArrayList<>();
        for (com.municipalidad.licencias.model.Enums.Distrito d :
             new com.municipalidad.licencias.model.Enums.Distrito[]{com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO}) {
            java.util.Map<String,Object> info = new java.util.HashMap<>();
            info.put("nombre", d.name().replace("_"," "));
            info.put("nombreEnum", d.name());
            info.put("numSolicitudes", solicitudRepo.findByDistrito(d).size());
            info.put("numFiscalizadores",
                usuarioRepo.findByRolAndDistrito(
                    com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR, d).size());
            info.put("numInspectores",
                usuarioRepo.findByRolAndDistrito(
                    com.municipalidad.licencias.model.Enums.Rol.INSPECTOR, d).size());
            info.put("gerente", usuarioRepo.findByRolAndDistrito(
                com.municipalidad.licencias.model.Enums.Rol.GERENTE_DISTRITAL, d)
                .stream().findFirst().map(u -> u.getNombreCompleto()).orElse(null));
            info.put("subgerente", usuarioRepo.findByRolAndDistrito(
                com.municipalidad.licencias.model.Enums.Rol.SUBGERENTE, d)
                .stream().findFirst().map(u -> u.getNombreCompleto()).orElse(null));
            distritos.add(info);
        }
        model.addAttribute("distritos", distritos);
        com.municipalidad.licencias.model.Enums.Distrito trujillo = com.municipalidad.licencias.model.Enums.Distrito.TRUJILLO;
        model.addAttribute("gerentes", usuarioRepo.findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol.GERENTE_DISTRITAL, trujillo));
        model.addAttribute("subgerentes", usuarioRepo.findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol.SUBGERENTE, trujillo));
        model.addAttribute("fiscalizadores", usuarioRepo.findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR, trujillo));
        model.addAttribute("inspectores", usuarioRepo.findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol.INSPECTOR, trujillo));
        return "gerente/dashboard-municipal";
    }
}

// ── Portal público (seguimiento sin login) ───────────────────────────────────
@org.springframework.stereotype.Controller
class PublicoController {

    private final com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo;
    private final com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo;
    private final com.municipalidad.licencias.repository.LicenciaRepository licenciaRepo;
    private final com.municipalidad.licencias.service.FlowService flowService;
    private final com.municipalidad.licencias.service.EmailService emailService;

    PublicoController(com.municipalidad.licencias.repository.SolicitudRepository solicitudRepo,
                      com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo,
                      com.municipalidad.licencias.repository.LicenciaRepository licenciaRepo,
                      com.municipalidad.licencias.service.FlowService flowService,
                      com.municipalidad.licencias.service.EmailService emailService) {
        this.solicitudRepo  = solicitudRepo;
        this.inspeccionRepo = inspeccionRepo;
        this.licenciaRepo   = licenciaRepo;
        this.flowService    = flowService;
        this.emailService   = emailService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/publico")
    String inicio() { return "publico/inicio"; }

    @org.springframework.web.bind.annotation.GetMapping("/publico/nueva-solicitud")
    String nuevaSolicitudPublica(org.springframework.ui.Model model) {
        model.addAttribute("solicitudDto", new com.municipalidad.licencias.dto.SolicitudDto());
        model.addAttribute("rubros", Rubros.LISTA);
        return "solicitud/nueva";
    }

    @org.springframework.web.bind.annotation.GetMapping("/seguimiento")
    String seguimiento(
            @org.springframework.web.bind.annotation.RequestParam(required=false) String codigo,
            @org.springframework.web.bind.annotation.RequestParam(required=false) String dni,
            org.springframework.ui.Model model) {
        if (codigo != null && dni != null) {
            var sol = solicitudRepo.findByCodigoSeguimientoAndDni(
                codigo.trim().toUpperCase(), dni.trim());
            if (sol.isPresent()) {
                var s = sol.get();
                model.addAttribute("solicitud", s);
                inspeccionRepo.findBySolicitud(s).stream()
                    .filter(i -> i.getResultado() != com.municipalidad.licencias.model.Enums.ResultadoInspeccion.PENDIENTE)
                    .reduce((a,b) -> b)
                    .ifPresent(i -> model.addAttribute("inspeccion", i));
                licenciaRepo.findBySolicitud(s)
                    .ifPresent(l -> model.addAttribute("licencia", l));
            } else {
                model.addAttribute("error", "No se encontró ningún trámite con ese código y DNI. Verifica los datos.");
            }
        }
        return "publico/seguimiento";
    }

    @org.springframework.web.bind.annotation.GetMapping("/publico/licencia/{id}/renovar")
    String renovarForm(@org.springframework.web.bind.annotation.PathVariable Long id,
                       org.springframework.ui.Model model,
                       jakarta.servlet.http.HttpServletRequest request) {
        com.municipalidad.licencias.model.Licencia licencia = licenciaRepo.findById(id).orElseThrow();
        model.addAttribute("licencia", licencia);
        return "publico/renovar-pago";
    }

    @org.springframework.web.bind.annotation.PostMapping("/publico/licencia/{id}/renovar/flow")
    String renovarFlow(@org.springframework.web.bind.annotation.PathVariable Long id,
                       jakarta.servlet.http.HttpServletRequest request,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            com.municipalidad.licencias.model.Licencia licencia = licenciaRepo.findById(id).orElseThrow();
            com.municipalidad.licencias.model.Solicitud s = licencia.getSolicitud();
            String scheme = request.getHeader("X-Forwarded-Proto") != null ? request.getHeader("X-Forwarded-Proto") : request.getScheme();
            String host = request.getHeader("X-Forwarded-Host") != null ? request.getHeader("X-Forwarded-Host") : request.getServerName();
            String baseUrl = scheme + "://" + host;
            String urlRetorno = baseUrl + "/publico/licencia/" + id + "/renovar/retorno";
            String urlConfirmacion = baseUrl + "/publico/licencia/" + id + "/renovar/confirmar";
            String email = s.getCorreoElectronico() != null ? s.getCorreoElectronico() : "publico@licencias.gob.pe";
            String nombre = s.getNombreRepresentante() != null ? s.getNombreRepresentante() : "Ciudadano";
            com.municipalidad.licencias.service.FlowService.OrdenFlow orden =
                flowService.crearOrden(id + 10000L, email, nombre, 2.0, urlRetorno, urlConfirmacion);
            return "redirect:" + orden.url();
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al conectar con el sistema de pago: " + e.getMessage());
            return "redirect:/publico/licencia/" + id + "/renovar";
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/publico/licencia/{id}/renovar/retorno")
    String renovarRetorno(@org.springframework.web.bind.annotation.PathVariable Long id,
                          @org.springframework.web.bind.annotation.RequestParam(required = false) String token,
                          org.springframework.ui.Model model,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null && estado.path("status").asInt() == 2) {
                    com.municipalidad.licencias.model.Licencia licencia = licenciaRepo.findById(id).orElseThrow();
                    // Renovar: extender vencimiento 1 año
                    java.time.LocalDate nuevaFecha = (licencia.getFechaVencimiento() != null &&
                        licencia.getFechaVencimiento().isAfter(java.time.LocalDate.now()))
                        ? licencia.getFechaVencimiento().plusYears(1)
                        : java.time.LocalDate.now().plusYears(1);
                    licencia.setFechaVencimiento(nuevaFecha);
                    licencia.setEstado(com.municipalidad.licencias.model.Enums.EstadoLicencia.VIGENTE);
                    licenciaRepo.save(licencia);
                    // Enviar correo
                    com.municipalidad.licencias.model.Solicitud s = licencia.getSolicitud();
                    if (s.getCorreoElectronico() != null) {
                        emailService.enviarCodigoSeguimiento(
                            s.getCorreoElectronico(), s.getRazonSocial(),
                            "RENOVACION-" + licencia.getNumeroLicencia(),
                            s.getDistrito() != null ? s.getDistrito().name() : "TRUJILLO");
                    }
                    model.addAttribute("licencia", licencia);
                    model.addAttribute("nuevaFecha", nuevaFecha);
                    return "publico/comprobante-renovacion";
                } else {
                    ra.addFlashAttribute("error", "El pago no fue confirmado.");
                }
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al verificar el pago.");
        }
        return "redirect:/publico/licencia/" + id + "/renovar";
    }

    @org.springframework.web.bind.annotation.PostMapping("/publico/licencia/{id}/renovar/confirmar")
    @org.springframework.web.bind.annotation.ResponseBody
    String renovarConfirmar(@org.springframework.web.bind.annotation.PathVariable Long id,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String token) {
        return "OK";
    }

    @org.springframework.web.bind.annotation.GetMapping("/publico/licencia/{id}/descargar")
    org.springframework.http.ResponseEntity<byte[]> descargarPublico(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        try {
            com.municipalidad.licencias.model.Licencia licencia = licenciaRepo.findById(id).orElseThrow();
            byte[] pdf = com.municipalidad.licencias.util.PdfLicenciaUtil.generar(licencia);
            return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"licencia-" + licencia.getNumeroLicencia() + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }
}

// ── Registro de nuevos negocios ──────────────────────────────────────────────
@org.springframework.stereotype.Controller
class RegistroController {

    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    RegistroController(com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo,
                       org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.encoder     = encoder;
    }

    @org.springframework.web.bind.annotation.GetMapping("/auth/registro")
    String registroForm() {
        return "auth/registro";
    }

    @org.springframework.web.bind.annotation.PostMapping("/auth/registro")
    String registrar(
            @org.springframework.web.bind.annotation.RequestParam String nombreCompleto,
            @org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestParam String email,
            @org.springframework.web.bind.annotation.RequestParam String password,
            @org.springframework.web.bind.annotation.RequestParam String confirmar,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {

        if (!password.equals(confirmar)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/auth/registro";
        }
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "redirect:/auth/registro";
        }
        if (usuarioRepo.findByUsername(username).isPresent()) {
            ra.addFlashAttribute("error", "El usuario '" + username + "' ya existe.");
            return "redirect:/auth/registro";
        }
        com.municipalidad.licencias.model.Usuario usuario =
            com.municipalidad.licencias.model.Usuario.builder()
                .username(username)
                .password(encoder.encode(password))
                .email(email)
                .nombreCompleto(nombreCompleto)
                .rol(com.municipalidad.licencias.model.Enums.Rol.NEGOCIO)
                .activo(true)
                .build();
        usuarioRepo.save(usuario);
        ra.addFlashAttribute("exito", "Cuenta creada correctamente. Ya puedes iniciar sesión.");
        return "redirect:/auth/login";
    }
}

// ── Gestión de inspectores (admin) ───────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/admin/inspectores")
class InspectorAdminController {

    private final com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    InspectorAdminController(com.municipalidad.licencias.repository.UsuarioRepository usuarioRepo,
                              org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.encoder     = encoder;
    }

    @org.springframework.web.bind.annotation.GetMapping
    String listar(@org.springframework.web.bind.annotation.RequestParam(required = false) String distrito,
                  org.springframework.ui.Model model) {
        java.util.List<com.municipalidad.licencias.model.Usuario> todos = new java.util.ArrayList<>();
        com.municipalidad.licencias.model.Enums.Distrito distritoEnum = null;
        if (distrito != null && !distrito.isBlank()) {
            try { distritoEnum = com.municipalidad.licencias.model.Enums.Distrito.valueOf(distrito); } catch (Exception ignored) {}
        }
        for (com.municipalidad.licencias.model.Enums.Rol rol : new com.municipalidad.licencias.model.Enums.Rol[]{
            com.municipalidad.licencias.model.Enums.Rol.FISCALIZADOR,
            com.municipalidad.licencias.model.Enums.Rol.INSPECTOR,
            com.municipalidad.licencias.model.Enums.Rol.SUBGERENTE,
            com.municipalidad.licencias.model.Enums.Rol.GERENTE_DISTRITAL
        }) {
            if (distritoEnum != null) {
                todos.addAll(usuarioRepo.findByRolAndDistrito(rol, distritoEnum));
            } else {
                todos.addAll(usuarioRepo.findByRol(rol));
            }
        }
        model.addAttribute("inspectores", todos);
        model.addAttribute("distritoFiltro", distrito);
        model.addAttribute("distritos", com.municipalidad.licencias.model.Enums.Distrito.values());
        return "admin/inspectores";
    }

    @org.springframework.web.bind.annotation.GetMapping("/nuevo")
    String nuevoForm(org.springframework.ui.Model model) {
        model.addAttribute("dto", new com.municipalidad.licencias.dto.InspectorDto());
        return "admin/inspector-form";
    }

    @org.springframework.web.bind.annotation.PostMapping("/nuevo")
    String crear(@jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("dto")
                     com.municipalidad.licencias.dto.InspectorDto dto,
                 org.springframework.validation.BindingResult errors,
                 org.springframework.web.servlet.mvc.support.RedirectAttributes ra,
                 org.springframework.ui.Model model) {
        if (errors.hasErrors()) return "admin/inspector-form";
        if (usuarioRepo.findByUsername(dto.getUsername()).isPresent()) {
            model.addAttribute("errorUsername", "El usuario ya existe.");
            return "admin/inspector-form";
        }
        // Determinar rol desde dto
        com.municipalidad.licencias.model.Enums.Rol rolFinal = com.municipalidad.licencias.model.Enums.Rol.INSPECTOR;
        try {
            if (dto.getRol() != null && !dto.getRol().isBlank())
                rolFinal = com.municipalidad.licencias.model.Enums.Rol.valueOf(dto.getRol());
        } catch (Exception ignored) {}

        com.municipalidad.licencias.model.Usuario inspector =
            com.municipalidad.licencias.model.Usuario.builder()
                .username(dto.getUsername())
                .password(encoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .nombreCompleto(dto.getNombreCompleto())
                .rol(rolFinal)
                .activo(true)
                .build();
        inspector.setDni(dto.getDni());
        inspector.setTelefono(dto.getTelefono());
        inspector.setCargo(dto.getCargo());
        inspector.setNumeroColegiatura(dto.getNumeroColegiatura());
        inspector.setFechaNombramiento(dto.getFechaNombramiento());
        inspector.setEspecialidad(dto.getEspecialidad());
        inspector.setRegimentLaboral(dto.getRegimentLaboral());
        inspector.setResolucionDesignacion(dto.getResolucionDesignacion());
        inspector.setFechaResolucion(dto.getFechaResolucion());
        inspector.setCodigoPlaza(dto.getCodigoPlaza());
        inspector.setCodigoFiscalizador(dto.getCodigoFiscalizador());
        inspector.setCertificacionFiscalizacion(dto.getCertificacionFiscalizacion());
        if (dto.getDistrito() != null && !dto.getDistrito().isBlank()) {
            try {
                inspector.setDistrito(com.municipalidad.licencias.model.Enums.Distrito.valueOf(dto.getDistrito()));
            } catch (Exception ignored) {}
        }
        usuarioRepo.save(inspector);
        ra.addFlashAttribute("exito", "Funcionario " + dto.getNombreCompleto() + " creado correctamente.");
        ra.addFlashAttribute("exito", "Inspector " + dto.getNombreCompleto() + " creado correctamente.");
        return "redirect:/admin/inspectores";
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}/editar")
    String editarForm(@org.springframework.web.bind.annotation.PathVariable Long id,
                      org.springframework.ui.Model model) {
        com.municipalidad.licencias.model.Usuario inspector = usuarioRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inspector no encontrado"));
        com.municipalidad.licencias.dto.InspectorDto dto = new com.municipalidad.licencias.dto.InspectorDto();
        dto.setNombreCompleto(inspector.getNombreCompleto());
        dto.setDni(inspector.getDni());
        dto.setEmail(inspector.getEmail());
        dto.setUsername(inspector.getUsername());
        dto.setPassword(""); // no mostrar password
        dto.setTelefono(inspector.getTelefono());
        dto.setCargo(inspector.getCargo());
        dto.setNumeroColegiatura(inspector.getNumeroColegiatura());
        dto.setFechaNombramiento(inspector.getFechaNombramiento());
        model.addAttribute("dto", dto);
        model.addAttribute("inspectorId", id);
        return "admin/inspector-form";
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/editar")
    String actualizar(@org.springframework.web.bind.annotation.PathVariable Long id,
                      @org.springframework.web.bind.annotation.ModelAttribute("dto")
                          com.municipalidad.licencias.dto.InspectorDto dto,
                      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        com.municipalidad.licencias.model.Usuario inspector = usuarioRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inspector no encontrado"));
        inspector.setNombreCompleto(dto.getNombreCompleto());
        inspector.setDni(dto.getDni());
        inspector.setEmail(dto.getEmail());
        inspector.setTelefono(dto.getTelefono());
        inspector.setCargo(dto.getCargo());
        inspector.setNumeroColegiatura(dto.getNumeroColegiatura());
        inspector.setFechaNombramiento(dto.getFechaNombramiento());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            inspector.setPassword(encoder.encode(dto.getPassword()));
        }
        usuarioRepo.save(inspector);
        ra.addFlashAttribute("exito", "Inspector actualizado correctamente.");
        return "redirect:/admin/inspectores";
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/desactivar")
    String desactivar(@org.springframework.web.bind.annotation.PathVariable Long id,
                      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        com.municipalidad.licencias.model.Usuario inspector = usuarioRepo.findById(id).orElseThrow();
        inspector.setActivo(!inspector.isActivo());
        usuarioRepo.save(inspector);
        ra.addFlashAttribute("exito", inspector.isActivo() ? "Inspector activado." : "Inspector desactivado.");
        return "redirect:/admin/inspectores";
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/eliminar")
    String eliminar(@org.springframework.web.bind.annotation.PathVariable Long id,
                    org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            com.municipalidad.licencias.model.Usuario inspector = usuarioRepo.findById(id).orElseThrow();
            String nombre = inspector.getNombreCompleto();
            usuarioRepo.deleteById(id);
            ra.addFlashAttribute("exito", "Inspector " + nombre + " eliminado definitivamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se puede eliminar: el inspector tiene inspecciones o registros asociados.");
        }
        return "redirect:/admin/inspectores";
    }
}

// ── Buscador de licencias (inspector) ────────────────────────────────────────
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.RequestMapping("/inspector")
class BuscadorLicenciaController {

    private final com.municipalidad.licencias.repository.LicenciaRepository licenciaRepo;
    private final com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo;
    private final com.municipalidad.licencias.repository.ObservacionRepository observacionRepo;
    private final com.municipalidad.licencias.service.MultaService multaService;

    BuscadorLicenciaController(
            com.municipalidad.licencias.repository.LicenciaRepository licenciaRepo,
            com.municipalidad.licencias.repository.InspeccionRepository inspeccionRepo,
            com.municipalidad.licencias.repository.ObservacionRepository observacionRepo,
            com.municipalidad.licencias.service.MultaService multaService) {
        this.licenciaRepo    = licenciaRepo;
        this.inspeccionRepo  = inspeccionRepo;
        this.observacionRepo = observacionRepo;
        this.multaService    = multaService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/buscar")
    String buscar(@org.springframework.web.bind.annotation.RequestParam(required = false) String numero,
                  org.springframework.ui.Model model) {
        if (numero != null && !numero.isBlank()) {
            var licOpt = licenciaRepo.findByNumeroLicencia(numero.trim().toUpperCase());
            if (licOpt.isPresent()) {
                var lic = licOpt.get();
                model.addAttribute("licencia", lic);
                // Inspecciones de la solicitud
                model.addAttribute("inspecciones",
                    inspeccionRepo.findBySolicitud(lic.getSolicitud()));
                // Observaciones de todas las inspecciones
                java.util.List<com.municipalidad.licencias.model.Observacion> obs = new java.util.ArrayList<>();
                for (var insp : inspeccionRepo.findBySolicitud(lic.getSolicitud())) {
                    obs.addAll(observacionRepo.findByInspeccion(insp));
                }
                model.addAttribute("observaciones", obs);
                // Multas
                model.addAttribute("multas", multaService.obtenerPorLicencia(lic.getId()));
            } else {
                model.addAttribute("error", "No se encontró ninguna licencia con el número: " + numero);
            }
        }
        return "inspector/buscar-licencia";
    }
}

// ── API de validación SUNAT/RENIEC (llamadas AJAX desde el formulario) ────────
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/validar")
class ValidacionApiController {

    private final com.municipalidad.licencias.service.FactilizaService factilizaService;

    ValidacionApiController(com.municipalidad.licencias.service.FactilizaService factilizaService) {
        this.factilizaService = factilizaService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/ruc/{ruc}")
    org.springframework.http.ResponseEntity<?> consultarRuc(
        @org.springframework.web.bind.annotation.PathVariable String ruc) {
        var datos = factilizaService.consultarRuc(ruc);
        if (datos == null)
            return org.springframework.http.ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "RUC no encontrado en SUNAT"));
        return org.springframework.http.ResponseEntity.ok(datos);
    }

    @org.springframework.web.bind.annotation.GetMapping("/dni/{dni}")
    org.springframework.http.ResponseEntity<?> consultarDni(
        @org.springframework.web.bind.annotation.PathVariable String dni) {
        var datos = factilizaService.consultarDni(dni);
        if (datos == null)
            return org.springframework.http.ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "DNI no encontrado en RENIEC"));
        return org.springframework.http.ResponseEntity.ok(datos);
    }

    @org.springframework.web.bind.annotation.PostMapping("/ruc-dni")
    org.springframework.http.ResponseEntity<?> validarRucDni(
        @org.springframework.web.bind.annotation.RequestBody java.util.Map<String,String> body) {
        String ruc = body.get("ruc");
        String dni = body.get("dni");
        var resultado = factilizaService.validarRucYDni(ruc, dni);
        if (!resultado.valido())
            return org.springframework.http.ResponseEntity.badRequest().body(resultado);
        return org.springframework.http.ResponseEntity.ok(resultado);
    }
// ── Retorno público de Flow (sin autenticación requerida) ────────────────────
@org.springframework.stereotype.Controller
class FlowRetornoController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FlowRetornoController.class);
    private final com.municipalidad.licencias.service.FlowService flowService;
    private final com.municipalidad.licencias.service.SolicitudService solicitudService;

    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final com.municipalidad.licencias.service.MultaService multaService;

    FlowRetornoController(com.municipalidad.licencias.service.FlowService flowService,
                          com.municipalidad.licencias.service.SolicitudService solicitudService,
                          org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
                          com.municipalidad.licencias.service.MultaService multaService) {
        this.flowService         = flowService;
        this.solicitudService    = solicitudService;
        this.userDetailsService  = userDetailsService;
        this.multaService        = multaService;
    }

    @org.springframework.web.bind.annotation.RequestMapping(
        value = "/pago/multa/retorno/{id}",
        method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                  org.springframework.web.bind.annotation.RequestMethod.POST})
    String retornoMulta(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String token,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String u,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long lid,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null && estado.path("status").asInt() == 2) {
                    multaService.pagarMulta(id);
                    // Auto-autenticar al usuario
                    if (u != null && !u.isBlank()) {
                        try {
                            org.springframework.security.core.userdetails.UserDetails ud =
                                userDetailsService.loadUserByUsername(u);
                            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    ud, null, ud.getAuthorities());
                            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                            request.getSession(true).setAttribute(
                                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                org.springframework.security.core.context.SecurityContextHolder.getContext());
                        } catch (Exception ex) {
                            log.warn("No se pudo restaurar sesion tras pago multa: {}", ex.getMessage());
                        }
                    }
                    ra.addFlashAttribute("exito", "¡Multa pagada correctamente!");
                    return "redirect:/multas/" + id + "/detalle";
                }
            }
        } catch (Exception e) {
            log.error("Error retorno pago multa {}: {}", id, e.getMessage());
        }
        ra.addFlashAttribute("error", "No se pudo confirmar el pago.");
        return "redirect:/multas/" + id + "/detalle";
    }

    @org.springframework.web.bind.annotation.PostMapping("/pago/multa/confirmar/{id}")
    @org.springframework.web.bind.annotation.ResponseBody
    String confirmarMultaWebhook(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String token) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null && estado.path("status").asInt() == 2) {
                    multaService.pagarMulta(id);
                    log.info("Multa {} pagada via webhook", id);
                }
            }
        } catch (Exception e) {
            log.error("Error webhook multa {}: {}", id, e.getMessage());
        }
        return "OK";
    }

    @org.springframework.web.bind.annotation.GetMapping("/pago/exito/{id}")
    String exitoPago(@org.springframework.web.bind.annotation.PathVariable Long id,
                     org.springframework.ui.Model model) {
        try {
            model.addAttribute("solicitud", solicitudService.obtenerPorId(id));
        } catch (Exception e) {
            // solicitud no encontrada
        }
        return "pago/exito";
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/pago/retorno/{id}", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    String retorno(@org.springframework.web.bind.annotation.PathVariable Long id,
                   @org.springframework.web.bind.annotation.RequestParam(required = false) String token,
                   @org.springframework.web.bind.annotation.RequestParam(required = false) String u,
                   jakarta.servlet.http.HttpServletRequest request,
                   org.springframework.ui.Model model,
                   org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null && estado.path("status").asInt() == 2) {
                    com.municipalidad.licencias.model.Solicitud s = solicitudService.enviarConPago(id, token);
                    model.addAttribute("solicitud", s);
                    model.addAttribute("codigoSeguimiento", s.getCodigoSeguimiento());
                    return "solicitud/comprobante";
                } else {
                    ra.addFlashAttribute("error", "El pago no fue confirmado. Intenta nuevamente.");
                }
            }
        } catch (Exception e) {
            log.error("Error procesando retorno Flow solicitud {}: {}", id, e.getMessage());
            ra.addFlashAttribute("error", "Error al verificar el pago.");
        }
        return "redirect:/solicitud/" + id + "/pago";
    }

    @org.springframework.web.bind.annotation.PostMapping("/pago/confirmar/{id}")
    @org.springframework.web.bind.annotation.ResponseBody
    String confirmar(@org.springframework.web.bind.annotation.PathVariable Long id,
                     @org.springframework.web.bind.annotation.RequestParam(required = false) String token) {
        try {
            if (token != null) {
                com.fasterxml.jackson.databind.JsonNode estado = flowService.verificarPago(token);
                if (estado != null && estado.path("status").asInt() == 2) {
                    solicitudService.enviarConPago(id, token);
                    log.info("Webhook Flow: pago confirmado solicitud {}", id);
                }
            }
        } catch (Exception e) {
            log.error("Webhook Flow error solicitud {}: {}", id, e.getMessage());
        }
        return "OK";
    }
}

}

