package com.moac.android.opensecretsanta.test.acceptance;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ListView;

import com.jayway.android.robotium.solo.Solo;
import com.moac.android.opensecretsanta.test.DateTestUtils;

import static com.moac.android.opensecretsanta.test.DateTestUtils.getGroupLabel;

/**
 * Acceptance tests for managing Groups
 *
 * As this is a blackbox test, we cannot have any application code dependencies
 */
public class GroupManagementTest extends ActivityInstrumentationTestCase2<Activity> {

    private static final String LAUNCHER_ACTIVITY_FULL_CLASSNAME =
            "com.moac.android.opensecretsanta.activity.MainActivity";
    public static final String GROUPS_HEADER = "My Groups";
    public static final String ADD_GROUP = "Add Group";

    private static Class launcherActivityClass;

    static {
        try {
            launcherActivityClass = Class
                    .forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Solo solo;

    @SuppressWarnings("unchecked")
    public GroupManagementTest() throws ClassNotFoundException {
        super(launcherActivityClass);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    /**
     * Verify the Group List contains a default created Group
     */
    public void testGroupManagementAcceptance() throws Exception {

        /*
         * Verify Default Group exists
         */

        // Check that we have the right activity under test
        solo.assertCurrentActivity("Incorrect Activity", launcherActivityClass);

        // Get the approximate creation time of the Group
        long group1Time = System.currentTimeMillis();
        String group1Label = getGroupLabel(DateTestUtils.getDate("yyyy", group1Time), 1);
        assertTrue(solo.waitForText(String.format(group1Label)));

        // Get the list views
        ListView memberListView = solo.getCurrentViews(ListView.class).get(0);
        ListView groupListView = solo.getCurrentViews(ListView.class).get(1);

        // Verify default Group is empty
        assertEquals(0, memberListView.getCount());

        // Open sliding drawer using application button
        // This doesn't seem to work, at least on my Nexus 4 API Level 16 emulator
        //solo.clickOnActionBarHomeButton();
        // FIXME This is a workaround
        View home = solo.getView(android.R.id.home);
        solo.clickOnView(home);

        // Wait for the Drawer to open and the header text to be visible
        assertTrue(solo.waitForText(GROUPS_HEADER));

        // Verify we have an Add Group "button"
        assertTrue(solo.searchText(ADD_GROUP, true));

        // Verify we have a header, but no footer item.
        assertEquals(1, groupListView.getHeaderViewsCount());
        assertEquals(0, groupListView.getFooterViewsCount());

        // Verify length includes Header, Button and Default Item
        assertEquals(3, groupListView.getCount());

        // Verify the default item is checked
        assertEquals(2, groupListView.getCheckedItemPosition());

        // Verify the contents of the default Group
        assertTrue(solo.searchText(group1Label, true));
        assertTrue(solo.searchText(DateTestUtils.getDate("d MMM yyyy", group1Time), true));

        /*
         * Create a Group
         */

        // Ensure the "Add Group" button is available
        assertTrue(solo.waitForText(ADD_GROUP));

        // Click on the "Add Group" button (the whole row acts a button)
        solo.clickInList(2, 0);

        // Verify new item added to the Group List
        assertEquals(4, groupListView.getCount());

        // Verify the new item is checked
        assertEquals(3, groupListView.getCheckedItemPosition());

        // Verify that the Draw is closed
        long group2Time = System.currentTimeMillis();
        String group2Label = getGroupLabel(DateTestUtils.getDate("yyyy", group2Time), 2);
        assertTrue(solo.waitForText(String.format(group2Label)));
        assertFalse(solo.searchText(GROUPS_HEADER, true));

        // Get the Members List
        assertEquals(0, memberListView.getCount());

        /**
         * Verify that we can swap between groups
         */

        // Open the Drawer
        solo.clickOnView(home);

        // Wait for the Drawer to open and the header text to be visible
        assertTrue(solo.waitForText(GROUPS_HEADER));

        // Click on Group #1 item in Group List
        solo.clickInList(3, 0);

        // Verify the Group #1 item is checked
        assertEquals(2, groupListView.getCheckedItemPosition());

        // Verify Drawer is closed
        assertFalse(solo.searchText(GROUPS_HEADER, true));

        // Verify is shown as Member List
        assertTrue(solo.waitForText(group1Label));

        /**
         * Verify toggling does not change current Group
         */

        // Open the Drawer again
        solo.clickOnView(home);

        // Wait for the Drawer to open and the header text to be visible
        assertTrue(solo.waitForText(GROUPS_HEADER));

        // Close the Drawer
        solo.clickOnView(home);

        // Verify Drawer is closed
        assertFalse(solo.searchText(GROUPS_HEADER, true));

        // Verify the Group #1 item is still checked
        assertEquals(2, groupListView.getCheckedItemPosition());

        // Verify is still shown as Member List
        assertTrue(solo.waitForText(group1Label));

        /**
         * Verify reselecting same Group shows correct Group
         */

        // Open the Drawer again
        solo.clickOnView(home);

        // Wait for the Drawer to open and the header text to be visible
        assertTrue(solo.waitForText(GROUPS_HEADER));

        // Click on Group #1 item in Group List
        solo.clickInList(3, 0);

        // Verify Drawer is closed
        assertFalse(solo.searchText(GROUPS_HEADER, true));

        // Verify the Group #1 item is still checked
        assertEquals(2, groupListView.getCheckedItemPosition());

        // Verify is still shown as Member List
        assertTrue(solo.waitForText(group1Label));
    }

}
