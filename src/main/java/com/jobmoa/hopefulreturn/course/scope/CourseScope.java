package com.jobmoa.hopefulreturn.course.scope;

import java.util.Set;

public record CourseScope(Set<Long> courseIds) {

    public static final CourseScope UNRESTRICTED = new CourseScope(null);

    public boolean unrestricted() {
        return courseIds == null;
    }

    public boolean allowsCourse(Long courseId) {
        return courseIds == null || courseIds.contains(courseId);
    }
}
