package com.atm.management.service;

import com.atm.management.entity.Login;
import com.atm.management.repository.LoginRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSecurityService {

    private final LoginRepository loginRepository;
    private final int maxAttempts;

    public AccountSecurityService(
            LoginRepository loginRepository,
            @Value("${app.security.max-login-attempts}") int maxAttempts) {
        this.loginRepository = loginRepository;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Commits the failed attempt in an independent transaction so it is
     * not rolled back when the caller throws an auth exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String cardNo) {
        loginRepository.findByCardNo(cardNo).ifPresent(login -> {
            login.setFailedAttempts(login.getFailedAttempts() + 1);
            if (login.getFailedAttempts() >= maxAttempts) {
                login.lock();
            }
            loginRepository.save(login);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(String cardNo) {
        loginRepository.findByCardNo(cardNo).ifPresent(login -> {
            login.setFailedAttempts(0);
            loginRepository.save(login);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlock(String cardNo) {
        loginRepository.findByCardNo(cardNo).ifPresent(login -> {
            login.setFailedAttempts(0);
            login.unlock();
            loginRepository.save(login);
        });
    }

    public boolean isLocked(String cardNo) {
        return loginRepository.findByCardNo(cardNo)
                .map(Login::isLocked)
                .orElse(false);
    }

    public int remainingAttempts(String cardNo) {
        return loginRepository.findByCardNo(cardNo)
                .map(login -> Math.max(0, maxAttempts - login.getFailedAttempts()))
                .orElse(0);
    }
}
