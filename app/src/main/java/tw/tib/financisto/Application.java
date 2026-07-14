package tw.tib.financisto;

import android.os.StrictMode;

import androidx.multidex.MultiDexApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application extends MultiDexApplication {
    private static Application instance;
    private static ExecutorService executor;

    public static Application getInstance() {
        return instance;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        executor = Executors.newCachedThreadPool();

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            //        .detectLeakedSqlLiteObjects()
            //        .detectLeakedClosableObjects()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }
}
