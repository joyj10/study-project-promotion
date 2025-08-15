package com.shop.userservice.repository;

import com.shop.userservice.entity.User;
import com.shop.userservice.entity.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {
    List<UserLoginHistory> findByUserOrderByLoginTimeDesc(User user);
}