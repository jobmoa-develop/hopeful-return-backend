package com.jobmoa.hopefulreturn.participantmemo.controller;

import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoRequestDto;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoResponseDto;
import com.jobmoa.hopefulreturn.participantmemo.service.ParticipantMemoService;
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

@Tag(name = "ParticipantMemo")
@RestController
@RequestMapping("/participantmemo")
@RequiredArgsConstructor
public class ParticipantMemoController {

    private final ParticipantMemoService participantMemoService;

    @Operation(summary = "Create participant memo")
    @PostMapping
    public ParticipantMemoResponseDto create(@RequestBody ParticipantMemoRequestDto requestDto) {
        return participantMemoService.create(requestDto);
    }

    @Operation(summary = "Find participant memo by id")
    @GetMapping("/{id}")
    public ParticipantMemoResponseDto findById(@PathVariable Long id) {
        return participantMemoService.findById(id);
    }

    @Operation(summary = "Find all participant memos")
    @GetMapping
    public List<ParticipantMemoResponseDto> findAll() {
        return participantMemoService.findAll();
    }

    @Operation(summary = "Update participant memo")
    @PutMapping("/{id}")
    public ParticipantMemoResponseDto update(@PathVariable Long id, @RequestBody ParticipantMemoRequestDto requestDto) {
        return participantMemoService.update(id, requestDto);
    }

    @Operation(summary = "Delete participant memo")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        participantMemoService.delete(id);
    }
}
