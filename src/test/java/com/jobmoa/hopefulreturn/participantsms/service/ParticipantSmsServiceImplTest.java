package com.jobmoa.hopefulreturn.participantsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.MessageFormat;
import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsPageResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsImageRepository;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.sms.SmsDeliveryState;
import com.jobmoa.hopefulreturn.sms.SmsMessageResult;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 문자 발송 서비스 단위 테스트 — {name} 치환·형식(SMS/LMS/MMS) 판별·바이트 상한 검증.
 */
@ExtendWith(MockitoExtension.class)
class ParticipantSmsServiceImplTest {

    @Mock
    private ParticipantSmsRepository participantSmsRepository;
    @Mock
    private ParticipantSmsImageRepository participantSmsImageRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private ParticipantSmsServiceImpl service;

    private CourseParticipantEntity cp(Long id, String name, String phone) {
        return CourseParticipantEntity.builder()
                .courseParticipantId(id)
                .participant(ParticipantEntity.builder().name(name).phone(phone).build())
                .build();
    }

    private ParticipantSmsEntity pendingRow(Long smsId, Long courseParticipantId, String requestId) {
        return ParticipantSmsEntity.builder()
                .smsId(smsId)
                .courseParticipantId(courseParticipantId)
                .requestId(requestId)
                .sendStatus(SendStatus.PENDING)
                .sentAt(LocalDateTime.of(2026, 7, 24, 15, 20, 10))
                .build();
    }

    private void stubSaveReturnsWithId() {
        when(participantSmsRepository.save(any())).thenAnswer(invocation -> {
            ParticipantSmsEntity entity = invocation.getArgument(0);
            entity.setSmsId(1L);
            return entity;
        });
    }

    @Test
    @DisplayName("발송: {name} 을 수신자별 성명으로 치환하고 SMS 로 판별한다")
    void send_substitutesNameAndDetectsSms() {
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222"), cp(2L, "김철수", "01033334444")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        SendSmsRequest request = new SendSmsRequest(List.of(1L, 2L), null, "{name}님 안녕하세요", null, null);
        SendSmsResponse response = service.send(9L, request);

        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        SmsSendCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo("SMS");
        assertThat(command.recipients()).extracting(SmsSendCommand.Recipient::content)
                .containsExactly("홍길동님 안녕하세요", "김철수님 안녕하세요");
        assertThat(response.messageFormat()).isEqualTo("SMS");
        assertThat(response.successCount()).isEqualTo(2);
        assertThat(response.totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("발송: 90바이트 초과 본문은 LMS 로 판별한다")
    void send_detectsLmsWhenOver90Bytes() {
        String longContent = "가".repeat(100); // EUC-KR 2바이트/자 = 200바이트
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        SendSmsResponse response =
                service.send(9L, new SendSmsRequest(List.of(1L), "제목", longContent, null, null));

        assertThat(response.messageFormat()).isEqualTo("LMS");
    }

    @Test
    @DisplayName("발송: 이미지가 있으면 MMS 로 판별하고 이미지 이력을 저장한다")
    void send_detectsMmsWhenImages() {
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of("file-1")));
        stubSaveReturnsWithId();

        SendSmsResponse response = service.send(9L,
                new SendSmsRequest(List.of(1L), "제목", "짧은 내용", null, List.of("BASE64DATA")));

        assertThat(response.messageFormat()).isEqualTo("MMS");
        verify(participantSmsImageRepository).save(any());
    }

    @Test
    @DisplayName("발송: 2000바이트 초과 본문은 거부한다")
    void send_rejectsOver2000Bytes() {
        String tooLong = "가".repeat(1001); // EUC-KR 2002바이트
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222")));

        assertThatThrownBy(() -> service.send(9L, new SendSmsRequest(List.of(1L), "제목", tooLong, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SMS_CONTENT_TOO_LONG);
    }

    @Test
    @DisplayName("발송: SENS 발송 실패 시 롤백하지 않고 FAIL 로 저장한다")
    void send_savesFailWhenSendFails() {
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222"), cp(2L, "김철수", "01033334444")));
        when(smsService.send(any())).thenThrow(new BusinessException(ErrorCode.SMS_SEND_FAILED));
        stubSaveReturnsWithId();

        SendSmsRequest request = new SendSmsRequest(List.of(1L, 2L), null, "{name}님 안녕하세요", null, null);
        SendSmsResponse response = service.send(9L, request);

        ArgumentCaptor<ParticipantSmsEntity> captor = ArgumentCaptor.forClass(ParticipantSmsEntity.class);
        verify(participantSmsRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ParticipantSmsEntity::getSendStatus)
                .containsOnly(SendStatus.FAIL);
        assertThat(response.successCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(2);
        assertThat(response.statusName()).isEqualTo("fail");
    }

    @Test
    @DisplayName("발송: 입력 오류(SMS_IMAGE_INVALID)는 전파하고 내역을 저장하지 않는다")
    void send_propagatesInputError() {
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenThrow(new BusinessException(ErrorCode.SMS_IMAGE_INVALID));

        assertThatThrownBy(() -> service.send(9L,
                new SendSmsRequest(List.of(1L), "제목", "짧은 내용", null, List.of("BASE64DATA"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SMS_IMAGE_INVALID);

        verify(participantSmsRepository, never()).save(any());
    }

    @Test
    @DisplayName("발송: 접수(requestId 존재) 시 PENDING·request_id 로 저장한다")
    void send_savesPendingWhenAccepted() {
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(List.of(1L)))
                .thenReturn(List.of(cp(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        service.send(9L, new SendSmsRequest(List.of(1L), null, "안녕하세요", null, null));

        ArgumentCaptor<ParticipantSmsEntity> captor = ArgumentCaptor.forClass(ParticipantSmsEntity.class);
        verify(participantSmsRepository).save(captor.capture());
        assertThat(captor.getValue().getSendStatus()).isEqualTo(SendStatus.PENDING);
        assertThat(captor.getValue().getRequestId()).isEqualTo("req-1");
    }

    @Test
    @DisplayName("폴링: SENS 결과로 PENDING 을 수신번호 매칭해 SUCCESS/FAIL 로 갱신한다")
    void poll_updatesPendingFromSensResult() {
        ParticipantSmsEntity ok = pendingRow(11L, 101L, "req-1");
        ParticipantSmsEntity failed = pendingRow(12L, 102L, "req-1");
        when(participantSmsRepository.findBySendStatusAndRequestIdIsNotNullAndSentAtAfter(
                eq(SendStatus.PENDING), any())).thenReturn(List.of(ok, failed));
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(any()))
                .thenReturn(List.of(cp(101L, "홍길동", "01011112222"), cp(102L, "김철수", "01033334444")));
        when(smsService.lookupResults("req-1")).thenReturn(List.of(
                new SmsMessageResult("m-1", "01011112222", SmsDeliveryState.SUCCESS, "0", "success",
                        LocalDateTime.of(2026, 7, 24, 15, 20, 12)),
                new SmsMessageResult("m-2", "01033334444", SmsDeliveryState.FAIL, "3018", "발신번호 변작", null)));

        int updated = service.pollPendingResults();

        assertThat(updated).isEqualTo(2);
        assertThat(ok.getSendStatus()).isEqualTo(SendStatus.SUCCESS);
        assertThat(ok.getMessageId()).isEqualTo("m-1");
        assertThat(ok.getCompleteTime()).isEqualTo(LocalDateTime.of(2026, 7, 24, 15, 20, 12));
        assertThat(failed.getSendStatus()).isEqualTo(SendStatus.FAIL);
        assertThat(failed.getResultCode()).isEqualTo("3018");
        assertThat(failed.getResultMessage()).isEqualTo("발신번호 변작");
    }

    @Test
    @DisplayName("재조회: 특정 이력을 SENS 결과로 갱신해 상세를 반환한다")
    void refresh_updatesAndReturnsDetail() {
        ParticipantSmsEntity row = pendingRow(11L, 101L, "req-1");
        row.setMessageFormat(MessageFormat.SMS);
        when(participantSmsRepository.findById(11L)).thenReturn(Optional.of(row));
        when(courseParticipantRepository.findWithParticipantByCourseParticipantIdIn(any()))
                .thenReturn(List.of(cp(101L, "홍길동", "01011112222")));
        when(smsService.lookupResults("req-1")).thenReturn(List.of(
                new SmsMessageResult("m-1", "01011112222", SmsDeliveryState.SUCCESS, "0", "success",
                        LocalDateTime.of(2026, 7, 24, 15, 20, 12))));
        when(participantSmsImageRepository.findBySmsIdOrderBySortOrderAsc(11L)).thenReturn(List.of());

        ParticipantSmsDetailResponse detail = service.refreshResult(11L);

        assertThat(detail.sendStatus()).isEqualTo("SUCCESS");
        assertThat(detail.messageId()).isEqualTo("m-1");
        assertThat(detail.resultCode()).isEqualTo("0");
        assertThat(detail.completeTime()).isEqualTo(LocalDateTime.of(2026, 7, 24, 15, 20, 12));
    }

    @Test
    @DisplayName("내역조회: 엔티티를 항목으로 매핑하고 페이지 메타를 반환한다")
    void history_mapsItemsAndPageMeta() {
        ParticipantSmsEntity row = ParticipantSmsEntity.builder()
                .smsId(501L)
                .courseParticipantId(101L)
                .messageFormat(MessageFormat.LMS)
                .title("수료 안내")
                .content("홍길동님, 수료를 축하합니다.")
                .sendStatus(SendStatus.SUCCESS)
                .sentBy(9L)
                .sentAt(LocalDateTime.of(2026, 7, 24, 15, 20, 10))
                .sender(UsersEntity.builder().name("관리자").build())
                .courseParticipant(CourseParticipantEntity.builder()
                        .courseParticipantId(101L)
                        .participant(ParticipantEntity.builder().name("홍길동").phone("01012345678").build())
                        .course(CourseEntity.builder()
                                .courseName("양천5기").courseNumber(5)
                                .region(RegionEntity.builder().name("양천").build())
                                .build())
                        .build())
                .build();
        when(participantSmsRepository.findPageByFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));

        ParticipantSmsPageResponse res = service.findSmsHistoryPage(
                null, null, null, null, null, null, null, 0, 10);

        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.content()).hasSize(1);
        ParticipantSmsPageResponse.Item item = res.content().get(0);
        assertThat(item.smsId()).isEqualTo(501L);
        assertThat(item.participantName()).isEqualTo("홍길동");
        assertThat(item.phone()).isEqualTo("01012345678");
        assertThat(item.regionName()).isEqualTo("양천");
        assertThat(item.courseName()).isEqualTo("양천5기");
        assertThat(item.courseNumber()).isEqualTo(5);
        assertThat(item.messageFormat()).isEqualTo("LMS");
        assertThat(item.sendStatus()).isEqualTo("SUCCESS");
        assertThat(item.senderName()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("내역조회: 스코프(sentBy)·상태·기간경계(+1일)·키워드 trim 을 리포지토리로 전달한다")
    void history_passesScopeAndDateBoundary() {
        when(participantSmsRepository.findPageByFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.findSmsHistoryPage(
                7L, "SUCCESS", null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), " 홍 ", 0, 10);

        ArgumentCaptor<Long> sentBy = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<SendStatus> status = ArgumentCaptor.forClass(SendStatus.class);
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(participantSmsRepository).findPageByFilters(
                sentBy.capture(), status.capture(), any(), any(),
                from.capture(), to.capture(), keyword.capture(), any());
        assertThat(sentBy.getValue()).isEqualTo(7L);
        assertThat(status.getValue()).isEqualTo(SendStatus.SUCCESS);
        assertThat(from.getValue()).isEqualTo(LocalDate.of(2026, 7, 1).atStartOfDay());
        assertThat(to.getValue()).isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay()); // 종료일 하루 포함
        assertThat(keyword.getValue()).isEqualTo("홍");
    }

    @Test
    @DisplayName("내역조회: 잘못된 상태값은 INVALID_INPUT 으로 거부한다")
    void history_rejectsInvalidStatus() {
        assertThatThrownBy(() -> service.findSmsHistoryPage(
                null, "NOPE", null, null, null, null, null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
