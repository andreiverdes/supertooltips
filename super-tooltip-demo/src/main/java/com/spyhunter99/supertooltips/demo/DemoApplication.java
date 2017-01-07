package com.spyhunter99.supertooltips.demo;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * created on 1/7/2017.
 *
 * @author Alex O'Ree
 */
@ReportsCrashes(formUri = "")
public class DemoApplication  extends Application {

    static String PACKAGE="";
    @Override
    public void onCreate() {
        super.onCreate();
        PACKAGE = getPackageName();
        try {
            LeakCanary.install(this);
        } catch (Throwable ex) {

            //this can happen on androidx86 getExternalStorageDir is not writable or if there is a
            //permission issue
            ex.printStackTrace();
        }
        Thread.currentThread().setUncaughtExceptionHandler(new OsmUncaughtExceptionHandler());

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);


        try {
            // Initialise ACRA
            ACRA.init(this);
            ACRA.getErrorReporter().setReportSender(new ErrorFileWriter());
        } catch (Throwable t) {
            t.printStackTrace();
            //this can happen on androidx86 getExternalStorageDir is not writable or if there is a
            //permissions issue
        }


    }

    public static class OsmUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e("UncaughtException", "Got an uncaught exception: " + ex.toString());
            if (ex.getClass().equals(OutOfMemoryError.class)) {
                writeHprof();
            }
            ex.printStackTrace();
        }
    }

    /**
     * writes the current heap to the file system at /sdcard/osmdroid/trace-{timestamp}.hprof
     * again, used only during out CI/memory leak tests
     */
    public static void writeHprof() {
        try {
            android.os.Debug.dumpHprofData(Environment.getExternalStorageDirectory().getAbsolutePath() + "/trace-"+PACKAGE+ + System.currentTimeMillis() + ".hprof");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Writes hard crash stack traces to a file on the SD card.
     */
    private static class ErrorFileWriter implements ReportSender {

        @Override
        public void send(Context context, CrashReportData crashReportData) throws ReportSenderException {
            try {
                String rootDirectory = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();

                File f = new File(rootDirectory
                    + File.separatorChar
                    + PACKAGE
                    + "crash.log");
                if (f.exists())
                    f.delete();


                f.createNewFile();
                PrintWriter pw = new PrintWriter(new FileWriter(f));
                pw.println(crashReportData.toString());
                pw.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }


        }
    }
}
