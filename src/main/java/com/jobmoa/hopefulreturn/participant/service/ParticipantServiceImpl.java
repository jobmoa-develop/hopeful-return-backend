package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.participant.support.MatchKeyGenerator;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ParticipantRepository participantRepository;

    @Override
    public ParticipantCreatedResponse create(CreateParticipantRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ParticipantEntity participant = ParticipantEntity.builder()
                .name(request.name())
                .birthYear(request.birthYear())
                .phone(request.phone())
                .matchKey(MatchKeyGenerator.generate(request.name(), request.birthYear(), request.phone()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        ParticipantEntity saved = participantRepository.save(participant);
        return new ParticipantCreatedResponse(saved.getParticipantId());
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantListResponse findAll(Integer page, Integer size, String name, String phone) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "participantId"));
        Page<ParticipantEntity> participants = findParticipants(pageable, normalize(name), normalize(phone));
        List<ParticipantListResponse.Item> content = participants.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new ParticipantListResponse(
                content,
                participants.getNumber(),
                participants.getSize(),
                participants.getTotalElements(),
                participants.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPhoneResponse checkPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return participantRepository.findFirstByPhoneOrderByParticipantIdAsc(phone.trim())
                .map(existing -> new CheckPhoneResponse(true, existing.getParticipantId()))
                .orElseGet(() -> new CheckPhoneResponse(false, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponse findById(Long participantId) {
        ParticipantEntity participant = findParticipant(participantId);
        return new ParticipantResponse(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone(),
                participant.getCreatedAt());
    }

    @Override
    public ParticipantUpdatedResponse update(Long participantId, UpdateParticipantRequest request) {
        ParticipantEntity participant = findParticipant(participantId);
        participant.setName(request.name());
        participant.setBirthYear(request.birthYear());
        participant.setPhone(request.phone());
        participant.setMatchKey(MatchKeyGenerator.generate(request.name(), request.birthYear(), request.phone()));
        participant.setUpdatedAt(LocalDateTime.now());
        participantRepository.save(participant);
        return new ParticipantUpdatedResponse(participant.getParticipantId(), true);
    }

    @Override
    public ParticipantDeletedResponse delete(Long participantId) {
        ParticipantEntity participant = findParticipant(participantId);
        participantRepository.delete(participant);
        return new ParticipantDeletedResponse(true);
    }

    private Page<ParticipantEntity> findParticipants(Pageable pageable, String name, String phone) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasPhone = StringUtils.hasText(phone);

        if (hasName && hasPhone) {
            return participantRepository.findByNameContainingAndPhoneContaining(name, phone, pageable);
        }
        if (hasName) {
            return participantRepository.findByNameContaining(name, pageable);
        }
        if (hasPhone) {
            return participantRepository.findByPhoneContaining(phone, pageable);
        }
        return participantRepository.findAll(pageable);
    }

    private ParticipantEntity findParticipant(Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private ParticipantListResponse.Item toListItem(ParticipantEntity participant) {
        return new ParticipantListResponse.Item(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone());
    }

    private int sanitizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int sanitizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
