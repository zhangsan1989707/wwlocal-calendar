package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.UserEntity;
import com.wwlocal.calendar.dto.Requests.UserRequest;
import com.wwlocal.calendar.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseCrudService<UserEntity, UserRequest> {
    public UserService(UserRepository repository) {
        super(repository);
    }
}
