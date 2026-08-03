package com.jobmoa.hopefulreturn.sms;

import java.time.LocalDateTime;

/**
 * SENS 발송결과 조회로 확보한 수신 건별 결과.
 * to(수신번호)로 발송 이력 행과 매칭하고, messageId·결과코드/사유·완료시각을 이력에 반영한다.
 */
public record SmsMessageResult(
        String messageId,
        String to,
        SmsDeliveryState state,
        String resultCode,
        String resultName,
        LocalDateTime completeTime) {
}
