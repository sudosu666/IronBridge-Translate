package com.ironbridge.translate;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class TfliteTranslationEngine implements Closeable {

    private Interpreter interpreter;
    private File modelFile;

    public synchronized void load(File file) throws IOException {
        close();
        modelFile = file;
        MappedByteBuffer modelBuffer = mapFile(file);
        Interpreter.Options options = new Interpreter.Options()
                .setNumThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        interpreter = new Interpreter(modelBuffer, options);
        validateInterpreter();
    }

    public synchronized boolean isLoaded() {
        return interpreter != null;
    }

    public synchronized String translate(String text) {
        if (interpreter == null) {
            throw new IllegalStateException("TFLite interpreter is not loaded");
        }
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        validateInterpreter();

        String[] input = new String[] { text };
        String[] output = new String[] { "" };
        interpreter.run(input, output);
        return output[0] == null ? "" : output[0];
    }

    public synchronized File getModelFile() {
        return modelFile;
    }

    @Override
    public synchronized void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        modelFile = null;
    }

    private void validateInterpreter() {
        if (interpreter == null) {
            return;
        }

        if (interpreter.getInputTensorCount() != 1 || interpreter.getOutputTensorCount() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported TFLite model: expected exactly one input tensor and one output tensor."
            );
        }

        if (interpreter.getInputTensor(0).dataType() != DataType.STRING) {
            throw new IllegalArgumentException(
                    "Unsupported TFLite model: input tensor must be STRING."
            );
        }

        if (interpreter.getOutputTensor(0).dataType() != DataType.STRING) {
            throw new IllegalArgumentException(
                    "Unsupported TFLite model: output tensor must be STRING."
            );
        }
    }

    private MappedByteBuffer mapFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             FileChannel channel = inputStream.getChannel()) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }
}
