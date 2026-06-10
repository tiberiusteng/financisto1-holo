package tw.tib.financisto;

import androidx.multidex.MultiDexApplication;

public class Application extends MultiDexApplication {
    private static Application instance;

    public static Application getInstance() {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
    }
}
