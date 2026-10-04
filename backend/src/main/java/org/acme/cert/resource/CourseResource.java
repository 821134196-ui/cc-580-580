package org.acme.cert.resource;

import java.util.List;

import org.acme.cert.dto.Dtos.CourseDto;
import org.acme.cert.dto.Dtos.CourseUpsert;
import org.acme.cert.model.Course;
import org.acme.cert.service.BusinessException;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/courses")
public class CourseResource {

    @Inject
    EntityManager em;

    @GET
    @Transactional
    public List<CourseDto> list() {
        return em.createQuery("select c from Course c order by c.id", Course.class)
                .getResultList().stream().map(this::toDto).toList();
    }

    @POST
    @Transactional
    public CourseDto create(CourseUpsert input) {
        validate(input);
        Course course = new Course();
        course.code = input.code().trim();
        course.title = input.title().trim();
        course.requiredHours = input.requiredHours();
        course.passScore = input.passScore();
        em.persist(course);
        return toDto(course);
    }

    /**
     * 修改课程信息。已签发过证书的课程拒绝修改——证书上的课程信息
     * 以签发时快照为准，不允许事后变动。
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public CourseDto update(@PathParam("id") Long id, CourseUpsert input) {
        Course course = em.find(Course.class, id);
        if (course == null) {
            throw BusinessException.notFound("课程不存在: " + id);
        }
        long certCount = certCount(id);
        if (certCount > 0) {
            throw BusinessException.conflict(
                    "该课程已签发 " + certCount + " 张证书，课程信息不可再修改");
        }
        validate(input);
        course.code = input.code().trim();
        course.title = input.title().trim();
        course.requiredHours = input.requiredHours();
        course.passScore = input.passScore();
        return toDto(course);
    }

    private long certCount(Long courseId) {
        return em.createQuery("select count(c) from Certificate c where c.course.id = :cid", Long.class)
                .setParameter("cid", courseId).getSingleResult();
    }

    private CourseDto toDto(Course course) {
        long count = certCount(course.id);
        return new CourseDto(course.id, course.code, course.title, course.requiredHours, course.passScore,
                count, count > 0);
    }

    private static void validate(CourseUpsert input) {
        if (input == null || input.code() == null || input.code().isBlank()
                || input.title() == null || input.title().isBlank()
                || input.requiredHours() == null || input.requiredHours() <= 0
                || input.passScore() == null || input.passScore() < 0 || input.passScore() > 100) {
            throw BusinessException.badRequest("课程信息不完整或学时/分数线不合法");
        }
    }
}
