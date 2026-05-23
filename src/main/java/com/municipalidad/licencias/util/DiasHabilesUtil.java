package com.municipalidad.licencias.util;

import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Component
public class DiasHabilesUtil {

    // Feriados nacionales del Perú (año actual - se puede externalizar a DB)
    private static final Set<String> FERIADOS = Set.of(
        "01-01", // Año nuevo
        "04-17", // Jueves Santo (variable - ejemplo fijo)
        "04-18", // Viernes Santo
        "05-01", // Día del Trabajo
        "06-07", // Batalla de Arica
        "06-29", // San Pedro y San Pablo
        "07-28", // Fiestas Patrias
        "07-29", // Fiestas Patrias
        "08-06", // Batalla de Junín
        "08-30", // Santa Rosa de Lima
        "10-08", // Combate de Angamos
        "11-01", // Todos los Santos
        "12-08", // Inmaculada Concepción
        "12-25"  // Navidad
    );

    public boolean esDiaHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) return false;
        String mmdd = String.format("%02d-%02d", fecha.getMonthValue(), fecha.getDayOfMonth());
        return !FERIADOS.contains(mmdd);
    }

    public LocalDate sumarDiasHabiles(LocalDate inicio, int dias) {
        LocalDate fecha = inicio;
        int contador = 0;
        while (contador < dias) {
            fecha = fecha.plusDays(1);
            if (esDiaHabil(fecha)) contador++;
        }
        return fecha;
    }

    public LocalDate siguienteDiaHabil(LocalDate fecha) {
        LocalDate siguiente = fecha;
        while (!esDiaHabil(siguiente)) {
            siguiente = siguiente.plusDays(1);
        }
        return siguiente;
    }
}
