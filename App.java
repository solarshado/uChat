import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

public class App extends Frame implements WindowListener, ActionListener, Runnable {

InetAddress addrs;
int port = 12121;
MulticastSocket sock;

TextField name;
TextArea disp;
TextField input;
Button send;

public App() throws Exception {
super("uChat");

addrs = InetAddress.getByName("230.12.12.12");
sock = new MulticastSocket(port);
sock.joinGroup(addrs);
sock.setTimeToLive(255);

disp = new TextArea("",0,0,TextArea.SCROLLBARS_VERTICAL_ONLY);
input = new TextField();
name = new TextField();
send = new Button("Send");

disp.setEditable(false);

Panel pnl1 = new Panel(new BorderLayout());
pnl1.add(new Label("Name:"), BorderLayout.WEST);
pnl1.add(name, BorderLayout.CENTER);

Panel pnl2 = new Panel(new BorderLayout());
pnl2.add(input,BorderLayout.CENTER);
pnl2.add(send,BorderLayout.EAST);

setLayout(new BorderLayout());
add(pnl1, BorderLayout.NORTH);
add(disp, BorderLayout.CENTER);
add(pnl2, BorderLayout.SOUTH);

Thread t = new Thread(this);
t.start();

send.addActionListener(this);
input.addActionListener(this);
addWindowListener(this);
setVisible(true);
}

public void setVisible(boolean b) {
super.setVisible(b);
if(b) {
pack();
Dimension scrn = Toolkit.getDefaultToolkit().getScreenSize();
setLocation((scrn.width-this.getSize().width)/1,(scrn.height-this.getSize().height)/1);
}
}

public static void main(String arg[]) {
try {
new App();
}
catch(Exception e) { handle(e); }
}

public void actionPerformed(ActionEvent evt) {
if(name.getText().trim().equals("")) {return;}
if(input.getText().trim().equals("")) {return;}
byte[] data = name.getText().concat(": ").concat(input.getText()).getBytes();
DatagramPacket out = new DatagramPacket(data, data.length,addrs,port);
try {
sock.send(out);
} catch(Exception e) { handle(e); }
input.setText("");
}

public void windowClosing(WindowEvent evt) {
 try {
 sock.leaveGroup(addrs);
 sock.close();
 } catch(Exception e) { handle(e); }
 System.exit(0);
}
public void windowOpened(WindowEvent evt) { }
public void windowClosed(WindowEvent evt) { }
public void windowIconified(WindowEvent evt) { }
public void windowDeiconified(WindowEvent evt) { }
public void windowActivated(WindowEvent evt) { }
public void windowDeactivated(WindowEvent evt) { }


public static void handle(Exception e) {
StringWriter sb = new StringWriter();
PrintWriter out = new PrintWriter(sb);
e.printStackTrace(out);

TextArea disp = new TextArea(sb.getBuffer().toString());

Frame f0 = new Frame("Oops");
f0.setLayout(new BorderLayout());
f0.addWindowListener(new WindowAdapter() {
public void windowClosing(WindowEvent evt) { ((Window)evt.getSource()).dispose(); }
});

f0.add(new Label("Oops, something went wrong. uC may or may not still work."), BorderLayout.NORTH);
f0.add(disp, BorderLayout.CENTER);

}


boolean die = false;
public void run() {
while(!die) {
byte[] data = new byte[1024];
DatagramPacket in = new DatagramPacket(data, data.length);
try {
sock.receive(in);
} catch(Exception e) { handle(e); }
disp.append(new String(data, 0, in.getLength())+"\n");
toFront();
Toolkit.getDefaultToolkit().beep();
}


}

}