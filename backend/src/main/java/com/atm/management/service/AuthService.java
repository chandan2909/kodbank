package com.atm.management.service;

import com.atm.management.dto.request.LoginRequest;
import com.atm.management.dto.request.RegisterRequest;
import com.atm.management.dto.request.ResetPinRequest;
import com.atm.management.dto.response.JwtResponse;
import com.atm.management.dto.response.MessageResponse;
import com.atm.management.dto.response.RefreshResponse;
import com.atm.management.dto.response.RegistrationResponse;
import com.atm.management.dto.response.SecurityQuestionResponse;
import com.atm.management.entity.Login;
import com.atm.management.entity.Signup;
import com.atm.management.exception.AccountLockedException;
import com.atm.management.exception.BusinessRuleException;
import com.atm.management.exception.ConflictException;
import com.atm.management.exception.InvalidCredentialsException;
import com.atm.management.exception.NotFoundException;
import com.atm.management.repository.LoginRepository;
import com.atm.management.repository.SignupRepository;
import com.atm.management.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final SignupRepository signupRepository;
    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PinValidator pinValidator;
    private final AccountSecurityService accountSecurityService;
    private final RefreshTokenService refreshTokenService;
    private final AccountService accountService;
    private final int maxAttempts;
    private final BigDecimal openingBalance;
    private final BigDecimal dailyLimit;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            SignupRepository signupRepository,
            LoginRepository loginRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PinValidator pinValidator,
            AccountSecurityService accountSecurityService,
            RefreshTokenService refreshTokenService,
            AccountService accountService,
            @Value("${app.security.max-login-attempts}") int maxAttempts,
            @Value("${app.account.opening-balance}") long openingBalance,
            @Value("${app.account.daily-limit}") long dailyLimit) {
        this.signupRepository = signupRepository;
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.pinValidator = pinValidator;
        this.accountSecurityService = accountSecurityService;
        this.refreshTokenService = refreshTokenService;
        this.accountService = accountService;
        this.maxAttempts = maxAttempts;
        this.openingBalance = BigDecimal.valueOf(openingBalance);
        this.dailyLimit = BigDecimal.valueOf(dailyLimit);
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        LocalDate dob = request.parsedDob();
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        if (dob.isAfter(eighteenYearsAgo)) {
            throw new BusinessRuleException("You must be at least 18 years old to register");
        }

        if (signupRepository.findByPan(request.pan()).isPresent()) {
            throw new ConflictException("An account with this PAN number already exists");
        }
        if (signupRepository.findByAadhar(request.aadhar()).isPresent()) {
            throw new ConflictException("An account with this Aadhar number already exists");
        }

        String formNo = generateFormNo();
        String cardNo = generateCardNumber();
        String rawPin = generatePin();

        pinValidator.validate(rawPin);

        Signup signup = new Signup();
        signup.setFormNo(formNo);
        signup.setName(request.name().trim());
        signup.setFatherName(request.fatherName().trim());
        signup.setDob(dob);
        signup.setGender(request.gender());
        signup.setEmail(request.email().trim());
        signup.setMarital(request.marital());
        signup.setAddress(request.address().trim());
        signup.setCity(request.city().trim());
        signup.setState(request.state());
        signup.setPincode(request.pincode());
        signup.setReligion(request.religion());
        signup.setCategory(request.category());
        signup.setIncome(request.income());
        signup.setEducation(request.education());
        signup.setOccupation(request.occupation());
        signup.setPan(request.pan().trim().toUpperCase());
        signup.setAadhar(request.aadhar().trim());
        signup.setSenior(request.seniorCitizen());
        signup.setExisting(request.existingAccount());
        signup.setAccountType(request.accountType());
        signup.setFacilities(request.facilities());
        signup.setCreatedAt(LocalDateTime.now());
        signupRepository.save(signup);

        var account = accountService.createAccountReturning(
                formNo, request.accountType(), openingBalance, dailyLimit);

        Login login = new Login();
        login.setCardNo(cardNo);
        login.setFormNo(formNo);
        login.setPin(passwordEncoder.encode(rawPin));
        login.setFailedAttempts(0);
        login.setLocked(0);
        login.setSecurityQuestion(request.securityQuestion());
        login.setSecurityAnswer(passwordEncoder.encode(request.securityAnswer().trim()));
        login.setRole("USER");
        login.setAccountId(account.getAccountId());
        login.setCreatedAt(LocalDateTime.now());
        loginRepository.save(login);

        return new RegistrationResponse(formNo, cardNo, rawPin, account.getAccountNo());
    }

    public JwtResponse login(LoginRequest request) {
        boolean hasCard = request.cardNumber() != null && !request.cardNumber().isBlank();
        boolean hasUsername = request.username() != null && !request.username().isBlank();
        if (hasCard == hasUsername) {
            throw new InvalidCredentialsException(
                    "Provide either Card Number or Username with PIN");
        }

        Login login;
        if (hasUsername) {
            String username = request.username().trim();
            login = loginRepository.findByUsername(username)
                    .orElseThrow(() -> new InvalidCredentialsException(
                            "Incorrect Username or PIN"));
            if (!"ADMIN".equalsIgnoreCase(login.getRole())) {
                throw new InvalidCredentialsException("Incorrect Username or PIN");
            }
        } else {
            login = loginRepository.findByCardNo(request.cardNumber().trim())
                    .orElseThrow(() -> new InvalidCredentialsException(
                            "Incorrect Card Number or PIN"));
        }

        if (login.isLocked()) {
            throw new AccountLockedException(
                    "This account is locked due to security concerns. Please contact your bank for assistance.");
        }

        if (!passwordEncoder.matches(request.pin(), login.getPin())) {
            accountSecurityService.recordFailedAttempt(login.getCardNo());

            if (accountSecurityService.isLocked(login.getCardNo())) {
                throw new AccountLockedException(
                        "Too many failed attempts. Your account has been locked. Please contact your bank for assistance.");
            }
            int remaining = accountSecurityService.remainingAttempts(login.getCardNo());
            String idLabel = hasUsername ? "Username" : "Card Number";
            throw new InvalidCredentialsException(
                    String.format("Incorrect %s or PIN. Attempts remaining: %d", idLabel, remaining));
        }

        accountSecurityService.resetFailedAttempts(login.getCardNo());

        String role = login.getRole() == null || login.getRole().isBlank() ? "USER" : login.getRole();
        String token = jwtService.generateToken(login.getCardNo(), role);
        String refreshToken = refreshTokenService.issue(login.getCardNo());
        return new JwtResponse(
                token,
                refreshToken,
                login.getCardNo(),
                login.getUsername(),
                role,
                jwtService.getExpirationMs());
    }

    public SecurityQuestionResponse getSecurityQuestion(String cardNumber) {
        Login login = loginRepository.findByCardNo(cardNumber)
                .orElseThrow(() -> new NotFoundException("Unable to retrieve security question for this card."));

        if (login.isLocked()) {
            throw new AccountLockedException(
                    "This account is locked due to security concerns. Please contact your bank for assistance.");
        }
        if (login.getSecurityQuestion() == null || login.getSecurityQuestion().isBlank()) {
            throw new NotFoundException("Unable to retrieve security question for this card.");
        }

        return new SecurityQuestionResponse(login.getCardNo(), login.getSecurityQuestion());
    }

    private boolean isLegacySecurityAnswer(String stored) {
        return stored != null && !stored.isBlank() && !stored.startsWith("$2");
    }

    private boolean matchesSecurityAnswer(String stored, String provided) {
        if (stored == null || stored.isBlank() || provided == null || provided.isBlank()) {
            return false;
        }
        if (isLegacySecurityAnswer(stored)) {
            return stored.trim().equalsIgnoreCase(provided.trim());
        }
        return passwordEncoder.matches(provided.trim(), stored);
    }

    public MessageResponse resetPin(ResetPinRequest request) {
        Login login = loginRepository.findByCardNo(request.cardNumber())
                .orElseThrow(() -> new NotFoundException("Unable to retrieve security question for this card."));

        if (login.isLocked()) {
            throw new AccountLockedException(
                    "This account is locked due to security concerns. Please contact your bank for assistance.");
        }

        boolean answerMatches = matchesSecurityAnswer(login.getSecurityAnswer(), request.securityAnswer());

        if (!answerMatches) {
            accountSecurityService.recordFailedAttempt(login.getCardNo());

            if (accountSecurityService.isLocked(login.getCardNo())) {
                throw new AccountLockedException(
                        "Too many failed security verification attempts. Your account has been locked. Please contact your bank for assistance.");
            }
            throw new InvalidCredentialsException("Incorrect answer to security question.");
        }

        if (answerMatches && isLegacySecurityAnswer(login.getSecurityAnswer())) {
            login.setSecurityAnswer(passwordEncoder.encode(request.securityAnswer().trim()));
            loginRepository.save(login);
        }

        if (!request.newPin().equals(request.confirmPin())) {
            throw new BusinessRuleException("PINs do not match");
        }

        pinValidator.validate(request.newPin());

        login.setPin(passwordEncoder.encode(request.newPin()));
        login.setFailedAttempts(0);
        loginRepository.save(login);

        return new MessageResponse(
                "PIN has been successfully reset. Please login with your new PIN.");
    }

    public MessageResponse changePin(String cardNumber, String newPin, String confirmPin) {
        Login login = loginRepository.findByCardNo(cardNumber)
                .orElseThrow(() -> new NotFoundException("Account not found."));

        if (login.isLocked()) {
            throw new AccountLockedException(
                    "This account is locked. Please contact your bank for assistance.");
        }
        if (!newPin.equals(confirmPin)) {
            throw new BusinessRuleException("PINs do not match");
        }

        pinValidator.validate(newPin);

        login.setPin(passwordEncoder.encode(newPin));
        loginRepository.save(login);

        return new MessageResponse("PIN changed successfully");
    }

    public RefreshResponse refresh(String rawRefreshToken) {
        String cardNo = refreshTokenService.rotate(rawRefreshToken);
        Login login = loginRepository.findByCardNo(cardNo)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));
        if (login.isLocked()) {
            throw new AccountLockedException("This account is locked.");
        }
        String role = login.getRole() == null || login.getRole().isBlank() ? "USER" : login.getRole();
        String newAccess = jwtService.generateToken(cardNo, role);
        String newRefresh = refreshTokenService.issue(cardNo);
        return new RefreshResponse(
                newAccess,
                newRefresh,
                cardNo,
                login.getUsername(),
                role,
                jwtService.getExpirationMs());
    }

    private String generateFormNo() {
        String formNo;
        do {
            formNo = String.format("%04d", random.nextInt(10000));
        } while (signupRepository.existsById(formNo));
        return formNo;
    }

    private String generateCardNumber() {
        String cardNo;
        do {
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));
            }
            cardNo = sb.toString();
        } while (loginRepository.existsById(cardNo));
        return cardNo;
    }

    private String generatePin() {
        String pin;
        do {
            pin = String.format("%04d", random.nextInt(10000));
        } while (!isSafeGeneratedPin(pin));
        return pin;
    }

    private boolean isSafeGeneratedPin(String pin) {
        try {
            pinValidator.validate(pin);
            return true;
        } catch (BusinessRuleException e) {
            return false;
        }
    }
}
