package com.jobmoa.hopefulreturn.adminsms.entity;

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
 * 관리자 임의 문자 MMS 첨부 이미지(admin_sms_image, V29). admin_sms 1건당 여러 장.
 * image_url 에는 SENS 파일 ID(참조)를 저장한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin_sms_image")
public class AdminSmsImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_sms_image_id", nullable = false)
    private Long adminSmsImageId;

    @Column(name = "admin_sms_id", nullable = false)
    private Long adminSmsId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
