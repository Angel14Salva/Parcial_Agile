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

    DashboardController(UsuarioRepository usuarioRepo,
                        SolicitudService solicitudService,
                        InspeccionService inspeccionService) {
        this.usuarioRepo       = usuarioRepo;
        this.solicitudService  = solicitudService;
        this.inspeccionService = inspeccionService;
    }

    @GetMapping("/dashboard")
    String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        Usuario usuario = getUsuario(ud);
        model.addAttribute("usuario", usuario);
        if (usuario.getRol() == Enums.Rol.NEGOCIO) {
            model.addAttribute("solicitudes", solicitudService.obtenerPorUsuario(usuario));
            return "solicitud/dashboard-negocio";
        } else if (usuario.getRol() == Enums.Rol.INSPECTOR) {
            model.addAttribute("inspeccionesPendientes",
                inspeccionService.obtenerPendientesPorInspector(usuario));
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

    private final SolicitudService solicitudService;
    private final LicenciaService  licenciaService;
    private final UsuarioRepository usuarioRepo;

    SolicitudController(SolicitudService solicitudService,
                        LicenciaService licenciaService,
                        UsuarioRepository usuarioRepo) {
        this.solicitudService = solicitudService;
        this.licenciaService  = licenciaService;
        this.usuarioRepo      = usuarioRepo;
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
                         @AuthenticationPrincipal UserDetails ud,
                         RedirectAttributes ra, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("rubros", Rubros.LISTA);
            return "solicitud/nueva";
        }
        Usuario usuario = usuarioRepo.findByUsername(ud.getUsername()).orElseThrow();
        Solicitud s = solicitudService.crearBorrador(dto, usuario);
        return "redirect:/solicitud/" + s.getId() + "/plano";
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
        model.addAttribute("solicitud", s);
        model.addAttribute("licencia", licenciaService.obtenerPorSolicitud(s));
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
