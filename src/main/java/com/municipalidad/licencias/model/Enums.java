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
        INSPECTOR,
        FISCALIZADOR,
        SUBGERENTE,
        GERENTE_DISTRITAL,
        GERENTE_MUNICIPAL,
        ADMIN
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

    
}
