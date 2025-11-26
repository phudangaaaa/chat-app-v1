package com.chatapp.client.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

/**
 * Media streaming utility for audio/video calls
 */
public class MediaStreamManager {
    private static final int VIDEO_PORT_BASE = 9000;
    private static final int AUDIO_PORT_BASE = 9100;

    private Socket videoSocket;
    private Socket audioSocket;
    private Thread videoSendThread;
    private Thread audioSendThread;
    private Thread videoReceiveThread;
    private Thread audioReceiveThread;

    private volatile boolean streaming = false;
    private String serverHost;
    private int callId;

    // Audio format
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        16000, // Sample rate
        16,    // Sample size in bits
        1,     // Channels (mono)
        2,     // Frame size
        16000, // Frame rate
        false  // Big endian
    );

    public MediaStreamManager(String serverHost, int callId) {
        this.serverHost = serverHost;
        this.callId = callId;
    }

    /**
     * Start video streaming with webcam
     */
    public void startVideoStream(VideoStreamCallback callback) {
        streaming = true;

        videoSendThread = new Thread(() -> {
            try {
                // In a real implementation, you would:
                // 1. Initialize webcam using webcam-capture library
                // 2. Capture frames
                // 3. Compress frames (JPEG)
                // 4. Send to peer via socket

                // Placeholder for actual implementation
                System.out.println("Video streaming started...");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        videoReceiveThread = new Thread(() -> {
            try {
                // In a real implementation, you would:
                // 1. Receive compressed frames from socket
                // 2. Decompress frames
                // 3. Display in UI via callback

                System.out.println("Video receiving started...");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        videoSendThread.start();
        videoReceiveThread.start();
    }

    /**
     * Start audio streaming with microphone
     */
    public void startAudioStream() {
        streaming = true;

        audioSendThread = new Thread(() -> {
            try {
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(AUDIO_FORMAT);
                microphone.start();

                byte[] buffer = new byte[1024];

                while (streaming) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // In real implementation: send to peer via socket
                        // For now, just capture audio
                    }
                }

                microphone.stop();
                microphone.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        audioReceiveThread = new Thread(() -> {
            try {
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
                SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speaker.open(AUDIO_FORMAT);
                speaker.start();

                byte[] buffer = new byte[1024];

                while (streaming) {
                    // In real implementation: receive audio from socket and play
                    // For now, just prepare speaker
                }

                speaker.stop();
                speaker.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        audioSendThread.start();
        audioReceiveThread.start();
    }

    /**
     * Stop all streaming
     */
    public void stopStreaming() {
        streaming = false;

        try {
            if (videoSocket != null) videoSocket.close();
            if (audioSocket != null) audioSocket.close();

            if (videoSendThread != null) videoSendThread.interrupt();
            if (videoReceiveThread != null) videoReceiveThread.interrupt();
            if (audioSendThread != null) audioSendThread.interrupt();
            if (audioReceiveThread != null) audioReceiveThread.interrupt();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface VideoStreamCallback {
        void onFrameReceived(Image frame);
    }

    /**
     * Convert BufferedImage to JavaFX Image
     */
    private Image convertToFxImage(BufferedImage image) {
        WritableImage wr = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                pw.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return wr;
    }
}
