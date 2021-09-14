import java.util.*;
import java.net.*;
import java.io.*;

import java.awt.*;
import java.awt.event.*;

import java.util.regex.*;

public class App extends Frame implements WindowListener, ActionListener, Runnable {

	private static final int PORT = 12121;
	private final InetAddress addrs;

	private final DatagramSocket sock;

	private final TextField name;
	private final TextArea disp;
	private final TextField input;
	private final Button send;
	private final Button connect;

	private final Map<InetAddress, String> lookupTable = new HashMap<>();

	public App() throws Exception {
		super("uChat");

		this.addrs = InetAddress.getByName("255.255.255.255");
		this.sock = new DatagramSocket(PORT);

		this.disp = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
		this.input = new TextField();
		this.name = new TextField();
		this.send = new Button("Send");
		this.connect = new Button("Join");

		this.disp.setEditable(false);

		final Panel namePanel = new Panel(new BorderLayout());
		namePanel.add(new Label("Name:"), BorderLayout.WEST);
		namePanel.add(this.name, BorderLayout.CENTER);
		namePanel.add(this.connect, BorderLayout.EAST);

		final Panel messagePanel = new Panel(new BorderLayout());
		messagePanel.add(this.input, BorderLayout.CENTER);
		messagePanel.add(this.send, BorderLayout.EAST);

		this.setLayout(new BorderLayout());
		add(namePanel, BorderLayout.NORTH);
		add(this.disp, BorderLayout.CENTER);
		add(messagePanel, BorderLayout.SOUTH);

		new Thread(this).start();

		this.send.addActionListener(this);
		this.connect.addActionListener(this);
		this.input.addActionListener(this);
		this.name.addActionListener(this);
		this.addWindowListener(this);
		this.setVisible(true);
	}

	public void setVisible(final boolean b) {
		super.setVisible(b);
		if(b) {
			this.pack();
			final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

			// move to bottom-left of screen
			final int newX = screenSize.width - this.getSize().width;
			final int newY = screenSize.height - this.getSize().height;
			this.setLocation(newX, newY);
		}
	}

	public static void main(final String[] args) {
		try {
			new App();
		}
		catch(final Exception e) {
			App.handle(e);
		}
	}

	public void actionPerformed(final ActionEvent evt) {
		if(this.name.getText().trim().equals(""))
			return;

		byte[] data = null;
		try {
			if(evt.getSource() == this.input || evt.getSource() == this.send) {
				if(this.input.getText().trim().equals(""))
					return;
				if(!this.connect.getLabel().equals("Leave"))
					return;
				// collapse spaces to prevent exploiting text wrapping
				this.input.setText(
						Pattern.compile(" {3,}")
						.matcher(this.input.getText().trim())
						.replaceAll("  ")
					);
				data = "SAY:".concat(this.input.getText()).getBytes("ISO-8859-1");
			}
			else if(evt.getSource() == this.connect || evt.getSource() == this.name) {
				if(this.connect.getLabel() == "Join") {
					this.name.setEditable(false);
					data = "JOIN".concat(this.name.getText()).getBytes("ISO-8859-1");
					this.connect.setLabel("Leave");
					this.input.requestFocus();
				}
				else if (this.connect.getLabel() == "Leave") {
					data = "LEAV".concat(this.name.getText()).getBytes("ISO-8859-1");
					this.connect.setLabel("Join");
					this.name.setEditable(true);
				}
				else { /* shouldn't happen */
					return;
				}
			}
			else { /* shouldn't happen */
				return;
			}

			this.sock.send(new DatagramPacket(data, data.length, addrs, PORT));
		}
		catch(final Exception e) {
			App.handle(e);
		}
		this.input.setText("");
	}

	private boolean die = false;
	public void run() {
		while(!die) {
			final byte[] data = new byte[1024];
			final DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
			try {
				this.sock.receive(incomingPacket);

				final String inStr = new String(data, "ISO-8859-1");

				if(inStr.startsWith("SAY:")) {
					final String who = this.lookupTable.get(incomingPacket.getAddress());
					this.disp.append(who+": "+new String(data, 4, incomingPacket.getLength()-4, "ISO-8859-1")+"\n");
					this.toFront();
					Toolkit.getDefaultToolkit().beep();
				}
				else if(inStr.startsWith("JOIN")) {
					final InetAddress tmp = incomingPacket.getAddress();
					final String tmpS = new String(data, 4, incomingPacket.getLength()-4, "ISO-8859-1");
					this.lookupTable.put(tmp, tmpS);
					this.disp.append(tmpS+'('+tmp.getHostAddress()+')'+" has joined\n");

					byte[] send = "HERE".concat(this.name.getText()).getBytes("ISO-8859-1");
					this.sock.send(new DatagramPacket(send, send.length, tmp, PORT));
				}
				else if(inStr.startsWith("HERE")) {
					final InetAddress tmp = incomingPacket.getAddress();
					final String tmpS = new String(data, 4, incomingPacket.getLength()-4, "ISO-8859-1");
					this.lookupTable.put(tmp, tmpS);

					if(!tmpS.equals(this.name.getText())) { //needed to suppress "you are here"
						this.disp.append(tmpS+'('+tmp.getHostAddress()+')'+" is here\n");
					}
				}
				else if(inStr.startsWith("LEAV")) {
					final InetAddress tmp = incomingPacket.getAddress();
					final String tmpS = new String(data, 4, incomingPacket.getLength()-4, "ISO-8859-1");
					this.lookupTable.remove(tmp);
					this.disp.append(tmpS+'('+tmp.getHostAddress()+')'+" has left\n");
				}
				else { /* unknown, silently ignore */ }
			}
			catch(final Exception e) {
				App.handle(e);
			}
		}
		try {
			this.sock.close();
		}
		catch(final Exception e) {
			App.handle(e);
		}
	}

	public void windowClosing(final WindowEvent evt) {
		this.setVisible(false);

		if(this.connect.getLabel() == "Join") { //only send LEAV if we're JOINed
			try {
				final byte[] data = "LEAV".concat(name.getText()).getBytes("ISO-8859-1");
				this.sock.send(new DatagramPacket(data, data.length, addrs, PORT));
			}
			catch(final Exception e) {
				App.handle(e);
			}
		}

		this.die = true;
		this.dispose();
		// System.exit(0);
	}
	public void windowOpened(final WindowEvent evt) { }
	public void windowClosed(final WindowEvent evt) { }
	public void windowIconified(final WindowEvent evt) { }
	public void windowDeiconified(final WindowEvent evt) { }
	public void windowActivated(final WindowEvent evt) { }
	public void windowDeactivated(final WindowEvent evt) { }

	public static void handle(final Exception e) {
		final StringWriter sb = new StringWriter();

		e.printStackTrace(new PrintWriter(sb));
		e.printStackTrace(System.out);

		final TextArea disp = new TextArea(sb.getBuffer().toString());

		final Frame errorDisplayFrame = new Frame("Oops");
		errorDisplayFrame.setLayout(new BorderLayout());
		errorDisplayFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent evt) { errorDisplayFrame.dispose(); }
		});

		errorDisplayFrame.add(new Label("Oops, something went wrong. uC may or may not still work."), BorderLayout.NORTH);
		errorDisplayFrame.add(disp, BorderLayout.CENTER);

		errorDisplayFrame.pack();
		errorDisplayFrame.setVisible(true);
	}
}
