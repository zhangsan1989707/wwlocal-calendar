package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.UserEntity;
import com.wwlocal.calendar.dto.Requests.UserRequest;
import com.wwlocal.calendar.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
public class UserController extends CrudController<UserEntity, UserRequest> {
    public UserController(UserService service) {
        super(service);
    }
}
