package com.jobmoa.hopefulreturn.participant.entity;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "participant")
public class ParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "match_key", length = 100)
    private String matchKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "participant", fetch = FetchType.LAZY)
    private List<CourseParticipantEntity> courseParticipants;
}
