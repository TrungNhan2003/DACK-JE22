package com.fashionshop.service;

import com.fashionshop.dto.AccountUpdateDTO;
import com.fashionshop.dto.RegisterDTO;
import com.fashionshop.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {

    User register(RegisterDTO dto);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    List<User> findAll();

    void toggleEnabled(Long id);

    boolean existsByEmail(String email);

    void updateAccount(String email, AccountUpdateDTO dto);
}