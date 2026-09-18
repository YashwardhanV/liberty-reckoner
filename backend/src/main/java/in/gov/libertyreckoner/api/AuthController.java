package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.UserAccount;
import in.gov.libertyreckoner.repository.UserAccountRepository;
import in.gov.libertyreckoner.service.AuthService;
import in.gov.libertyreckoner.service.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserAccountRepository accounts;

    @Value("${libertyreckoner.jwt.secure-cookie}")
    private boolean secureCookie;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse result = authService.login(request);
        ResponseCookie cookie = ResponseCookie.from("LIBERTY_RECKONER_SESSION", result.accessToken())
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/")
                .maxAge(result.expiresInSeconds()).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return new LoginResponse(null, result.expiresInSeconds(), result.user());
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("LIBERTY_RECKONER_SESSION", "")
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @GetMapping("/me")
    public UserView me(Authentication authentication) {
        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return new UserView(account.getId(), account.getFullName(), account.getEmail(), account.getRole());
    }

    @GetMapping("/csrf")
    public java.util.Map<String, String> csrf(CsrfToken token) {
        return java.util.Map.of("token", token.getToken());
    }
}
