package com.municipalidad.licencias.repository;

import com.municipalidad.licencias.model.Enums;
import com.municipalidad.licencias.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findByRol(Enums.Rol rol);
}
