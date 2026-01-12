package com.lingframe.example.user.service.impl;

import com.lingframe.api.annotation.Auditable;
import com.lingframe.api.annotation.LingService;
import com.lingframe.api.annotation.RequiresPermission;
import com.lingframe.example.user.dto.UserDTO;
import com.lingframe.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 注入 JdbcTemplate -> 将被 LingFrame 自动代理
    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = "users", key = "#userId") // 被缓存代理拦截
    @LingService(id = "query_user", desc = "根据ID查询用户")
    @Override
    public Optional<UserDTO> queryUser(String userId) {
        String sql = "SELECT * FROM t_user WHERE id = ?";
        try {
            UserDTO user = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(UserDTO.class), userId);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Auditable(action = "list_users", resource = "user")
    @LingService(id = "list_users", desc = "列出所有用户")
    @Override
    public List<UserDTO> listUsers() {
        return jdbcTemplate.query("SELECT * FROM t_user", new BeanPropertyRowMapper<>(UserDTO.class));
    }

    @LingService(id = "create_user", desc = "创建新用户")
    @RequiresPermission("user:write") // 业务层权限 (可选)
    @Auditable(action = "CREATE_USER", resource = "user")
    @Override
    public UserDTO createUser(String name, String email) {
        // 执行 INSERT 操作 -> 将被 storage:sql, WRITE 权限拦截
        String sql = "INSERT INTO t_user (name, email) VALUES (?, ?)";
        jdbcTemplate.update(sql, name, email);

        // 简单返回一个对象（ID 暂不回填）
        return new UserDTO("0", name, email);
    }

    @LingService(id = "update_user", desc = "更新用户信息")
    @RequiresPermission("user:write")
    @Auditable(action = "UPDATE_USER", resource = "user")
    @Override
    public UserDTO updateUser(String id, String name, String email) {
        String sql = "UPDATE t_user SET name = ?, email = ? WHERE id = ?";
        jdbcTemplate.update(sql, name, email, id);
        return new UserDTO(id, name, email);
    }

    @LingService(id = "delete_user", desc = "删除用户")
    @RequiresPermission("user:delete")
    @Auditable(action = "DELETE_USER", resource = "user")
    @Override
    public boolean deleteUser(String id) {
        String sql = "DELETE FROM t_user WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    @Override
    public void saveUser(UserDTO userDTO) {
        createUser(userDTO.getName(), userDTO.getEmail());
    }
}