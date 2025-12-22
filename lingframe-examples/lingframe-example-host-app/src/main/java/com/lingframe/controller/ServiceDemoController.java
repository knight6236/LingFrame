package com.lingframe.controller;

import com.lingframe.api.annotation.LingReference;
import com.lingframe.example.order.api.OrderService;
import com.lingframe.example.order.dto.UserDTO;
import com.lingframe.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/service-demo")
public class ServiceDemoController {

    @Autowired
    private DemoService demoService;

    @LingReference
    private OrderService orderService;

    @GetMapping("/plugins/info")
    public String getPluginInfo() {
        return demoService.getPluginInfo();
    }

    @GetMapping("/user/{operation}")
    public String callUserService(@PathVariable String operation,
                                  @RequestParam(required = false) String param1,
                                  @RequestParam(required = false) String param2) {
        log.info("callUserService: operation = {}, param1 = {}, param2 = {}", operation, param1, param2);
        if (param1 != null && param2 != null) {
            return demoService.callUserService(operation, param1, param2);
        } else if (param1 != null) {
            return demoService.callUserService(operation, param1);
        } else {
            return demoService.callUserService(operation);
        }
    }

    @GetMapping("/order/{userId}")
    public UserDTO getUserById(@PathVariable Long userId) {
        return orderService.getUserById(userId);
    }
}