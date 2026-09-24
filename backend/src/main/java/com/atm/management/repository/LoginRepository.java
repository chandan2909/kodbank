package com.atm.management.repository;

import com.atm.management.entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginRepository extends JpaRepository<Login, String> {

    Optional<Login> findByCardNo(String cardNo);

    Optional<Login> findByUsername(String username);
}
