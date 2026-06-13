package pvt.muxalma.android;

import android.content.Context;

import java.io.File;
import java.util.UUID;

public interface TransportService {

    void prepare(Context context, File workDir);

    void runTransport(File workDir, UUID clientId, int port, Runnable onStarted) throws Exception;

    void terminate(File workDir) throws Exception;
}
