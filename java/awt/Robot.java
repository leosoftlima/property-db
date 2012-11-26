/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.awt;

import java.awt.peer.*;
import java.awt.image.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import sun.awt.ComponentFactory;
import sun.awt.SunToolkit;
import sun.security.util.SecurityConstants;

/** {@collect.stats} 
 * This class is used to generate native system input events
 * for the purposes of test automation, self-running demos, and
 * other applications where control of the mouse and keyboard
 * is needed. The primary purpose of Robot is to facilitate
 * automated testing of Java platform implementations.
 * <p>
 * Using the class to generate input events differs from posting
 * events to the AWT event queue or AWT components in that the
 * events are generated in the platform's native input
 * queue. For example, <code>Robot.mouseMove</code> will actually move
 * the mouse cursor instead of just generating mouse move events.
 * <p>
 * Note that some platforms require special privileges or extensions
 * to access low-level input control. If the current platform configuration
 * does not allow input control, an <code>AWTException</code> will be thrown
 * when trying to construct Robot objects. For example, X-Window systems
 * will throw the exception if the XTEST 2.2 standard extension is not supported
 * (or not enabled) by the X server.
 * <p>
 * Applications that use Robot for purposes other than self-testing should
 * handle these error conditions gracefully.
 *
 * @author      Robi Khan
 * @since       1.3
 */
public class Robot {
    private static final int MAX_DELAY = 60000;
    private RobotPeer peer;
    private boolean isAutoWaitForIdle = false;
    private int autoDelay = 0;
    private static final int LEGAL_BUTTON_MASK =
                                            InputEvent.BUTTON1_MASK|
                                            InputEvent.BUTTON2_MASK|
                                            InputEvent.BUTTON3_MASK;

    // location of robot's GC, used in mouseMove(), getPixelColor() and captureScreenImage()
    private Point gdLoc;

    private DirectColorModel screenCapCM = null;

    /** {@collect.stats} 
     * Constructs a Robot object in the coordinate system of the primary screen.
     * <p>
     *
     * @throws  AWTException if the platform configuration does not allow
     * low-level input control.  This exception is always thrown when
     * GraphicsEnvironment.isHeadless() returns true
     * @throws  SecurityException if <code>createRobot</code> permission is not granted
     * @see     java.awt.GraphicsEnvironment#isHeadless
     * @see     SecurityManager#checkPermission
     * @see     AWTPermission
     */
    public Robot() throws AWTException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new AWTException("headless environment");
        }
        init(GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice());
    }

    /** {@collect.stats} 
     * Creates a Robot for the given screen device. Coordinates passed
     * to Robot method calls like mouseMove and createScreenCapture will
     * be interpreted as being in the same coordinate system as the
     * specified screen. Note that depending on the platform configuration,
     * multiple screens may either:
     * <ul>
     * <li>share the same coordinate system to form a combined virtual screen</li>
     * <li>use different coordinate systems to act as independent screens</li>
     * </ul>
     * This constructor is meant for the latter case.
     * <p>
     * If screen devices are reconfigured such that the coordinate system is
     * affected, the behavior of existing Robot objects is undefined.
     *
     * @param screen    A screen GraphicsDevice indicating the coordinate
     *                  system the Robot will operate in.
     * @throws  AWTException if the platform configuration does not allow
     * low-level input control.  This exception is always thrown when
     * GraphicsEnvironment.isHeadless() returns true.
     * @throws  IllegalArgumentException if <code>screen</code> is not a screen
     *          GraphicsDevice.
     * @throws  SecurityException if <code>createRobot</code> permission is not granted
     * @see     java.awt.GraphicsEnvironment#isHeadless
     * @see     GraphicsDevice
     * @see     SecurityManager#checkPermission
     * @see     AWTPermission
     */
    public Robot(GraphicsDevice screen) throws AWTException {
        checkIsScreenDevice(screen);
        init(screen);
    }

    private void init(GraphicsDevice screen) throws AWTException {
        checkRobotAllowed();
        gdLoc = screen.getDefaultConfiguration().getBounds().getLocation();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit instanceof ComponentFactory) {
            peer = ((ComponentFactory)toolkit).createRobot(this, screen);
            disposer = new RobotDisposer(peer);
            sun.java2d.Disposer.addRecord(anchor, disposer);
        }
    }

    /* determine if the security policy allows Robot's to be created */
    private void checkRobotAllowed() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(SecurityConstants.CREATE_ROBOT_PERMISSION);
        }
    }

    /* check if the given device is a screen device */
    private void checkIsScreenDevice(GraphicsDevice device) {
        if (device == null || device.getType() != GraphicsDevice.TYPE_RASTER_SCREEN) {
            throw new IllegalArgumentException("not a valid screen device");
        }
    }

    private transient Object anchor = new Object();

    static class RobotDisposer implements sun.java2d.DisposerRecord {
        private final RobotPeer peer;
        public RobotDisposer(RobotPeer peer) {
            this.peer = peer;
        }
        public void dispose() {
            if (peer != null) {
                peer.dispose();
            }
        }
    }

    private transient RobotDisposer disposer;

    /** {@collect.stats} 
     * Moves mouse pointer to given screen coordinates.
     * @param x         X position
     * @param y         Y position
     */
    public synchronized void mouseMove(int x, int y) {
        peer.mouseMove(gdLoc.x + x, gdLoc.y + y);
        afterEvent();
    }

    /** {@collect.stats} 
     * Presses one or more mouse buttons.  The mouse buttons should
     * be released using the <code>mouseRelease</code> method.
     *
     * @param buttons   the Button mask; a combination of one or more
     * of these flags:
     * <ul>
     * <li><code>InputEvent.BUTTON1_MASK</code>
     * <li><code>InputEvent.BUTTON2_MASK</code>
     * <li><code>InputEvent.BUTTON3_MASK</code>
     * </ul>
     * @throws  IllegalArgumentException if the button mask is not a
     *          valid combination
     * @see #mouseRelease(int)
     */
    public synchronized void mousePress(int buttons) {
        checkButtonsArgument(buttons);
        peer.mousePress(buttons);
        afterEvent();
    }

    /** {@collect.stats} 
     * Releases one or more mouse buttons.
     *
     * @param buttons   the Button mask; a combination of one or more
     * of these flags:
     * <ul>
     * <li><code>InputEvent.BUTTON1_MASK</code>
     * <li><code>InputEvent.BUTTON2_MASK</code>
     * <li><code>InputEvent.BUTTON3_MASK</code>
     * </ul>
     * @see #mousePress(int)
     * @throws  IllegalArgumentException if the button mask is not a valid
     *          combination
     */
    public synchronized void mouseRelease(int buttons) {
        checkButtonsArgument(buttons);
        peer.mouseRelease(buttons);
        afterEvent();
    }

    private void checkButtonsArgument(int buttons) {
        if ( (buttons|LEGAL_BUTTON_MASK) != LEGAL_BUTTON_MASK ) {
            throw new IllegalArgumentException("Invalid combination of button flags");
        }
    }

    /** {@collect.stats} 
     * Rotates the scroll wheel on wheel-equipped mice.
     *
     * @param wheelAmt  number of "notches" to move the mouse wheel
     *                  Negative values indicate movement up/away from the user,
     *                  positive values indicate movement down/towards the user.
     *
     * @since 1.4
     */
    public synchronized void mouseWheel(int wheelAmt) {
        peer.mouseWheel(wheelAmt);
        afterEvent();
    }

    /** {@collect.stats} 
     * Presses a given key.  The key should be released using the
     * <code>keyRelease</code> method.
     * <p>
     * Key codes that have more than one physical key associated with them
     * (e.g. <code>KeyEvent.VK_SHIFT</code> could mean either the
     * left or right shift key) will map to the left key.
     *
     * @param   keycode Key to press (e.g. <code>KeyEvent.VK_A</code>)
     * @throws  IllegalArgumentException if <code>keycode</code> is not
     *          a valid key
     * @see     #keyRelease(int)
     * @see     java.awt.event.KeyEvent
     */
    public synchronized void keyPress(int keycode) {
        checkKeycodeArgument(keycode);
        peer.keyPress(keycode);
        afterEvent();
    }

    /** {@collect.stats} 
     * Releases a given key.
     * <p>
     * Key codes that have more than one physical key associated with them
     * (e.g. <code>KeyEvent.VK_SHIFT</code> could mean either the
     * left or right shift key) will map to the left key.
     *
     * @param   keycode Key to release (e.g. <code>KeyEvent.VK_A</code>)
     * @throws  IllegalArgumentException if <code>keycode</code> is not a
     *          valid key
     * @see  #keyPress(int)
     * @see     java.awt.event.KeyEvent
     */
    public synchronized void keyRelease(int keycode) {
        checkKeycodeArgument(keycode);
        peer.keyRelease(keycode);
        afterEvent();
    }

    private void checkKeycodeArgument(int keycode) {
        // rather than build a big table or switch statement here, we'll
        // just check that the key isn't VK_UNDEFINED and assume that the
        // peer implementations will throw an exception for other bogus
        // values e.g. -1, 999999
        if (keycode == KeyEvent.VK_UNDEFINED) {
            throw new IllegalArgumentException("Invalid key code");
        }
    }

    /** {@collect.stats} 
     * Returns the color of a pixel at the given screen coordinates.
     * @param   x       X position of pixel
     * @param   y       Y position of pixel
     * @return  Color of the pixel
     */
    public synchronized Color getPixelColor(int x, int y) {
        Color color = new Color(peer.getRGBPixel(gdLoc.x + x, gdLoc.y + y));
        return color;
    }

    /** {@collect.stats} 
     * Creates an image containing pixels read from the screen.  This image does
     * not include the mouse cursor.
     * @param   screenRect      Rect to capture in screen coordinates
     * @return  The captured image
     * @throws  IllegalArgumentException if <code>screenRect</code> width and height are not greater than zero
     * @throws  SecurityException if <code>readDisplayPixels</code> permission is not granted
     * @see     SecurityManager#checkPermission
     * @see     AWTPermission
     */
    public synchronized BufferedImage createScreenCapture(Rectangle screenRect) {
        checkScreenCaptureAllowed();

        // according to the spec, screenRect is relative to robot's GD
        Rectangle translatedRect = new Rectangle(screenRect);
        translatedRect.translate(gdLoc.x, gdLoc.y);
        checkValidRect(translatedRect);

        BufferedImage image;
        DataBufferInt buffer;
        WritableRaster raster;

        if (screenCapCM == null) {
            /*
             * Fix for 4285201
             * Create a DirectColorModel equivalent to the default RGB ColorModel,
             * except with no Alpha component.
             */

            screenCapCM = new DirectColorModel(24,
                                               /* red mask */    0x00FF0000,
                                               /* green mask */  0x0000FF00,
                                               /* blue mask */   0x000000FF);
        }

        int pixels[];
        int[] bandmasks = new int[3];

        pixels = peer.getRGBPixels(translatedRect);
        buffer = new DataBufferInt(pixels, pixels.length);

        bandmasks[0] = screenCapCM.getRedMask();
        bandmasks[1] = screenCapCM.getGreenMask();
        bandmasks[2] = screenCapCM.getBlueMask();

        raster = Raster.createPackedRaster(buffer, translatedRect.width, translatedRect.height, translatedRect.width, bandmasks, null);

        image = new BufferedImage(screenCapCM, raster, false, null);

        return image;
    }

    private static void checkValidRect(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0) {
            throw new IllegalArgumentException("Rectangle width and height must be > 0");
        }
    }

    private static void checkScreenCaptureAllowed() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                SecurityConstants.READ_DISPLAY_PIXELS_PERMISSION);
        }
    }

    /*
     * Called after an event is generated
     */
    private void afterEvent() {
        autoWaitForIdle();
        autoDelay();
    }

    /** {@collect.stats} 
     * Returns whether this Robot automatically invokes <code>waitForIdle</code>
     * after generating an event.
     * @return Whether <code>waitForIdle</code> is automatically called
     */
    public synchronized boolean isAutoWaitForIdle() {
        return isAutoWaitForIdle;
    }

    /** {@collect.stats} 
     * Sets whether this Robot automatically invokes <code>waitForIdle</code>
     * after generating an event.
     * @param   isOn    Whether <code>waitForIdle</code> is automatically invoked
     */
    public synchronized void setAutoWaitForIdle(boolean isOn) {
        isAutoWaitForIdle = isOn;
    }

    /*
     * Calls waitForIdle after every event if so desired.
     */
    private void autoWaitForIdle() {
        if (isAutoWaitForIdle) {
            waitForIdle();
        }
    }

    /** {@collect.stats} 
     * Returns the number of milliseconds this Robot sleeps after generating an event.
     */
    public synchronized int getAutoDelay() {
        return autoDelay;
    }

    /** {@collect.stats} 
     * Sets the number of milliseconds this Robot sleeps after generating an event.
     * @throws  IllegalArgumentException If <code>ms</code> is not between 0 and 60,000 milliseconds inclusive
     */
    public synchronized void setAutoDelay(int ms) {
        checkDelayArgument(ms);
        autoDelay = ms;
    }

    /*
     * Automatically sleeps for the specified interval after event generated.
     */
    private void autoDelay() {
        delay(autoDelay);
    }

    /** {@collect.stats} 
     * Sleeps for the specified time.
     * To catch any <code>InterruptedException</code>s that occur,
     * <code>Thread.sleep()</code> may be used instead.
     * @param   ms      time to sleep in milliseconds
     * @throws  IllegalArgumentException if <code>ms</code> is not between 0 and 60,000 milliseconds inclusive
     * @see     java.lang.Thread#sleep
     */
    public synchronized void delay(int ms) {
        checkDelayArgument(ms);
        try {
            Thread.sleep(ms);
        } catch(InterruptedException ite) {
            ite.printStackTrace();
        }
    }

    private void checkDelayArgument(int ms) {
        if (ms < 0 || ms > MAX_DELAY) {
            throw new IllegalArgumentException("Delay must be to 0 to 60,000ms");
        }
    }

    /** {@collect.stats} 
     * Waits until all events currently on the event queue have been processed.
     * @throws  IllegalThreadStateException if called on the AWT event dispatching thread
     */
    public synchronized void waitForIdle() {
        checkNotDispatchThread();
        // post a dummy event to the queue so we know when
        // all the events before it have been processed
        try {
            SunToolkit.flushPendingEvents();
            EventQueue.invokeAndWait( new Runnable() {
                                            public void run() {
                                                // dummy implementation
                                            }
                                        } );
        } catch(InterruptedException ite) {
            System.err.println("Robot.waitForIdle, non-fatal exception caught:");
            ite.printStackTrace();
        } catch(InvocationTargetException ine) {
            System.err.println("Robot.waitForIdle, non-fatal exception caught:");
            ine.printStackTrace();
        }
    }

    private void checkNotDispatchThread() {
        if (EventQueue.isDispatchThread()) {
            throw new IllegalThreadStateException("Cannot call method from the event dispatcher thread");
        }
    }

    /** {@collect.stats} 
     * Returns a string representation of this Robot.
     *
     * @return  the string representation.
     */
    public synchronized String toString() {
        String params = "autoDelay = "+getAutoDelay()+", "+"autoWaitForIdle = "+isAutoWaitForIdle();
        return getClass().getName() + "[ " + params + " ]";
    }
}
