package com.barclays.testservice.service;

import com.barclays.testservice.exception.InvalidUserCredentialsSuppliedException;
import com.barclays.testservice.repository.UserRepository;
import com.barclays.testservice.util.JWTUtil;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public String getAuthenticationToken(String userId, String password) {
        var userDetails = loadUserByUsername(userId);

        if(!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new InvalidUserCredentialsSuppliedException();
        }

        return jwtUtil.generateToken(userDetails.getUsername());
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        var user = userRepository.findById(userId)
                .orElseThrow(InvalidUserCredentialsSuppliedException::new);

        return User.builder()
                .username(user.getId())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}
