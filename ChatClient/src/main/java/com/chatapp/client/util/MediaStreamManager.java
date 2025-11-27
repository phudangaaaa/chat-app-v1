package com.chatapp.client.util;

import com.chatapp.client.model.Protocol;
import com.chatapp.client.service.NetworkManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

/**
 * Media streaming utility for audio/video calls
 * Streams video/audio through server relay
 */
public class MediaStreamManager {
    private final NetworkManager networkManager;
    private final WebcamManager webcamManager;
    private final int callId;

    private Thread videoSendThread;
    private Thread audioSendThread;
    private Thread audioReceiveThread;

    private volatile boolean streaming = false;

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

    public MediaStreamManager(int callId, WebcamManager webcamManager) {
        this.networkManager = NetworkManager.getInstance();
        this.callId = callId;
        this.webcamManager = webcamManager;
    }

    /**
     * Start video streaming with webcam
     */
    public void startVideoStream(int otherUserId, ImageView remoteVideoView) {
        streaming = true;

        System.out.println("[MediaStream] Setting up VIDEO_FRAME receiver for user " + otherUserId);

        // Setup receiver for incoming video frames
        networkManager.setNotificationHandler("VIDEO_FRAME", protocol -> {
            try {
                System.out.println("[MediaStream] Received VIDEO_FRAME notification");

                // Server wraps data in "data" field, so unwrap it first
                JsonObject actualData = protocol.getData().has("data")
                    ? protocol.getData().get("data").getAsJsonObject()
                    : protocol.getData();

                if (!actualData.has("frame")) {
                    System.err.println("[MediaStream] VIDEO_FRAME missing 'frame' field");
                    return;
                }

                String base64Frame = actualData.get("frame").getAsString();
                byte[] imageBytes = Base64.getDecoder().decode(base64Frame);

                ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                BufferedImage bufferedImage = ImageIO.read(bis);

                if (bufferedImage != null) {
                    System.out.println("[MediaStream] Decoded video frame: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                    Image fxImage = convertToFxImage(bufferedImage);
                    Platform.runLater(() -> {
                        if (remoteVideoView != null) {
                            remoteVideoView.setImage(fxImage);
                        }
                    });
                } else {
                    System.err.println("[MediaStream] Failed to decode video frame");
                }
            } catch (Exception e) {
                System.err.println("[MediaStream] Error receiving video frame: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Send video frames from webcam
        videoSendThread = new Thread(() -> {
            System.out.println("[MediaStream] Video send thread started for user " + otherUserId);

            // Wait a bit for webcam to be ready
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }

            int frameCount = 0;
            try {
                while (streaming && !Thread.interrupted()) {
                    // Get frame from webcam
                    if (webcamManager != null && webcamManager.isWebcamAvailable()) {
                        BufferedImage frame = webcamManager.captureFrame();

                        if (frame != null) {
                            frameCount++;

                            // Compress to JPEG
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(frame, "jpg", baos);
                            byte[] imageBytes = baos.toByteArray();
                            String base64Frame = Base64.getEncoder().encodeToString(imageBytes);

                            // Send to other user via server
                            JsonObject data = new JsonObject();
                            data.addProperty("receiverId", otherUserId);
                            data.addProperty("callId", callId);
                            data.addProperty("type", "VIDEO_FRAME");
                            data.addProperty("frame", base64Frame);

                            networkManager.sendNotification(Protocol.ACTION_CALL_SIGNAL, data);

                            if (frameCount % 30 == 0) {
                                System.out.println("[MediaStream] Sent " + frameCount + " video frames to user " + otherUserId);
                            }
                        } else {
                            System.err.println("[MediaStream] Webcam returned null frame");
                        }
                    } else {
                        System.err.println("[MediaStream] Webcam not available");
                    }

                    // Limit to ~15 FPS to reduce bandwidth
                    Thread.sleep(66);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[MediaStream] Error in video send thread: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("[MediaStream] Video streaming stopped. Total frames sent: " + frameCount);
        });

        videoSendThread.setDaemon(true);
        videoSendThread.start();
    }

    /**
     * Start audio streaming with microphone
     */
    public void startAudioStream(int otherUserId) {
        streaming = true;

        System.out.println("[MediaStream] Setting up AUDIO_CHUNK receiver for user " + otherUserId);

        // Setup receiver for incoming audio
        networkManager.setNotificationHandler("AUDIO_CHUNK", protocol -> {
            try {
                System.out.println("[MediaStream] Received AUDIO_CHUNK notification");

                // Server wraps data in "data" field, so unwrap it first
                JsonObject actualData = protocol.getData().has("data")
                    ? protocol.getData().get("data").getAsJsonObject()
                    : protocol.getData();

                if (!actualData.has("audio")) {
                    System.err.println("[MediaStream] AUDIO_CHUNK missing 'audio' field");
                    return;
                }

                String base64Audio = actualData.get("audio").getAsString();
                byte[] audioData = Base64.getDecoder().decode(base64Audio);

                // Play audio
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
                if (!AudioSystem.isLineSupported(speakerInfo)) {
                    System.err.println("[MediaStream] Speaker line not supported");
                    return;
                }

                SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speaker.open(AUDIO_FORMAT);
                speaker.start();
                speaker.write(audioData, 0, audioData.length);
                speaker.drain();
                speaker.close();

                System.out.println("[MediaStream] Played audio chunk: " + audioData.length + " bytes");

            } catch (Exception e) {
                System.err.println("[MediaStream] Error receiving audio: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Send audio from microphone
        audioSendThread = new Thread(() -> {
            System.out.println("[MediaStream] Audio send thread started for user " + otherUserId);

            try {
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);

                if (!AudioSystem.isLineSupported(micInfo)) {
                    System.err.println("[MediaStream] Microphone line not supported!");
                    return;
                }

                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(AUDIO_FORMAT);
                microphone.start();

                System.out.println("[MediaStream] Microphone opened and started");

                byte[] buffer = new byte[1024];
                int chunkCount = 0;

                while (streaming && !Thread.interrupted()) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        chunkCount++;

                        // Encode and send
                        String base64Audio = Base64.getEncoder().encodeToString(buffer);

                        JsonObject data = new JsonObject();
                        data.addProperty("receiverId", otherUserId);
                        data.addProperty("callId", callId);
                        data.addProperty("type", "AUDIO_CHUNK");
                        data.addProperty("audio", base64Audio);

                        networkManager.sendNotification(Protocol.ACTION_CALL_SIGNAL, data);

                        if (chunkCount % 50 == 0) {
                            System.out.println("[MediaStream] Sent " + chunkCount + " audio chunks to user " + otherUserId);
                        }
                    }
                }

                microphone.stop();
                microphone.close();

                System.out.println("[MediaStream] Audio streaming stopped. Total chunks sent: " + chunkCount);

            } catch (Exception e) {
                System.err.println("[MediaStream] Error in audio send thread: " + e.getMessage());
                e.printStackTrace();
            }
        });

        audioSendThread.setDaemon(true);
        audioSendThread.start();
    }

    /**
     * Stop all streaming
     */
    public void stopStreaming() {
        System.out.println("[MediaStream] Stopping all streams...");
        streaming = false;

        try {
            // Interrupt threads
            if (videoSendThread != null) videoSendThread.interrupt();
            if (audioSendThread != null) audioSendThread.interrupt();
            if (audioReceiveThread != null) audioReceiveThread.interrupt();

            // Wait for threads to finish
            if (videoSendThread != null && videoSendThread.isAlive()) {
                System.out.println("[MediaStream] Waiting for video send thread to stop...");
                videoSendThread.join(2000); // Wait up to 2 seconds
            }

            if (audioSendThread != null && audioSendThread.isAlive()) {
                System.out.println("[MediaStream] Waiting for audio send thread to stop...");
                audioSendThread.join(2000);
            }

            if (audioReceiveThread != null && audioReceiveThread.isAlive()) {
                audioReceiveThread.join(1000);
            }

            // Remove handlers
            networkManager.removeNotificationHandler("VIDEO_FRAME");
            networkManager.removeNotificationHandler("AUDIO_CHUNK");

            System.out.println("[MediaStream] All streams stopped successfully");

        } catch (Exception e) {
            System.err.println("[MediaStream] Error stopping stream: " + e.getMessage());
            e.printStackTrace();
        }
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
