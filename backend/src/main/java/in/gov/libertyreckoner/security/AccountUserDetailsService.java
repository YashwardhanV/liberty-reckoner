package in.gov.libertyreckoner.security;

import in.gov.libertyreckoner.domain.UserAccount;
import in.gov.libertyreckoner.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {
    private final UserAccountRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount account = repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .disabled(!account.isActive())
                .build();
    }
}

