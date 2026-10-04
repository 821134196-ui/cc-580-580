package org.acme.cert.resource;

import java.util.List;

import org.acme.cert.dto.Dtos.ExamAttemptDto;
import org.acme.cert.dto.Dtos.StudentDto;
import org.acme.cert.dto.Dtos.StudentUpsert;
import org.acme.cert.dto.Dtos.StudyRecordDto;
import org.acme.cert.model.ExamAttempt;
import org.acme.cert.model.Student;
import org.acme.cert.model.StudyRecord;
import org.acme.cert.service.BusinessException;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/students")
public class StudentResource {

    @Inject
    EntityManager em;

    @GET
    public List<StudentDto> list() {
        return em.createQuery("select s from Student s order by s.id", Student.class)
                .getResultList().stream().map(StudentResource::toDto).toList();
    }

    @POST
    @Transactional
    public StudentDto create(StudentUpsert input) {
        if (input == null || input.studentNo() == null || input.studentNo().isBlank()
                || input.fullName() == null || input.fullName().isBlank()
                || input.email() == null || input.email().isBlank()
                || input.idCardNo() == null || input.idCardNo().isBlank()) {
            throw BusinessException.badRequest("学员信息不完整");
        }
        Student student = new Student();
        student.studentNo = input.studentNo().trim();
        student.fullName = input.fullName().trim();
        student.email = input.email().trim();
        student.idCardNo = input.idCardNo().trim();
        em.persist(student);
        return toDto(student);
    }

    @GET
    @Path("/{id}/records")
    @Transactional
    public List<StudyRecordDto> records(@PathParam("id") Long id) {
        return em.createQuery("select r from StudyRecord r where r.student.id = :sid order by r.id",
                StudyRecord.class)
                .setParameter("sid", id).getResultList().stream()
                .map(r -> new StudyRecordDto(r.id, r.student.id, r.course.id, r.course.title,
                        r.completedHours, r.updatedAt))
                .toList();
    }

    @GET
    @Path("/{id}/attempts")
    @Transactional
    public List<ExamAttemptDto> attempts(@PathParam("id") Long id) {
        return em.createQuery("select a from ExamAttempt a where a.student.id = :sid order by a.id",
                ExamAttempt.class)
                .setParameter("sid", id).getResultList().stream()
                .map(a -> new ExamAttemptDto(a.id, a.student.id, a.course.id, a.course.title,
                        a.attemptNo, a.score, a.takenAt))
                .toList();
    }

    static StudentDto toDto(Student s) {
        return new StudentDto(s.id, s.studentNo, s.fullName, s.email, s.idCardNo);
    }
}
