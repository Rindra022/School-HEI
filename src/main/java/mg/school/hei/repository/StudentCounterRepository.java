package mg.school.hei.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class StudentCounterRepository {
  private final JdbcTemplate jdbcTemplate;

  @Transactional
  public int incrementAndGet(int year) {
    jdbcTemplate.update(
        "INSERT INTO student_counter (year, count) VALUES (?, 1) "
            + "ON CONFLICT (year) DO UPDATE SET count = student_counter.count + 1",
        year);
    return jdbcTemplate.queryForObject(
        "SELECT count FROM student_counter WHERE year = ?", Integer.class, year);
  }
}
