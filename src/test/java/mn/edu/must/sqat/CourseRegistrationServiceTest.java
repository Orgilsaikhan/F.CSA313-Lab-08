package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "B231234567";
    private static final String EMAIL = "b231234567@must.edu.mn";
    private static final String COURSE_ID = "CS313";

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private static Student student(boolean active, String... coursesTaken) {
        return new Student(STUDENT_ID, EMAIL, active, List.of(coursesTaken));
    }

    // A1 — when + verify
    @Test
    @DisplayName("A1: Идэвхтэй, өмнөх холбоо хангасан оюутан → OK, бүртгэл хадгалагдаж, и-мэйл илгээгдэнэ")
    void register_activeStudentPrerequisitesMet_returnsOkSavesAndSendsEmail() {
        // Arrange
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, "CS201"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));

        // Act
        String result = service.register(STUDENT_ID, COURSE_ID);

        // Assert
        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // A2 — verify(..., never())
    @Test
    @DisplayName("A2: Байхгүй оюутан → ERROR_NO_STUDENT, сан руу хадгалаагүй, и-мэйл яваагүй")
    void register_unknownStudent_returnsNoStudentAndNothingSavedOrSent() {
        // Arrange
        when(repository.findStudent("B239999999")).thenReturn(null);

        // Act
        String result = service.register("B239999999", COURSE_ID);

        // Assert
        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3 — verifyNoInteractions
    @Test
    @DisplayName("A3: Идэвхгүй оюутан → ERROR_INACTIVE_STUDENT, и-мэйл илгээгчтэй огт харьцаагүй")
    void register_inactiveStudent_returnsInactiveAndNoEmailInteraction() {
        // Arrange
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(false, "CS201"));

        // Act
        String result = service.register(STUDENT_ID, COURSE_ID);

        // Assert
        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A4 — when + never
    @Test
    @DisplayName("A4: Өмнөх холбоо хичээл дутуу → ERROR_PREREQUISITES, бүртгэл хадгалаагүй")
    void register_missingPrerequisite_returnsPrerequisitesAndNotSaved() {
        // Arrange
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, "MATH101"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));

        // Act
        String result = service.register(STUDENT_ID, COURSE_ID);

        // Assert
        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A5 — doThrow(...).when(...)
    @Test
    @DisplayName("A5: И-мэйл илгээгч exception шидсэн ч → OK, бүртгэл хадгалагдсан")
    void register_emailSenderThrows_stillReturnsOkAndSaved() {
        // Arrange
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, "CS201"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));
        doThrow(new RuntimeException("SMTP сервер унасан"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        // Act
        String result = service.register(STUDENT_ID, COURSE_ID);

        // Assert
        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // A6 — assertThrows
    @Test
    @DisplayName("A6: studentId эсвэл courseId null → тус бүр IllegalArgumentException, хамаарлуудтай огт харьцаагүй")
    void register_nullArgument_throwsAndNoInteractions() {
        // Act + Assert — аргумент БҮРИЙН null-ийг тусад нь (мутант 3-ын дараа нэмсэн)
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, COURSE_ID), "studentId = null");
        assertThrows(IllegalArgumentException.class,
                () -> service.register(STUDENT_ID, null), "courseId = null");

        verifyNoInteractions(repository, emailSender);
    }

    // A7 — ArgumentCaptor
    @Test
    @DisplayName("A7: Илгээгдсэн и-мэйлийн биед хичээлийн код (CS313) орсон байна")
    void register_success_emailBodyContainsCourseCode() {
        // Arrange
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, "CS201"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        service.register(STUDENT_ID, COURSE_ID);

        // Assert
        verify(emailSender).send(eq(EMAIL), eq("Бүртгэл баталгаажлаа"), bodyCaptor.capture());
        String body = bodyCaptor.getValue();
        assertTrue(body.contains(COURSE_ID), "и-мэйлийн бие: " + body);
    }

    // A8 — @ParameterizedTest + @CsvSource
    @ParameterizedTest(name = "A8[{index}]: studentExists={0}, active={1}, courseExists={2} → {3}")
    @CsvSource({
            "false, true,  true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_NO_COURSE"
    })
    @DisplayName("A8: Гурван алдааны анги → алдааны код, бүртгэл хадгалаагүй, и-мэйл яваагүй")
    void register_errorClass_returnsErrorCodeAndNothingSaved(
            boolean studentExists, boolean active, boolean courseExists, String expected) {
        // Arrange
        when(repository.findStudent(STUDENT_ID)).thenReturn(studentExists ? student(active, "CS201") : null);
        // Мөр бүр register()-ийг өөр шатанд зогсоодог тул доорх stub-ууд зарим мөрөнд
        // ашиглагдахгүй: [1],[2] → courseExists + prerequisites, [3] → prerequisites.
        // Strict stubs үүнийг UnnecessaryStubbingException гэж үздэг тул lenient().
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(courseExists);
        lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));

        // Act
        String result = service.register(STUDENT_ID, COURSE_ID);

        // Assert
        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}
