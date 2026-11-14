package com.cinemax.usuariosservice.repository;

import com.cinemax.usuariosservice.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByResetPasswordToken(String token);
    boolean existsByEmail(String email);
    boolean existsByDocumentNumber(String documentNumber);
    boolean existsByRole_IdRoleAndStatus(Integer idRole, String status);
    boolean existsByRole_IdRoleAndIdVenueAndStatus(Integer idRole, Integer idVenue, String status);

    Optional<UserAccount> findByRole_IdRoleAndStatus(Integer idRole, String status);
    Optional<UserAccount> findByRole_IdRoleAndIdVenueAndStatus(Integer idRole, Integer idVenue, String status);

    // Personal del cine: todos menos los clientes, filtrados en la base y no en memoria.
    @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.role r WHERE r IS NULL OR r.roleName <> :rolCliente ORDER BY u.idUser")
    List<UserAccount> findPersonal(@Param("rolCliente") String rolCliente);

    @Query("SELECT u.idVenue FROM UserAccount u WHERE u.role.idRole = :roleId AND u.status = 'Activo' AND u.idVenue IS NOT NULL")
    List<Integer> findOccupiedVenueIdsByRole(@Param("roleId") Integer roleId);
}