package com.jobmoa.hopefulreturn.courseopenreminder.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderConfigResponse;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.UpdateCourseOpenReminderConfigRequest;
import com.jobmoa.hopefulreturn.courseopenreminder.repository.CourseOpenReminderConfigRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseOpenReminderConfigServiceImpl implements CourseOpenReminderConfigService {

    private final CourseOpenReminderConfigRepository configRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public CourseOpenReminderConfigResponse get() {
        return CourseOpenReminderConfigResponse.from(load());
    }

    @Override
    @Transactional
    public CourseOpenReminderConfigResponse update(UpdateCourseOpenReminderConfigRequest request, Long updatedBy) {
        CourseOpenReminderConfigEntity config = load();
        config.setEnabled(Boolean.TRUE.equals(request.enabled()));
        config.setDaysBefore(request.daysBefore());
        config.setSendTime(request.sendTime());
        config.setUpdatedAt(LocalDateTime.now(clock));
        config.setUpdatedBy(updatedBy);
        return CourseOpenReminderConfigResponse.from(configRepository.save(config));
    }

    private CourseOpenReminderConfigEntity load() {
        return configRepository.findTopByOrderByReminderConfigIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "개강 문자 발송 설정이 없습니다."));
    }
}
