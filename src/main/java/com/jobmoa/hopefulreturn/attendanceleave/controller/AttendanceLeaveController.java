package com.jobmoa.hopefulreturn.attendanceleave.controller;

import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveRequestDto;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponseDto;
import com.jobmoa.hopefulreturn.attendanceleave.service.AttendanceLeaveService;
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

@Tag(name = "AttendanceLeave")
@RestController
@RequestMapping("/attendanceleave")
@RequiredArgsConstructor
public class AttendanceLeaveController {

    private final AttendanceLeaveService attendanceLeaveService;

    @Operation(summary = "Create attendance leave")
    @PostMapping
    public AttendanceLeaveResponseDto create(@RequestBody AttendanceLeaveRequestDto requestDto) {
        return attendanceLeaveService.create(requestDto);
    }

    @Operation(summary = "Find attendance leave by id")
    @GetMapping("/{id}")
    public AttendanceLeaveResponseDto findById(@PathVariable Long id) {
        return attendanceLeaveService.findById(id);
    }

    @Operation(summary = "Find all attendance leaves")
    @GetMapping
    public List<AttendanceLeaveResponseDto> findAll() {
        return attendanceLeaveService.findAll();
    }

    @Operation(summary = "Update attendance leave")
    @PutMapping("/{id}")
    public AttendanceLeaveResponseDto update(@PathVariable Long id, @RequestBody AttendanceLeaveRequestDto requestDto) {
        return attendanceLeaveService.update(id, requestDto);
    }

    @Operation(summary = "Delete attendance leave")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        attendanceLeaveService.delete(id);
    }
}
