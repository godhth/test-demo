package com.webox.webox.service;

import com.webox.webox.dto.RegisterForm;
import com.webox.webox.entity.User;
import com.webox.webox.model.LoginUser;
import com.webox.webox.repository.UserRepository;
import com.webox.webox.support.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginUser register(RegisterForm form) {
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            throw new BusinessException("邮箱已注册");
        }
        String salt = passwordEncoder.newSalt();
        String hash = passwordEncoder.hash(form.getPassword(), salt);
        User u = new User();
        u.setEmail(form.getEmail());
        u.setName(form.getName());
        u.setSalt(salt);
        u.setPasswordHash(hash);
        u.setCreatedAt(Instant.now());
        u = userRepository.save(u);
        return new LoginUser(u.getId(), u.getEmail(), u.getName());
    }

    public Optional<LoginUser> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getSalt(), u.getPasswordHash()))
                .map(u -> new LoginUser(u.getId(), u.getEmail(), u.getName()));
    }
}
