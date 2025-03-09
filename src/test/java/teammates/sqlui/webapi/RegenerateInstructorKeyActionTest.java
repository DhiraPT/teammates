package teammates.sqlui.webapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static teammates.ui.webapi.RegenerateInstructorKeyAction.SUCCESSFUL_REGENERATION_BUT_EMAIL_FAILED;
import static teammates.ui.webapi.RegenerateInstructorKeyAction.SUCCESSFUL_REGENERATION_WITH_EMAIL_SENT;
import static teammates.ui.webapi.RegenerateInstructorKeyAction.UNSUCCESSFUL_REGENERATION;

import org.apache.http.HttpStatus;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InstructorUpdateException;
import teammates.common.util.Const;
import teammates.common.util.EmailType;
import teammates.common.util.EmailWrapper;
import teammates.storage.sqlentity.Instructor;
import teammates.ui.output.MessageOutput;
import teammates.ui.output.RegenerateKeyData;
import teammates.ui.webapi.RegenerateInstructorKeyAction;

/**
 * SUT: {@link RegenerateInstructorKeyAction}.
 */
public class RegenerateInstructorKeyActionTest extends BaseActionTest<RegenerateInstructorKeyAction> {

    private Instructor instructor;

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.INSTRUCTOR_KEY;
    }

    @Override
    protected String getRequestMethod() {
        return POST;
    }

    @BeforeMethod
    void setUp() {
        Mockito.reset(mockLogic, mockSqlEmailGenerator);

        instructor = getTypicalInstructor();
        EmailWrapper mockEmail = mock(EmailWrapper.class);

        when(mockSqlEmailGenerator.generateFeedbackSessionSummaryOfCourse(
                instructor.getCourseId(),
                instructor.getEmail(),
                EmailType.INSTRUCTOR_COURSE_LINKS_REGENERATED
        )).thenReturn(mockEmail);
        mockEmailSender.setShouldFail(false);
    }

    @Test
    void testExecute_successfulRegenerationWithEmailSent_success()
            throws EntityDoesNotExistException, InstructorUpdateException {
        when(mockLogic.regenerateInstructorRegistrationKey(instructor.getCourseId(), instructor.getEmail()))
                .thenReturn(instructor);

        String[] params = {
                Const.ParamsNames.COURSE_ID, instructor.getCourseId(),
                Const.ParamsNames.INSTRUCTOR_EMAIL, instructor.getEmail(),
        };

        RegenerateInstructorKeyAction action = getAction(params);
        RegenerateKeyData actionOutput = (RegenerateKeyData) getJsonResult(action).getOutput();

        verify(mockLogic, times(1)).regenerateInstructorRegistrationKey(instructor.getCourseId(), instructor.getEmail());
        verify(mockSqlEmailGenerator, times(1)).generateFeedbackSessionSummaryOfCourse(
                instructor.getCourseId(),
                instructor.getEmail(),
                EmailType.INSTRUCTOR_COURSE_LINKS_REGENERATED
        );
        verifyNumberOfEmailsSent(1);
        assertEquals(SUCCESSFUL_REGENERATION_WITH_EMAIL_SENT, actionOutput.getMessage());
        assertNotNull(actionOutput.getNewRegistrationKey());
    }

    @Test
    void testExecute_successfulRegenerationWithEmailFailed_success()
            throws EntityDoesNotExistException, InstructorUpdateException {
        when(mockLogic.regenerateInstructorRegistrationKey(instructor.getCourseId(), instructor.getEmail()))
                .thenReturn(instructor);
        mockEmailSender.setShouldFail(true);

        String[] params = {
                Const.ParamsNames.COURSE_ID, instructor.getCourseId(),
                Const.ParamsNames.INSTRUCTOR_EMAIL, instructor.getEmail(),
        };

        RegenerateInstructorKeyAction action = getAction(params);
        RegenerateKeyData actionOutput = (RegenerateKeyData) getJsonResult(action).getOutput();

        verify(mockLogic, times(1)).regenerateInstructorRegistrationKey(instructor.getCourseId(), instructor.getEmail());
        verify(mockSqlEmailGenerator, times(1)).generateFeedbackSessionSummaryOfCourse(
                instructor.getCourseId(),
                instructor.getEmail(),
                EmailType.INSTRUCTOR_COURSE_LINKS_REGENERATED
        );
        verifyNoEmailsSent();
        assertEquals(SUCCESSFUL_REGENERATION_BUT_EMAIL_FAILED, actionOutput.getMessage());
        assertNotNull(actionOutput.getNewRegistrationKey());
    }

    @Test
    void testExecute_entityDoesNotExist_throwsEntityNotFoundException()
            throws EntityDoesNotExistException, InstructorUpdateException {
        when(mockLogic.regenerateInstructorRegistrationKey(instructor.getCourseId(), instructor.getEmail()))
                .thenThrow(new EntityDoesNotExistException("Instructor not found"));

        String[] params = {
                Const.ParamsNames.COURSE_ID, instructor.getCourseId(),
                Const.ParamsNames.INSTRUCTOR_EMAIL, instructor.getEmail(),
        };

        verify(mockLogic, never()).regenerateInstructorRegistrationKey(any(), any());
        verify(mockSqlEmailGenerator, never()).generateFeedbackSessionSummaryOfCourse(any(), any(), any());
        verifyNoEmailsSent();
        verifyEntityNotFound(params);
    }

    @Test
    void testExecute_instructorUpdateException_failure()
            throws EntityDoesNotExistException, InstructorUpdateException {
        when(mockLogic.regenerateInstructorRegistrationKey(instructor.getCourseId(), instructor.getEmail()))
                .thenThrow(new InstructorUpdateException("Instructor update failed"));

        String[] params = {
                Const.ParamsNames.COURSE_ID, instructor.getCourseId(),
                Const.ParamsNames.INSTRUCTOR_EMAIL, instructor.getEmail(),
        };

        RegenerateInstructorKeyAction action = getAction(params);
        MessageOutput actionOutput = (MessageOutput) getJsonResult(action, HttpStatus.SC_INTERNAL_SERVER_ERROR).getOutput();

        verify(mockLogic, times(1)).regenerateInstructorRegistrationKey(any(), any());
        verify(mockSqlEmailGenerator, never()).generateFeedbackSessionSummaryOfCourse(any(), any(), any());
        verifyNoEmailsSent();
        assertEquals(UNSUCCESSFUL_REGENERATION, actionOutput.getMessage());
    }

    @Test
    void testExecute_noParameters_throwsInvalidParametersException() {
        verifyHttpParameterFailure();
    }

    @Test
    void testExecute_missingInstructorEmail_throwsInvalidParametersException() {
        String[] params = {
                Const.ParamsNames.COURSE_ID, instructor.getCourseId(),
        };
        verifyHttpParameterFailure(params);
    }

    @Test
    void testExecute_missingCourseId_throwsInvalidParametersException() {
        String[] params = {
                Const.ParamsNames.INSTRUCTOR_EMAIL, instructor.getEmail(),
        };
        verifyHttpParameterFailure(params);
    }

    @Test
    void testSpecificAccessControl_admin_canAccess() {
        loginAsAdmin();
        verifyCanAccess();
    }

    @Test
    void testSpecificAccessControl_instructor_cannotAccess() {
        loginAsInstructor("instructor-googleId");
        verifyCannotAccess();
    }

    @Test
    void testSpecificAccessControl_student_cannotAccess() {
        loginAsStudent("student-googleId");
        verifyCannotAccess();
    }

    @Test
    void testSpecificAccessControl_loggedOut_cannotAccess() {
        logoutUser();
        verifyCannotAccess();
    }
}
