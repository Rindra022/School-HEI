package mg.school.hei.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GroupRequest;
import mg.school.hei.endpoint.rest.controller.dto.GroupResponse;
import mg.school.hei.mapper.AppGroupMapper;
import mg.school.hei.model.AppGroup;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.GroupMembershipRepository;
import mg.school.hei.repository.model.JAppGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final AppGroupRepository appGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final AppGroupMapper appGroupMapper;

    @Transactional
    public GroupResponse create(GroupRequest request) {
        JAppGroup saved =
                appGroupRepository.save(JAppGroup.builder().ref(request.ref()).track(request.track()).build());
        return toResponse(appGroupMapper.toModel(saved));
    }

    public List<GroupResponse> list() {
        return appGroupRepository.findAll().stream()
                .map(appGroupMapper::toModel)
                .map(this::toResponse)
                .toList();
    }

    public GroupResponse get(UUID id) {
        return toResponse(appGroupMapper.toModel(findOrThrow(id)));
    }

    @Transactional
    public GroupResponse update(UUID id, GroupRequest request) {
        JAppGroup entity = findOrThrow(id);
        entity.setRef(request.ref());
        entity.setTrack(request.track());
        return toResponse(appGroupMapper.toModel(appGroupRepository.save(entity)));
    }

    @Transactional
    public void delete(UUID id) {
        findOrThrow(id);

        boolean hasMemberships =
                groupMembershipRepository.findAll().stream().anyMatch(m -> m.getGroup().getId().equals(id));
        boolean hasAssignments =
                courseAssignmentRepository.findAll().stream().anyMatch(a -> a.getGroup().getId().equals(id));

        if (hasMemberships || hasAssignments) {
            throw new IllegalStateException("Group still referenced by memberships or assignments");
        }

        appGroupRepository.deleteById(id);
    }

    private JAppGroup findOrThrow(UUID id) {
        return appGroupRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Group not found"));
    }

    private GroupResponse toResponse(AppGroup g) {
        return new GroupResponse(g.id(), g.ref(), g.track());
    }
}