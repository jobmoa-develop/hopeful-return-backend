package com.jobmoa.hopefulreturn.participant.controller;

import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantRequestDto;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponseDto;
import com.jobmoa.hopefulreturn.participant.service.ParticipantService;
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

@Tag(name = "Participant")
@RestController
@RequestMapping("/participant")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(summary = "Create participant")
    @PostMapping
    public ParticipantResponseDto create(@RequestBody ParticipantRequestDto requestDto) {
        return participantService.create(requestDto);
    }

    @Operation(summary = "Find participant by id")
    @GetMapping("/{id}")
    public ParticipantResponseDto findById(@PathVariable Long id) {
        return participantService.findById(id);
    }

    @Operation(summary = "Find all participants")
    @GetMapping
    public List<ParticipantResponseDto> findAll() {
        return participantService.findAll();
    }

    @Operation(summary = "Update participant")
    @PutMapping("/{id}")
    public ParticipantResponseDto update(@PathVariable Long id, @RequestBody ParticipantRequestDto requestDto) {
        return participantService.update(id, requestDto);
    }

    @Operation(summary = "Delete participant")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        participantService.delete(id);
    }
}
