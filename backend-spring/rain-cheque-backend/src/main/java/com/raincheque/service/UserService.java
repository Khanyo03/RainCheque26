package com.raincheque.service;

import com.raincheque.entity.User;
import com.raincheque.exception.EmailAlreadyRegisteredException;
import com.raincheque.exception.InvalidCredentialsException;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User registerUser(String email, String rawPassword, String fullName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .fullName(fullName)
            .role(User.Role.USER)
            .emailVerified(false)
            .build();

        return userRepository.save(user);
    }
    
    @Transactional
    public User authenticateUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
