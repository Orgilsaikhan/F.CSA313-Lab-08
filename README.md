# Lab 08 - B222270044 - Г. Оргилсайхан

**Сэдэв:** Mock объектууд — Mockito-гоор хамаарлыг орлуулж тестлэх (F.CSA313, 2026)

```
$ java -version
openjdk version "25.0.5.0.3" 2026-10-24
OpenJDK Runtime Environment (build 25.0.5.0.3+1-1-26.04.1-Ubuntu)
OpenJDK 64-Bit Server VM (build 25.0.5.0.3+1-1-26.04.1-Ubuntu, mixed mode, sharing)

$ mvn -version
Apache Maven 3.9.12
Maven home: /usr/share/maven
Java version: 25.0.5.0.3, vendor: Ubuntu, runtime: /usr/lib/jvm/java-25-openjdk-amd64
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "7.0.0-43-generic", arch: "amd64", family: "unix"
```

Орчин: Ubuntu 26.04.1 LTS · OpenJDK 25 (`maven.compiler.release` = 17) · Maven 3.9.12 · JUnit 5.10.2 · Mockito 5.11.0 · Surefire 3.2.5

## Товч үр дүн

| Үзүүлэлт | Үр дүн |
|---|---|
| Тестийн методын тоо | **8** (A1–A8) — 7 `@Test` + 1 `@ParameterizedTest` (3 мөр) |
| `results/mvn-test.txt` | **Tests run: 10**, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS |
| `results/mvn-test-mutant-1.txt` (идэвхгүй шалгалт комментлосон) | Tests run: 10, **Failures: 2** — BUILD FAILURE |
| `results/mvn-test-mutant-2.txt` (`saveRegistration` комментлосон) | Tests run: 10, **Failures: 2** — BUILD FAILURE |
| `results/mvn-test-mutant-3-survived.txt` (`\|\| courseId == null` устгасан, анхны A6) | Tests run: 10, Failures: 0 — BUILD SUCCESS → **мутант амьд үлдсэн** |
| `results/mvn-test-mutant-3.txt` (A6-г зассаны дараа) | Tests run: 10, **Failures: 1** — BUILD FAILURE |
| `results/mvn-test-A8-strict-stubs.txt` (A8 `lenient()`-гүй) | Tests run: 10, **Errors: 3** (`UnnecessaryStubbingException`) — BUILD FAILURE |

## Явц

- [x] Алхам 1 — Starter төсөл, `mvn test` → "No tests to run." + BUILD SUCCESS
- [x] Хэсэг A — Найман тест (A1–A8), A8-д strict stubs алдаа → `lenient()`
- [x] Хэсэг B — Мутант 1, 2 барьсан; мутант 3 амьд үлдсэн → A6-г засаж барьдаг болгосон
- [x] `src/main` starter-тай яг ижил болгож сэргээсэн (`diff -r` хоосон), `results/mvn-test.txt` ногоон
- [x] Репог өөр хавтаст clone хийж `mvn test` → Tests run: 10, BUILD SUCCESS
- [x] Дүгнэлт

## Репогийн бүтэц

```
pom.xml                                         # starter-ийнх, өөрчлөөгүй (JUnit 5.10.2 + Mockito 5.11.0)
src/main/java/mn/edu/must/sqat/
  Student.java                                  # record
  StudentRepository.java                        # интерфейс — хэрэгжүүлэлтгүй, тестэд mock
  EmailSender.java                              # интерфейс — хэрэгжүүлэлтгүй, тестэд mock
  CourseRegistrationService.java                # тестлэгдэх класс (starter-ийнх, өөрчлөөгүй)
src/test/java/mn/edu/must/sqat/
  CourseRegistrationServiceTest.java            # A1–A8
results/mvn-test.txt                            # эцсийн ногоон гаралт — Tests run: 10
results/mvn-test-A8-strict-stubs.txt            # A8 lenient()-гүй үед — Errors: 3
results/mvn-test-mutant-1.txt                   # мутант 1 — Failures: 2
results/mvn-test-mutant-2.txt                   # мутант 2 — Failures: 2
results/mvn-test-mutant-3-survived.txt          # мутант 3, анхны A6 — бүгд ногоон (цоорхой)
results/mvn-test-mutant-3.txt                   # мутант 3, зассан A6 — Failures: 1
```

Өөрөө ажиллуулж шалгах бол:

```bash
mvn test 2>&1 | tee results/mvn-test.txt
```

---

## Алхам 1: Starter төсөл

Starter-ийн `pom.xml` ба `src/main`-ийг репогийн root-д хуулаад `mvn test` ажиллуулахад `No tests to run.` + `BUILD SUCCESS` гарсан. JDK 25 дээр тест ажиллахад гардаг `WARNING: A Java agent has been loaded dynamically (… byte-buddy-agent-1.14.12.jar)` нь Mockito-гийн inline mock maker-ийн хэвийн анхааруулга тул засаагүй.

`register()`-ийн Javadoc ба кодын дарааллаас гаргасан oracle:

1. `studentId` эсвэл `courseId` нь `null` → `IllegalArgumentException` (сан руу хандахаас ӨМНӨ).
2. `findStudent` → `null` бол `ERROR_NO_STUDENT`.
3. `!student.active()` бол `ERROR_INACTIVE_STUDENT`.
4. `!courseExists` бол `ERROR_NO_COURSE`.
5. `prerequisites` − `coursesTaken` хоосон биш бол `ERROR_PREREQUISITES`.
6. Тэгээд `saveRegistration` → `emailSender.send(email, "Бүртгэл баталгаажлаа", "<courseId> хичээлд амжилттай бүртгэгдлээ.")` → `"OK"`. И-мэйл `RuntimeException` шидсэн ч `"OK"`.

## Хэсэг A: Найман тест

`@ExtendWith(MockitoExtension.class)`, `@Mock StudentRepository repository`, `@Mock EmailSender emailSender`, `@BeforeEach`-д `new CourseRegistrationService(repository, emailSender)`. Тест бүр Arrange–Act–Assert бүтэцтэй, нэр нь `юуг_ямарНөхцөлд_юуХүлээгдэнэ` хэв маягтай, `@DisplayName` монголоор.

| № | Метод | Stub | Oracle | Гол хэрэгсэл |
|---|---|---|---|---|
| A1 | `register_activeStudentPrerequisitesMet_returnsOkSavesAndSendsEmail` | идэвхтэй, `["CS201"]`; `CS313` байгаа, урьдач `["CS201"]` | `"OK"`; `verify(repository).saveRegistration("B231234567","CS313")`; `verify(emailSender).send(eq(EMAIL), anyString(), anyString())` | `when` + `verify` |
| A2 | `register_unknownStudent_returnsNoStudentAndNothingSavedOrSent` | `findStudent` → `null` | `"ERROR_NO_STUDENT"`; `saveRegistration`, `send` хоёулаа `never()` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsInactiveAndNoEmailInteraction` | идэвхгүй оюутан | `"ERROR_INACTIVE_STUDENT"`; хадгалаагүй; `verifyNoInteractions(emailSender)` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisite_returnsPrerequisitesAndNotSaved` | үзсэн `["MATH101"]`, урьдач `["CS201"]` | `"ERROR_PREREQUISITES"`; `saveRegistration`, `send` `never()` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndSaved` | A1-ийнх + `doThrow(new RuntimeException("SMTP сервер унасан")).when(emailSender).send(anyString(), anyString(), anyString())` | `"OK"`; `saveRegistration` дуудагдсан; `send` оролдсон | `doThrow(...).when(...)` |
| A6 | `register_nullArgument_throwsAndNoInteractions` | — | `register(null, "CS313")` ба `register("B231234567", null)` тус бүр `IllegalArgumentException`; `verifyNoInteractions(repository, emailSender)` | `assertThrows` |
| A7 | `register_success_emailBodyContainsCourseCode` | A1-ийнх | `verify(emailSender).send(eq(EMAIL), eq("Бүртгэл баталгаажлаа"), bodyCaptor.capture())`; биед `"CS313"` орсон | `ArgumentCaptor` |
| A8 | `register_errorClass_returnsErrorCodeAndNothingSaved` | 3 мөр — доор | алдааны код; `saveRegistration`, `send` `never()` | `@ParameterizedTest` + `@CsvSource` |

A8-ийн мөрүүд (`studentExists, active, courseExists, expected`) — A1–A7-д ороогүй `ERROR_NO_COURSE`-ийг энд хамруулсан:

```java
@CsvSource({
        "false, true,  true,  ERROR_NO_STUDENT",
        "true,  false, true,  ERROR_INACTIVE_STUDENT",
        "true,  true,  false, ERROR_NO_COURSE"
})
```

**Matcher-ийн дүрэм:** нэг дуудалтад matcher хэрэглэсэн газар бүх аргументад matcher (`eq(EMAIL), anyString(), anyString()`), matcher-гүй газар бүгд бодит утга (`saveRegistration("B231234567","CS313")`) — хольж бичээгүй.

### A8 ба `lenient()`

Эхлээд A8-ийг A1-тэй ижил энгийн `when(...)`-ээр бичиж ажиллуулахад 3 мөр **бүгд** `UnnecessaryStubbingException`-аар унасан (`results/mvn-test-A8-strict-stubs.txt` — `Tests run: 10, Failures: 0, Errors: 3`, BUILD FAILURE). Файлд заасан мөрүүдээр: [1], [2]-т `courseExists` ба `prerequisites` stub (тухайн үеийн тестийн файлын мөр 177, 178), [3]-т `prerequisites` stub (мөр 178) ашиглагдаагүй.

**Тайлбар:** `register()` мөр бүрт өөр шатанд (оюутан → идэвх → хичээл) зогсдог тул нэг ерөнхий Arrange-ийн зарим stub тухайн мөрөнд хэзээ ч дуудагдахгүй бөгөөд MockitoExtension-ий strict stubs горим үүнийг "хэрэггүй stub" гэж алдаа өгдөг. Тиймээс зөвхөн тэр хоёр stub-ийг `lenient().when(...)` болгосон, харин мөр бүрт ашиглагддаг `findStudent` stub strict хэвээр.

```java
when(repository.findStudent(STUDENT_ID)).thenReturn(studentExists ? student(active, "CS201") : null);
lenient().when(repository.courseExists(COURSE_ID)).thenReturn(courseExists);
lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS201"));
```

### Үр дүн

```bash
mkdir -p results && mvn test 2>&1 | tee results/mvn-test.txt
```

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Tests run: 10** = 7 энгийн тест (A1–A7) + A8-ийн 3 мөр.

## Хэсэг B: Мини мутаци

Мутант бүрийг `CourseRegistrationService.java`-д гараар оруулж `mvn test 2>&1 | tee results/mvn-test-mutant-N.txt` ажиллуулаад, дараа нь `git checkout -- src/main`-ээр сэргээсэн. Эцэст нь `diff -r src/main <starter>/src/main` хоосон гарсан — репод `src/main` эх хувилбараараа орсон.

| Мутант | Өөрчлөлт | Файл | Tests run / Failures / Errors | Унасан тест | Мессеж |
|---|---|---|---|---|---|
| **1** | `// if (!student.active()) return "ERROR_INACTIVE_STUDENT";` | `results/mvn-test-mutant-1.txt` | 10 / **2** / 0 — BUILD FAILURE | A3 `register_inactiveStudent_…` | `expected: <ERROR_INACTIVE_STUDENT> but was: <ERROR_NO_COURSE>` |
| | | | | A8 `[2]` (идэвхгүй мөр) | `expected: <ERROR_INACTIVE_STUDENT> but was: <OK>` |
| **2** | `// repository.saveRegistration(studentId, courseId);` | `results/mvn-test-mutant-2.txt` | 10 / **2** / 0 — BUILD FAILURE | A1 `register_activeStudentPrerequisitesMet_…` | `Wanted but not invoked: repository.saveRegistration("B231234567", "CS313")` |
| | | | | A5 `register_emailSenderThrows_…` | `Wanted but not invoked: repository.saveRegistration(…)` |
| **3** (анхны A6) | `if (studentId == null)` — `\|\| courseId == null` устгасан | `results/mvn-test-mutant-3-survived.txt` | 10 / 0 / 0 — **BUILD SUCCESS** | **нэг ч тест унаагүй — мутант амьд** | — |
| **3** (зассан A6) | ижил | `results/mvn-test-mutant-3.txt` | 10 / **1** / 0 — BUILD FAILURE | A6 `register_nullArgument_throwsAndNoInteractions` | `courseId = null ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.` |

Гурван мутант бүгд **Failures**-ээр унасан, `UnnecessaryStubbingException` (Errors) гараагүй: эвдсэн код нь stub-тай методуудыг дуудсаар байсан тул strict stubs-д хэрэггүй stub үлдээгүй. Errors зөвхөн Хэсэг A-гийн `lenient()`-гүй A8 туршилтад гарсан.

### Ажиглалтууд

- **Мутант 1, A3:** `OK` биш `ERROR_NO_COURSE` буцсан — A3 зөвхөн `findStudent`-ийг stub хийдэг тул идэвхийн шалгалт алга болоход код цааш `courseExists`-ийг дуудаж, mock-ийн анхдагч утга `false`-г авсан. Mock-ийн stub хийгээгүй `boolean` метод `false`, объект `null`, жагсаалт хоосон буцаадгийг санах хэрэгтэй — тест өөр шалтгаанаар ч "зөв" унаж болно. A8 `[2]`-т `courseExists`, `prerequisites` stub байсан тул мутант бүр `OK` хүртэл явсан.
- **Мутант 2:** A1, A5 л барьсан — хоёулаа `verify(repository).saveRegistration(...)`-тэй. A7 ногоон үлдсэн: зөвхөн и-мэйлийг шалгадаг тул "бүртгэлгүйгээр баталгаажуулах и-мэйл явуулдаг" согогийг ганцаараа барихгүй. Алдааны тестүүдийн `never()` энд үргэлж үнэн тул тус болохгүй.
- **Мутант 3 — тестийн цоорхой:** анхны A6 зөвхөн `register(null, "CS313")`-ийг шалгадаг байсан тул `courseId == null`-ийн шалгалт алга болсныг хэн ч анзаараагүй (`mvn-test-mutant-3-survived.txt` — бүгд ногоон). Мутантын үед `register("B231234567", null)` exception шидэхгүй, `findStudent` mock `null` буцааж `ERROR_NO_STUDENT` өгдөг.
  **Засвар:** A6-д `courseId = null` тохиолдлын хоёр дахь `assertThrows` нэмсэн — нэг метод хэвээр тул тестийн тоо 10 хэвээр.

```diff
     assertThrows(IllegalArgumentException.class,
-            () -> service.register(null, COURSE_ID));
+            () -> service.register(null, COURSE_ID), "studentId = null");
+    assertThrows(IllegalArgumentException.class,
+            () -> service.register(STUDENT_ID, null), "courseId = null");

     verifyNoInteractions(repository, emailSender);
```

Мутант 1, 2-ыг анхны A6-тай ажиллуулсан; A6-гийн өөрчлөлт тэр хоёр мутантын хэсэгт хамаарахгүй. Сэргээсний дараа `results/mvn-test.txt`-ийг дахин ажиллуулсан — **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS**.

## Дүгнэлт

Mock нь Mockito-гийн ажиллах үед үүсгэдэг, зан төлөвийг нь тест бүр stub-аар заадаг, "юу дуудагдсан бэ" гэдгийг `verify`-аар шалгах боломжтой объект бол гараар бичсэн fake нь (жишээ нь `HashMap` дээр суурилсан `InMemoryStudentRepository`) жинхэнэ ажилладаг хялбаршуулсан хэрэгжүүлэлт бөгөөд үр дүнгээр нь буюу төлөвөөр нь шалгадаг. Энэ лабд mock тохиромжтой байсан: `EmailSender` гаднын SMTP, `StudentRepository` өгөгдлийн сан тул хоёулаа удаан, тогтворгүй, бодит хэрэгжүүлэлтгүй — A5-ын "SMTP унасан" нөхцөлийг `doThrow` нэг мөрөөр үүсгэсэн нь fake-ээр хийхэд илүү код шаардах байсан. Mock хийх ёсгүй зүйл: `Student` record шиг энгийн өгөгдлийн объект, `ArrayList` шиг стандарт сан, мөн тестлэгдэж буй класс өөрөө — эдгээрийг бодитоор нь ашиглах нь илүү энгийн бөгөөд найдвартай. Мутант 1-ийн A3 нь stub хийгээгүй `courseExists`-ийн анхдагч `false`-аас болж `ERROR_NO_COURSE` авсан нь mock-ийн "чимээгүй" анхдагч утгууд тестийг санамсаргүйгээр өөр замаар явуулж болохыг харуулсан. `verify`-г хэтрүүлбэл тест "юу хийх ёстой вэ" биш "яаж хийдэг вэ"-г шалгаж эхэлнэ: жишээ нь `findStudent`, `courseExists`, `prerequisites` бүрийн дуудлагын тоо, дарааллыг `verify` хийвэл кодыг үр дүнг нь өөрчлөхгүйгээр рефактор хийхэд (жишээ нь кэш нэмэхэд) тестүүд хуурамчаар унана. Тиймээс би зөвхөн гаднаас ажиглагдах үр дагавартай дуудлагуудыг (`saveRegistration`, `send`) verify хийсэн, уншдаг методуудыг зөвхөн stub хийсэн. Мутант 2 үүнийг баталсан — `saveRegistration`-ийг verify хийсэн A1, A5 л согогийг барьсан бол үр дүнгийн утга (`"OK"`) ганцаараа хангалтгүй байв. Мутант 3 хамгийн их сургамж өгсөн: 10 тест бүгд ногоон байхад нэг аргументын шалгалт устсаныг ямар ч тест анзаараагүй тул тестийн тоо, ногоон өнгө нь тестийн чанарыг баталдаггүй, харин кодыг санаатай эвдэж үзэх нь л баталдаг.
