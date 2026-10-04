package org.acme.cert.resource;

import java.time.LocalDateTime;
import java.util.List;

import org.acme.cert.dto.Dtos.ExamAttemptCreate;
import org.acme.cert.dto.Dtos.ExamAttemptDto;
import org.acme.cert.dto.Dtos.StudyRecordDto;
import org.acme.cert.dto.Dtos.StudyRecordUpsert;
import org.acme.cert.model.Course;
import org.acme.cert.model.ExamAttempt;
import org.acme.cert.model.Student;
import org.acme.cert.model.StudyRecord;
import org.acme.cert.service.BusinessException;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 * 教务录入：学时维护 + 考试成绩登记（补考/重修即新增一条成绩记录）。
 */
@Path("/api/records")
public class RecordResource {

    @Inject
    EntityManager em;

    /** 录入学时（同一学员同一课程只保留一条进度，重复提交为更新） */
    @PUT
    @Path("/study")
    @Transactional
    public StudyRecordDto upsertStudy(StudyRecordUpsert input) {
        if (input == null || input.studentId() == null || input.courseId() == null
                || input.completedHours() == null || input.completedHours() < 0) {
            throw BusinessException.badRequest("学时不合法");
        }
        Student student = em.find(Student.class, input.studentId());
        Course course = em.find(Course.class, input.courseId());
        if (student == null || course == null) {
            throw BusinessException.notFound("学员或课程不存在");
        }
        StudyRecord record = em.createQuery(
                "select r from StudyRecord r where r.student.id = :sid and r.course.id = :cid", StudyRecord.class)
                .setParameter("sid", student.id).setParameter("cid", course.id)
                .getResultStream().findFirst().orElse(null);
        if (record == null) {
            record = new StudyRecord();
            record.student = student;
            record.course = course;
            record.completedHours = input.completedHours();
            em.persist(record);
        } else {
            record.completedHours = input.completedHours();
        }
        record.updatedAt = LocalDateTime.now();
        return new StudyRecordDto(record.id, student.id, course.id, course.title,
                record.completedHours, record.updatedAt);
    }

    /** 登记一次考试成绩（补考/重修会产生新的 attempt_no） */
    @POST
    @Path("/exam")
    @Transactional
    public ExamAttemptDto addAttempt(ExamAttemptCreate input) {
        if (input == null || input.studentId() == null || input.courseId() == null
                || input.score() == null || input.score() < 0 || input.score() > 100) {
            throw BusinessException.badRequest("成绩不合法（0-100）");
        }
        Student student = em.find(Student.class, input.studentId());
        Course course = em.find(Course.class, input.courseId());
        if (student == null || course == null) {
            throw BusinessException.notFound("学员或课程不存在");
        }
        int nextNo = em.createQuery(
                "select coalesce(max(a.attemptNo), 0) + 1 from ExamAttempt a "
                        + "where a.student.id = :sid and a.course.id = :cid", Integer.class)
                .setParameter("sid", student.id).setParameter("cid", course.id)
                .getSingleResult();
        ExamAttempt attempt = new ExamAttempt();
        attempt.student = student;
        attempt.course = course;
        attempt.attemptNo = nextNo;
        attempt.score = input.score();
        em.persist(attempt);
        return new ExamAttemptDto(attempt.id, student.id, course.id, course.title,
                attempt.attemptNo, attempt.score, attempt.takenAt);
    }

    @GET
    @Path("/exam")
    @Transactional
    public List<ExamAttemptDto> attempts(@QueryParam("studentId") Long studentId,
            @QueryParam("courseId") Long courseId) {
        return em.createQuery(
                "select a from ExamAttempt a where a.student.id = :sid and a.course.id = :cid "
                        + "order by a.attemptNo", ExamAttempt.class)
                .setParameter("sid", studentId).setParameter("cid", courseId)
                .getResultList().stream()
                .map(a -> new ExamAttemptDto(a.id, a.student.id, a.course.id, a.course.title,
                        a.attemptNo, a.score, a.takenAt))
                .toList();
    }
}
