package com.example.qubaatisystem.Security;

import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads the Basic Auth principal by username (mirrors SecEX's {@code MyUserDetails}). The {@link User} entity
 * itself implements {@code UserDetails}, so it is returned directly; the password (BCrypt hash) and authority
 * (role name) come from the entity. Returns a {@link UsernameNotFoundException} when the user does not exist.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
        return user;
    }
}
