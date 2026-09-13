package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.UserAccount;
import in.gov.libertyreckoner.repository.UserAccountRepository;
import in.gov.libertyreckoner.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserAccountRepository accounts;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(), request.password()));
        UserDetails details = userDetailsService.loadUserByUsername(request.email());
        UserAccount account = accounts.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return new LoginResponse(jwtService.generate(details), jwtService.expirationSeconds(),
                new UserView(account.getId(), account.getFullName(), account.getEmail(), account.getRole()));
    }
}

