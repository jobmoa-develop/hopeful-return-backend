package com.jobmoa.hopefulreturn.systemmessagetemplate.entity;

/**
 * 시스템 문자 템플릿 고정 key. DB {@code system_message_template.template_key} 및 발송 코드가 참조한다.
 * 신규 항목은 V30 이후 마이그레이션 시드와 함께 추가한다.
 */
public enum SystemMessageTemplateKey {
    ASSIGN_NEW,
    ASSIGN_CHANGED,
    ASSIGN_REMOVED,
    COURSE_OPEN_REMINDER,
    VERIFICATION_CODE
}
