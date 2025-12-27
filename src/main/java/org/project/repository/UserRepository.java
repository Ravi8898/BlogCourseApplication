package org.project.repository;

import org.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String userName);
    boolean existsByUsername(String userName);
    Optional<User> findByIdAndIsActive(Long userId, String isActive);
    List<User> findByIsActive(String isActive);
    @Modifying
    @Transactional
    @Query("""
           UPDATE User u 
           SET u.isActive = 'N' 
           WHERE u.id = :id 
           AND u.isActive = 'Y'
           """)
    int deactivateUserById(@Param("id") Long id);


}
