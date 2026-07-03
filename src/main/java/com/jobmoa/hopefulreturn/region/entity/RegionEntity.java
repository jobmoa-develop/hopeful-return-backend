package com.jobmoa.hopefulreturn.region.entity;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "region")
public class RegionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "parent_region_id")
    private Long parentRegionId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private RegionLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_region_id", insertable = false, updatable = false)
    private RegionEntity parentRegion;

    @OneToMany(mappedBy = "parentRegion", fetch = FetchType.LAZY)
    private List<RegionEntity> childRegions;

    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private List<CourseEntity> courses;
}
