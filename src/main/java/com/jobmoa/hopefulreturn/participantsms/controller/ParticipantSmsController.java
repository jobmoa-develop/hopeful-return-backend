package com.jobmoa.hopefulreturn.participantsms.controller;

import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsRequestDto;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsResponseDto;
import com.jobmoa.hopefulreturn.participantsms.service.ParticipantSmsService;
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

@Tag(name = "ParticipantSms")
@RestController
@RequestMapping("/participantsms")
@RequiredArgsConstructor
public class ParticipantSmsController {

    private final ParticipantSmsService participantSmsService;

    @Operation(summary = "Create participant sms")
    @PostMapping
    public ParticipantSmsResponseDto create(@RequestBody ParticipantSmsRequestDto requestDto) {
        return participantSmsService.create(requestDto);
    }

    @Operation(summary = "Find participant sms by id")
    @GetMapping("/{id}")
    public ParticipantSmsResponseDto findById(@PathVariable Long id) {
        return participantSmsService.findById(id);
    }

    @Operation(summary = "Find all participant sms")
    @GetMapping
    public List<ParticipantSmsResponseDto> findAll() {
        return participantSmsService.findAll();
    }

    @Operation(summary = "Update participant sms")
    @PutMapping("/{id}")
    public ParticipantSmsResponseDto update(@PathVariable Long id, @RequestBody ParticipantSmsRequestDto requestDto) {
        return participantSmsService.update(id, requestDto);
    }

    @Operation(summary = "Delete participant sms")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        participantSmsService.delete(id);
    }
}
