package com.jobmoa.hopefulreturn.course.scope;

import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.security.AuthScopeSupport;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CourseScopeResolver {

    private final CourseStaffRepository courseStaffRepository;

    @Transactional(readOnly = true)
    public CourseScope resolve(Authentication authentication, Long userId) {
        if (AuthScopeSupport.hasUnrestrictedScope(authentication)) {
            return CourseScope.UNRESTRICTED;
        }
        if (userId == null) {
            return new CourseScope(Set.of());
        }
        if (!AuthScopeSupport.hasCourseAssignedScope(authentication)) {
            return CourseScope.UNRESTRICTED;
        }

        Set<Long> courseIds = courseStaffRepository.findByUserId(userId).stream()
                .map(CourseStaffEntity::getCourseId)
                .collect(Collectors.toSet());
        return new CourseScope(courseIds);
    }
}
