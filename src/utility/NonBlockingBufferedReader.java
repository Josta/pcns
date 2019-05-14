package utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NonBlockingBufferedReader {
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
    private volatile boolean closed = false;
    private Thread backgroundReaderThread = null;

    public NonBlockingBufferedReader(final InputStream input) {
        backgroundReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
                    while (!Thread.interrupted()) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                        lines.add(line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    closed = true;
                }
            }
        });
        backgroundReaderThread.setDaemon(true);
        backgroundReaderThread.start();
    }

    public String readLine() {
        try {
            return closed && lines.isEmpty() ? null : lines.poll(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        	e.printStackTrace();
        	return null;
        }
    }

    public void close() {
        if (backgroundReaderThread != null) {
            backgroundReaderThread.interrupt();
            backgroundReaderThread = null;
        }
    }
}