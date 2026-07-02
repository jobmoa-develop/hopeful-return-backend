package com.jobmoa.hopefulreturn.courseparticipant.controller;

import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantRequestDto;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantResponseDto;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
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

@Tag(name = "CourseParticipant")
@RestController
@RequestMapping("/courseparticipant")
@RequiredArgsConstructor
public class CourseParticipantController {

    private final CourseParticipantService courseParticipantService;

    @Operation(summary = "Create course participant")
    @PostMapping
    public CourseParticipantResponseDto create(@RequestBody CourseParticipantRequestDto requestDto) {
        return courseParticipantService.create(requestDto);
    }

    @Operation(summary = "Find course participant by id")
    @GetMapping("/{id}")
    public CourseParticipantResponseDto findById(@PathVariable Long id) {
        return courseParticipantService.findById(id);
    }

    @Operation(summary = "Find all course participants")
    @GetMapping
    public List<CourseParticipantResponseDto> findAll() {
        return courseParticipantService.findAll();
    }

    @Operation(summary = "Update course participant")
    @PutMapping("/{id}")
    public CourseParticipantResponseDto update(@PathVariable Long id, @RequestBody CourseParticipantRequestDto requestDto) {
        return courseParticipantService.update(id, requestDto);
    }

    @Operation(summary = "Delete course participant")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        courseParticipantService.delete(id);
    }
}
