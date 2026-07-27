package com.jobmoa.hopefulreturn.participantsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsImageRepository;
import com.jobmoa.hopefulreturn.participantsms.repository.ParticipantSmsRepository;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import java.util.List;
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
}
