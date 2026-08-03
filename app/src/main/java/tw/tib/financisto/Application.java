package tw.tib.financisto;

import android.os.StrictMode;

import androidx.multidex.MultiDexApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public class Application extends MultiDexApplication {
    private static Application instance;
    private static ExecutorService executor;
    // transaction ID -> copied timestamp millis
    private static Long2LongOpenHashMap copiedUneditedTransactions;

    public static Application getInstance() {
        return instance;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static Long2LongOpenHashMap getCopiedUneditedTransactions() {
        return copiedUneditedTransactions;
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        executor = Executors.newCachedThreadPool();
        copiedUneditedTransactions = new Long2LongOpenHashMap();

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
