package com.zeepseek.backend.domain.user.repository;

import com.zeepseek.backend.domain.user.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Integer> {
}