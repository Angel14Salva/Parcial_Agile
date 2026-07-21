package com.municipalidad.licencias.config;
import com.municipalidad.licencias.model.Usuario;
import com.municipalidad.licencias.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioRepository usuarioRepo;

    public SecurityConfig(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/auth/**", "/error", "/seguimiento", "/publico/**", "/").permitAll()
                .requestMatchers("/pago/**").permitAll()
                .requestMatchers("/cajero/pago/qr/confirmar", "/cajero/pago/qr/retorno").permitAll()
                .requestMatchers("/publico/licencia/*/renovar", "/publico/licencia/*/renovar/flow", "/publico/licencia/*/renovar/retorno", "/publico/licencia/*/renovar/confirmar").permitAll()
                .requestMatchers("/solicitud/nueva", "/solicitud/*/pago", "/solicitud/*/pago/flow", "/solicitud/*/pago/retorno", "/solicitud/*/plano", "/solicitud/*/observaciones", "/solicitud/*/observaciones/subsanar", "/solicitud/*/observacion/*/subsanar").permitAll()
                .requestMatchers("/multas/*/detalle").authenticated()
                .requestMatchers("/api/validar/**").permitAll()
                .requestMatchers("/inspector/**").hasAnyRole("INSPECTOR","FISCALIZADOR")
                .requestMatchers("/cajero/**").hasAnyRole("CAJERO", "ADMIN")
                .requestMatchers("/subgerente/**").hasAnyRole("SUBGERENTE", "ADMIN", "GERENTE_MUNICIPAL")
                .requestMatchers("/solicitud/*/detalle").hasAnyRole("SUBGERENTE", "ADMIN", "GERENTE_MUNICIPAL", "GERENTE_DISTRITAL", "INSPECTOR", "FISCALIZADOR")
                .requestMatchers("/gerente/**").hasAnyRole("GERENTE_DISTRITAL", "ADMIN", "GERENTE_MUNICIPAL")
                .requestMatchers("/gerente-municipal/**").hasAnyRole("GERENTE_MUNICIPAL", "ADMIN")
                .requestMatchers("/fiscalizacion/**").hasAnyRole("INSPECTOR", "FISCALIZADOR", "SUBGERENTE", "ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "GERENTE_MUNICIPAL", "GERENTE_DISTRITAL")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/auth/logout"))
                .logoutSuccessUrl("/auth/login?logout=true")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/validar/**")
                .ignoringRequestMatchers("/pago/**")
                .ignoringRequestMatchers("/cajero/pago/qr/confirmar")
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Usuario usuario = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
            return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
