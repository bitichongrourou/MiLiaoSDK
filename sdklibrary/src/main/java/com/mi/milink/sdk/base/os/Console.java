
package com.mi.milink.sdk.base.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 控制台执行类<br>
 * <br>
 * 用于执行控制台指令，获得其输出的字符串 <br>
 * <br>
 * 使用{@code Console.execute()}方法执行，timeout表示超时时间，防止调用者阻塞时间过长
 */
public class Console extends Thread {
    private static final long CONSOLE_STREAM_READER_TIMEOUT = 1000L;

    /**
     * 执行控制台指令，获得输出字符串
     *
     * @param commandLine 命令行
     * @param timeout 超时时间，单位ms
     * @return 控制台输出
     */
    public static String execute(String commandLine, long timeout) {
        return execute(commandLine, timeout, CONSOLE_STREAM_READER_TIMEOUT);
    }

    public static String execute(String commandLine, long timeout, long readTimeout) {
        String data = null;

        ConsoleThread consoleThread = new ConsoleThread(commandLine, readTimeout);

        consoleThread.start();

        try {
            consoleThread.join(timeout);
        } catch (InterruptedException e) {

        }

        if (consoleThread.isAlive()) {
            consoleThread.interrupt();
        }

        data = consoleThread.getOutputData();

        return data;
    }

    public static class ConsoleReader extends Thread {
        private InputStream stream = null;

        private String outputData;

        public ConsoleReader(InputStream inputStream) {
            this.setStream(inputStream);
        }

        @Override
        public void run() {
            BufferedReader reader = null;

            StringBuilder resultBuilder = new StringBuilder();

            try {
                reader = new BufferedReader(new InputStreamReader(stream));

                String line = reader.readLine();

                while (line != null) {
                    resultBuilder.append(line);
                    line = reader.readLine();
                }

                reader.close();

                setOutputData(resultBuilder.toString());
            } catch (Exception e) {
                // e.print*StackTrace();
                setOutputData(null);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2) {

                    }
                }
            }
        };

        public void setStream(InputStream stream) {
            this.stream = stream;
        }

        public InputStream getStream() {
            return stream;
        }

        public String getOutputData() {
            return outputData;
        }

        public void setOutputData(String outputData) {
            this.outputData = outputData;
        }
    }

    public static class ConsoleThread extends Thread {
        private Integer exitCode = 0;

        private String outputData = null;

        private String command = null;

        private long timeout = CONSOLE_STREAM_READER_TIMEOUT;

        public ConsoleThread(String commandLine, long timeout) {
            this.setCommand(commandLine);
            this.setTimeout(timeout);
        }

        @Override
        public void run() {
            ConsoleReader reader = null;

            try {
                Runtime runtime = Runtime.getRuntime();

                Process process = runtime.exec(getCommand());

                int code = process.waitFor();

                String data = null;

                reader = new ConsoleReader(process.getInputStream());

                reader.start();

                try {
                    reader.join(CONSOLE_STREAM_READER_TIMEOUT);
                } catch (InterruptedException e) {

                }

                if (reader.isAlive()) {
                    reader.interrupt();
                }

                data = reader.getOutputData();

                setExitCode(code);
                setOutputData(data);

                process.destroy();
            } catch (IOException e) {
                setExitCode(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                setExitCode(Integer.MIN_VALUE);
            } finally {

            }
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public Integer getExitCode() {
            return exitCode;
        }

        public void setExitCode(Integer exitCode) {
            this.exitCode = exitCode;
        }

        public String getOutputData() {
            return outputData;
        }

        public void setOutputData(String outputData) {
            this.outputData = outputData;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }
}
