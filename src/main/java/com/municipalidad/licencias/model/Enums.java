package com.municipalidad.licencias.model;

public class Enums {

    public enum Distrito {
        TRUJILLO,
        EL_PORVENIR,
        LA_ESPERANZA,
        FLORENCIA_DE_MORA,
        VICTOR_LARCO,
        HUANCHACO,
        MOCHE,
        LAREDO,
        SALAVERRY,
        SIMBAL,
        POROTO
    }

    public enum Rol {
        NEGOCIO,
        CAJERO,
        INSPECTOR,
        FISCALIZADOR,
        SUBGERENTE,
        GERENTE_DISTRITAL,
        GERENTE_MUNICIPAL,
        ADMIN
    }

    public enum CanalTramite {
        VIRTUAL,
        PRESENCIAL
    }

    public enum MetodoPago {
        EFECTIVO,
        QR,
        // Factura consolidada de un pago dividido en varias partes (efectivo y/o QR).
        // No representa dinero recibido por si misma: cada parte real queda registrada
        // por separado con su propio metodo, para no descuadrar el arqueo de caja.
        MIXTO
    }

    public enum EstadoFactura {
        PENDIENTE,
        PAGADA,
        ANULADA
    }

    public enum EstadoTramite {
        BORRADOR,
        PENDIENTE_VALIDACION,
        ADMITIDO,
        INSPECCION_PROGRAMADA,
        EN_INSPECCION,
        OBSERVADO,
        SEGUNDA_INSPECCION_PROGRAMADA,
        APROBADO,
        DENEGADO
    }

    public enum EstadoLicencia {
        VIGENTE,
        POR_VENCER,
        EXPIRADA,
        REVOCADA
    }

    public enum ResultadoInspeccion {
        PENDIENTE,
        CONFORME,
        CON_OBSERVACIONES
    }

    public enum TipoInspeccion {
        PRIMERA,
        SEGUNDA,
        OFICIO
    }

    public enum TipoObservacion {
        DOCUMENTAL,
        FISICA
    }

    public enum EstadoSesionCaja {
        PENDIENTE_APERTURA,
        ABIERTA,
        PENDIENTE_CIERRE,
        CERRADA,
        RECHAZADA
    }

}
