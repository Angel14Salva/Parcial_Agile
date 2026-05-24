package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Licencia;
import com.municipalidad.licencias.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MultaRepository extends JpaRepository<Multa, Long> {
    List<Multa> findByLicenciaOrderByCreadoEnDesc(Licencia licencia);
    List<Multa> findAllByOrderByCreadoEnDesc();
}
