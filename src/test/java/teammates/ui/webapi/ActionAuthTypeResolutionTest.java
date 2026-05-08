package teammates.ui.webapi;

import org.testng.annotations.Test;

import teammates.common.util.Const;

/**
 * Tests for auth type resolution in {@link Action#initAuthInfo()}.
 *
 * <p>Uses {@link GetCourseAction} as a concrete REG_KEY-level action because its
 * checkSpecificAccessControl is straightforward to mock.
 */
public class ActionAuthTypeResolutionTest extends BaseActionTest<GetCourseAction> {

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.COURSE;
    }

    @Override
    protected String getRequestMethod() {
        return GET;
    }

    @Test
    void testInitAuthInfo_noLoginNoKey_setsPublic() {
        logoutUser();
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR,
        };
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.PUBLIC, action.authType);
    }

    @Test
    void testInitAuthInfo_noLoginEmptyKey_setsPublic() {
        logoutUser();
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR,
                Const.ParamsNames.REGKEY, "",
        };
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.PUBLIC, action.authType);
    }

    @Test
    void testInitAuthInfo_noLoginWithKey_setsRegKey() {
        logoutUser();
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR,
                Const.ParamsNames.REGKEY, "some-regkey",
        };
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.REG_KEY, action.authType);
    }

    @Test
    void testInitAuthInfo_loggedInNoKey_setsLoggedIn() {
        loginAsStudent("student-google-id");
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.STUDENT,
        };
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.LOGGED_IN, action.authType);
    }

    @Test
    void testInitAuthInfo_loggedInWithKey_loggedInTakesPrecedence() {
        loginAsStudent("student-google-id");
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.STUDENT,
                Const.ParamsNames.REGKEY, "some-regkey",
        };
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.LOGGED_IN, action.authType);
    }

    @Test
    void testInitAuthInfo_adminMasquerade_setsMasquerade() {
        loginAsAdmin();
        String masqueradeId = "student-google-id";
        String[] params = addUserIdToParams(masqueradeId, new String[] {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.STUDENT,
        });
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.MASQUERADE, action.authType);
    }

    @Test
    void testRegKeyLevelGate_noKey_rejectedBeforeSpecificCheck() {
        logoutUser();
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR,
        };
        // No regkey → PUBLIC < REG_KEY → gate rejects with generic message before specific check
        verifyCannotAccess(params);
    }

    @Test
    void testRegKeyLevelGate_withKey_passesGate() {
        logoutUser();
        // Any non-empty key string passes the level gate regardless of DB validity;
        // the specific check (getUnregisteredInstructor) then validates the entity.
        // Here we just verify the gate itself passes (specific check would fail, but
        // that is tested in GetCourseActionTest).
        String[] params = {
                Const.ParamsNames.COURSE_ID, "course-id",
                Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR,
                Const.ParamsNames.REGKEY, "some-regkey",
        };
        GetCourseAction action = getAction(params);
        assertEquals(AuthType.REG_KEY, action.authType);
        // The level check itself passes; any failure is in checkSpecificAccessControl.
        // We can verify by inspecting the authType level vs the action's min level:
        assertTrue(action.authType.getLevel() >= action.getMinAuthLevel().getLevel());
    }

}
