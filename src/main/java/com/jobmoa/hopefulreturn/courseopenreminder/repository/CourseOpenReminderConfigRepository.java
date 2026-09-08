package com.jobmoa.hopefulreturn.courseopenreminder.repository;

import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOpenReminderConfigRepository
        extends JpaRepository<CourseOpenReminderConfigEntity, Long> {

    /** 설정은 단일 행이므로 가장 앞선 행을 설정으로 사용한다(V27 시드로 1행 보장). */
    Optional<CourseOpenReminderConfigEntity> findTopByOrderByReminderConfigIdAsc();
}
