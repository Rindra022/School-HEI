package mg.school.hei.mapper;

import mg.school.hei.model.AppUser;
import mg.school.hei.repository.model.JAppUser;
import org.springframework.stereotype.Component;

@Component
public class AppUserMapper {
  public AppUser toModel(JAppUser entity) {
    return AppUser.builder()
        .id(entity.getId())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .birthdate(entity.getBirthdate())
        .email(entity.getEmail())
        .password(entity.getPassword())
        .phone(entity.getPhone())
        .role(entity.getRole())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  public JAppUser toEntity(AppUser model) {
    return JAppUser.builder()
        .id(model.id())
        .firstName(model.firstName())
        .lastName(model.lastName())
        .birthdate(model.birthdate())
        .email(model.email())
        .password(model.password())
        .phone(model.phone())
        .role(model.role())
        .createdAt(model.createdAt())
        .build();
  }
}
