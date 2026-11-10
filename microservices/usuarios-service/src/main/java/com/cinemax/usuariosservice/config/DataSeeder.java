package com.cinemax.usuariosservice.config;

import com.cinemax.usuariosservice.model.entity.DocumentType;
import com.cinemax.usuariosservice.model.entity.Role;
import com.cinemax.usuariosservice.model.entity.UserAccount;
import com.cinemax.usuariosservice.repository.DocumentTypeRepository;
import com.cinemax.usuariosservice.repository.RoleRepository;
import com.cinemax.usuariosservice.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@cinemax.pe")) {
            return; // idempotente: no duplica en cada arranque
        }

        Role rolAdmin = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol ADMIN no existe. Revisa la migración V2__datos_semilla.sql."));

        DocumentType dni = documentTypeRepository.findByDocName("DNI")
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de documento DNI no existe. Revisa la migración V2__datos_semilla.sql."));

        UserAccount admin = new UserAccount();
        admin.setFirstName("Admin");
        admin.setLastName("CineMax");
        admin.setEmail("admin@cinemax.pe");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setDocumentNumber("00000001");
        admin.setDocumentType(dni);
        admin.setRole(rolAdmin);
        admin.setStatus("Activo");

        userRepository.save(admin);
        System.out.println(">>> Usuario semilla creado: admin@cinemax.pe / Admin123!");
    }
}