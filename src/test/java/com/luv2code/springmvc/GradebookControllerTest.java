package com.luv2code.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luv2code.springmvc.models.CollegeStudent;
import com.luv2code.springmvc.models.MathGrade;
import com.luv2code.springmvc.repository.HistoryGradesDao;
import com.luv2code.springmvc.repository.MathGradesDao;
import com.luv2code.springmvc.repository.ScienceGradesDao;
import com.luv2code.springmvc.repository.StudentDao;
import com.luv2code.springmvc.service.StudentAndGradeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/application-test.properties")
@Transactional
public class GradebookControllerTest {
    private static MockHttpServletRequest request;

//    POMOCU OVE ANOTACIJE SE INJECT-UJE JPA ENTITY MANAGER, I ZBOG TOGA JE UVEDENA ANOTACIJA @Transactional
    @PersistenceContext
    private EntityManager entityManager;

    @Mock
    private StudentAndGradeService studentAndGradeService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private MathGradesDao mathGradeDao;

    @Autowired
    private ScienceGradesDao scienceGradeDao;

    @Autowired
    private HistoryGradesDao historyGradeDao;

    @Autowired
    private StudentAndGradeService studentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollegeStudent student;

    @Value("${sql.script.create.student}")
    private String sqlAddStudent;

    @Value("${sql.script.create.math.grade}")
    private String sqlAddMathGrade;

    @Value("${sql.script.create.science.grade}")
    private String sqlAddScienceGrade;

    @Value("${sql.script.create.history.grade}")
    private String sqlAddHistoryGrade;

    @Value("${sql.script.delete.student}")
    private String sqlDeleteStudent;

    @Value("${sql.script.delete.math.grade}")
    private String sqlDeleteMathGrade;

    @Value("${sql.script.delete.science.grade}")
    private String sqlDeleteScienceGrade;

    @Value("${sql.script.delete.history.grade}")
    private String sqlDeleteHistoryGrade;

//    OVO SE KORISTI KADA SE VERIFIKUJE JSON RESPONSE
    public static final MediaType APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON;

    @BeforeAll
    public static void beforeAll(){
        request = new MockHttpServletRequest();

        request.setParameter("firstname", "Eric");
        request.setParameter("lastname", "Kripke");
        request.setParameter("emailAddress", "eric@test.com");
    }

    @BeforeEach
    public void setupDatabase() {
        jdbc.execute(sqlAddStudent);
        jdbc.execute(sqlAddMathGrade);
        jdbc.execute(sqlAddScienceGrade);
        jdbc.execute(sqlAddHistoryGrade);
    }

    @Test
    public void getStudentsHttpRequest() throws Exception{

        student.setFirstname("John");
        student.setLastname("Doe");
        student.setEmailAddress("john@test.com");

        entityManager.persist(student);
        entityManager.flush();

//        OVAKO TESTIRAM REST CONTROLLER, PROVERAVAM STATUS, CONTENT TYPE I JSON BODY
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
//                SA jsonPath SE ISPITUJE JSON BODY, GDE SA '$' PRISTUPAM KORENU JSON ARRAY-A
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void addStudentHttpRequest() throws Exception{
        student.setFirstname("John");
        student.setLastname("Doe");
        student.setEmailAddress("john@test.com");

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
//                OBJECT MAPPER GENERISE JSON STRING OD JAVA OBJEKTA
                .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        CollegeStudent verifyStudent = studentDao.findByEmailAddress("john@test.com");

        assertNotNull(verifyStudent);
    }

    @Test
    public void deleteStudentHttpRequest() throws Exception{
        assertTrue(studentDao.findById(1).isPresent());

        mockMvc.perform(delete("/student/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(0)));

        assertFalse(studentDao.findById(1).isPresent());
    }

    @Test
    public void deleteStudentHttpRequestErrorPage() throws Exception{
        assertFalse(studentDao.findById(0).isPresent());

        mockMvc.perform(delete("/student/{id}", 0))
                .andExpect(status().is4xxClientError())
//                SADA CE REST API DA VRATI JSON ERROR MESSAGE, A NJOJ PRISTUPAM SA $.NESTO
//                (SAM ERROR MESSAGE SE SASTOJI IZ status, message, timestamp)
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void getStudentInformationHttpRequest() throws Exception{
        Optional<CollegeStudent> optional = studentDao.findById(1);

        assertTrue(optional.isPresent());

        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Kripke")))
                .andExpect(jsonPath("$.emailAddress", is("eric@test.com")));
    }

    @Test
    public void getStudentInformationHttpRequestErrorPage() throws Exception{
        assertFalse(studentDao.findById(0).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}", 0))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void createGradeHttpRequest() throws Exception{
        mockMvc.perform(post("/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("grade", "85.00")
                        .param("gradeType", "math")
                        .param("studentId", "1"))
                        .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Kripke")))
                .andExpect(jsonPath("$.emailAddress", is("eric@test.com")))

//                IMAO SAM JEDNU OCENU ZA OVU OSOBU, ALI U OVOM TESTU SAM DODAO DRUGU
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(2)));
    }

    @Test
    public void createGradeHttpRequestErrorPage() throws Exception{
        assertFalse(studentDao.findById(0).isPresent());

        mockMvc.perform(post("/grades")
                        .contentType(APPLICATION_JSON_UTF8)
                        .param("grade", "85.00")
                        .param("gradeType", "math")
                        .param("studentId", "0"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void createGradeHttpRequestInvalidGradetype() throws Exception{
        mockMvc.perform(post("/grades")
                .contentType(APPLICATION_JSON_UTF8)
                .param("grade", "85.00")
                .param("gradeType", "biology")
                .param("studentId", "1"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void deleteGradeHttpRequest() throws Exception{
        Optional<MathGrade> optional = mathGradeDao.findById(1);

        assertTrue(optional.isPresent());

        mockMvc.perform(delete("/grades/{id}/{gradeType}", 1, "math"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Kripke")))
                .andExpect(jsonPath("$.emailAddress", is("eric@test.com")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(0)));
    }

    @Test
    public void deleteGradeHttpRequestInvalidGradeId() throws Exception{
        mockMvc.perform(delete("/grades/{id}/{gradeType}", 2, "math"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void deleteGradeHttpRequestInvalidGradetype() throws Exception{
        mockMvc.perform(delete("/grades/{id}/{gradeType}", 1, "biology"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @AfterEach
    public void setupAfterTransaction() {
        jdbc.execute(sqlDeleteStudent);
        jdbc.execute(sqlDeleteMathGrade);
        jdbc.execute(sqlDeleteScienceGrade);
        jdbc.execute(sqlDeleteHistoryGrade);
    }

}