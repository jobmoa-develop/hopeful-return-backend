package com.jobmoa.hopefulreturn.followup.controller;

import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpRequestDto;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpResponseDto;
import com.jobmoa.hopefulreturn.followup.service.FollowUpService;
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

@Tag(name = "FollowUp")
@RestController
@RequestMapping("/followup")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @Operation(summary = "Create follow up")
    @PostMapping
    public FollowUpResponseDto create(@RequestBody FollowUpRequestDto requestDto) {
        return followUpService.create(requestDto);
    }

    @Operation(summary = "Find follow up by id")
    @GetMapping("/{id}")
    public FollowUpResponseDto findById(@PathVariable Long id) {
        return followUpService.findById(id);
    }

    @Operation(summary = "Find all follow ups")
    @GetMapping
    public List<FollowUpResponseDto> findAll() {
        return followUpService.findAll();
    }

    @Operation(summary = "Update follow up")
    @PutMapping("/{id}")
    public FollowUpResponseDto update(@PathVariable Long id, @RequestBody FollowUpRequestDto requestDto) {
        return followUpService.update(id, requestDto);
    }

    @Operation(summary = "Delete follow up")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        followUpService.delete(id);
    }
}
