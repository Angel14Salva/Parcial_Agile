package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Licencia;
import com.municipalidad.licencias.model.Renovacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RenovacionRepository extends JpaRepository<Renovacion, Long> {
    List<Renovacion> findByLicencia(Licencia licencia);
}
