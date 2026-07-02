package com.jobmoa.hopefulreturn.coursestaff.controller;

import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffRequestDto;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffResponseDto;
import com.jobmoa.hopefulreturn.coursestaff.service.CourseStaffService;
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

@Tag(name = "CourseStaff")
@RestController
@RequestMapping("/coursestaff")
@RequiredArgsConstructor
public class CourseStaffController {

    private final CourseStaffService courseStaffService;

    @Operation(summary = "Create course staff")
    @PostMapping
    public CourseStaffResponseDto create(@RequestBody CourseStaffRequestDto requestDto) {
        return courseStaffService.create(requestDto);
    }

    @Operation(summary = "Find course staff by id")
    @GetMapping("/{id}")
    public CourseStaffResponseDto findById(@PathVariable Long id) {
        return courseStaffService.findById(id);
    }

    @Operation(summary = "Find all course staff")
    @GetMapping
    public List<CourseStaffResponseDto> findAll() {
        return courseStaffService.findAll();
    }

    @Operation(summary = "Update course staff")
    @PutMapping("/{id}")
    public CourseStaffResponseDto update(@PathVariable Long id, @RequestBody CourseStaffRequestDto requestDto) {
        return courseStaffService.update(id, requestDto);
    }

    @Operation(summary = "Delete course staff")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        courseStaffService.delete(id);
    }
}
