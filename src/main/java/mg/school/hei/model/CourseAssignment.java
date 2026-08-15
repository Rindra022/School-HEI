package mg.school.hei.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record CourseAssignment(
    UUID id, Course course, AppUser teacher, AppGroup group, Integer academicYear) {}
