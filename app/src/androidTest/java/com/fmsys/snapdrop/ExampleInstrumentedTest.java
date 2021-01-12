package com.fmsys.snapdrop;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    /**

    // InstrumentedTests are currently not executed in our CI
    // If it should be necessary in the future, following GitHub Actions workflow might help

jobs:
  test:
    runs-on: macOS-latest
    steps:
    - name: checkout
      uses: actions/checkout@v1
      with:
        fetch-depth: 1

    - name: run tests
      uses: reactivecircus/android-emulator-runner@v1
      with:
        api-level: 29
        script: ./gradlew connectedCheck
    */


    @Test
    public void useAppContext() {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.snapdrop", appContext.getPackageName());
    }
}
