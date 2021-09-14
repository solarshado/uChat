import java.util.*;
import java.net.*;
import java.io.*;

import java.awt.*;
import java.awt.event.*;

import java.util.regex.*;

public class App extends Frame implements WindowListener, ActionListener, Runnable {

 InetAddress addrs;
 int port = 12121;
 DatagramSocket sock;

 TextField name;
 TextArea disp;
 TextField input;
 Button send;
 Button connect;

 Map<InetAddress, String> lookupTable;

public App() throws Exception {
 super("uChat");

 addrs = InetAddress.getByName("255.255.255.255");
 sock = new DatagramSocket(port);

 lookupTable = new HashMap<InetAddress, String>();

 disp = new TextArea("",0,0,TextArea.SCROLLBARS_VERTICAL_ONLY);
 input = new TextField();
 name = new TextField();
 send = new Button("Send");
 connect = new Button("Join");

 disp.setEditable(false);

 Panel pnl1 = new Panel(new BorderLayout());
 pnl1.add(new Label("Name:"), BorderLayout.WEST);
 pnl1.add(name, BorderLayout.CENTER);
 pnl1.add(connect, BorderLayout.EAST);

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
 connect.addActionListener(this);
 input.addActionListener(this);
 name.addActionListener(this);
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
 try { new App(); }
 catch(Exception e) { handle(e); }
}

public void actionPerformed(ActionEvent evt) {
 if(name.getText().trim().equals("")) {return;}
 byte[] data= null;
 try {
  if(evt.getSource() == input || evt.getSource() == send) {
   if(input.getText().trim().equals("")) {return;}
   if(!connect.getLabel().equals("Leave")) {return;}
   { // a little pre-processing to prevent old-skool hax (exploiting text wrapping)
     // replaces every instance of 3+ space chars with 2 space chars
    input.setText(
	Pattern.compile(" {3,}")
	.matcher(input.getText().trim())
	.replaceAll("  ")			); //end of setText()
   }
   data = "SAY:".concat(input.getText()).getBytes("ISO-8859-1");
  }
  else if(evt.getSource() == connect || evt.getSource() == name) {
   if(connect.getLabel() == "Join") {
    name.setEditable(false);
    data = "JOIN".concat(name.getText()).getBytes("ISO-8859-1");
    connect.setLabel("Leave");
    input.requestFocus();
   }
   else if (connect.getLabel() == "Leave") {
    data = "LEAV".concat(name.getText()).getBytes("ISO-8859-1");
    connect.setLabel("Join");
    name.setEditable(true);    
   }
   else { return; /* shouldn't happen*/ }
  }
  else { return; /* shouldn't happen*/ }

  DatagramPacket out = new DatagramPacket(data, data.length,addrs,port);
  sock.send(out);
 } catch(Exception e) { handle(e); }
 input.setText("");
}

boolean die = false;
public void run() {
 while(!die) {
  byte[] data = new byte[1024];
  DatagramPacket in = new DatagramPacket(data, data.length);
  try {
   sock.receive(in);

   String tmpIN = new String(data, "ISO-8859-1");

   if(tmpIN.startsWith("SAY:")) {
    String who = lookupTable.get(in.getAddress());
    disp.append(who+": "+new String(data, 4, in.getLength()-4, "ISO-8859-1")+"\n");
    toFront();
    Toolkit.getDefaultToolkit().beep();
   }
   else if(tmpIN.startsWith("JOIN")) {
    InetAddress tmp = in.getAddress();
    String tmpS = new String(data, 4,in.getLength()-4, "ISO-8859-1");
    lookupTable.put(tmp, tmpS);
    disp.append(tmpS+'('+tmp.getHostAddress()+')'+" has joined\n");

    byte[] send = "HERE".concat(name.getText()).getBytes("ISO-8859-1");
    DatagramPacket outbound = new DatagramPacket(send, send.length, tmp, port);
    sock.send(outbound);
   }
   else if(tmpIN.startsWith("HERE")) {
    InetAddress tmp = in.getAddress();
    String tmpS = new String(data, 4,in.getLength()-4, "ISO-8859-1");
    lookupTable.put(tmp, tmpS);

    if(!tmpS.equals(name.getText())) { //needed to suppress "you are here"
     disp.append(tmpS+'('+tmp.getHostAddress()+')'+" is here\n");
    }
   }
   else if(tmpIN.startsWith("LEAV")) {
    InetAddress tmp = in.getAddress();
    String tmpS = new String(data, 4,in.getLength()-4, "ISO-8859-1");
    lookupTable.remove(tmp);
    disp.append(tmpS+'('+tmp.getHostAddress()+')'+" has left\n");
   }
   else { /* unknown, silently ignore */ }
  } catch(Exception e) { handle(e); }
 }
 try {
  sock.close();
 } catch(Exception e) { handle(e); }
}

public void windowClosing(WindowEvent evt) {
 setVisible(false);

 if(connect.getLabel() == "Join") { //only send LEAV if we're JOINed
  try {
   byte[] data = "LEAV".concat(name.getText()).getBytes("ISO-8859-1");
   DatagramPacket out = new DatagramPacket(data, data.length,addrs,port);
   sock.send(out);
  } catch(Exception e) { handle(e); }
 }

 die = true;
 dispose();
// System.exit(0);
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
 e.printStackTrace(System.out);

 TextArea disp = new TextArea(sb.getBuffer().toString());

 Frame f0 = new Frame("Oops");
 f0.setLayout(new BorderLayout());
 f0.addWindowListener(new WindowAdapter() {
  public void windowClosing(WindowEvent evt) { ((Window)evt.getSource()).dispose(); }
 });

 f0.add(new Label("Oops, something went wrong. uC may or may not still work."), BorderLayout.NORTH);
 f0.add(disp, BorderLayout.CENTER);

 f0.pack();
 f0.setVisible(true);
}

}