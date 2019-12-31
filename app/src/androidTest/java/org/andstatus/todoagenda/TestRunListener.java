package org.andstatus.todoagenda;

import android.util.Log;

import org.andstatus.todoagenda.prefs.AllSettings;
import org.andstatus.todoagenda.provider.EventProviderType;
import org.andstatus.todoagenda.provider.MockCalendarContentProvider;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * @author yvolk@yurivolkov.com
 */
public class TestRunListener extends RunListener {
    private static final String TAG = "testSuite";

    public TestRunListener() {
        Log.i(TAG,  "TestRunListener created");
    }

    @Override
    public void testSuiteFinished(Description description) throws Exception {
        super.testSuiteFinished(description);
        Log.i(TAG, "Test Suite finished: " + description);
        if (description.toString().equals("null")) restoreApp();
    }

    private void restoreApp() {
        MockCalendarContentProvider.tearDown();

        EnvironmentChangedReceiver.sleep(2000);

        AllSettings.forget();
        EventProviderType.forget();
        EnvironmentChangedReceiver.forget();

        Log.i(TAG, "App restored");
    }
}
