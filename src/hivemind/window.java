/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hivemind;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;

/**
 *
 * @author Dracoix
 */
public class window extends JFrame {

    int posX = 0, posY = 0;
    static public long newDelay = 30000;

    public window() {
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                posX = e.getX();
                posY = e.getY();
            }
        });
        this.setUndecorated(
                true);
        this.addMouseMotionListener(new MouseAdapter() {

            public void mouseDragged(MouseEvent evt) {
                //sets frame position when mouse dragged			
                setLocation(evt.getXOnScreen() - posX, evt.getYOnScreen() - posY);

            }

        }
        );

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (posY >= 20 || posY <= 24) {
                    if (posX >= 88 && posX <= 92) {
                        newDelay -= 100;
                    }

                    if (posX >= 96 && posX <= 100) {
                        newDelay += 100;
                    }
                }
            }
        });

    }
}
