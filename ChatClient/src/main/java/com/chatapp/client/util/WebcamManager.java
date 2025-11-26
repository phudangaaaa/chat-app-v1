package com.chatapp.client.util;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * Webcam manager using webcam-capture library
 */
public class WebcamManager {
    private Webcam webcam;
    private Thread captureThread;
    private volatile boolean capturing = false;
    private ImageView targetView;

    public WebcamManager() {
        // Get default webcam
        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("No webcam detected!");
        }
    }

    /**
     * Start capturing video from webcam
     */
    public void startCapture(ImageView imageView) {
        if (webcam == null) {
            System.err.println("No webcam available");
            return;
        }

        this.targetView = imageView;
        capturing = true;

        // Set resolution
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();

        captureThread = new Thread(() -> {
            try {
                while (capturing && !Thread.interrupted()) {
                    BufferedImage image = webcam.getImage();

                    if (image != null) {
                        // Update UI on JavaFX thread
                        Platform.runLater(() -> {
                            if (targetView != null) {
                                targetView.setImage(SwingFXUtils.toFXImage(image, null));
                            }
                        });
                    }

                    // Limit to ~30 FPS
                    Thread.sleep(33);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stop capturing
     */
    public void stopCapture() {
        capturing = false;

        if (captureThread != null) {
            captureThread.interrupt();
        }

        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    /**
     * Check if webcam is available
     */
    public boolean isWebcamAvailable() {
        return webcam != null;
    }

    /**
     * Get current frame for streaming
     */
    public BufferedImage getCurrentFrame() {
        if (webcam != null && webcam.isOpen()) {
            return webcam.getImage();
        }
        return null;
    }
}
