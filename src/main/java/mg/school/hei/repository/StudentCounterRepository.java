package mg.school.hei.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StudentCounterRepository extends JpaRepository<Object, Integer> {

  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO student_counter (year, count) VALUES (:year, 1) "
              + "ON CONFLICT (year) DO UPDATE SET count = student_counter.count + 1",
      nativeQuery = true)
  void incrementCounter(@Param("year") int year);

  @Query(value = "SELECT count FROM student_counter WHERE year = :year", nativeQuery = true)
  int getCount(@Param("year") int year);
}
