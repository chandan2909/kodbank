package com.atm.management.security;

import com.atm.management.repository.LoginRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CardUserDetailsService implements UserDetailsService {

    private final LoginRepository loginRepository;

    public CardUserDetailsService(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String cardNumber) throws UsernameNotFoundException {
        return loginRepository.findByCardNo(cardNumber)
                .map(login -> User.builder()
                        .username(login.getCardNo())
                        .password(login.getPin())
                        .authorities(() -> "ADMIN".equalsIgnoreCase(login.getRole())
                                ? "ROLE_ADMIN"
                                : "ROLE_USER")
                        .build())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Card number not found: " + cardNumber));
    }
}
