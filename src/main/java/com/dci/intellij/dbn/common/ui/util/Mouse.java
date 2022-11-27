package com.dci.intellij.dbn.common.ui.util;

import com.dci.intellij.dbn.common.routine.Consumer;
import com.dci.intellij.dbn.common.util.Guarded;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class Mouse {
    private Mouse() {}

    public static void processMouseEvent(MouseEvent e, MouseListener listener) {
        int id = e.getID();
        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                listener.mousePressed(e);
                break;
            case MouseEvent.MOUSE_RELEASED:
                listener.mouseReleased(e);
                break;
            case MouseEvent.MOUSE_CLICKED:
                listener.mouseClicked(e);
                break;
            case MouseEvent.MOUSE_EXITED:
                listener.mouseExited(e);
                break;
            case MouseEvent.MOUSE_ENTERED:
                listener.mouseEntered(e);
                break;
        }
    }

    public static boolean isNavigationEvent(MouseEvent e) {
        int button = e.getButton();
        return button == MouseEvent.BUTTON2 || (e.isControlDown() && button == MouseEvent.BUTTON1);
    }

    public static void removeMouseListeners(JComponent root) {
        UserInterface.visitRecursively(root, component -> {
            MouseListener[] mouseListeners = component.getMouseListeners();
            for (MouseListener mouseListener : mouseListeners) {
                root.removeMouseListener(mouseListener);
            }
        });
    }


    public static Listener listener() {
        return new Listener();
    }

    public static class Listener implements MouseListener, MouseMotionListener {
        private Consumer<MouseEvent> clickConsumer;
        private Consumer<MouseEvent> pressConsumer;
        private Consumer<MouseEvent> releaseConsumer;
        private Consumer<MouseEvent> enterConsumer;
        private Consumer<MouseEvent> exitConsumer;
        private Consumer<MouseEvent> moveConsumer;
        private Consumer<MouseEvent> dragConsumer;

        @Override
        public void mouseClicked(MouseEvent e) {
            consume(e, clickConsumer);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            consume(e, pressConsumer);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            consume(e, releaseConsumer);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            consume(e, enterConsumer);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            consume(e, exitConsumer);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            consume(e, dragConsumer);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            consume(e, moveConsumer);
        }

        public Listener onClick(Consumer<MouseEvent> consumer) {
            this.clickConsumer = consumer;
            return this;
        }

        public Listener onPress(Consumer<MouseEvent> consumer) {
            this.pressConsumer = consumer;
            return this;
        }

        public Listener onRelease(Consumer<MouseEvent> consumer) {
            this.releaseConsumer = consumer;
            return this;
        }

        public Listener onEnter(Consumer<MouseEvent> consumer) {
            this.enterConsumer = consumer;
            return this;
        }

        public Listener onExit(Consumer<MouseEvent> consumer) {
            this.exitConsumer = consumer;
            return this;
        }

        public Listener onMove(Consumer<MouseEvent> consumer) {
            this.moveConsumer = consumer;
            return this;
        }

        public Listener onDrag(Consumer<MouseEvent> consumer) {
            this.dragConsumer = consumer;
            return this;
        }

        private void consume(MouseEvent e, @Nullable Consumer<MouseEvent> consumer) {
            if (consumer == null) return;
            Guarded.run(() -> consumer.accept(e));
        }
    }
}
