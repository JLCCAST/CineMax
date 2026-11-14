package com.cinemax.usuariosservice.service.user;

import com.cinemax.common.cloudinary.CloudinaryService;
import com.cinemax.usuariosservice.clients.VenueFeignClient;
import com.cinemax.usuariosservice.model.dto.auth.UserRegisterDTO;
import com.cinemax.usuariosservice.model.dto.auth.ValidateCredentialsRequestDTO;
import com.cinemax.usuariosservice.model.dto.auth.ValidateCredentialsResponseDTO;
import com.cinemax.usuariosservice.model.dto.user.*;
import com.cinemax.usuariosservice.model.dto.venue.VenueDTO;
import com.cinemax.usuariosservice.model.entity.DocumentType;
import com.cinemax.usuariosservice.model.entity.Permission;
import com.cinemax.usuariosservice.model.entity.Role;
import com.cinemax.usuariosservice.model.entity.UserAccount;
import com.cinemax.usuariosservice.repository.RoleRepository;
import com.cinemax.usuariosservice.repository.UserAccountRepository;
import com.cinemax.usuariosservice.service.email.EmailService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final VenueFeignClient venueFeignClient;

    // Constantes para no quemar strings mágicos por todo el código
    private static final String ADMIN = "ADMIN";
    private static final String GERENTE_GENERAL = "GERENTE_GENERAL";
    private static final String GERENTE_MARKETING = "GERENTE_MARKETING";
    private static final String GERENTE_OPERACIONES = "GERENTE_OPERACIONES";
    private static final String CLIENTE = "CLIENTE";
    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_INACTIVO = "Inactivo";

    private boolean rolRequiereSede(String roleName) {
        return GERENTE_MARKETING.equals(roleName) || GERENTE_OPERACIONES.equals(roleName);
    }

    // ADMIN y GERENTE_GENERAL son roles globales únicos: no se atan a una sede
    // y solo puede existir un usuario activo con cada uno de estos roles.
    private boolean rolEsUnicoGlobal(String roleName) {
        return GERENTE_GENERAL.equals(roleName) || ADMIN.equals(roleName);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findPersonal(CLIENTE).stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    public UserResponseDTO createUser(UserCreateDTO request) {
        Role rol = roleRepository.findById(request.getIdRole())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado."));

        String roleName = rol.getRoleName();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: El correo ya está registrado.");
        }
        if (userRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new RuntimeException("Error: El número de documento ya está registrado.");
        }

        if (rolEsUnicoGlobal(roleName)) {
            if (userRepository.existsByRole_IdRoleAndStatus(rol.getIdRole(), ESTADO_ACTIVO)) {
                throw new RuntimeException("Error: Ya existe un usuario activo con rol " + roleName + " en CineMax.");
            }
            request.setIdVenue(null);
        } else if (rolRequiereSede(roleName)) {
            if (request.getIdVenue() == null || request.getIdVenue() == 0) {
                throw new RuntimeException("Error: Este rol requiere ser asignado a una Sede.");
            }
            if (userRepository.existsByRole_IdRoleAndIdVenueAndStatus(rol.getIdRole(), request.getIdVenue(), ESTADO_ACTIVO)) {
                throw new RuntimeException("Error: La sede seleccionada ya tiene un gerente activo de este tipo.");
            }
        }

        UserAccount nuevoUsuario = new UserAccount();
        nuevoUsuario.setFirstName(request.getFirstName());
        nuevoUsuario.setLastName(request.getLastName());
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setStatus(ESTADO_ACTIVO);
        nuevoUsuario.setDocumentNumber(request.getDocumentNumber());

        DocumentType docType = new DocumentType();
        docType.setIdDocType(request.getIdDocumentType());
        nuevoUsuario.setDocumentType(docType);

        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRole(rol);
        nuevoUsuario.setIdVenue(rolRequiereSede(roleName) ? request.getIdVenue() : null);

        UserAccount guardado = userRepository.save(nuevoUsuario);
        return mapToResponseDTO(guardado);
    }

    @Override
    public UserResponseDTO updateUserRole(Integer idUser, UserRoleUpdateDTO request) {
        UserAccount usuarioExistente = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        Role nuevoRol = roleRepository.findById(request.getIdRole())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado."));

        String roleName = nuevoRol.getRoleName();

        if (rolEsUnicoGlobal(roleName)) {
            UserAccount gerenteActual = userRepository.findByRole_IdRoleAndStatus(nuevoRol.getIdRole(), ESTADO_ACTIVO).orElse(null);
            if (gerenteActual != null && !gerenteActual.getIdUser().equals(idUser)) {
                throw new RuntimeException("Error: Ya existe un usuario activo con rol " + roleName + " en CineMax.");
            }
            usuarioExistente.setIdVenue(null);
        } else if (rolRequiereSede(roleName)) {
            if (request.getIdVenue() == null || request.getIdVenue() == 0) {
                throw new RuntimeException("Error: Este rol requiere ser asignado a una Sede.");
            }
            UserAccount gerenteSede = userRepository.findByRole_IdRoleAndIdVenueAndStatus(nuevoRol.getIdRole(), request.getIdVenue(), ESTADO_ACTIVO).orElse(null);

            if (gerenteSede != null && !gerenteSede.getIdUser().equals(idUser)) {
                throw new RuntimeException("Error: La sede seleccionada ya tiene un gerente activo de este tipo.");
            }
            usuarioExistente.setIdVenue(request.getIdVenue());
        } else {
            usuarioExistente.setIdVenue(null);
        }

        usuarioExistente.setRole(nuevoRol);
        UserAccount usuarioActualizado = userRepository.save(usuarioExistente);
        return mapToResponseDTO(usuarioActualizado);
    }

    @Override
    public void deleteUser(Integer idUser) {
        UserAccount usuario = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        usuario.setStatus(ESTADO_INACTIVO);
        userRepository.save(usuario);
    }

    @Override
    public void activateUser(Integer idUser) {
        UserAccount user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (user.getRole() != null) {
            String roleName = user.getRole().getRoleName();
            Integer idRole = user.getRole().getIdRole();

            if (rolEsUnicoGlobal(roleName)) {
                if (userRepository.existsByRole_IdRoleAndStatus(idRole, ESTADO_ACTIVO)) {
                    throw new RuntimeException("No se puede reactivar: Ya existe un usuario activo con rol " + roleName + ".");
                }
            } else if (rolRequiereSede(roleName) && user.getIdVenue() != null) {
                if (userRepository.existsByRole_IdRoleAndIdVenueAndStatus(idRole, user.getIdVenue(), ESTADO_ACTIVO)) {
                    throw new RuntimeException("No se puede reactivar: La sede con id " + user.getIdVenue() + " ya está ocupada.");
                }
            }
        }

        user.setStatus(ESTADO_ACTIVO);
        userRepository.save(user);
    }

    @Override
    public void registerClient(UserRegisterDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: El correo ya está registrado.");
        }
        if (userRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new RuntimeException("Error: El número de documento ya está registrado.");
        }

        // Antes: roleRepository.findAll().stream().filter(...).findFirst()
        // Ahora: un solo query, sin traer toda la tabla de roles a memoria
        Role rolCliente = roleRepository.findByRoleName(CLIENTE)
                .orElseThrow(() -> new RuntimeException("Rol CLIENTE no configurado en la base de datos."));

        UserAccount nuevoUsuario = new UserAccount();
        nuevoUsuario.setFirstName(request.getFirstName());
        nuevoUsuario.setLastName(request.getLastName());
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setStatus(ESTADO_ACTIVO);
        nuevoUsuario.setDocumentNumber(request.getDocumentNumber());

        DocumentType docType = new DocumentType();
        docType.setIdDocType(request.getIdDocumentType());
        nuevoUsuario.setDocumentType(docType);

        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRole(rolCliente);
        nuevoUsuario.setIdVenue(null); // Cliente no tiene sede

        userRepository.save(nuevoUsuario);
    }

    @Override
    public ValidateCredentialsResponseDTO validateCredentials(ValidateCredentialsRequestDTO request) {
        UserAccount user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !ESTADO_ACTIVO.equalsIgnoreCase(user.getStatus()) || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            ValidateCredentialsResponseDTO response = new ValidateCredentialsResponseDTO();
            response.setValid(false);
            return response;
        }

        List<String> permissions = user.getRole().getPermissions().stream()
                .map(Permission::getPermissionName)
                .toList();

        ValidateCredentialsResponseDTO response = new ValidateCredentialsResponseDTO();
        response.setValid(true);
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().getRoleName());
        response.setFirstName(user.getFirstName());
        response.setIdVenue(user.getIdVenue());
        response.setPermissions(permissions);
        return response;
    }

    @Override
    public void requestPasswordReset(String email) {
        UserAccount user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setTokenExpirationDate(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token);
            } catch (Exception e) {
                System.err.println("Error enviando email: " + e.getMessage());
            }
        });
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        UserAccount user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no existe."));

        if (user.getTokenExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El enlace ha expirado.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setTokenExpirationDate(null);
        userRepository.save(user);
    }

    @Override
    public boolean validateResetToken(String token) {
        return userRepository.findByResetPasswordToken(token)
                .map(user -> user.getTokenExpirationDate() != null && user.getTokenExpirationDate().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    public UserResponseDTO getMyProfile(String email) {
        UserAccount user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return mapToResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateProfile(UserUpdateDTO updateDTO, MultipartFile image, String email) {
        UserAccount user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (image != null && !image.isEmpty()) {
            try {
                String uploadedUrl = cloudinaryService.uploadImage(image);
                user.setImageUrl(uploadedUrl);
            } catch (Exception e) {
                throw new RuntimeException("Error al subir imagen a Cloudinary.");
            }
        }

        if (updateDTO.getFirstName() != null) user.setFirstName(updateDTO.getFirstName());
        if (updateDTO.getLastName() != null) user.setLastName(updateDTO.getLastName());
        if (updateDTO.getPhone() != null) user.setPhone(updateDTO.getPhone());
        if (updateDTO.getDatebirth() != null) user.setDatebirth(updateDTO.getDatebirth());

        if (updateDTO.getNewPassword() != null && !updateDTO.getNewPassword().isEmpty()) {
            if (updateDTO.getOldPassword() == null || !passwordEncoder.matches(updateDTO.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Contraseña actual incorrecta.");
            }
            user.setPassword(passwordEncoder.encode(updateDTO.getNewPassword()));
        }

        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    public void deleteMyAccount(String email) {
        UserAccount user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        if (!CLIENTE.equals(user.getRole().getRoleName())) {
            throw new RuntimeException("Los colaboradores no pueden eliminar su propia cuenta.");
        }
        user.setStatus(ESTADO_INACTIVO);
        userRepository.save(user);
    }

    @Override
    public List<VenueDTO> getAvailableVenuesForRole(Integer idRole) {
        Role rol = roleRepository.findById(idRole).orElseThrow(() -> new RuntimeException("Rol no encontrado."));

        if (!rolRequiereSede(rol.getRoleName())) {
            throw new RuntimeException("Este rol no requiere asignación de sede.");
        }

        List<VenueDTO> sedesActivas;
        try {
            sedesActivas = venueFeignClient.getVenues(ESTADO_ACTIVO);
        } catch (Exception e) {
            throw new RuntimeException("Servicio de Sucursales no disponible. Intenta nuevamente.");
        }

        List<Integer> idsOcupados = userRepository.findOccupiedVenueIdsByRole(idRole);
        return sedesActivas.stream().filter(v -> !idsOcupados.contains(v.getIdVenue())).toList();
    }

    private UserResponseDTO mapToResponseDTO(UserAccount usuario) {
        UserResponseDTO response = new UserResponseDTO();
        response.setIdUser(usuario.getIdUser());
        response.setFirstName(usuario.getFirstName());
        response.setLastName(usuario.getLastName());
        response.setEmail(usuario.getEmail());
        response.setStatus(usuario.getStatus());
        response.setDocumentNumber(usuario.getDocumentNumber());
        response.setPhone(usuario.getPhone());
        response.setDatebirth(usuario.getDatebirth());
        response.setImageUrl(usuario.getImageUrl());
        response.setIdVenue(usuario.getIdVenue());

        if (usuario.getRole() != null) {
            response.setRoleName(usuario.getRole().getRoleName());
            response.setIdRole(usuario.getRole().getIdRole());
        }
        return response;
    }
}