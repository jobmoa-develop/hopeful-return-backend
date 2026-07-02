package com.jobmoa.hopefulreturn.attendance.controller;

import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceRequestDto;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponseDto;
import com.jobmoa.hopefulreturn.attendance.service.AttendanceService;
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

@Tag(name = "Attendance")
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "Create attendance")
    @PostMapping
    public AttendanceResponseDto create(@RequestBody AttendanceRequestDto requestDto) {
        return attendanceService.create(requestDto);
    }

    @Operation(summary = "Find attendance by id")
    @GetMapping("/{id}")
    public AttendanceResponseDto findById(@PathVariable Long id) {
        return attendanceService.findById(id);
    }

    @Operation(summary = "Find all attendances")
    @GetMapping
    public List<AttendanceResponseDto> findAll() {
        return attendanceService.findAll();
    }

    @Operation(summary = "Update attendance")
    @PutMapping("/{id}")
    public AttendanceResponseDto update(@PathVariable Long id, @RequestBody AttendanceRequestDto requestDto) {
        return attendanceService.update(id, requestDto);
    }

    @Operation(summary = "Delete attendance")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        attendanceService.delete(id);
    }
}
