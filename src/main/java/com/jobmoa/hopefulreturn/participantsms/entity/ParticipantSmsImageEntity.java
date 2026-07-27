package com.jobmoa.hopefulreturn.participantsms.entity;

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
 * MMS 첨부 이미지. participant_sms 1건당 여러 장. image_url 에는 SENS 파일 ID(참조)를 저장한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "participant_sms_image")
public class ParticipantSmsImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_sms_image_id", nullable = false)
    private Long participantSmsImageId;

    @Column(name = "sms_id", nullable = false)
    private Long smsId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
