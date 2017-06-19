package ro.upt.edu.mapzenver;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapzen.android.core.MapzenManager;

/**
 * Base activity that sets Mapzen API key if provided via Gradle properties.
 */
public class BaseActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.MAPZEN_API_KEY != null &&
                !BuildConfig.MAPZEN_API_KEY.equals(MapzenManager.API_KEY_DEFAULT_VALUE)) {
            MapzenManager.instance(this).setApiKey(BuildConfig.MAPZEN_API_KEY);
        }

        super.onCreate(savedInstanceState);
    }
}
