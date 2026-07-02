package com.jobmoa.hopefulreturn.users.controller;

import com.jobmoa.hopefulreturn.users.model.dto.UsersRequestDto;
import com.jobmoa.hopefulreturn.users.model.dto.UsersResponseDto;
import com.jobmoa.hopefulreturn.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @Operation(summary = "Create users")
    @PostMapping
    public UsersResponseDto create(@RequestBody UsersRequestDto requestDto) {
        return usersService.create(requestDto);
    }

    @Operation(summary = "Find users by id")
    @GetMapping("/{id}")
    public UsersResponseDto findById(@PathVariable Long id) {
        return usersService.findById(id);
    }

    @Operation(summary = "Find all users")
    @GetMapping
    public List<UsersResponseDto> findAll() {
        return usersService.findAll();
    }

    @Operation(summary = "Update users")
    @PutMapping("/{id}")
    public UsersResponseDto update(@PathVariable Long id, @RequestBody UsersRequestDto requestDto) {
        return usersService.update(id, requestDto);
    }

    @Operation(summary = "Delete users")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        usersService.delete(id);
    }
}
