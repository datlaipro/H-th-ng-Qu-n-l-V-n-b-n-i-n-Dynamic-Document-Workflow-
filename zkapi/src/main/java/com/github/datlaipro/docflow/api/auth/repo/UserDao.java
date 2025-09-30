package com.github.datlaipro.docflow.api.auth.repo;

import com.github.datlaipro.docflow.api.auth.entity.User;

public interface UserDao {
    User findByEmail(String email) throws Exception;
    User findById(long id) throws Exception;
    String findPasswordHashByEmail(String email) throws Exception;
    long createEmployee(String email, String fullName, String bcryptHash) throws Exception;
}
