package com.atm.management.repository;

import com.atm.management.entity.Signup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignupRepository extends JpaRepository<Signup, String> {

    Optional<Signup> findByPan(String pan);

    Optional<Signup> findByAadhar(String aadhar);

    Optional<Signup> findByEmail(String email);
}
