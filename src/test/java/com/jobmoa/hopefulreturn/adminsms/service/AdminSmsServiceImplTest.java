package com.jobmoa.hopefulreturn.adminsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.adminsms.entity.AdminSmsEntity;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsRequest;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsDetailResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsPageResponse;
import com.jobmoa.hopefulreturn.adminsms.repository.AdminSmsImageRepository;
import com.jobmoa.hopefulreturn.adminsms.repository.AdminSmsRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.participantsms.entity.MessageFormat;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 임의 문자 발송 서비스 단위 테스트 — 정규화/검증/중복제거·형식(SMS/LMS/MMS)·상태·예약·폴링·이력.
 */
@ExtendWith(MockitoExtension.class)
class AdminSmsServiceImplTest {

    @Mock
    private AdminSmsRepository adminSmsRepository;
    @Mock
    private AdminSmsImageRepository adminSmsImageRepository;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AdminSmsServiceImpl service;

    private void stubSaveReturnsWithId() {
        when(adminSmsRepository.save(any())).thenAnswer(invocation -> {
            AdminSmsEntity entity = invocation.getArgument(0);
            entity.setAdminSmsId(1L);
            return entity;
        });
    }

    private AdminSmsEntity pendingRow(Long id, String toPhone, String requestId) {
        return AdminSmsEntity.builder()
                .adminSmsId(id)
                .toPhone(toPhone)
                .requestId(requestId)
                .sendStatus(SendStatus.PENDING)
                .sentAt(LocalDateTime.of(2026, 9, 8, 15, 20, 10))
                .build();
    }

    private AdminSmsEntity reservedRow(Long id, String toPhone, String reserveId) {
        return AdminSmsEntity.builder()
                .adminSmsId(id)
                .toPhone(toPhone)
                .requestId("req-1")
                .reserveId(reserveId)
                .reserveTime(LocalDateTime.of(2999, 1, 1, 9, 0))
                .sendStatus(SendStatus.RESERVED)
                .build();
    }

    @Test
    @DisplayName("발송: 하이픈 무관 정규화 후 중복번호를 제거하고 SMS 로 판별한다")
    void send_normalizesDedupAndDetectsSms() {
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        // 두 입력이 정규화 후 동일 → 1건으로 중복제거.
        AdminSendSmsRequest request = new AdminSendSmsRequest(
                List.of("010-1111-2222", "01011112222"), null, "안녕하세요", null, null, null);
        AdminSendSmsResponse response = service.send(9L, request);

        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        SmsSendCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo("SMS");
        assertThat(command.recipients()).extracting(SmsSendCommand.Recipient::to)
                .containsExactly("01011112222");
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.messageFormat()).isEqualTo("SMS");
        verify(adminSmsRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("발송: 잘못된 전화번호가 섞이면 INVALID_INPUT 으로 거부하고 발송하지 않는다")
    void send_rejectsInvalidPhone() {
        assertThatThrownBy(() -> service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222", "123"), null, "안녕하세요", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verify(smsService, never()).send(any());
        verify(adminSmsRepository, never()).save(any());
    }

    @Test
    @DisplayName("발송: 빈 수신자 목록은 INVALID_INPUT 으로 거부한다")
    void send_rejectsEmptyRecipients() {
        assertThatThrownBy(() -> service.send(9L, new AdminSendSmsRequest(
                List.of(), null, "안녕하세요", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("발송: 90바이트 초과 본문은 LMS 로 판별한다")
    void send_detectsLmsWhenOver90Bytes() {
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        String longContent = "가".repeat(100); // EUC-KR 200바이트
        AdminSendSmsResponse response = service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222"), "제목", longContent, null, null, null));

        assertThat(response.messageFormat()).isEqualTo("LMS");
    }

    @Test
    @DisplayName("발송: 이미지가 있으면 MMS 로 판별하고 이미지 이력을 저장한다")
    void send_detectsMmsWhenImages() {
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of("file-1")));
        stubSaveReturnsWithId();

        AdminSendSmsResponse response = service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222"), "제목", "짧은 내용", null, List.of("BASE64DATA"), null));

        assertThat(response.messageFormat()).isEqualTo("MMS");
        verify(adminSmsImageRepository).save(any());
    }

    @Test
    @DisplayName("발송: 2000바이트 초과 본문은 거부한다")
    void send_rejectsOver2000Bytes() {
        String tooLong = "가".repeat(1001); // EUC-KR 2002바이트
        assertThatThrownBy(() -> service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222"), "제목", tooLong, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SMS_CONTENT_TOO_LONG);
    }

    @Test
    @DisplayName("발송: SENS 발송 실패 시 롤백하지 않고 FAIL 로 저장한다")
    void send_savesFailWhenSendFails() {
        when(smsService.send(any())).thenThrow(new BusinessException(ErrorCode.SMS_SEND_FAILED));
        stubSaveReturnsWithId();

        AdminSendSmsResponse response = service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222", "01033334444"), null, "안녕하세요", null, null, null));

        ArgumentCaptor<AdminSmsEntity> captor = ArgumentCaptor.forClass(AdminSmsEntity.class);
        verify(adminSmsRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(AdminSmsEntity::getSendStatus)
                .containsOnly(SendStatus.FAIL);
        assertThat(response.successCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(2);
        assertThat(response.statusName()).isEqualTo("fail");
    }

    @Test
    @DisplayName("발송: 접수(requestId 존재) 시 PENDING·request_id·to_phone 으로 저장한다")
    void send_savesPendingWhenAccepted() {
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        service.send(9L, new AdminSendSmsRequest(List.of("010-1111-2222"), null, "안녕하세요", null, null, null));

        ArgumentCaptor<AdminSmsEntity> captor = ArgumentCaptor.forClass(AdminSmsEntity.class);
        verify(adminSmsRepository).save(captor.capture());
        AdminSmsEntity saved = captor.getValue();
        assertThat(saved.getSendStatus()).isEqualTo(SendStatus.PENDING);
        assertThat(saved.getRequestId()).isEqualTo("req-1");
        assertThat(saved.getToPhone()).isEqualTo("01011112222");
    }

    @Test
    @DisplayName("발송: 미연동(requestId 없음)은 PENDING 이 아닌 SUCCESS 로 저장한다")
    void send_savesSuccessWhenNoOp() {
        // NoOp 구현은 requestId 를 주지 않는다 → 폴러 대상이 아니므로 즉시 SUCCESS 여야 한다.
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("200", "success", "", List.of()));
        stubSaveReturnsWithId();

        service.send(9L, new AdminSendSmsRequest(List.of("01011112222"), null, "안녕하세요", null, null, null));

        ArgumentCaptor<AdminSmsEntity> captor = ArgumentCaptor.forClass(AdminSmsEntity.class);
        verify(adminSmsRepository).save(captor.capture());
        assertThat(captor.getValue().getSendStatus()).isEqualTo(SendStatus.SUCCESS);
    }

    @Test
    @DisplayName("예약발송: reserveTime 지정 시 RESERVED·reserve_id 저장, sent_at 은 null 이다")
    void send_savesReservedWhenReserveTimeGiven() {
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));
        stubSaveReturnsWithId();

        AdminSendSmsResponse response = service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222"), null, "예약 문자", null, null, "2999-01-01 09:00"));

        ArgumentCaptor<AdminSmsEntity> captor = ArgumentCaptor.forClass(AdminSmsEntity.class);
        verify(adminSmsRepository).save(captor.capture());
        AdminSmsEntity saved = captor.getValue();
        assertThat(saved.getSendStatus()).isEqualTo(SendStatus.RESERVED);
        assertThat(saved.getReserveId()).isEqualTo("req-1");
        assertThat(saved.getReserveTime()).isEqualTo(LocalDateTime.of(2999, 1, 1, 9, 0));
        assertThat(saved.getSentAt()).isNull();
        assertThat(response.reserveId()).isEqualTo("req-1");
    }

    @Test
    @DisplayName("예약발송: 과거 예약 시각은 SMS_RESERVE_TIME_INVALID 로 거부한다")
    void send_rejectsPastReserveTime() {
        assertThatThrownBy(() -> service.send(9L, new AdminSendSmsRequest(
                List.of("01011112222"), null, "예약 문자", null, null, "2000-01-01 09:00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SMS_RESERVE_TIME_INVALID);
        verify(adminSmsRepository, never()).save(any());
    }

    @Test
    @DisplayName("승격: 예약시각 도래한 RESERVED 를 PENDING 으로 올리고 sent_at 을 채운다")
    void promote_promotesDueReservations() {
        AdminSmsEntity due = AdminSmsEntity.builder()
                .adminSmsId(21L).toPhone("01011112222").requestId("req-1")
                .sendStatus(SendStatus.RESERVED)
                .reserveTime(LocalDateTime.of(2020, 1, 1, 9, 0))
                .reserveId("reserve-1")
                .build();
        when(adminSmsRepository.findBySendStatusAndReserveTimeLessThanEqual(eq(SendStatus.RESERVED), any()))
                .thenReturn(List.of(due));

        int promoted = service.promoteDueReservations();

        assertThat(promoted).isEqualTo(1);
        assertThat(due.getSendStatus()).isEqualTo(SendStatus.PENDING);
        assertThat(due.getSentAt()).isNotNull();
        verify(adminSmsRepository).save(due);
    }

    @Test
    @DisplayName("예약취소 프리뷰: 같은 reserve_id 의 RESERVED 대상 수·수신번호를 반환한다")
    void cancelPreview_countsReservedTargets() {
        when(adminSmsRepository.findByReserveId("reserve-1")).thenReturn(List.of(
                reservedRow(31L, "01011112222", "reserve-1"),
                reservedRow(32L, "01033334444", "reserve-1")));

        AdminReservationCancelPreviewResponse preview = service.previewReservationCancel("reserve-1");

        assertThat(preview.targetCount()).isEqualTo(2);
        assertThat(preview.recipients()).containsExactly("01011112222", "01033334444");
    }

    @Test
    @DisplayName("예약취소: SENS 취소 후 해당 reserve_id 의 RESERVED 만 CANCELED 로 전이한다")
    void cancel_deletesSensAndMarksCanceled() {
        AdminSmsEntity r1 = reservedRow(31L, "01011112222", "reserve-1");
        AdminSmsEntity r2 = reservedRow(32L, "01033334444", "reserve-1");
        AdminSmsEntity alreadySent = reservedRow(33L, "01055556666", "reserve-1");
        alreadySent.setSendStatus(SendStatus.SUCCESS);
        when(adminSmsRepository.findByReserveId("reserve-1")).thenReturn(List.of(r1, r2, alreadySent));
        stubSaveReturnsWithId();

        int canceled = service.cancelReservation("reserve-1");

        assertThat(canceled).isEqualTo(2);
        verify(smsService).cancelReservation("reserve-1");
        assertThat(r1.getSendStatus()).isEqualTo(SendStatus.CANCELED);
        assertThat(r2.getSendStatus()).isEqualTo(SendStatus.CANCELED);
        assertThat(alreadySent.getSendStatus()).isEqualTo(SendStatus.SUCCESS);
    }

    @Test
    @DisplayName("예약취소: 취소 가능한 RESERVED 가 없으면 SENS 호출 없이 예외를 던진다")
    void cancel_throwsWhenNoReserved() {
        AdminSmsEntity sent = reservedRow(31L, "01011112222", "reserve-1");
        sent.setSendStatus(SendStatus.SUCCESS);
        when(adminSmsRepository.findByReserveId("reserve-1")).thenReturn(List.of(sent));

        assertThatThrownBy(() -> service.cancelReservation("reserve-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SMS_RESERVATION_NOT_CANCELABLE);
        verify(smsService, never()).cancelReservation(any());
    }

    @Test
    @DisplayName("폴링: SENS 결과로 PENDING 을 수신번호 매칭해 SUCCESS/FAIL 로 갱신한다")
    void poll_updatesPendingFromSensResult() {
        AdminSmsEntity ok = pendingRow(11L, "01011112222", "req-1");
        AdminSmsEntity failed = pendingRow(12L, "01033334444", "req-1");
        when(adminSmsRepository.findBySendStatusAndRequestIdIsNotNullAndSentAtAfter(
                eq(SendStatus.PENDING), any())).thenReturn(List.of(ok, failed));
        when(smsService.lookupResults("req-1")).thenReturn(List.of(
                new SmsMessageResult("m-1", "01011112222", SmsDeliveryState.SUCCESS, "0", "success",
                        LocalDateTime.of(2026, 9, 8, 15, 20, 12)),
                new SmsMessageResult("m-2", "01033334444", SmsDeliveryState.FAIL, "3018", "발신번호 변작", null)));

        int updated = service.pollPendingResults();

        assertThat(updated).isEqualTo(2);
        assertThat(ok.getSendStatus()).isEqualTo(SendStatus.SUCCESS);
        assertThat(ok.getMessageId()).isEqualTo("m-1");
        assertThat(ok.getCompleteTime()).isEqualTo(LocalDateTime.of(2026, 9, 8, 15, 20, 12));
        assertThat(failed.getSendStatus()).isEqualTo(SendStatus.FAIL);
        assertThat(failed.getResultCode()).isEqualTo("3018");
        assertThat(failed.getResultMessage()).isEqualTo("발신번호 변작");
    }

    @Test
    @DisplayName("재조회: 특정 이력을 SENS 결과로 갱신해 상세를 반환한다")
    void refresh_updatesAndReturnsDetail() {
        AdminSmsEntity row = pendingRow(11L, "01011112222", "req-1");
        row.setMessageFormat(MessageFormat.SMS);
        when(adminSmsRepository.findById(11L)).thenReturn(Optional.of(row));
        when(smsService.lookupResults("req-1")).thenReturn(List.of(
                new SmsMessageResult("m-1", "01011112222", SmsDeliveryState.SUCCESS, "0", "success",
                        LocalDateTime.of(2026, 9, 8, 15, 20, 12))));
        when(adminSmsImageRepository.findByAdminSmsIdOrderBySortOrderAsc(11L)).thenReturn(List.of());

        AdminSmsDetailResponse detail = service.refreshResult(11L);

        assertThat(detail.sendStatus()).isEqualTo("SUCCESS");
        assertThat(detail.messageId()).isEqualTo("m-1");
        assertThat(detail.toPhone()).isEqualTo("01011112222");
        assertThat(detail.completeTime()).isEqualTo(LocalDateTime.of(2026, 9, 8, 15, 20, 12));
    }

    @Test
    @DisplayName("내역조회: 엔티티를 항목으로 매핑하고 페이지 메타를 반환한다")
    void history_mapsItemsAndPageMeta() {
        AdminSmsEntity row = AdminSmsEntity.builder()
                .adminSmsId(501L)
                .toPhone("01012345678")
                .recipientLabel("김담당")
                .messageFormat(MessageFormat.LMS)
                .title("안내")
                .content("안녕하세요. 안내드립니다.")
                .sendStatus(SendStatus.SUCCESS)
                .sentBy(9L)
                .sentAt(LocalDateTime.of(2026, 9, 8, 15, 20, 10))
                .sender(UsersEntity.builder().name("관리자").build())
                .build();
        when(adminSmsRepository.findPageByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));

        AdminSmsPageResponse res = service.findSmsHistoryPage(null, null, null, null, null, 0, 10);

        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.content()).hasSize(1);
        AdminSmsPageResponse.Item item = res.content().get(0);
        assertThat(item.adminSmsId()).isEqualTo(501L);
        assertThat(item.toPhone()).isEqualTo("01012345678");
        assertThat(item.recipientLabel()).isEqualTo("김담당");
        assertThat(item.messageFormat()).isEqualTo("LMS");
        assertThat(item.sendStatus()).isEqualTo("SUCCESS");
        assertThat(item.senderName()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("내역조회: 스코프(sentBy)·상태·기간경계(+1일)·키워드 trim 을 리포지토리로 전달한다")
    void history_passesScopeAndDateBoundary() {
        when(adminSmsRepository.findPageByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.findSmsHistoryPage(
                7L, "SUCCESS", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), " 010 ", 0, 10);

        ArgumentCaptor<Long> sentBy = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<SendStatus> status = ArgumentCaptor.forClass(SendStatus.class);
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(adminSmsRepository).findPageByFilters(
                sentBy.capture(), status.capture(), from.capture(), to.capture(), keyword.capture(), any());
        assertThat(sentBy.getValue()).isEqualTo(7L);
        assertThat(status.getValue()).isEqualTo(SendStatus.SUCCESS);
        assertThat(from.getValue()).isEqualTo(LocalDate.of(2026, 7, 1).atStartOfDay());
        assertThat(to.getValue()).isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay()); // 종료일 하루 포함
        assertThat(keyword.getValue()).isEqualTo("010");
    }

    @Test
    @DisplayName("내역조회: 잘못된 상태값은 INVALID_INPUT 으로 거부한다")
    void history_rejectsInvalidStatus() {
        assertThatThrownBy(() -> service.findSmsHistoryPage(
                null, "NOPE", null, null, null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
