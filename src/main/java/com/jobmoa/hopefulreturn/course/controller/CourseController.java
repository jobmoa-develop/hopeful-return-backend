package com.jobmoa.hopefulreturn.course.controller;

import com.jobmoa.hopefulreturn.course.model.dto.CourseRequestDto;
import com.jobmoa.hopefulreturn.course.model.dto.CourseResponseDto;
import com.jobmoa.hopefulreturn.course.service.CourseService;
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

@Tag(name = "Course")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Create course")
    @PostMapping
    public CourseResponseDto create(@RequestBody CourseRequestDto requestDto) {
        return courseService.create(requestDto);
    }

    @Operation(summary = "Find course by id")
    @GetMapping("/{id}")
    public CourseResponseDto findById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @Operation(summary = "Find all courses")
    @GetMapping
    public List<CourseResponseDto> findAll() {
        return courseService.findAll();
    }

    @Operation(summary = "Update course")
    @PutMapping("/{id}")
    public CourseResponseDto update(@PathVariable Long id, @RequestBody CourseRequestDto requestDto) {
        return courseService.update(id, requestDto);
    }

    @Operation(summary = "Delete course")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
