package com.training.cert.rest;

import com.training.cert.dto.ApiDtos.StudentCreate;
import com.training.cert.dto.ApiDtos.StudentView;
import com.training.cert.model.Student;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/students")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StudentResource {

    @Inject
    EntityManager em;

    @Inject
    DtoMapper mapper;

    @GET
    public List<StudentView> list() {
        return em.createQuery("SELECT s FROM Student s ORDER BY s.id", Student.class)
                .getResultList().stream().map(mapper::student).toList();
    }

    @POST
    @Transactional
    public StudentView create(StudentCreate req) {
        if (req == null || req.fullName() == null || req.fullName().isBlank()
                || req.email() == null || req.email().isBlank()) {
            throw new BadRequestException("姓名与邮箱不能为空");
        }
        Student s = new Student();
        s.fullName = req.fullName().trim();
        s.email = req.email().trim();
        em.persist(s);
        return mapper.student(s);
    }
}
