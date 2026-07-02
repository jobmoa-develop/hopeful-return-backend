package com.jobmoa.hopefulreturn.allowance.controller;

import com.jobmoa.hopefulreturn.allowance.model.dto.AllowanceRequestDto;
import com.jobmoa.hopefulreturn.allowance.model.dto.AllowanceResponseDto;
import com.jobmoa.hopefulreturn.allowance.service.AllowanceService;
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

@Tag(name = "Allowance")
@RestController
@RequestMapping("/allowance")
@RequiredArgsConstructor
public class AllowanceController {

    private final AllowanceService allowanceService;

    @Operation(summary = "Create allowance")
    @PostMapping
    public AllowanceResponseDto create(@RequestBody AllowanceRequestDto requestDto) {
        return allowanceService.create(requestDto);
    }

    @Operation(summary = "Find allowance by id")
    @GetMapping("/{id}")
    public AllowanceResponseDto findById(@PathVariable Long id) {
        return allowanceService.findById(id);
    }

    @Operation(summary = "Find all allowances")
    @GetMapping
    public List<AllowanceResponseDto> findAll() {
        return allowanceService.findAll();
    }

    @Operation(summary = "Update allowance")
    @PutMapping("/{id}")
    public AllowanceResponseDto update(@PathVariable Long id, @RequestBody AllowanceRequestDto requestDto) {
        return allowanceService.update(id, requestDto);
    }

    @Operation(summary = "Delete allowance")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        allowanceService.delete(id);
    }
}
