package com.jobmoa.hopefulreturn.systemmessagetemplate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.systemmessagetemplate.entity.SystemMessageTemplateEntity;
import com.jobmoa.hopefulreturn.systemmessagetemplate.entity.SystemMessageTemplateKey;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.UpdateSystemMessageTemplateRequest;
import com.jobmoa.hopefulreturn.systemmessagetemplate.repository.SystemMessageTemplateRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 시스템 문자 템플릿 서비스 단위 테스트 — 발송 코드용 resolveContent 폴백과 update 갱신을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SystemMessageTemplateServiceImplTest {

    @Mock
    private SystemMessageTemplateRepository repository;
    @InjectMocks
    private SystemMessageTemplateServiceImpl service;

    @Test
    @DisplayName("resolveContent 는 DB 본문이 있으면 그 값을 반환한다")
    void resolveContentReturnsDbValue() {
        when(repository.findByTemplateKey("VERIFICATION_CODE"))
                .thenReturn(Optional.of(entity("[코드] {code}")));

        String content = service.resolveContent(
                SystemMessageTemplateKey.VERIFICATION_CODE, "[인증코드] {code}");

        assertThat(content).isEqualTo("[코드] {code}");
    }

    @Test
    @DisplayName("resolveContent 는 행이 없으면 폴백을 반환한다")
    void resolveContentFallsBackWhenMissing() {
        when(repository.findByTemplateKey("VERIFICATION_CODE")).thenReturn(Optional.empty());

        String content = service.resolveContent(
                SystemMessageTemplateKey.VERIFICATION_CODE, "[인증코드] {code}");

        assertThat(content).isEqualTo("[인증코드] {code}");
    }

    @Test
    @DisplayName("resolveContent 는 본문이 공백이면 폴백을 반환한다")
    void resolveContentFallsBackWhenBlank() {
        when(repository.findByTemplateKey("VERIFICATION_CODE")).thenReturn(Optional.of(entity("   ")));

        assertThat(service.resolveContent(SystemMessageTemplateKey.VERIFICATION_CODE, "FB"))
                .isEqualTo("FB");
    }

    @Test
    @DisplayName("update 는 본문·수정자·수정시각을 갱신하고 updated=true 를 반환한다")
    void updateSetsContent() {
        SystemMessageTemplateEntity entity = entity("old");
        entity.setSystemMessageTemplateId(3L);
        when(repository.findByTemplateKey("ASSIGN_NEW")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemMessageTemplateUpdatedResponse res = service.update(
                "ASSIGN_NEW", new UpdateSystemMessageTemplateRequest("new body"), 7L);

        assertThat(res.updated()).isTrue();
        assertThat(res.systemMessageTemplateId()).isEqualTo(3L);
        assertThat(entity.getContent()).isEqualTo("new body");
        assertThat(entity.getUpdatedBy()).isEqualTo(7L);
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("update 는 없는 key 면 예외를 던진다")
    void updateThrowsWhenMissing() {
        when(repository.findByTemplateKey("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                "NONE", new UpdateSystemMessageTemplateRequest("x"), 1L))
                .isInstanceOf(BusinessException.class);
    }

    private SystemMessageTemplateEntity entity(String content) {
        return SystemMessageTemplateEntity.builder()
                .templateKey("VERIFICATION_CODE").name("n").content(content).build();
    }
}
