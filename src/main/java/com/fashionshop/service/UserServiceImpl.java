package com.fashionshop.service;

import com.fashionshop.dto.AccountUpdateDTO;
import com.fashionshop.dto.RegisterDTO;
import com.fashionshop.entity.User;
import com.fashionshop.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterDTO dto) {
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(User.Role.ROLE_USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void toggleEnabled(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(!user.getEnabled());
            userRepository.save(user);
        });
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updateAccount(String email, AccountUpdateDTO dto) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFullName(dto.getFullName());
            user.setPhone(dto.getPhone());
            user.setAddress(dto.getAddress());
            userRepository.save(user);
        });
    }
}