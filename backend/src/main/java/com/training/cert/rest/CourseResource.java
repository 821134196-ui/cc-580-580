package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.CourseCreate;
import com.training.cert.dto.ApiDtos.CourseUpdate;
import com.training.cert.dto.ApiDtos.CourseView;
import com.training.cert.model.Course;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/courses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CourseResource {

    @Inject
    EntityManager em;

    @Inject
    DtoMapper mapper;

    @GET
    public List<CourseView> list() {
        return em.createQuery("SELECT c FROM Course c ORDER BY c.id", Course.class)
                .getResultList().stream().map(mapper::course).toList();
    }

    @POST
    @Transactional
    public CourseView create(CourseCreate req) {
        if (req == null || req.code() == null || req.code().isBlank()
                || req.title() == null || req.title().isBlank()
                || req.requiredHours() == null || req.requiredHours() <= 0
                || req.passingScore() == null || req.passingScore() < 0 || req.passingScore() > 100) {
            throw new BadRequestException("课程编号、名称、所需学时(>0)、及格分(0-100)均为必填");
        }
        Course c = new Course();
        c.code = req.code().trim();
        c.title = req.title().trim();
        c.requiredHours = req.requiredHours();
        c.passingScore = req.passingScore();
        em.persist(c);
        return mapper.course(c);
    }

    /**
     * 已签发过证书的课程,其信息不得直接修改 —— 证书上是签发时刻的课程快照,
     * 修改课程定义会让历史证书与课程现状不一致,故此处直接拒绝。
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public CourseView update(@PathParam("id") Long id, CourseUpdate req) {
        Course c = em.find(Course.class, id);
        if (c == null) {
            throw new NotFoundException("课程不存在: " + id);
        }
        long issued = em.createQuery(
                        "SELECT COUNT(cert) FROM Certificate cert WHERE cert.enrollment.course = :c", Long.class)
                .setParameter("c", c)
                .getSingleResult();
        if (issued > 0) {
            throw new ClientErrorException("该课程已签发 " + issued + " 张证书,课程信息不可直接修改", 409);
        }
        if (req.title() != null && !req.title().isBlank()) {
            c.title = req.title().trim();
        }
        if (req.requiredHours() != null && req.requiredHours() > 0) {
            c.requiredHours = req.requiredHours();
        }
        if (req.passingScore() != null && req.passingScore() >= 0 && req.passingScore() <= 100) {
            c.passingScore = req.passingScore();
        }
        return mapper.course(c);
    }
}
