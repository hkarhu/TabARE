package fi.conf.tabare.demo;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.UIManager;

import fi.conf.tabare.CamImageProcessor;

public class Demo {

	static CamImageProcessor input;
	
	public static void main(String[] args) {
		
		//Decoration stuff
		JFrame.setDefaultLookAndFeelDecorated(true);
		try {
			System.out.println("Setting look and feel");

			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				// windows-support so doesn't look ugly
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				// linux gtk only such nice-look(tm)
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			}
		} catch (Exception e) {
			System.out.println("Unable to set LookAndFeel");
		}

        /* Create and display the form */
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CamImageProcessor().setVisible(true);
            }
        });
		
	}

}
