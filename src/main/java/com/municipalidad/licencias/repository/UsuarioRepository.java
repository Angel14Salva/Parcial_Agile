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
    java.util.List<Usuario> findByDistrito(com.municipalidad.licencias.model.Enums.Distrito distrito);
    java.util.List<Usuario> findByRolAndDistrito(com.municipalidad.licencias.model.Enums.Rol rol, com.municipalidad.licencias.model.Enums.Distrito distrito);
    java.util.Optional<Usuario> findByRolAndDistritoAndActivo(com.municipalidad.licencias.model.Enums.Rol rol, com.municipalidad.licencias.model.Enums.Distrito distrito, boolean activo);
}
