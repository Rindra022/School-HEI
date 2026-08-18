CREATE TABLE app_user (
                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          first_name  VARCHAR(100) NOT NULL,
                          last_name   VARCHAR(100) NOT NULL,
                          birthdate   DATE,
                          email       VARCHAR(255) NOT NULL UNIQUE,
                          password    VARCHAR(255) NOT NULL,
                          phone       VARCHAR(30),
                          role        VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
                          created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE promotion (
                           id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           year  INT NOT NULL UNIQUE
);

CREATE TABLE student_counter (
                                 year   INT PRIMARY KEY,
                                 count  INT NOT NULL DEFAULT 0
);

CREATE TABLE student (
                         id            UUID PRIMARY KEY REFERENCES app_user(id),
                         std           VARCHAR(20) NOT NULL UNIQUE,
                         promotion_id  UUID NOT NULL REFERENCES promotion(id)
);

CREATE TABLE app_group (
                           id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           ref    VARCHAR(100) NOT NULL,
                           track  VARCHAR(20) NOT NULL CHECK (track IN ('EL', 'TN', 'COMMUN'))
);

CREATE TABLE group_membership (
                                  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  student_id  UUID NOT NULL REFERENCES student(id),
                                  group_id    UUID NOT NULL REFERENCES app_group(id),
                                  start_date  DATE NOT NULL,
                                  end_date    DATE,
                                  CONSTRAINT chk_membership_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_membership_student ON group_membership(student_id);
CREATE INDEX idx_membership_group   ON group_membership(group_id);

CREATE TABLE course (
                        id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        ref      VARCHAR(100) NOT NULL UNIQUE,
                        title    VARCHAR(255) NOT NULL,
                        credits  INT NOT NULL CHECK (credits > 0)
);

CREATE TABLE course_assignment (
                                   id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   course_id      UUID NOT NULL REFERENCES course(id),
                                   teacher_id     UUID NOT NULL REFERENCES app_user(id),
                                   group_id       UUID NOT NULL REFERENCES app_group(id),
                                   academic_year  INT NOT NULL,
                                   UNIQUE (course_id, teacher_id, group_id, academic_year)
);

CREATE INDEX idx_assignment_group_year ON course_assignment(group_id, academic_year);
CREATE INDEX idx_assignment_teacher    ON course_assignment(teacher_id);


CREATE TABLE exam (
                      id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      assignment_id  UUID NOT NULL REFERENCES course_assignment(id),
                      date_exam      TIMESTAMPTZ NOT NULL,
                      coefficient    NUMERIC(4,3) NOT NULL CHECK (coefficient > 0 AND coefficient <= 1)
);

CREATE INDEX idx_exam_assignment ON exam(assignment_id);

CREATE TABLE grade (
                       id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       student_id         UUID NOT NULL REFERENCES student(id),
                       exam_id            UUID NOT NULL REFERENCES exam(id),
                       value              NUMERIC(4,2) NOT NULL CHECK (value >= 0 AND value <= 20),
                       graded_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                       reason             TEXT,
                       previous_grade_id  UUID REFERENCES grade(id),
                       is_current         BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_grade_student ON grade(student_id);
CREATE INDEX idx_grade_exam    ON grade(exam_id);

CREATE UNIQUE INDEX uq_grade_current ON grade(student_id, exam_id) WHERE is_current;