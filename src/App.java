package view;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.*;

import com.ireasoning.protocol.ErrorMsg;
import com.ireasoning.protocol.Msg;
import com.ireasoning.protocol.snmp.SnmpPdu;
import com.ireasoning.protocol.snmp.SnmpPoller;
import com.ireasoning.protocol.snmp.SnmpSession;
import com.ireasoning.protocol.snmp.SnmpVarBind;


public class App {
	
	protected String host1 = "192.168.10.1";
	protected String host2 = "192.168.20.1";
	protected String host3 = "192.168.30.1";
	protected int port = 161;
	protected String community = "si2019";
	protected int version = 1;
	SnmpPoller poller;
	protected String[] oids = new String[] {
			".1.3.6.1.2.1.11.1.0",
			".1.3.6.1.2.1.11.2.0",
			".1.3.6.1.2.1.11.4.0",
			".1.3.6.1.2.1.11.15.0",
			".1.3.6.1.2.1.11.17.0",
			".1.3.6.1.2.1.11.29.0"};
	
	static int interval = 10;
	static int timer = 0;
	
	private JPanel panel = new JPanel(new GridLayout(4, 9, 0, 0));
	private JButton[] refresh = new JButton[3];
	private JButton[] history = new JButton[3];
	JFrame frame = new JFrame("SNMP");
	private ArrayList<ArrayList<ArrayList<String>>> dt = new ArrayList<ArrayList<ArrayList<String>>>();
	
	private String[] columns = {"Ruter","Dolazni SNMP paketi", "Odlazni SNMP paketi", "Invalid community", "Get zahtevi", "Set zahtevi", "Trap", "Refresh", "History"};
	
	private void addHeaders() {
		for(int i = 0; i<columns.length; i++) {
			panel.add(new JLabel(columns[i]));
		}
	}
	
	private void initData() {
		for(int i = 0; i<3; i++) {
			dt.add(new ArrayList<ArrayList<String>>());
			dt.get(i).add(new ArrayList<String>());
			dt.get(i).get(0).add("R" + (i + 1));
			dt.get(i).get(0).add("");
			dt.get(i).get(0).add("");
			dt.get(i).get(0).add("");
			dt.get(i).get(0).add("");
			dt.get(i).get(0).add("");
			dt.get(i).get(0).add("");
			dt.get(i).get(0).add("");
		}
	}
	
	private void addData() {
		for(int i = 0; i<dt.size(); i++) {
			int lastIndex = dt.get(i).size()-1;
			for(int j = 0; j<dt.get(i).get(lastIndex).size() - 1; j++) { //-1 bez time parametra
				JLabel label = new JLabel(dt.get(i).get(lastIndex).get(j));
				label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				panel.add(label);
			}
			refresh[i] = new JButton("Refresh");
			history[i] = new JButton("History");
			panel.add(refresh[i]);
			panel.add(history[i]);
		}
	}
	
	private void configureFrame() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setSize(1300, 150);
	    frame.setResizable(false);
	    frame.getContentPane().add(panel);
	    frame.setVisible(true);
	}
	
	private void setButtonListeners() {
		
		for(int i = 0; i < refresh.length; i++) {
			refresh[i].addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					for(int i = 0; i < refresh.length; i++) {
						if(e.getSource() == refresh[i]) {
							refreshData();
							break;
						}
					}
				}
			});
			
			history[i].addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					for(int i = 0; i < history.length; i++) {
						if(e.getSource() == history[i]) {
							openHistory(i);
							break;
						}
					}
				}
			});
		}
	}
	
	private void openHistory(int index) {
		new HistoryDialog(frame, index);
	}
	
	private void refreshData() {
		frame.getContentPane().remove(panel);
		panel = new JPanel(new GridLayout(4, 9, 0, 0));
		addHeaders();
		addData();
		setButtonListeners();
		frame.getContentPane().add(panel);
		frame.setVisible(true);
	}
	
	private void pokreni(String host, int index) throws IOException {
		
		SnmpSession session = new SnmpSession(host, port, community, community, version);
		poller = new SnmpPoller(session);
		
		poller.addListener((sender, msg)-> {
	        if(msg.getType() != Msg.ERROR_TYPE)
	        {
	            SnmpPdu pdu = (SnmpPdu) msg;
	            SnmpVarBind[] varbinds = pdu.getVarBinds();
	            
	            ArrayList<String> arr = new ArrayList<String>();
            	dt.get(index).add(arr);
            	
            	arr.add("R" + (index+1));
	            for (int i = 0; i < varbinds.length ; i++)    
	            {	  
	            	arr.add(varbinds[i].getValue().toString());
	            }
	            arr.add(Calendar.getInstance().getTime().toString());
	            timer += 10;
	    		if(timer >= 180) {
	    			refreshData();
	    			timer = 0;
	    		}
	        }
	        else
	        {
	            ErrorMsg error = (ErrorMsg)msg;
	            Exception e = error.getException();
	            System.out.println( e );
	        }
		});
		poller.snmpGetPoll(oids, interval);
	}
	
	public App() throws IOException {
		addHeaders();
		initData();
		addData();
		setButtonListeners();
		configureFrame();
		pokreni(host1, 0);
		pokreni(host2, 1);
		pokreni(host3, 2);
	}
	
	public static void main(String[] args) throws IOException{
		App app = new App();
	}
	
	private class HistoryDialog extends JDialog{
		
		private JButton close = new JButton("Close");
		
		JRadioButton jRadioButton1;
		JRadioButton jRadioButton2;
		JRadioButton jRadioButton3;
		JButton jButton;
		ButtonGroup G1;
		JLabel L1;
		JPanel dp = null;
		
		private void loadWindow(JFrame owner, int index) {
			setTitle("R" + (index + 1) + " history");
			setBounds(owner.getX() + owner.getWidth()/2, owner.getY() + owner.getHeight()/2 ,1300, 220);
			
			setModalityType(ModalityType.APPLICATION_MODAL);
			
			JPanel buttons = new JPanel();
			buttons.add(close);
			close.addActionListener((ae)->{
				HistoryDialog.this.dispose();
			});
			
			add(buttons, BorderLayout.SOUTH);
			
			JPanel rb = new JPanel();
			
			jRadioButton1 = new JRadioButton();
	        jRadioButton2 = new JRadioButton();
	        jRadioButton3 = new JRadioButton();
	        jButton = new JButton("Show");
	        
	        G1 = new ButtonGroup();
	        L1 = new JLabel("Last:"); //bilo bi smislenije da stoje sati, ali radi lakse provere su stavljeni minuti
	        jRadioButton1.setText("1 minutes");
	        jRadioButton2.setText("2 minutes");
	        jRadioButton3.setText("5 minutes");
	        
	        jRadioButton1.setBounds(120, 30, 120, 50);
	        jRadioButton2.setBounds(250, 30, 80, 50);
	        jRadioButton2.setBounds(340, 30, 80, 50);
	        
	        jButton.setBounds(125, 90, 80, 30);
	        L1.setBounds(20, 200, 150, 50);

	        rb.add(L1);
	        rb.add(jRadioButton1);
	        rb.add(jRadioButton2);
	        rb.add(jRadioButton3);
	        rb.add(jButton);
	        
	        jButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(jRadioButton1.isSelected()){
						add(buildContentPane(index, 1));
						setVisible(true);
			        }
			        else if(jRadioButton2.isSelected()){
			        	add(buildContentPane(index, 2));
			        	setVisible(true);
			        }
			        else if(jRadioButton3.isSelected()) {
			        	add(buildContentPane(index, 5));
			        	setVisible(true);
			        }
				}
	        });
	  
	        G1.add(jRadioButton1);
	        G1.add(jRadioButton2);
	        G1.add(jRadioButton3);
	        
	        add(rb, BorderLayout.NORTH);
			
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dispose();
				}
			});
			
			setVisible(true);
		}
		
		private JPanel buildContentPane(int index, int minutes) {
			if(dp != null) {
				remove(dp);
			}
	        JPanel panel = new JPanel(new BorderLayout());
	         
	        int seconds = minutes*60;
	        int rows = seconds/10 + 1;
	        int n = 0;
	        
	        JPanel subPanel = new JPanel();
	        if(rows > dt.get(index).size()) n = dt.get(index).size();
	        else n = rows;
	        
	        subPanel.setLayout(new GridLayout(n, 1));
	        
	        subPanel.add(historyHeader());
	        
	        for (int i = dt.get(index).size() - 1; i >= dt.get(index).size() - n + 1; i--) {
	        	
	        	JPanel currPanel = new JPanel();
    	        currPanel.setLayout(new GridLayout(1,8));
    	        
	        	for(int j = 0; j<dt.get(index).get(i).size(); j++) {
	        		JLabel label = new JLabel(dt.get(index).get(i).get(j));
	        		label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	    	        currPanel.add(label);
	        	}
	        	subPanel.add(currPanel);
	        }
	        JScrollPane scroller = new JScrollPane(subPanel);
	        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	        panel.add(scroller, BorderLayout.CENTER);
	        setVisible(true);
	        dp = panel;
	        return panel;
	    }
	    private JPanel historyHeader() {
	    	JPanel dataPanel = new JPanel();
			dataPanel.setLayout(new GridLayout(1, 8, 0, 0));
			for(int i = 0; i<columns.length - 2; i++) {
				dataPanel.add(new JLabel(columns[i]));
			}
			dataPanel.add(new JLabel("Time"));
			return dataPanel;
	    }
		
		public HistoryDialog(JFrame owner, int index) {
			super(owner);
			loadWindow(owner, index);
		}
	}
}