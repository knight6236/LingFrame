package com.lingframe.example.order.service;

import com.lingframe.api.annotation.LingReference;
import com.lingframe.example.order.api.OrderService;
import com.lingframe.example.order.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderServiceImpl {

    @LingReference
    private OrderService orderService;

    public UserDTO getUserById(Long userId) {
        log.info("getUserById, userId: {}", userId);
        UserDTO userDTO = orderService.getUserById(userId);
        log.info("getUserById, userDTO: {}", userDTO);
        return userDTO;
    }
}
