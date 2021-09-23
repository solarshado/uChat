import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.charset.*;

import java.awt.*;
import java.awt.event.*;

import java.util.regex.*;

public class App extends Frame implements ActionListener, Runnable {

	private static final Charset CHARSET = StandardCharsets.ISO_8859_1;
	private static final int PORT = 12121;
	private final InetAddress addrs;

	private final DatagramSocket sock;
	private final Thread netListenerThread = new Thread(this);

	private final TextField name = new TextField();
	private final TextArea disp = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
	private final TextField input = new TextField();
	private final Button send = new Button("Send");
	private final Button connect = new Button("Join");

	private final Map<InetAddress, String> lookupTable = new HashMap<>();

	public App() throws Exception {
		super("uChat");

		this.addrs = InetAddress.getByName("255.255.255.255");
		this.sock = new DatagramSocket(PORT);
		this.sock.setSoTimeout(500); // 500 milliseconds

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

		this.send.addActionListener(this);
		this.connect.addActionListener(this);
		this.input.addActionListener(this);
		this.name.addActionListener(this);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent evt) {
				App.this.windowClosing();
			}
		});

		this.netListenerThread.start();
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

	private static String stringFromBytes(final byte[] bytes, final int length) {
		return new String(bytes, 0, length, CHARSET);
	}

	private static byte[] bytesFromString(final String str) {
		return str.getBytes(CHARSET);
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
				data = bytesFromString("SAY:".concat(this.input.getText()));
			}
			else if(evt.getSource() == this.connect || evt.getSource() == this.name) {
				if(this.connect.getLabel() == "Join") {
					this.name.setEditable(false);
					data = bytesFromString("JOIN".concat(this.name.getText()));
					this.connect.setLabel("Leave");
					this.input.requestFocus();
				}
				else if (this.connect.getLabel() == "Leave") {
					data = bytesFromString("LEAV".concat(this.name.getText()));
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

	private volatile boolean die = false;
	public void run() {
		while(!die) {
			final byte[] data = new byte[1024];
			final DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
			try {
				this.sock.receive(incomingPacket);

				final InetAddress srcAddr = incomingPacket.getAddress();
				final String inStr = stringFromBytes(data, incomingPacket.getLength());
				final String msgCode = inStr.substring(0,4);
				final String msgBody = inStr.substring(4);

				if("SAY:".equals(msgCode)) {
					final String who = this.lookupTable.get(srcAddr);
					this.disp.append(who+": "+msgBody+"\n");
					this.toFront();
					Toolkit.getDefaultToolkit().beep();
				}
				else if("JOIN".equals(msgCode)) {
					this.lookupTable.put(srcAddr, msgBody);
					this.disp.append(msgBody+'('+srcAddr.getHostAddress()+')'+" has joined\n");

					final byte[] send = bytesFromString("HERE".concat(this.name.getText()));
					this.sock.send(new DatagramPacket(send, send.length, srcAddr, PORT));
				}
				else if("HERE".equals(msgCode)) {
					this.lookupTable.put(srcAddr, msgBody);

					if(!this.name.getText().equals(msgBody)) { //needed to suppress "you are here"
						this.disp.append(msgBody+'('+srcAddr.getHostAddress()+')'+" is here\n");
					}
				}
				else if("LEAV".equals(msgCode)) {
					this.lookupTable.remove(srcAddr);
					this.disp.append(msgBody+'('+srcAddr.getHostAddress()+')'+" has left\n");
				}
				else { /* unknown, silently ignore */ }
			}
			catch(final SocketTimeoutException e) {
				// ignore it, it's to be expected
			}
			catch(final Exception e) {
				App.handle(e);
			}
		}
	}

	private void windowClosing() {
		this.setVisible(false);
		this.dispose();

		if(this.connect.getLabel() == "Join") { //only send LEAV if we're JOINed
			try {
				final byte[] data = bytesFromString("LEAV".concat(name.getText()));
				this.sock.send(new DatagramPacket(data, data.length, addrs, PORT));
			}
			catch(final Exception e) {
				App.handle(e);
			}
		}

		this.die = true;
		while(this.netListenerThread.isAlive()) {
			try {
				this.netListenerThread.join();
			}
			catch(final InterruptedException e) { /* ignore and try again */ }
		}

		try {
			this.sock.close();
		}
		catch(final Exception e) {
			App.handle(e);
		}
	}

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
