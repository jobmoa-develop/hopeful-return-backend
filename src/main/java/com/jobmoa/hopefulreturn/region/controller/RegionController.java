package com.jobmoa.hopefulreturn.region.controller;

import com.jobmoa.hopefulreturn.region.model.dto.RegionRequestDto;
import com.jobmoa.hopefulreturn.region.model.dto.RegionResponseDto;
import com.jobmoa.hopefulreturn.region.service.RegionService;
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

@Tag(name = "Region")
@RestController
@RequestMapping("/region")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "Create region")
    @PostMapping
    public RegionResponseDto create(@RequestBody RegionRequestDto requestDto) {
        return regionService.create(requestDto);
    }

    @Operation(summary = "Find region by id")
    @GetMapping("/{id}")
    public RegionResponseDto findById(@PathVariable Long id) {
        return regionService.findById(id);
    }

    @Operation(summary = "Find all regions")
    @GetMapping
    public List<RegionResponseDto> findAll() {
        return regionService.findAll();
    }

    @Operation(summary = "Update region")
    @PutMapping("/{id}")
    public RegionResponseDto update(@PathVariable Long id, @RequestBody RegionRequestDto requestDto) {
        return regionService.update(id, requestDto);
    }

    @Operation(summary = "Delete region")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        regionService.delete(id);
    }
}
