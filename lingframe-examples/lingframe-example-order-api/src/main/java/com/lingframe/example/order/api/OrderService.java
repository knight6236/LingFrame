package com.lingframe.example.order.api;

import com.lingframe.example.order.dto.UserDTO;

public interface OrderService {

    UserDTO getUserById(Long userId);
}
