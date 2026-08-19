package com.jobmoa.hopefulreturn.staffschedule.notification;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.email.EmailService;
import com.jobmoa.hopefulreturn.email.dto.StaffUnavailableMail;
import com.jobmoa.hopefulreturn.staffschedule.event.StaffBecameUnavailableEvent;
import com.jobmoa.hopefulreturn.staffunavailablenotice.entity.NoticeSendStatus;
import com.jobmoa.hopefulreturn.staffunavailablenotice.entity.StaffUnavailableNoticeEntity;
import com.jobmoa.hopefulreturn.staffunavailablenotice.repository.StaffUnavailableNoticeRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/**
 * 인력이 배정된 회차의 날짜를 가능→불가로 전환하면, 인력배정 권한 관리자에게 메일로 알린다.
 *
 * <p>{@link TransactionalEventListener}(AFTER_COMMIT)로 원 변경 트랜잭션이 커밋된 뒤에만 동작하고,
 * {@link Async}로 별 스레드에서 실행해 원 요청 응답을 지연시키지 않는다. 발송 실패는 로깅만 하고
 * 예외를 전파하지 않는다(원 요청에 무영향).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaffScheduleChangeNotificationService {

    private final CourseStaffRepository courseStaffRepository;
    private final CourseRepository courseRepository;
    private final UsersRepository usersRepository;
    private final StaffUnavailableNoticeRepository noticeRepository;
    private final EmailService emailService;

    // AFTER_COMMIT 리스너는 원 트랜잭션이 완료되는 중 실행되므로 새 트랜잭션(REQUIRES_NEW)이 필요하다.
    // (@Async 로 별 스레드에서 돌며, LAZY 로딩·이력 저장을 위해 독립 트랜잭션을 연다.)
    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStaffBecameUnavailable(StaffBecameUnavailableEvent event) {
        try {
            handle(event);
        } catch (Exception e) {
            // 알림은 부가 기능 — 어떤 실패도 원 요청/후속에 영향 주지 않도록 삼킨다.
            log.error("근무불가 알림 처리 실패: staffScheduleId={}", event.staffScheduleId(), e);
        }
    }

    private void handle(StaffBecameUnavailableEvent event) {
        CourseEntity course = resolveCourse(event.courseStaffId());
        String staffName = usersRepository.findById(event.userId())
                .map(UsersEntity::getName)
                .orElse(null);

        List<UsersEntity> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            log.debug("근무불가 알림 수신 관리자 없음: staffScheduleId={}", event.staffScheduleId());
            return;
        }

        StaffUnavailableMail mail = new StaffUnavailableMail(
                staffName,
                course == null || course.getRegion() == null ? null : course.getRegion().getName(),
                round(course),
                event.scheduleDate(),
                sessionLabel(event.sessionType()),
                event.memo());

        for (UsersEntity recipient : recipients) {
            sendAndRecord(event, recipient, mail);
        }
    }

    private void sendAndRecord(StaffBecameUnavailableEvent event, UsersEntity recipient, StaffUnavailableMail mail) {
        NoticeSendStatus status = NoticeSendStatus.SUCCESS;
        try {
            emailService.sendStaffUnavailableNotice(recipient.getEmail(), mail);
        } catch (Exception e) {
            // 개별 수신자 실패가 나머지 수신자 발송을 막지 않게 한다.
            status = NoticeSendStatus.FAIL;
            log.error("근무불가 알림 발송 실패: recipient={}, staffScheduleId={}",
                    recipient.getUserId(), event.staffScheduleId(), e);
        }
        saveHistory(event, recipient, status);
    }

    private void saveHistory(StaffBecameUnavailableEvent event, UsersEntity recipient, NoticeSendStatus status) {
        LocalDateTime now = LocalDateTime.now();
        noticeRepository.save(StaffUnavailableNoticeEntity.builder()
                .staffScheduleId(event.staffScheduleId())
                .courseStaffId(event.courseStaffId())
                .staffUserId(event.userId())
                .recipientUserId(recipient.getUserId())
                .recipientEmail(recipient.getEmail())
                .scheduleDate(event.scheduleDate())
                .sessionType(event.sessionType())
                .sendStatus(status)
                .sentAt(now)
                .createdAt(now)
                .build());
    }

    private CourseEntity resolveCourse(Long courseStaffId) {
        if (courseStaffId == null) {
            return null;
        }
        return courseStaffRepository.findById(courseStaffId)
                .map(CourseStaffEntity::getCourseId)
                .flatMap(courseRepository::findById)
                .orElse(null);
    }

    /** {round}=지역회차(localCourseNumber) 우선, 없으면 전체회차(courseNumber). SMS 규칙과 동일. */
    private Integer round(CourseEntity course) {
        if (course == null) {
            return null;
        }
        return course.getLocalCourseNumber() != null ? course.getLocalCourseNumber() : course.getCourseNumber();
    }

    private String sessionLabel(SessionType sessionType) {
        if (sessionType == null) {
            return null;
        }
        return switch (sessionType) {
            case AM -> "오전";
            case PM -> "오후";
            case FULL -> "종일";
        };
    }

    /**
     * 알림 수신자 = 메일 발송 권한(can_send_email) 보유 + 미삭제 + 이메일 보유 사용자.
     * 수신 대상은 역할이 아니라 계정 단위 권한으로 관리한다(관리자가 "권한 관리" 페이지에서 부여).
     */
    private List<UsersEntity> resolveRecipients() {
        List<UsersEntity> recipients = new ArrayList<>();
        for (UsersEntity user : usersRepository.findByCanSendEmailTrue()) {
            if (!Boolean.TRUE.equals(user.getDeleted()) && StringUtils.hasText(user.getEmail())) {
                recipients.add(user);
            }
        }
        return recipients;
    }
}
