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

        // Stop any existing capture first
        stopCapture();

        this.targetView = imageView;
        capturing = true;

        // Set resolution and open
        try {
            webcam.setViewSize(new Dimension(640, 480));
            if (!webcam.isOpen()) {
                webcam.open();
            }
        } catch (Exception e) {
            System.err.println("Failed to open webcam: " + e.getMessage());
            return;
        }

        captureThread = new Thread(() -> {
            try {
                while (capturing && !Thread.interrupted()) {
                    // Check if webcam is still open before getting image
                    if (webcam != null && webcam.isOpen()) {
                        BufferedImage image = webcam.getImage();

                        if (image != null) {
                            // Update UI on JavaFX thread
                            Platform.runLater(() -> {
                                if (targetView != null && capturing) {
                                    targetView.setImage(SwingFXUtils.toFXImage(image, null));
                                }
                            });
                        }
                    } else {
                        // Webcam closed, stop loop
                        break;
                    }

                    // Limit to ~30 FPS
                    Thread.sleep(33);
                }
            } catch (InterruptedException e) {
                // Thread interrupted, exit gracefully
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error in webcam capture thread: " + e.getMessage());
            } finally {
                System.out.println("Webcam capture thread stopped");
            }
        });

        captureThread.setDaemon(true);
        captureThread.setName("WebcamCaptureThread");
        captureThread.start();
    }

    /**
     * Stop capturing
     */
    public void stopCapture() {
        // Signal thread to stop
        capturing = false;

        // Interrupt and wait for thread to finish
        if (captureThread != null && captureThread.isAlive()) {
            captureThread.interrupt();
            try {
                // Wait up to 1 second for thread to stop
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Now safe to close webcam
        if (webcam != null && webcam.isOpen()) {
            try {
                webcam.close();
                System.out.println("Webcam closed successfully");
            } catch (Exception e) {
                System.err.println("Error closing webcam: " + e.getMessage());
            }
        }

        captureThread = null;
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

    /**
     * Capture a single frame (alias for getCurrentFrame)
     */
    public BufferedImage captureFrame() {
        return getCurrentFrame();
    }
}
