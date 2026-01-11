package com.lingframe.example.order.api;

import com.lingframe.example.order.dto.UserDTO;

import java.util.Optional;

public interface UserQueryService {

    Optional<UserDTO> findById(Long userId);
}
