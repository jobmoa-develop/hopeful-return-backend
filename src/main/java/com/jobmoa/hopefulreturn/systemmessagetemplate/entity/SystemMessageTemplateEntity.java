package com.jobmoa.hopefulreturn.systemmessagetemplate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시스템(하드코딩 이관) 문자 템플릿. 목적별 고정 key 단일 행으로, 발송 코드가 본문을 읽어 쓴다.
 * 사용자 소유가 없고 create/delete 대상이 아니며, 관리자만 본문(content)을 수정한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "system_message_template")
public class SystemMessageTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_message_template_id", nullable = false)
    private Long systemMessageTemplateId;

    // 코드 상수와 1:1 대응하는 고정 식별자(유니크).
    @Column(name = "template_key", nullable = false, length = 50)
    private String templateKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 최종 수정자 계정. 시드 직후는 null.
    @Column(name = "updated_by")
    private Long updatedBy;
}
