package nuclideExposure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import danfulea.utils.ScanDiskLFGui;
import danfulea.db.DatabaseAgent;
//import jdf.db.DBConnection;
import danfulea.math.Convertor;
import danfulea.math.Interpolation;
import danfulea.phys.PhysUtilities;
import danfulea.utils.FrameUtilities;
import jxl.Cell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * Class designed to handle exposure to radio-nuclides of individuals, display radiation information for radioactive series and to perform basic 
 * dosimetry and shielding calculations. Individuals can be exposed to radiations coming from 
 * external sources (air and water submersion, ground surface exposure) and internal sources (ingestion 
 * and inhalation). For each scenario the program calculates equivalent dose in thyroid and 
 * the effective dose in human body for all parent-daughter chain. The chain activities 
 * are computed in three cases: secular equilibrium is assumed to be reached; no daughter at 
 * initial time (time ZERO) and all activities are computed using Bateman decay law; the 
 * equilibrium reached at a specific time (default 10 x parent half-life) and all activities 
 * are computed using Bateman decay law. 
 *   
 * @author Dan Fulea, 20 Jun. 2011
 *
 */

@SuppressWarnings("serial")
public class NuclideExposure extends JFrame implements ActionListener,
		ItemListener, Runnable {

	private final Dimension PREFERRED_SIZE = new Dimension(990, 730);
	private final Dimension sizeCb = new Dimension(90, 21);
	private final Dimension smallsizeCb = new Dimension(50, 21);
	private final Dimension textAreaSize = new Dimension(800, 600);
	// protected Color bkgColor = new Color(112, 178, 136, 255);//green default
	// protected Color bkgColor = new Color(130, 0, 0, 255);//brown-red..VAMPIRE
	// protected Color bkgColor = new Color(245, 255, 250, 255);//white -cream
	public static Color bkgColor = new Color(230, 255, 210, 255);// Linux mint
																	// green
																	// alike
	public static Color foreColor = Color.black;// Color.white;
	public static Color textAreaBkgColor = Color.white;// Color.black;
	public static Color textAreaForeColor = Color.black;// Color.yellow;
	public static boolean showLAF = true;

	private static final String BASE_RESOURCE_CLASS = "nuclideExposure.resources.NuclideExposureResources";
	private ResourceBundle resources = ResourceBundle
			.getBundle(BASE_RESOURCE_CLASS);

	private Connection conn = null;
	private ArrayList<Statement> statements = null;
	private ArrayList<ResultSet> resultsets = null;
	private String icrpDB = "";
	private String internalDB = "";
	private String externalDB = "";
	private String icrpTable = "";
	private String icrpRadTable = "";
	private String inhalationAdultTable = "";
	private String inhalation1Table = "";
	private String inhalation10Table = "";
	private String ingestionAdultTable = "";
	private String ingestion1Table = "";
	private String ingestion10Table = "";
	private String waterTable = "";
	private String airTable = "";
	private String groundSurfaceTable = "";
	private String infiniteSoilTable = "";

	private String externalTable = "";
	private String publicInhalationTable = "";
	private String publicIngestionTable = "";

	private Vector<String> nucV = new Vector<String>();

	private JLabel statusL = new JLabel();
	private volatile Thread computationTh = null;// computation thread!
	private volatile Thread statusTh = null;// status display thread!
	private int delay = 100;
	private int frameNumber = -1;
	private String statusRunS = "";

	private static final String EXIT_COMMAND = "EXIT";
	private static final String ABOUT_COMMAND = "ABOUT";
	private static final String HOWTO_COMMAND = "HOWTO";
	private static final String LOOKANDFEEL_COMMAND = "LOOKANDFEEL";
	private static final String GRAPH_COMMAND = "GRAPH";
	private static final String CALCULATE_COMMAND = "CALCULATE";
	private static final String DISPLAY_COMMAND = "DISPLAY";
	private static final String EVALUATE_COMMAND = "EVALUATE";
	private static final String KILL_COMMAND = "KILL";
	private boolean STOPCOMPUTATION = false;
	private boolean stopAppend = false;
	private String command = null;
	private Window parent = null;
	private boolean standalone = true;

	private WritableWorkbook workbookCA;
	private WritableSheet sheetCA;

	private JTextField xparentHalfLifeTf = new JTextField(5);
	private JTabbedPane tabs;
	private JTextArea textArea;
	private JTextField elapsedTimeTf = new JTextField(5);
	@SuppressWarnings("rawtypes")
	private JComboBox sampleTimeUnitsCb;
	private JRadioButton a_secRb, a_normRb, a_eqRb;
	@SuppressWarnings("rawtypes")
	private JComboBox dbCb, nucCb, shieldCb;
	private JRadioButton e_airRb, e_waterRb, e_gsRb, e_isRb, i_ingRb, i_inhRb;
	private JRadioButton thresh_allRb, thresh_1Rb, thresh_2Rb, thresh_5Rb,
			thresh_10Rb, thresh_20Rb;
	private JRadioButton rad_allRb, rad_gammaRb, rad_xrayRb, rad_annihRb,
			rad_betapRb, rad_betanRb, rad_iceRb, rad_augerRb, rad_alphaRb,
			rad_photonsRb, rad_electronsRb;
	private JRadioButton y1Rb, y10Rb, adultRb;
	private JCheckBox excelCh, parentCh, hvlCh;
	private JTextField activityTf = new JTextField(5);
	private String[] shieldS;
	private JTextField distanceTf = new JTextField(5);
	private JTextField thicknessTf = new JTextField(5);
	private JTextField mainEnergyTf = new JTextField(5);
	private JTextField measuredDoseRateTf = new JTextField(5);
	private JTextField desiredDoseRateTf = new JTextField(5);
	// -------
	private double shieldThickness = 0.3;
	private double distance = 100.0;
	private double mainEnergy = 0.125;
	private double measuredDoseRate = 2.2;
	private double desiredDoseRate = 0.2;

	private Vector<Double> ajV = new Vector<Double>();
	private Vector<Double> biV = new Vector<Double>();
	private Vector<Double> eiV = new Vector<Double>();
	private Vector<Double> yiV = new Vector<Double>();
	private Vector<Double> rmfpShieldV = new Vector<Double>();
	private Vector<Double> uiTissV = new Vector<Double>();
	private double doseratetotal = 0.0;
	private double doseratetotalshield = 0.0;
	private double hvl = 0.0;
	private double tvl = 0.0;
	private double dosehvl = 0.0;
	private double dosetvl = 0.0;
	public static int MAXITER = 10000;

	// private boolean evalB = false;
	private boolean hvlBoolean = false;
	// --------

	private boolean excelB = false;
	// ========
	private double yield_current = 1.0;
	private String parentNuclide = "";
	private Vector<Double> y1_old = new Vector<Double>();
	private Vector<Double> y2_next_chain = new Vector<Double>();
	private boolean colect_first_time = false;
	private boolean parent_dual = false;
	private Vector<Double> y1_fix = new Vector<Double>();
	private Vector<Integer> index = new Vector<Integer>();
	private Vector<String> nuclideS = new Vector<String>();
	private Vector<Double> atomicMassD = new Vector<Double>();
	private Vector<Double> yieldD = new Vector<Double>();
	private Vector<Integer> radLocI = new Vector<Integer>();
	private Vector<Integer> radLocN = new Vector<Integer>();
	private Vector<Double> halfLifeD = new Vector<Double>();
	private Vector<String> halfLifeUnitsS = new Vector<String>();
	private Vector<String> decayModeMainS = new Vector<String>();
	private Vector<Double> gammaConstantD = new Vector<Double>();

	private int ndaughter = 0;
	private Vector<Integer> nsave = new Vector<Integer>();
	private Vector<Double> yield2_readD = new Vector<Double>();
	private int ndaughterChainMax = 0;
	private String[][] nuc_chains;
	private String[][] hlu_chains;
	private double[][] hl_chains;
	private double[][] br_chains;
	private double[][] am_chains;
	private static final int nmax_chains = 100;
	private static final int nmax_daughters = 840;
	private int nchain = 0;
	// activity of each chain at time set
	private double[][] a_chains;
	// time integrated activity of each chain at time set
	private double[][] at_chains;
	// activity of each chain at time set at 10 x parent half life
	private double[][] a_chains_equilibrum;

	private int IRCODE = 1;
	private static final int IGAMMA = 1;
	private static final int IXRAY = 2;
	private static final int IANNIH = 3;
	private static final int IBETAP = 4;
	private static final int IBETAN = 5;
	private static final int IICE = 6;
	private static final int IAUGER = 7;
	private static final int IALPHA = 8;

	// rows=data;columns=nuclide index
	private String[][] rcode;
	private String[][] rtype;
	private double[][] ryield;
	private double[][] renergy;
	private String[] nucInChain;
	private double DTHRESH = 0.00;// all radiation
	private static final double DTHRESH0 = 0.00;// all radiation
	private static final double DTHRESH1 = 0.01;// cut yields below 1%
	private static final double DTHRESH2 = 0.02;// cut yields below 2%
	private static final double DTHRESH5 = 0.05;// cut yields below 5%
	private static final double DTHRESH10 = 0.10;// cut yields below 10%
	private static final double DTHRESH20 = 0.20;// cut yields below 20%

	private static final double activityUnit = 1.0;// Bq
	private double elapsedTime = 180.0;// 30.0;//days
	private int timeSteps = 1;// used in BATEMAN DECAY..no intermediate steps!
	// overall nuclides..from nuclideS=new Vector();
	private String[] nuclides_all;
	// overall activities from chains at time set (at t=0, A parent =1.0)
	private double[] activity_chain;
	// overall yields..from yieldD=new Vector();
	private double[] yield_all;
	// formal (not happen always) forced secular equilibrum:
	// activityUnit*yield_all[i];
	private double[] activity_secular;
	// overall normalized activities from chains at time set (at t=t, A parent
	// =1.0)
	private double[] activity_chain_scaled;
	// modified...activity_chain_scaled holds now the time integrated activities
	// during elapsed time. Elapsed time==Sample time!
	// overall normalized activities from chains at 10 x parent half life (at
	// t=t, A parent =1.0)..equilibrum auto-seek!
	private double[] activity_chain_equilibrum;
	// private double[] parentDaughterDecayCorr;
	private int IA_use = 3;
	private static final int IA_sec = 0;
	// if this is used, user wants secular equilibrum in sample!
	// Gamma spectrometry uses secular equilibrum by default when
	// computing activity (of parent) from any peak (of daughter).
	private static final int IA_elapsed = 1;
	// User wants to use equilibrum reached after a certain elapsed time!
	// which is the sample time. The secular equilibrum hypotesis
	// is rejected and use decay parent-daughter correction. The
	// sample time must be known!!!!!
	// Only the parent activity at time 0 with no daughters is considered.
	// Decay follows normal laws (bateman) during elapsed time.
	private static final int IA_norm = 2;
	// same as above but:
	// if this is used, user wants time integrated activities
	// for computing decay parent-daughter correction.
	// Only the parent activity at time 0 with no daughters is considered.
	// Decay follows normal laws (bateman) during elapsed time.
	private static final int IA_eq = 3;
	// similar to IA_elapsed but:
	// if this is used, user wants real equilibrum (computed at
	// 10 X parent half life) !
	private static double timesHalfLifeForEq = 10.0;
	private static final double lifetime_cancer_mortality = 582;
	// cases per 100000 pop exposed at 0.1 Sv(Gy) low LET radiation
	// (photons, electrons)=>BEIR VII

	//BEIR VII table 12-8 => gives total cancers of 582. overall all ages and both sexes.
	//Beir VII no data available for thyroid neither in table 12-8 nor 12 D-2
	//taken from EPA (table 12-8)
	//private static final double lifetime_cancer_mortality_thyroid = 3;
	//the risk is so low, no point in keeping track of it
	//total risk should be enough!!!!!
	
	private double activity = 0.0;// Bq/m3 for external; Bq for internal
	private double exposureTime = 0.0;// only for external;modified...this is
										// equal with elapsedTime!!
	// private double exposureTimeTemp=exposureTime;
	private double[][] dose_external;// all nuclides!; adrenals.liver...etc only
										// from photons and electrons=low LET;
										// alpha stopped at skin..not used!
	private double[][] dose_internal;// all nuclides!; adrenals.liver...etc only
										// from photons and electrons=low LET;
										// alpha stopped at ingestion/inhalation
										// location.
	private String[] organs_external;
	private String[] organs_internal;
	private double effectiveDoseTotal = 0;
	private double thyroidDoseTotal = 0;//since it is a very sensitive organ, makes sense 
	//to keep track of it...see FUKUSIMA impact study: dose in thyroid and effective dose
	//are the quantities of interest!
	//private double totalDose=0.0;
	/*
	 BEIR is like:
organ   cancer_cases per Gy
stomach	ks
colon   kc
.........
all     kall=ks+kc+.....

Naive approach of dall=ds+dc+... and risk = kall x dall is not good since
kall x dall != ks x ds + kc x dc + ......
Naive example:
 2 ->4
 3 ->5

(2+3)(4+5)=5 x 9 =45
2 x 4 + 3 x 5 = 8+15=23!!!!!!!!!!!!!

Total dose absorbed (or equivalent) in body is the total energy deposited divided
by phantom mass (multiplyied further by wr for equivalent dose) and NOT the sum 
over all organ doses which is hence a meaningless quantity. 
So basicly yes, either total dose absorbed in body, total equivalent dose in
body or effective dose must be multiplyied with kall!!!!!!!!!!!!!!!!!!!!!!

A more rigurously approach: take the sum ks x ds + kc x dc + ...... but be
carefull when hit "other". Dose in "other" should not be d_esophagus + 
d_gallBladder +...+dremainder but  somthing with same effect as energy/other_mass.
At least not a simple sum, but a weighted mean by mass (as we did when evaluate 
w..see detector construction in C++) for remainder organs! that is: 
(d_esophagus x mass_esophagus +..)/m_other Not so useful approach though, 
total absorbed dose or the effective dose works just fine!!!!!!!!!!!!!!!

//====================
Numerically, toral absorbed dose differs from effective dose always (usually is smaller)!
Proof:
Let's consider energy E = 10. Mass M=2 => Dabs = E/M = 5
Say
E = 3 goes to M=1 with w = 0.2
E = 7 goes to M=1 with w = 0.8
Total: 3+7=10;1+1=2 and 0.2+0.8=1 so all is good!
Deff = 3/1 x 0.2 + 7/1 x 0.8 = 0.6 + 5.6 = 6.2 != Dabs = 5 
QED
///======================================== 

Now, between total absorbed, total equivalent and efffective dose: 
since 1 Gy in left middle toe finger is not the same as 1 Gy in thyroid, 
makes sense the effective dose is much more suitable to assess the overall risk 
since it takes into account the fact different organs are more sensitive
to radiation then others!!!
//=====================
Conclusion: take all cancers from BEIR table and use effective dose to compute 
the overall risk!
	 */
	//========================
	private double risk = 0;

	protected double plotTime = 0.0;// default!
	protected int plotSteps = 50;// default!
	protected int plotNucNumber = 0;// default!
	protected String[] plotNucSymbol = new String[0];
	protected double[][] plotActiv = new double[0][0];
	
	
	//private Connection nuclidedbcon = null;

	/**
	 * Constructor
	 */
	public NuclideExposure() {
		//DBConnection.startDerby();
		this.setTitle(resources.getString("Application.NAME"));

		icrpDB = resources.getString("library.master.jaeri.db");
		icrpTable = resources.getString("library.master.jaeri.db.indexTable");
		icrpRadTable = resources.getString("library.master.jaeri.db.radTable");
		// ----------------
		internalDB = resources.getString("internal.db");
		inhalationAdultTable = resources
				.getString("internal.db.public.inhalationAdultTable");
		inhalation1Table = resources
				.getString("internal.db.public.inhalation1Table");
		inhalation10Table = resources
				.getString("internal.db.public.inhalation10Table");
		ingestionAdultTable = resources
				.getString("internal.db.public.ingestionAdultTable");
		ingestion1Table = resources
				.getString("internal.db.public.ingestion1Table");
		ingestion10Table = resources
				.getString("internal.db.public.ingestion10Table");
		// ----------
		externalDB = resources.getString("external.db");
		waterTable = resources.getString("external.db.waterTable");
		airTable = resources.getString("external.db.airTable");
		groundSurfaceTable = resources
				.getString("external.db.groundSurfaceTable");
		infiniteSoilTable = resources
				.getString("external.db.infiniteSoilTable");
		//===========================
		DatabaseAgent.ID_CONNECTION = DatabaseAgent.DERBY_CONNECTION;	
		//=====================
		initDBComponents();
		nuclides_all = new String[0];
		// the key to force decision made by attemptExit() method on close!!
		// otherwise...regardless the above decision, the application exit!
		// notes: solved this minor glitch in latest sun java!!
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				attemptExit();
			}
		});

		JMenuBar menuBar = createMenuBar(resources);
		setJMenuBar(menuBar);

		createGUI();
		setDefaultLookAndFeelDecorated(true);
		FrameUtilities.createImageIcon(
				this.resources.getString("form.icon.url"), this);

		FrameUtilities.centerFrameOnScreen(this);

		setVisible(true);
	}

	/**
	 * Constructor.
	 * @param frame the calling program
	 */
	public NuclideExposure(Window frame) {
		this();
		this.parent = frame;
		standalone = false;
	}

	/**
	 * Set master nuclide library. These are ICRP38 or JAERI. From my experience, 
	 * I recommend JAERI.
	 */
	private void setMasterTables() {
		String dbName = (String) dbCb.getSelectedItem();

		if (dbName.equals(resources.getString("library.master.db"))) {
			icrpDB = resources.getString("library.master.db");
			icrpTable = resources.getString("library.master.db.indexTable");
			icrpRadTable = resources.getString("library.master.db.radTable");
		} else if (dbName
				.equals(resources.getString("library.master.jaeri.db"))) {
			icrpDB = resources.getString("library.master.jaeri.db");
			icrpTable = resources
					.getString("library.master.jaeri.db.indexTable");
			icrpRadTable = resources
					.getString("library.master.jaeri.db.radTable");
		}
	}

	/**
	 * Initiate database components
	 */
	private void initDBComponents() {
		/*
		 * Under Linux we have only one simple option: Use a java database
		 * management system not MS acces (via odbc) or mysql because errors or
		 * undesired complications may appear. Therefore, derby.jar from Apache
		 * is appropriate for this task. We no longer need auxiliary software
		 * installed in order to run this application. We will use a simple
		 * embedded database driver located in lib folder along with other
		 * auxiliary packages such as jfreechart or itext. And of course this
		 * application is portable on any platform Windows,Mac or Linux or
		 * whatever OS...the only need is a JVM installed on target computer
		 * where this application is deployed and executed.
		 */
		// In linux, MsAcces database is not recognized.
		// via JDBC:ODBC on LInux platform. We change DBMS (Database management
		// system) using Apache derby driver.

		nucV = new Vector<String>();

		// list of Statements, PreparedStatements
		conn = null;
		statements = new ArrayList<Statement>();

		// PreparedStatement psInsert = null;
		// PreparedStatement psUpdate = null;
		Statement s = null;
		ResultSet rs = null;
		try {
			String datas = resources.getString("data.load");// Data
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;
			String dbName = icrpDB;// the name of the database
			opens = opens + file_sep + dbName;

			// createDerbyDatabase(internalDB);
			// createDerbyDatabase(externalDB);

			/*
			 * MsAccess cannot export properly tables data! Therefore the
			 * copying method of MsAcces tables via MsAcces exported file (e.g.
			 * text file) must be avoided! Only way: We must copy directly from
			 * MsAcces which means we must run this particular operation under
			 * WINDOWS!!! Once derby database is properly set, the application
			 * can run EVERYWHERE!!!
			 */

			// copyMsAccesIngestionTable();
			// copyMsAccesInhalationTable();
			// copyMsAccesAirTable();
			// copyMsAccesWaterTable();
			// copyMsAccesGroundTable();
			// copyMsAccesSoilTable();
			//===========
			//copyMsAccesIngestionTable1();
			//copyMsAccesIngestionTable10();
			//copyMsAccesInhalationTable1();
			//copyMsAccesInhalationTable10();

			conn = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			// We want to control transactions manually. Autocommit is on by
			// default in JDBC.
			conn.setAutoCommit(false);
			/*
			 * Creating a statement object that we can use for running various
			 * SQL statements commands against the database.
			 */
			s = conn.createStatement();
			statements.add(s);

			rs = s.executeQuery("SELECT * FROM " + icrpTable);

			if (rs != null)
				while (rs.next()) {
					String ss = rs.getString(2);
					nucV.addElement(ss);
				}

			conn.commit();

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			// release all open resources to avoid unnecessary memory usage

			// ResultSet
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
			} catch (Exception sqle) {
				sqle.printStackTrace();
			}

			// Statements and PreparedStatements
			int i = 0;
			while (!statements.isEmpty()) {
				// PreparedStatement extend Statement
				Statement st = (Statement) statements.remove(i);
				try {
					if (st != null) {
						st.close();
						st = null;
					}
				} catch (Exception sqle) {
					sqle.printStackTrace();
				}
			}

			// Connection
			try {
				if (conn != null) {
					conn.close();
					conn = null;
				}
			} catch (Exception sqle) {
				sqle.printStackTrace();
			}
		}
	}

	/**
	 * Create GUI
	 */
	private void createGUI() {

		JToolBar statusBar = new JToolBar();
		statusBar.setFloatable(false);
		initStatusBar(statusBar);

		JPanel content = new JPanel(new BorderLayout());

		tabs = createTabs();
		content.add(tabs, BorderLayout.CENTER);
		content.add(statusBar, BorderLayout.PAGE_END);
		setContentPane(new JScrollPane(content));
		// setContentPane(content);
		content.setOpaque(true); // content panes must be opaque
		pack();
	}

	/**
	 * Create main panel
	 * @return the panel
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createMainPanel() {
		Character mnemonic = null;
		JButton button = null;
		JLabel label = null;
		String buttonName = "";
		String buttonToolTip = "";
		String buttonIconName = "";

		parentCh = new JCheckBox(resources.getString("parentCh"), true);
		excelCh = new JCheckBox(resources.getString("excelCh"), true);
		parentCh.setBackground(bkgColor);
		parentCh.setForeground(foreColor);
		excelCh.setBackground(bkgColor);
		excelCh.setForeground(foreColor);
		hvlCh = new JCheckBox(resources.getString("hvlCh"), false);
		hvlCh.setBackground(bkgColor);
		hvlCh.setForeground(foreColor);

		String[] dbS = (String[]) resources.getObject("library.master.dbCb");
		dbCb = new JComboBox(dbS);
		String defaultS = dbS[1];// JAERI
		dbCb.setSelectedItem((Object) defaultS);
		dbCb.setMaximumRowCount(5);
		dbCb.setPreferredSize(sizeCb);
		dbCb.addItemListener(this);

		nucCb = new JComboBox();
		nucCb.setMaximumRowCount(15);
		nucCb.setPreferredSize(sizeCb);
		for (int i = 0; i < nucV.size(); i++) {
			nucCb.addItem((String) nucV.elementAt(i));
		}

		String[] hluS = (String[]) resources.getObject("library.inuse.hluCb");
		sampleTimeUnitsCb = new JComboBox(hluS);
		String str = hluS[1];// days
		sampleTimeUnitsCb.setSelectedItem((Object) str);
		sampleTimeUnitsCb.setMaximumRowCount(5);
		sampleTimeUnitsCb.setPreferredSize(smallsizeCb);

		a_secRb = new JRadioButton(resources.getString("library.a_secRb"));
		a_secRb.setBackground(bkgColor);
		a_secRb.setForeground(foreColor);
		a_normRb = new JRadioButton(resources.getString("library.a_afterRb"));
		a_normRb.setBackground(bkgColor);
		a_normRb.setForeground(foreColor);
		a_eqRb = new JRadioButton(resources.getString("library.a_eqRb"));
		a_eqRb.setBackground(bkgColor);
		a_eqRb.setForeground(foreColor);
		a_secRb.setToolTipText(resources.getString("library.a_secRb.toolTip"));
		a_normRb.setToolTipText(resources
				.getString("library.a_afterRb.toolTip"));
		a_eqRb.setToolTipText(resources.getString("library.a_eqRb.toolTip"));
		ButtonGroup groupA = new ButtonGroup();
		groupA.add(a_secRb);
		groupA.add(a_normRb);
		groupA.add(a_eqRb);
		a_eqRb.setSelected(true);

		e_airRb = new JRadioButton(resources.getString("e_airRb"));
		e_airRb.setBackground(bkgColor);
		e_airRb.setForeground(foreColor);
		e_waterRb = new JRadioButton(resources.getString("e_waterRb"));
		e_waterRb.setBackground(bkgColor);
		e_waterRb.setForeground(foreColor);
		e_gsRb = new JRadioButton(resources.getString("e_gsRb"));
		e_gsRb.setBackground(bkgColor);
		e_gsRb.setForeground(foreColor);
		e_isRb = new JRadioButton(resources.getString("e_isRb"));
		e_isRb.setBackground(bkgColor);
		e_isRb.setForeground(foreColor);
		i_ingRb = new JRadioButton(resources.getString("i_ingRb"));
		i_ingRb.setBackground(bkgColor);
		i_ingRb.setForeground(foreColor);
		i_inhRb = new JRadioButton(resources.getString("i_inhRb"));
		i_inhRb.setBackground(bkgColor);
		i_inhRb.setForeground(foreColor);
		ButtonGroup groupB = new ButtonGroup();
		groupB.add(e_airRb);
		groupB.add(e_waterRb);
		groupB.add(e_gsRb);
		groupB.add(e_isRb);
		groupB.add(i_ingRb);
		groupB.add(i_inhRb);
		e_airRb.setSelected(true);
		
		//------------
		y1Rb = new JRadioButton(resources.getString("y1Rb"));
		y10Rb = new JRadioButton(resources.getString("y10Rb"));
		adultRb = new JRadioButton(resources.getString("adultRb"));
		
		ButtonGroup groupAge = new ButtonGroup();
		groupAge.add(y1Rb);
		groupAge.add(y10Rb);
		groupAge.add(adultRb);
		
		adultRb.setSelected(true);
		//--------------

		rad_allRb = new JRadioButton(resources.getString("rad_allRb"));
		rad_gammaRb = new JRadioButton(resources.getString("rad_gammaRb"));
		rad_xrayRb = new JRadioButton(resources.getString("rad_xrayRb"));
		rad_annihRb = new JRadioButton(resources.getString("rad_annihRb"));
		rad_betapRb = new JRadioButton(resources.getString("rad_betapRb"));
		rad_betanRb = new JRadioButton(resources.getString("rad_betanRb"));
		rad_iceRb = new JRadioButton(resources.getString("rad_iceRb"));
		rad_augerRb = new JRadioButton(resources.getString("rad_augerRb"));
		rad_alphaRb = new JRadioButton(resources.getString("rad_alphaRb"));
		rad_photonsRb = new JRadioButton(resources.getString("rad_photonsRb"));
		rad_electronsRb = new JRadioButton(
				resources.getString("rad_electronsRb"));
		rad_allRb.setBackground(bkgColor);
		rad_allRb.setForeground(foreColor);
		rad_gammaRb.setBackground(bkgColor);
		rad_gammaRb.setForeground(foreColor);
		rad_xrayRb.setBackground(bkgColor);
		rad_xrayRb.setForeground(foreColor);
		rad_annihRb.setBackground(bkgColor);
		rad_annihRb.setForeground(foreColor);
		rad_betapRb.setBackground(bkgColor);
		rad_betapRb.setForeground(foreColor);
		rad_betanRb.setBackground(bkgColor);
		rad_betanRb.setForeground(foreColor);
		rad_iceRb.setBackground(bkgColor);
		rad_iceRb.setForeground(foreColor);
		rad_augerRb.setBackground(bkgColor);
		rad_augerRb.setForeground(foreColor);
		rad_alphaRb.setBackground(bkgColor);
		rad_alphaRb.setForeground(foreColor);
		rad_photonsRb.setBackground(bkgColor);
		rad_photonsRb.setForeground(foreColor);
		rad_electronsRb.setBackground(bkgColor);
		rad_electronsRb.setForeground(foreColor);
		ButtonGroup groupD = new ButtonGroup();
		groupD.add(rad_allRb);
		groupD.add(rad_gammaRb);
		groupD.add(rad_xrayRb);
		groupD.add(rad_annihRb);
		groupD.add(rad_betapRb);
		groupD.add(rad_betanRb);
		groupD.add(rad_iceRb);
		groupD.add(rad_augerRb);
		groupD.add(rad_alphaRb);
		groupD.add(rad_photonsRb);
		groupD.add(rad_electronsRb);
		rad_photonsRb.setSelected(true);

		JPanel p81 = new JPanel();
		p81.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p81.add(rad_allRb);
		p81.add(rad_gammaRb);
		p81.add(rad_xrayRb);
		p81.setBackground(bkgColor);

		JPanel p83 = new JPanel();
		p83.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p83.add(rad_annihRb);
		p83.add(rad_betapRb);
		p83.add(rad_betanRb);
		p83.setBackground(bkgColor);

		JPanel p84 = new JPanel();
		p84.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p84.add(rad_iceRb);
		p84.add(rad_augerRb);
		p84.setBackground(bkgColor);

		JPanel p85 = new JPanel();
		p85.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p85.add(rad_alphaRb);
		p85.add(rad_electronsRb);
		p85.add(rad_photonsRb);
		p85.setBackground(bkgColor);
		// =========
		// ================
		String datas = resources.getString("data.load");// "Data";
		String shieldings = resources.getString("data.shielding");// "Data";
		String currentDir = System.getProperty("user.dir");
		String file_sep = System.getProperty("file.separator");
		String opens = currentDir + file_sep + datas;
		String fileS = resources.getString("data.shielding.buildUpIndex");// "buildup_material_index.xls";
		String filename = opens + file_sep + shieldings + file_sep + fileS;

		try {
			Workbook workbook = Workbook.getWorkbook(new File(filename));
			Sheet sheet = workbook.getSheet(0);
			// always coeff.xls has sheet 0 pointed to TISSUE coefficients!!!

			// int ncol=sheet.getColumns();
			int nrow = sheet.getRows();

			shieldS = new String[nrow];
			for (int i = 0; i < nrow; i++) {
				Cell aCell = sheet.getCell(0, i);// col 0; row i
				shieldS[i] = aCell.getContents();
			}
			workbook.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		// ==================
		shieldCb = new JComboBox(shieldS);
		String str11 = shieldS[8];// Pb
		shieldCb.setSelectedItem((Object) str11);
		shieldCb.setMaximumRowCount(10);
		shieldCb.setPreferredSize(sizeCb);

		JPanel p00 = new JPanel();
		p00.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
		label = new JLabel(resources.getString("distanceTf"));
		label.setForeground(foreColor);
		p00.add(label);
		p00.add(distanceTf);
		distanceTf.setText("100.0");
		label = new JLabel(resources.getString("shieldCb"));
		label.setForeground(foreColor);
		p00.add(label);
		p00.add(shieldCb);
		label = new JLabel(resources.getString("thicknessTf"));
		label.setForeground(foreColor);
		p00.add(label);
		p00.add(thicknessTf);
		thicknessTf.setText("0.3");
		p00.add(hvlCh);
		p00.setBackground(bkgColor);

		JPanel p1 = new JPanel();
		BoxLayout bld = new BoxLayout(p1, BoxLayout.Y_AXIS);
		p1.setLayout(bld);
		p1.add(p81, null);
		p1.add(p83, null);
		p1.add(p84, null);
		p1.add(p85, null);
		p1.add(p00, null);
		// p1.add(p03,null);
		p1.setBackground(bkgColor);
		// p1.setBorder(FrameUtilities.getGroupBoxBorder(
		// resources.getString("radiationType.border"), foreColor));

		thresh_allRb = new JRadioButton(
				resources.getString("library.threshold.all"));
		thresh_allRb.setBackground(bkgColor);
		thresh_allRb.setForeground(foreColor);
		thresh_1Rb = new JRadioButton(
				resources.getString("library.threshold.cut1"));
		thresh_1Rb.setBackground(bkgColor);
		thresh_1Rb.setForeground(foreColor);
		thresh_2Rb = new JRadioButton(
				resources.getString("library.threshold.cut2"));
		thresh_2Rb.setBackground(bkgColor);
		thresh_2Rb.setForeground(foreColor);
		thresh_5Rb = new JRadioButton(
				resources.getString("library.threshold.cut5"));
		thresh_5Rb.setBackground(bkgColor);
		thresh_5Rb.setForeground(foreColor);
		thresh_10Rb = new JRadioButton(
				resources.getString("library.threshold.cut10"));
		thresh_10Rb.setBackground(bkgColor);
		thresh_10Rb.setForeground(foreColor);
		thresh_20Rb = new JRadioButton(
				resources.getString("library.threshold.cut20"));
		thresh_20Rb.setBackground(bkgColor);
		thresh_20Rb.setForeground(foreColor);
		ButtonGroup groupC = new ButtonGroup();
		groupC.add(thresh_allRb);
		groupC.add(thresh_1Rb);
		groupC.add(thresh_2Rb);
		groupC.add(thresh_5Rb);
		groupC.add(thresh_10Rb);
		groupC.add(thresh_20Rb);
		thresh_10Rb.setSelected(true);
		JPanel p2 = new JPanel();
		BoxLayout bld2 = new BoxLayout(p2, BoxLayout.Y_AXIS);
		p2.setLayout(bld2);
		p2.add(thresh_allRb, null);
		p2.add(thresh_1Rb, null);
		p2.add(thresh_2Rb, null);
		p2.add(thresh_5Rb, null);
		p2.add(thresh_10Rb, null);
		p2.add(thresh_20Rb, null);
		p2.setBackground(bkgColor);
		p2.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("threshold.border"), foreColor));

		JPanel p11 = new JPanel();
		p11.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
		p11.setBackground(bkgColor);
		p11.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("radiationType.border"), foreColor));
		p11.add(p1);
		p11.add(p2);

		JPanel p01 = new JPanel();
		p01.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		label = new JLabel(resources.getString("energyTf"));
		label.setForeground(foreColor);
		p01.add(label);
		p01.add(mainEnergyTf);
		mainEnergyTf.setText("0.125");
		label = new JLabel(resources.getString("measuredDoseRateTf"));
		label.setForeground(foreColor);
		p01.add(label);
		p01.add(measuredDoseRateTf);
		measuredDoseRateTf.setText("2.2");
		label = new JLabel(resources.getString("desiredDoseRateTf"));
		label.setForeground(foreColor);
		p01.add(label);
		p01.add(desiredDoseRateTf);
		desiredDoseRateTf.setText("0.2");
		p01.setBackground(bkgColor);

		JPanel p02 = new JPanel();
		p02.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		buttonName = resources.getString("evaluate.button");
		buttonToolTip = resources.getString("evaluate.button.toolTip");
		buttonIconName = resources.getString("img.set");
		button = FrameUtilities.makeButton(buttonIconName, EVALUATE_COMMAND,
				buttonToolTip, buttonName, this, this);
		mnemonic = (Character) resources.getObject("evaluate.button.mnemonic");
		button.setMnemonic(mnemonic.charValue());
		p02.add(button);
		p02.setBackground(bkgColor);

		JPanel p03 = new JPanel();
		BoxLayout bld3 = new BoxLayout(p03, BoxLayout.Y_AXIS);
		p03.setLayout(bld3);
		p03.add(p01, null);
		p03.add(p02, null);
		p03.setBackground(bkgColor);
		p03.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("quickEvaluation.border"), foreColor));

		JPanel p31 = new JPanel();
		p31.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		buttonName = resources.getString("display.button");
		buttonToolTip = resources.getString("display.button.toolTip");
		buttonIconName = resources.getString("img.set");
		button = FrameUtilities.makeButton(buttonIconName, DISPLAY_COMMAND,
				buttonToolTip, buttonName, this, this);
		mnemonic = (Character) resources.getObject("display.button.mnemonic");
		button.setMnemonic(mnemonic.charValue());
		p31.add(button);
		buttonName = resources.getString("stop.button");
		buttonToolTip = resources.getString("stop.button.toolTip");
		buttonIconName = resources.getString("img.close");
		button = FrameUtilities.makeButton(buttonIconName, KILL_COMMAND,
				buttonToolTip, buttonName, this, this);
		mnemonic = (Character) resources.getObject("stop.button.mnemonic");
		button.setMnemonic(mnemonic.charValue());
		p31.add(button);
		p31.setBackground(bkgColor);

		JPanel p32 = new JPanel();
		BoxLayout bld32 = new BoxLayout(p32, BoxLayout.Y_AXIS);
		p32.setLayout(bld32);
		p32.add(p11, null);
		p32.add(p31, null);
		p32.add(p03, null);
		p32.setBackground(bkgColor);
		p32.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("auxData.border"), foreColor));// @@@@@@@@@@@@@

		JPanel p41 = new JPanel();
		p41.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
		p41.add(e_airRb);
		p41.add(e_waterRb);
		p41.add(e_gsRb);
		p41.add(e_isRb);
		// p41.add(i_ingRb);
		// p41.add(i_inhRb);
		p41.setBackground(bkgColor);
		
		//===============
		JPanel page = new JPanel();
		page.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
		page.setBackground(bkgColor);
		page.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("age.border"), foreColor));
		page.add(y1Rb);page.add(y10Rb);page.add(adultRb);
		page.setBackground(bkgColor);
		//================
		JPanel p411 = new JPanel();
		p411.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
		p411.setBackground(bkgColor);
		p411.add(i_ingRb);
		p411.add(i_inhRb);p411.add(page);

		JPanel p42 = new JPanel();
		p42.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		buttonName = resources.getString("calculate.button");
		buttonToolTip = resources.getString("calculate.button.toolTip");
		buttonIconName = resources.getString("img.set");
		button = FrameUtilities.makeButton(buttonIconName, CALCULATE_COMMAND,
				buttonToolTip, buttonName, this, this);
		mnemonic = (Character) resources.getObject("calculate.button.mnemonic");
		button.setMnemonic(mnemonic.charValue());
		p42.add(parentCh);
		p42.add(button);
		p42.setBackground(bkgColor);

		JPanel p4 = new JPanel();
		BoxLayout bld4 = new BoxLayout(p4, BoxLayout.Y_AXIS);
		p4.setLayout(bld4);
		p4.add(p41, null);
		p4.add(p411, null);
		p4.add(p42, null);
		p4.setBackground(bkgColor);
		p4.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("exposure.border"), foreColor));

		JPanel g0 = new JPanel();
		g0.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		label = new JLabel(resources.getString("activityTf"));
		label.setForeground(foreColor);
		g0.add(label);
		g0.add(activityTf);
		activityTf.setText("400");
		label = new JLabel(resources.getString("activityTf.Lb"));
		label.setForeground(foreColor);
		g0.add(label);
		label = new JLabel(resources.getString("library.sampleTimeLb"));
		label.setForeground(foreColor);
		g0.add(label);
		g0.add(elapsedTimeTf);
		elapsedTimeTf.setText("180");
		label = new JLabel(resources.getString("library.sampleTimeUnitLb"));
		label.setForeground(foreColor);
		g0.add(label);
		g0.add(sampleTimeUnitsCb);
		g0.setBackground(bkgColor);

		JPanel p6 = new JPanel();
		p6.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		label = new JLabel(resources.getString("library.master.dbLabel"));
		label.setForeground(foreColor);
		p6.add(label);
		p6.add(dbCb);
		label = new JLabel(resources.getString("parentNuclideLb"));
		label.setForeground(foreColor);
		p6.add(label);
		p6.add(nucCb);
		// p6.add(parentCh);
		p6.add(excelCh);
		p6.setBackground(bkgColor);

		JPanel p51 = new JPanel();
		p51.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p51.setBackground(bkgColor);
		p51.add(a_secRb);

		JPanel p52 = new JPanel();
		p52.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p52.setBackground(bkgColor);
		p52.add(a_normRb);
		buttonName = resources.getString("graph.button");
		buttonToolTip = resources.getString("graph.button.toolTip");
		buttonIconName = resources.getString("img.chart.curve");
		button = FrameUtilities.makeButton(buttonIconName, GRAPH_COMMAND,
				buttonToolTip, buttonName, this, this);
		mnemonic = (Character) resources.getObject("graph.button.mnemonic");
		button.setMnemonic(mnemonic.charValue());
		p52.add(button);

		JPanel p53 = new JPanel();
		p53.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		p53.setBackground(bkgColor);
		p53.add(a_eqRb);
		p53.add(xparentHalfLifeTf);
		xparentHalfLifeTf.setText("10");
		label = new JLabel(resources.getString("library.a_eqRb.Lb"));
		label.setForeground(foreColor);
		p53.add(label);

		JPanel p5 = new JPanel();
		BoxLayout bld5 = new BoxLayout(p5, BoxLayout.Y_AXIS);
		p5.setLayout(bld5);
		p5.add(p51, null);
		p5.add(p52, null);
		p5.add(p53, null);
		p5.setBackground(bkgColor);
		p5.setBorder(FrameUtilities.getGroupBoxBorder(
				resources.getString("decay.border"), foreColor));

		JPanel mainP = new JPanel();
		BoxLayout bld6 = new BoxLayout(mainP, BoxLayout.Y_AXIS);
		mainP.setLayout(bld6);
		mainP.add(p6);
		mainP.add(g0);
		mainP.add(p5);
		mainP.add(p4);
		mainP.add(p32);// (p11); // mainP.add(p32);
		mainP.setBackground(bkgColor);

		return mainP;
	}

	/**
	 * Create the output panel
	 * @return the result
	 */
	private JPanel createOutputPanel() {

		textArea = new JTextArea();
		textArea.setCaretPosition(0);
		textArea.setEditable(false);
		textArea.setText("");
		textArea.setWrapStyleWord(true);
		textArea.setBackground(textAreaBkgColor);
		textArea.setForeground(textAreaForeColor);
		// textArea.setPreferredSize(textAreaSize);

		JPanel resultP = new JPanel(new BorderLayout());
		resultP.add(new JScrollPane(textArea), BorderLayout.CENTER);
		resultP.setPreferredSize(textAreaSize);
		resultP.setBackground(bkgColor);

		JPanel mainP = new JPanel(new BorderLayout());
		mainP.add(resultP, BorderLayout.CENTER);// main dimension !!
		mainP.setBackground(bkgColor);
		return mainP;
	}

	/**
	 * Create tabs
	 * @return the result
	 */
	private JTabbedPane createTabs() {

		JTabbedPane tabs = new JTabbedPane();

		JPanel mainPan = createMainPanel();
		JPanel outPan = createOutputPanel();

		String s = resources.getString("dataTab");
		tabs.add(s, mainPan);
		s = resources.getString("resultsTab");
		;
		tabs.add(s, outPan);

		return tabs;

	}

	/**
	 * Initialize status bar.
	 * 
	 * @param toolBar toolBar
	 */
	private void initStatusBar(JToolBar toolBar) {
		JPanel toolP = new JPanel();
		toolP.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 1));

		toolP.add(statusL);
		toolBar.add(toolP);
		statusL.setText(resources.getString("status.wait"));
	}

	// --------------
	/**
	 * Create menu bar
	 * @param resources the resources
	 * @return the result
	 */
	private JMenuBar createMenuBar(ResourceBundle resources) {
		// create the menus
		JMenuBar menuBar = new JMenuBar();

		String label;
		Character mnemonic;
		ImageIcon img;
		String imageName = "";

		// the file menu
		label = resources.getString("menu.file");
		mnemonic = (Character) resources.getObject("menu.file.mnemonic");
		JMenu fileMenu = new JMenu(label, true);
		fileMenu.setMnemonic(mnemonic.charValue());

		imageName = resources.getString("img.close");
		img = FrameUtilities.getImageIcon(imageName, this);
		label = resources.getString("menu.file.exit");
		mnemonic = (Character) resources.getObject("menu.file.exit.mnemonic");
		JMenuItem exitItem = new JMenuItem(label, mnemonic.charValue());
		exitItem.setActionCommand(EXIT_COMMAND);
		exitItem.addActionListener(this);
		exitItem.setIcon(img);
		exitItem.setToolTipText(resources.getString("menu.file.exit.toolTip"));
		fileMenu.add(exitItem);

		// the help menu
		label = resources.getString("menu.help");
		mnemonic = (Character) resources.getObject("menu.help.mnemonic");
		JMenu helpMenu = new JMenu(label);
		helpMenu.setMnemonic(mnemonic.charValue());

		imageName = resources.getString("img.about");
		img = FrameUtilities.getImageIcon(imageName, this);
		label = resources.getString("menu.help.about");
		mnemonic = (Character) resources.getObject("menu.help.about.mnemonic");
		JMenuItem aboutItem = new JMenuItem(label, mnemonic.charValue());
		aboutItem.setActionCommand(ABOUT_COMMAND);
		aboutItem.addActionListener(this);
		aboutItem.setIcon(img);
		aboutItem
				.setToolTipText(resources.getString("menu.help.about.toolTip"));

		imageName = resources.getString("img.about");
		img = FrameUtilities.getImageIcon(imageName, this);

		label = resources.getString("menu.help.howTo");
		mnemonic = (Character) resources.getObject("menu.help.howTo.mnemonic");
		JMenuItem howToItem = new JMenuItem(label, mnemonic.charValue());
		howToItem.setActionCommand(HOWTO_COMMAND);
		howToItem.addActionListener(this);
		howToItem.setIcon(img);
		howToItem
				.setToolTipText(resources.getString("menu.help.howTo.toolTip"));

		label = resources.getString("menu.help.LF");
		mnemonic = (Character) resources.getObject("menu.help.LF.mnemonic");
		JMenuItem lfItem = new JMenuItem(label, mnemonic.charValue());
		lfItem.setActionCommand(LOOKANDFEEL_COMMAND);
		lfItem.addActionListener(this);
		lfItem.setToolTipText(resources.getString("menu.help.LF.toolTip"));

		if (showLAF) {
			helpMenu.add(lfItem);
			helpMenu.addSeparator();
		}

		// helpMenu.add(howToItem);
		// helpMenu.addSeparator();

		helpMenu.add(aboutItem);

		// finally, glue together the menu and return it
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);

		return menuBar;
	}

	/**
	 * Setting up the frame size.
	 */
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	/**
	 * JCombobox related actions are set here
	 */
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == dbCb) {
			fetchNuclideBaseInfo();
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Retrieve nuclide basic information
	 */
	private void fetchNuclideBaseInfo() {

		nucV = new Vector<String>();

		setMasterTables();

		Connection conn12 = null;

		Statement s = null;
		ResultSet rs = null;
		try {
			String datas = resources.getString("data.load");// Data
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;
			String dbName = icrpDB;
			opens = opens + file_sep + dbName;

			conn12 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");

			s = conn12.createStatement();

			rs = s.executeQuery("SELECT * FROM " + icrpTable);

			nucCb.removeAllItems();

			if (rs != null)
				while (rs.next()) {
					String ss = rs.getString(2);
					nucV.addElement(ss);
					nucCb.addItem(ss);
				}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			// release all open resources to avoid unnecessary memory usage

			// ResultSet
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}

				if (s != null) {
					s.close();
					s = null;
				}
			} catch (Exception sqle) {
				sqle.printStackTrace();
			}

			// Connection
			try {
				if (conn12 != null) {
					conn12.close();
					conn12 = null;
				}
			} catch (Exception sqle) {
				sqle.printStackTrace();
			}
		}
	}

	/**
	 * Most actions are set here
	 */
	public void actionPerformed(ActionEvent evt) {
		command = evt.getActionCommand();
		if (command.equals(ABOUT_COMMAND)) {
			about();
		} else if (command.equals(EXIT_COMMAND)) {
			attemptExit();
		} else if (command.equals(LOOKANDFEEL_COMMAND)) {
			lookAndFeel();
		} else if (command.equals(GRAPH_COMMAND)) {
			// graph();//for letting computing finished, then perform graph!!!
			// Note: start thread force one computation thread to be executed!
			statusRunS = resources.getString("status.computing");
			startThread();
		} else if (command.equals(CALCULATE_COMMAND)) {
			statusRunS = resources.getString("status.computing");
			startThread();
		} else if (command.equals(DISPLAY_COMMAND)) {
			statusRunS = resources.getString("status.computing");
			startThread();
		} else if (command.equals(EVALUATE_COMMAND)) {
			statusRunS = resources.getString("status.computing");
			startThread();
		} else if (command.equals(KILL_COMMAND)) {
			kill();
		}
	}

	/**
	 * Save chain activities computed in default equilibrium condition (e.g. after a time 
	 * equal to 10 times the parent half life). Called by saveActivitiesChain.
	 */
	private void saveActivitiesChain_Equilibrum() {
		double norm = 1.0;
		// we have nuclideS...nuclides_all AND we have
		// nuc_chains and a_chains[][]
		if (nchain > 0)// 0,1 we have at least 2 chains
		{
			for (int i = 0; i < nuclides_all.length; i++) {
				double[] a_temp = new double[nchain + 1];
				for (int k = 0; k <= nchain; k++) {
					for (int j = 0; j < ndaughterChainMax; j++) {
						if (k < nmax_chains)
							if (nuclides_all[i].equals(nuc_chains[j][k])) {
								a_temp[k] = a_chains_equilibrum[j][k];
								// each nuclides has 1 occurance in each chain
								// System.out.println(""+a_temp[k]);
								break;
							}
					}
				}
				// look for duplicates!!
				Vector<Integer> dup = new Vector<Integer>();
				boolean foundB = false;
				for (int k = 0; k < nchain; k++) {
					if (k < nmax_chains) {
						foundB = false;
						int dupN = dup.size();
						if (dupN > 0) {
							for (int n = 0; n < dupN; n++) {
								Integer itg = (Integer) dup.elementAt(n);
								int indx = itg.intValue();
								if (k == indx) {
									foundB = true;// we have a skip
									break;
								}
							}
						}
						if (!foundB) {
							for (int l = k + 1; l <= nchain; l++) {
								if (a_temp[k] == a_temp[l]) {
									dup.addElement(new Integer(l));
								}
							}
						}
					}
				}
				// //END...look for duplicates!!

				if (dup.size() > 0)
					for (int k = 0; k < dup.size(); k++) {
						Integer itg = (Integer) dup.elementAt(k);
						int indx = itg.intValue();
						a_temp[indx] = 0.0;
					}

				double sum = 0.0;
				for (int k = 0; k <= nchain; k++) {
					if (k < nmax_chains) {
						// System.out.println(""+a_temp[k]);
						sum = sum + a_temp[k];
					}
				}

				activity_chain_equilibrum[i] = sum;

				norm = activity_chain_equilibrum[0];
				// System.out.println(" N: "+nuclides_all[i]+" Y: "+formatNumber(yield_all[i])+
				// " A: "+formatNumberScientific(activity_secular[i])+
				// " Ach: "+formatNumberScientific(activity_chain[i])+
				// " Ach_scaled: "+formatNumberScientific(activity_chain_scaled[i])
				// );

			}// for
				// normalise:
			for (int i = 0; i < nuclides_all.length; i++) {
				if (norm != 0.0)
					activity_chain_equilibrum[i] = activity_chain_equilibrum[i]
							/ norm;
				else
					activity_chain_equilibrum[i] = 0.0;
			}

		} else// if (nchain=0)
		{
			for (int i = 0; i < nuclides_all.length; i++) {
				int k = nchain;// aka 0 here!
				for (int j = 0; j < ndaughterChainMax; j++) {
					if (k < nmax_chains)
						if (nuclides_all[i].equals(nuc_chains[j][k])) {
							activity_chain_equilibrum[i] = a_chains_equilibrum[j][k];
							norm = activity_chain_equilibrum[0];
							// System.out.println(" N: "+nuclides_all[i]+" Y: "+yield_all[i]+" A: "+activity_secular[i]+
							// " Ach: "+activity_chain[i]+" Ach_scaled: "+activity_chain_scaled[i]);
							break;
						}
				}

			}
			// normalise:
			for (int i = 0; i < nuclides_all.length; i++) {
				if (norm != 0.0)
					activity_chain_equilibrum[i] = activity_chain_equilibrum[i]
							/ norm;
				else
					activity_chain_equilibrum[i] = 0.0;
			}

		}
	}

	/**
	 * Save chain activities.
	 */
	private void saveActivitiesChain() {
		// double norm = 1.0;
		String s = "";
		saveActivitiesChain_Equilibrum();
		s = "-------------------------------" + " \n";
		textArea.append(s);
		// if (IA_use == IA_sec)// secular
		// {
		// s = resources.getString("text.corr.type")
		// + resources.getString("db.equilibrum.secular");
		// } else if (IA_use == IA_elapsed)// known sample time
		// {
		// s = resources.getString("text.corr.type")
		// + resources.getString("db.equilibrum.sample") + sampleTimeS
		// + "_" + sampleTimeUnitsS;
		// } else if (IA_use == IA_norm) {
		// not used here!!
		// } else if (IA_use == IA_eq)// equilibrum reached at 10 x parent half
		// life
		// {
		// s = resources.getString("text.corr.type")
		// + resources.getString("db.equilibrum.default");
		// }
		// s = s + "\n";
		//textArea.append(s);
		s = "\n";
		textArea.append(s);
		// we have nuclideS...nuclides_all AND
		// we have nuc_chains and a_chains[][]
		if (nchain > 0)// 0,1 we have at least 2 chains
		{
			for (int i = 0; i < nuclides_all.length; i++) {
				double[] a_temp = new double[nchain + 1];
				double[] at_temp = new double[nchain + 1];
				for (int k = 0; k <= nchain; k++) {
					for (int j = 0; j < ndaughterChainMax; j++) {
						if (k < nmax_chains)
							if (nuclides_all[i].equals(nuc_chains[j][k])) {
								a_temp[k] = a_chains[j][k];
								// each nuclides has 1 occurance in each chain
								at_temp[k] = at_chains[j][k];
								// each nuclides has 1 occurance in each chain
								// System.out.println(""+a_temp[k]);
								break;
							}
					}
				}
				// look for duplicates!!
				Vector<Integer> dup = new Vector<Integer>();
				boolean foundB = false;
				for (int k = 0; k < nchain; k++) {
					if (k < nmax_chains) {
						foundB = false;
						int dupN = dup.size();
						if (dupN > 0) {
							for (int n = 0; n < dupN; n++) {
								Integer itg = (Integer) dup.elementAt(n);
								int indx = itg.intValue();
								if (k == indx) {
									foundB = true;// we have a skip
									break;
								}
							}
						}
						if (!foundB) {
							for (int l = k + 1; l <= nchain; l++) {
								if (a_temp[k] == a_temp[l]) {
									dup.addElement(new Integer(l));
								}
							}
						}
					}
				}
				// //END...look for duplicates!!

				if (dup.size() > 0)
					for (int k = 0; k < dup.size(); k++) {
						Integer itg = (Integer) dup.elementAt(k);
						int indx = itg.intValue();
						a_temp[indx] = 0.0;
						at_temp[indx] = 0.0;
					}

				double sum = 0.0;
				double sumt = 0.0;
				for (int k = 0; k <= nchain; k++) {
					if (k < nmax_chains) {
						// System.out.println(""+a_temp[k]);
						sum = sum + a_temp[k];
						sumt = sumt + at_temp[k];
					}
				}

				activity_chain[i] = sum;
				activity_chain_scaled[i] = sumt;

				// norm = activity_chain[0];
				// norm here has no physical meaning!
				// coud be case with parent->0 activ and daughter activ is
				// greater than parent
				// and of course lower than initial parent activity..BUT!!!!:
				// ->norming can cause A daughter>A parent initial!!!!!!!! NON
				// SENSE!
				// The only application is when we computed correction factor
				// due to parent-decay...corr factor is based on these normed
				// "virtual" activities...see GAMMA ANALYSIS!!

			}// for (int i=0;i<nuclides_all.length;i++)

			for (int i = 0; i < nuclides_all.length; i++) {
				// if (norm != 0.0)
				// activity_chain[i] = activity_chain[i] / norm;
				// else
				// activity_chain[i] = 0.0;
				// computeCorrection(i);
				s = resources.getString("text.nuclide")
						+ // "Nuclide: "+
						nuclides_all[i]
						+ "; "
						+ resources.getString("text.branchingRatio")
						+ // "; Yield: "+
							// Convertor.formatNumber(yield_all[i],5)+"; "+
						Convertor.formatNumberScientific(yield_all[i])
						+ "; "
						+ resources.getString("text.activitySecular")
						+ // "; Activ.secular: "+
						Convertor.formatNumberScientific(activity_secular[i])
						+ "; "
						+ resources.getString("text.activitySample")
						+ // "; Activ.elapsed: "+
						Convertor.formatNumberScientific(activity_chain[i])
						+ "; "
						+ resources.getString("text.integral2")
						+ // "; Time-integrated activ: "+
						Convertor
								.formatNumberScientific(activity_chain_scaled[i])
						+ "; " + resources.getString("text.activityEquilibrum")
						+ // "; Activ.equilibrum: "+
						Convertor
								.formatNumberScientific(activity_chain_equilibrum[i])
						// + "; "
						// + resources.getString("text.decayCorr")
						// + // "; DecayCorr: "+
						// Convertor
						// .formatNumberScientific(parentDaughterDecayCorr[i])
						+ " \n";
				textArea.append(s);
			}
		} else// if (nchain=0)
		{
			for (int i = 0; i < nuclides_all.length; i++) {
				int k = nchain;// aka 0 here!
				for (int j = 0; j < ndaughterChainMax; j++) {
					if (k < nmax_chains)
						if (nuclides_all[i].equals(nuc_chains[j][k])) {
							activity_chain[i] = a_chains[j][k];
							activity_chain_scaled[i] = at_chains[j][k];

							// norm = activity_chain[0];

							break;
						}
				}

			}

			for (int i = 0; i < nuclides_all.length; i++) {
				// if (norm != 0.0)
				// activity_chain[i] = activity_chain[i] / norm;
				// else
				// activity_chain[i] = 0.0;
				// computeCorrection(i);
				s = resources.getString("text.nuclide")
						+ // "Nuclide: "+
						nuclides_all[i]
						+ "; "
						+ resources.getString("text.branchingRatio")
						+ // "; Yield: "+
							// Convertor.formatNumber(yield_all[i],5)+"; "+
						Convertor.formatNumberScientific(yield_all[i])
						+ "; "
						+ resources.getString("text.activitySecular")
						+ // "; Activ.secular: "+
						Convertor.formatNumberScientific(activity_secular[i])
						+ "; "
						+ resources.getString("text.activitySample")
						+ // "; Activ.elapsed: "+
						Convertor.formatNumberScientific(activity_chain[i])
						+ "; "
						+ resources.getString("text.integral2")
						+ // ; Time-integrated activ: "+
						Convertor
								.formatNumberScientific(activity_chain_scaled[i])
						+ "; " + resources.getString("text.activityEquilibrum")
						+ // "; Activ.equilibrum: "+
						Convertor
								.formatNumberScientific(activity_chain_equilibrum[i])
						// + "; "
						// + resources.getString("text.decayCorr")
						// + // "; DecayCorr: "+
						// Convertor
						// .formatNumberScientific(parentDaughterDecayCorr[i])
						+ " \n";
				textArea.append(s);
			}
		}
		s = "\n";
		textArea.append(s);
		// END
		if (excelB) {
			int offset = nuclides_all.length + 3 + 5;
			Label label = new Label(0, offset,
					resources.getString("xls.overallFinal"));// "Overall activities (final results)");//col,row

			offset++;
			try {
				sheetCA.addCell(label);
				label = new Label(0, offset,
						resources.getString("text.nuclide"));// "Nuclide");//col,row
				sheetCA.addCell(label);
				label = new Label(1, offset,
						resources.getString("text.branchingRatio"));// "Yield");//col,row
				sheetCA.addCell(label);
				label = new Label(3, offset,
						resources.getString("text.activitySample"));// "Activity [Bq]");//col,row
				sheetCA.addCell(label);
				label = new Label(2, offset,
						resources.getString("text.activitySecular"));// "Forced activity at secular equilibrum [Bq]");//col,row
				sheetCA.addCell(label);

				label = new Label(4, offset,
						resources.getString("text.integral2"));// "Time-integrated activity [Bq x s]");//col,row
				sheetCA.addCell(label);

				label = new Label(5, offset,
						resources.getString("text.activityEquilibrum"));// "Activity at equilibrum [Bq]");//col,row
				sheetCA.addCell(label);

				offset++;

				for (int i = 0; i < nuclides_all.length; i++) {
					label = new Label(0, offset, nuclides_all[i]);// col,row
					sheetCA.addCell(label);

					jxl.write.Number number = new jxl.write.Number(1, offset,
							yield_all[i]);
					sheetCA.addCell(number);

					number = new jxl.write.Number(3, offset, activity_chain[i]);
					sheetCA.addCell(number);

					number = new jxl.write.Number(2, offset,
							activity_secular[i]);
					sheetCA.addCell(number);

					number = new jxl.write.Number(4, offset,
							activity_chain_scaled[i]);
					sheetCA.addCell(number);

					number = new jxl.write.Number(5, offset,
							activity_chain_equilibrum[i]);
					sheetCA.addCell(number);

					offset++;
				}

			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Save activities in secular equilibrium condition. it also initializes some 
	 * global variables.
	 */
	private void saveActivitiesSecularAndPrepareChainData()// and transient
	{
		int nnuc = nuclideS.size();
		nuclides_all = new String[nnuc];
		yield_all = new double[nnuc];
		activity_secular = new double[nnuc];
		activity_chain_equilibrum = new double[nnuc];
		activity_chain_scaled = new double[nnuc];
		activity_chain = new double[nnuc];
		// parentDaughterDecayCorr = new double[nnuc];

		for (int i = 0; i < nnuc; i++) {
			String nuc = (String) nuclideS.elementAt(i);
			Double yD = (Double) yieldD.elementAt(i);
			double yi = yD.doubleValue();

			nuclides_all[i] = nuc;
			yield_all[i] = yi;
			activity_secular[i] = activityUnit * yield_all[i];
		}
	}

	private boolean title1B = false;

	/**
	 * Computes chain activities using Bateman decay law.
	 * @param ichain index of the chain
	 */
	private void computeChainActivity(int ichain) {
		if (ichain >= nmax_chains) {
			return;
		}
		String s = "";
		int nnuc = 0;
		for (int i = 0; i < ndaughterChainMax; i++) {
			nnuc++;// 1 nuclide always

			if (nuc_chains[i][ichain].equals("-1")) {
				nnuc--;
				break;
			}
		}

		String[] nucs = new String[nnuc];
		double[] branchingRatio = new double[nnuc];
		double[] halfLife = new double[nnuc];
		double[] atomicMass = new double[nnuc];
		double[] initialMass = new double[nnuc];
		double[] initialNumberAtomsFromcSource = new double[nnuc];

		for (int i = 0; i < nnuc; i++) {
			nucs[i] = nuc_chains[i][ichain];
			branchingRatio[i] = br_chains[i][ichain];
			halfLife[i] = formatHalfLife(hl_chains[i][ichain],
					hlu_chains[i][ichain]);
			atomicMass[i] = am_chains[i][ichain];
			// compute initial mass number
			double dc = Math.log(2.0) / halfLife[i];
			double navogadro = 6.02214199E26;// atoms/kmol!!
			if (i == 0)
				initialMass[i] = activityUnit * atomicMass[i]
						/ (navogadro * dc);// only parent
			else
				initialMass[i] = 0.0;
			// System.out.println(" "+nucs[i]+"  mm "+initialMass[i]+"  hl "+
			// halfLife[i]+"  BR "+branchingRatio[i]
			// +"  "+hl_chains[i][ichain]+hlu_chains[i][ichain]+"   "+atomicMass[i]);
			initialNumberAtomsFromcSource[i] = 0.0;// no external sources
		}

		double[] dc = PhysUtilities.computeDecayConstant(halfLife);
		double[] pdc = PhysUtilities.computePartialDecayConstant(halfLife,
				branchingRatio);
		double[] initialNuclideNumbers = PhysUtilities
				.computeInitialNuclideNumbers(initialMass, atomicMass);

		PhysUtilities.steps = timeSteps;
		PhysUtilities.t = elapsedTime;
		// compute===================================================================
		PhysUtilities.bateman(nnuc, dc, pdc, initialNuclideNumbers,
				initialNumberAtomsFromcSource);

		double[] at = new double[PhysUtilities.at.length];
		double[] att = new double[PhysUtilities.att.length];
		double t = PhysUtilities.t;
		int steps = PhysUtilities.steps;
		int j = PhysUtilities.steps;

		for (int i = 0; i < PhysUtilities.at.length; i++) {
			at[i] = PhysUtilities.at[i][j];
			att[i] = PhysUtilities.att[i][j];
			// System.out.println("Nuclide: "+nucs[i]+"; Integrated Activ: "+att[i]);
		}
		// ===============compute activities at equilibrum regarded as being at
		// T1/2 of parent!===
		PhysUtilities.steps = 1;
		PhysUtilities.t = timesHalfLifeForEq * halfLife[0];// parent
															// half-life===always
															// is any chain
		// compute ONE MORE TIME
		PhysUtilities.bateman(nnuc, dc, pdc, initialNuclideNumbers,
				initialNumberAtomsFromcSource);

		s = "-------------------------------" + " \n";
		textArea.append(s);

		for (int i = 1; i <= nnuc; i++) {
			a_chains_equilibrum[i - 1][ichain] = PhysUtilities.at[i - 1][j];
			if (a_chains_equilibrum[i - 1][ichain] < 0.0)
				a_chains_equilibrum[i - 1][ichain] = 0.0;// residual error
		}
		if (!title1B) {
			s = resources.getString("xls.activChainsAtTime");// "Activity of radioactive chains computed at ELAPSED TIME ";
			s = s + elapsedTime + " sec. [" + elapsedTime / 3600. + " hours; "
					+ elapsedTime / (3600. * 24.) + " days; " + elapsedTime
					/ (3600. * 24. * 365.25) + " years]";
			textArea.append(s + "\n");
			s = resources.getString("xls.secularInfo");// "Overall forced activity at secular equilibrum is computed as parent activity [1Bq] times relative yield. THIS CAN BE NEVER REACHED!!";
			textArea.append(s + "\n");
			s = resources.getString("xls.noDaughtersInfo");// "Overall activity is resulted from decay chains at given ELAPSED TIME, assuming the parent activity is 1 Bq at t=0 and has NO daughters at t=0.";
			textArea.append(s + "\n");
			s = resources.getString("xls.integralInfo");// "Overall time integrated activity is computed during exposure time, assuming the parent activity is 1 Bq at t=0 and has NO daughters at t=0.";
			textArea.append(s + "\n");
			s = resources.getString("xls.eqInfo");// "Overall activity at equilibrum is normalised to parent activity and the calculations were performed at a fixed elapsed time of 10 x PARENT_HALF_LIFE. At this time the equilibrum (if any) is considered to be reached!";
			textArea.append(s + "\n\n");
			title1B = true;
		}

		for (int i = 1; i <= nnuc; i++) {
			a_chains[i - 1][ichain] = at[i - 1];// PhysUtilities.at[i-1][j];
			at_chains[i - 1][ichain] = att[i - 1];// PhysUtilities.at[i-1][j];
			if (a_chains[i - 1][ichain] < 0.0)
				a_chains[i - 1][ichain] = 0.0;// residual error
			if (at_chains[i - 1][ichain] < 0.0)
				at_chains[i - 1][ichain] = 0.0;// residual error

			s = resources.getString("text.nuclide")// "Nuclide: "
					+ nucs[i - 1]
					+ "; "
					+ resources.getString("text.activity")// "; Activ. [Bq]: "
					+ Convertor.formatNumberScientific(a_chains[i - 1][ichain])
					+ "; "
					+ resources.getString("text.activity.atTime")// "; at time [s]: "
					+ j
					* t
					/ steps
					+ "; "
					+ resources.getString("text.activityEquilibrum")// "; Activ.equilibrum [Bq]: "+
					+ Convertor
							.formatNumberScientific(a_chains_equilibrum[i - 1][ichain])
					+ "; "
					+ resources.getString("text.integral")// " Integral [Bq x s]= "
					+ Convertor
							.formatNumberScientific(at_chains[i - 1][ichain])
					+ " \n";
			textArea.append(s);
		}

		if (excelB) {
			try {
				// ==================
				String ss = resources.getString("xls.activChainsAtTime");// "Activity of radioactive chains computed at ELAPSED TIME ";
				ss = ss + elapsedTime + " sec. [" + elapsedTime / 3600.
						+ " hours; " + elapsedTime / (3600. * 24.) + " days; "
						+ elapsedTime / (3600. * 24. * 365.25) + " years]";
				Label label = new Label(0, 0, ss);
				sheetCA.addCell(label);

				ss = resources.getString("xls.secularInfo");// "Overall forced activity at secular equilibrum is computed as parent activity [1Bq] times relative yield. THIS CAN BE NEVER REACHED!!";
				label = new Label(0, 1, ss);// col,row
				sheetCA.addCell(label);

				ss = resources.getString("xls.noDaughtersInfo");// "Overall activity is resulted from decay chains at given ELAPSED TIME, assuming the parent activity is 1 Bq at t=0 and has NO daughters at t=0.";
				label = new Label(0, 2, ss);// col,row
				sheetCA.addCell(label);

				// ss="Overall activity normalised is overall activity normalised to 1. Decay chain at given ELAPSED TIME has the parent activity normalised, of 1 Bq!";
				ss = resources.getString("xls.integralInfo");// "Overall time integrated activity is computed during exposure time, assuming the parent activity is 1 Bq at t=0 and has NO daughters at t=0.";
				label = new Label(0, 3, ss);// col,row
				sheetCA.addCell(label);

				ss = resources.getString("xls.eqInfo");// "Overall activity at equilibrum is normalised to parent activity and the calculations were performed at a fixed elapsed time of 10 x PARENT_HALF_LIFE. At this time the equilibrum (if any) is considered to be reached!";
				label = new Label(0, 4, ss);// col,row
				sheetCA.addCell(label);

				// =======================
				// label = new Label(ichain*4, 1+5, "Nuclide");//col,row
				label = new Label(ichain * 5, 1 + 5,
						resources.getString("text.nuclide"));// "Nuclide");//
																// col,row
				sheetCA.addCell(label);
				label = new Label(ichain * 5 + 1, 1 + 5,
						resources.getString("text.activity"));// "Activity [Bq]");//
																// col,row
				sheetCA.addCell(label);
				label = new Label(ichain * 5 + 2, 1 + 5,
						resources.getString("text.activity.atTime"));// "at Time [s]");//
																		// col,row
				sheetCA.addCell(label);

				label = new Label(ichain * 5 + 3, 1 + 5,
						resources.getString("text.activityEquilibrum"));// "Activity_eq [Bq]");//
																		// col,row
				sheetCA.addCell(label);

				label = new Label(ichain * 5 + 4, 1 + 5,
						resources.getString("text.integral"));// "Integral [Bq x s]");//
																// col,row
				sheetCA.addCell(label);

				for (int i = 1; i <= nnuc; i++) {
					// a_chains[i-1][ichain]=at[i-1];//PhysUtilities.at[i-1][j];
					// at_chains[i-1][ichain]=att[i-1];//PhysUtilities.at[i-1][j];
					// if(a_chains[i-1][ichain]<0.0)
					// a_chains[i-1][ichain]=0.0;//residual error
					// if(at_chains[i-1][ichain]<0.0)
					// at_chains[i-1][ichain]=0.0;//residual error

					// =============
					// System.out.println("nuc: "+nucs[i-1]+" a(t)= "+a_chains[i-1][ichain]+//PhysUtilities.at[i-1][j]+
					// " at step: "+j+" time[s]= "+j*t/steps+"; ore= "+
					// j*t/(3600.*steps)+"; zile= "+
					// j*t/(24*3600.*steps));
					// s="Nuclide: "+nucs[i-1]+"; Activ. [Bq]: "+Convertor.formatNumberScientific(a_chains[i-1][ichain])+
					// "; at time [s]: "+j*t/steps+"; Activ.equilibrum [Bq]: "+
					// Convertor.formatNumberScientific(a_chains_equilibrum[i-1][ichain])+
					// " Integral [Bq x s]= "+Convertor.formatNumberScientific(at_chains[i-1][ichain])+" \n";
					// textArea.append(s);
					// ==============
					// label = new Label(ichain*4, i+1+5, nucs[i-1]);//col,row
					label = new Label(ichain * 5, i + 1 + 5, nucs[i - 1]);// col,row
					sheetCA.addCell(label);
					jxl.write.Number number = new jxl.write.Number(
							ichain * 5 + 1, i + 1 + 5, a_chains[i - 1][ichain]);// PhysUtilities.at[i-1][j]);
					sheetCA.addCell(number);
					number = new jxl.write.Number(ichain * 5 + 2, i + 1 + 5, j
							* t / steps);// j*PhysUtilities.t/PhysUtilities.steps);
					sheetCA.addCell(number);

					number = new jxl.write.Number(ichain * 5 + 3, i + 1 + 5,
							a_chains_equilibrum[i - 1][ichain]);// PhysUtilities.at[i-1][j]);
					sheetCA.addCell(number);

					number = new jxl.write.Number(ichain * 5 + 4, i + 1 + 5,
							at_chains[i - 1][ichain]);// PhysUtilities.at[i-1][j]);
					sheetCA.addCell(number);

				}
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}

	/**
	 * Reset some variables.
	 */
	private void performCleaningUp() {
		y1_old = null;
		y2_next_chain = null;
		y1_fix = null;
		index = null;
		nucV = null;

		radLocI = null;
		radLocN = null;
		decayModeMainS = null;
		gammaConstantD = null;

		nuclideS = null;
		atomicMassD = null;
		yieldD = null;
		halfLifeD = null;
		halfLifeUnitsS = null;

		rcode = null;
		rtype = null;
		ryield = null;
		renergy = null;
		nucInChain = null;

		nsave = null;
		yield2_readD = null;

		nuc_chains = null;
		hlu_chains = null;
		hl_chains = null;
		br_chains = null;
		am_chains = null;
		a_chains = null;
		at_chains = null;
		a_chains_equilibrum = null;

		nuclides_all = null;// not used in copy
		yield_all = null;// not used in copy

		activity_chain = null;
		activity_secular = null;
		activity_chain_scaled = null;
		activity_chain_equilibrum = null;
		// parentDaughterDecayCorr = null;
	}

	/**
	 * Reset all variables.
	 */
	private void resetAll() {
		externalTable = "";
		publicInhalationTable = "";
		publicIngestionTable = "";

		yield_current = 1.0;
		y1_old = new Vector<Double>();
		y2_next_chain = new Vector<Double>();
		y1_fix = new Vector<Double>();
		index = new Vector<Integer>();

		rcode = new String[0][0];
		rtype = new String[0][0];
		ryield = new double[0][0];
		renergy = new double[0][0];
		nucInChain = new String[0];

		colect_first_time = false;
		parent_dual = false;

		nuclideS = new Vector<String>();
		atomicMassD = new Vector<Double>();
		yieldD = new Vector<Double>();
		radLocI = new Vector<Integer>();
		radLocN = new Vector<Integer>();
		halfLifeD = new Vector<Double>();
		halfLifeUnitsS = new Vector<String>();
		nucV = new Vector<String>();
		parentNuclide = "";
		decayModeMainS = new Vector<String>();
		gammaConstantD = new Vector<Double>();

		ndaughter = 0;
		nsave = new Vector<Integer>();
		yield2_readD = new Vector<Double>();

		nchain = 0;
		nuc_chains = new String[0][0];
		hlu_chains = new String[0][0];
		hl_chains = new double[0][0];
		br_chains = new double[0][0];
		am_chains = new double[0][0];
		ndaughterChainMax = 0;
		a_chains = new double[0][0];
		at_chains = new double[0][0];
		a_chains_equilibrum = new double[0][0];

		nuclides_all = new String[0];
		// overall nuclides..from nuclideS=new Vector();
		activity_chain = new double[0];
		// overall activities from chains at time set (at t=0, A parent =1.0)
		yield_all = new double[0];
		// overall yields..from yieldD=new Vector();
		activity_secular = new double[0];
		// formal (not happen always) forced secular equilibrum:
		// activityUnit*yield_all[i];
		activity_chain_scaled = new double[0];
		// overall normalized activities from chains at time set (at t=t, A
		// parent =1.0)
		activity_chain_equilibrum = new double[0];
		// overall normalized activities from chains at 10 x parent half life
		// (at t=t, A parent =1.0)..equilibrum auto-seek!
		// parentDaughterDecayCorr = new double[0];
		IA_use = 3;
		elapsedTime = 180.0;// days
		timeSteps = 1;

		activity = 0.0;// Bq/m3 for external; Bq for internal
		exposureTime = elapsedTime;// 0.0;//only for external;
		dose_external = new double[0][0];// all nuclides!; adrenals.liver...etc
											// only from photons and
											// electrons=low LET; alpha stopped
											// at skin..not used!
		dose_internal = new double[0][0];// all nuclides!; adrenals.liver...etc
											// only from photons and
											// electrons=low LET; alpha stopped
											// at ingestion/inhalation location.
		organs_external = new String[0];
		organs_internal = new String[0];
		effectiveDoseTotal = 0.0;
		//totalDose=0.0;
		thyroidDoseTotal = 0.0;
		risk = 0.0;
	}

	/**
	 * Create decay information XLS file
	 */
	private void createDecayInfoExcelFile() {
		textArea.selectAll();
		textArea.replaceSelection("");

		String s = "";
		s = resources.getString("text.chainDecayInfo") + " \n";
		textArea.append(s);

		for (int i = 0; i < nuclideS.size(); i++) {
			Double yD = (Double) yieldD.elementAt(i);
			double yi = yD.doubleValue();
			String dmm = (String) decayModeMainS.elementAt(i);
			Double amD = (Double) atomicMassD.elementAt(i);
			double am = amD.doubleValue();
			Double gcD = (Double) gammaConstantD.elementAt(i);
			double gc = gcD.doubleValue();
			Double hlD = (Double) halfLifeD.elementAt(i);
			double hl = hlD.doubleValue();
			String hlu = (String) halfLifeUnitsS.elementAt(i);

			s = resources.getString("text.nuclide")// "Nuclide: "
					+ (String) nuclideS.elementAt(i)
					+ "; "
					+ resources.getString("text.atomicMass")// "; Atomic mass: "
					+ Convertor.formatNumber(am, 2)
					+ "; "
					+ resources.getString("text.halfLife")// "; Half life: "
					+ Convertor.formatNumberScientific(hl) + " "
					+ hlu
					+ "; "
					+ resources.getString("text.mainDecay")// "; Main decay: "
					+ dmm + "; "
					+ resources.getString("text.branchingRatio")// BR
					+ Convertor.formatNumberScientific(yi)
					+ "; "
					+ resources.getString("text.gammaConstant")// "; Gamma constant [Gy X m^2/(Bq X s)]: "
					+ Convertor.formatNumberScientific(gc) + "; " + " \n";
			textArea.append(s);
		}
		// s="------------------------"+" \n";
		// textArea.append(s);
		if (!excelB) {
			return;
		}
		try {
			WritableWorkbook workbook = Workbook.createWorkbook(new File(
					parentNuclide + "_decayInfo.xls"));
			WritableSheet sheet = workbook.createSheet("First Sheet", 0);
			// ------------------------------------------------
			Label label = new Label(0, 2, resources.getString("text.nuclide"));// "Nuclide");//
																				// col,row
			sheet.addCell(label);
			label = new Label(1, 2, resources.getString("text.atomicMass"));
			sheet.addCell(label);
			label = new Label(2, 2, resources.getString("text.gammaConstant1"));
			sheet.addCell(label);
			label = new Label(3, 2, resources.getString("text.mainDecay"));
			sheet.addCell(label);
			label = new Label(4, 2, resources.getString("text.halfLife"));
			sheet.addCell(label);
			label = new Label(5, 2, resources.getString("text.halfLifeUnits"));
			sheet.addCell(label);
			label = new Label(6, 2, resources.getString("text.branchingRatio"));// "Relative Yield");
			sheet.addCell(label);

			label = new Label(2, 3, resources.getString("text.gammaConstant2"));// "Gy X m^2/(Bq X s)");
			sheet.addCell(label);

			// label = new Label(0, 0,
			// "Relative yield of a daughter is related mainly to its parent!");
			// sheet.addCell(label);
			// ------------------------------------------------------------------
			for (int i = 0; i < nuclideS.size(); i++) {
				String nuc = (String) nuclideS.elementAt(i);
				Double yD = (Double) yieldD.elementAt(i);
				double yi = yD.doubleValue();
				String dmm = (String) decayModeMainS.elementAt(i);
				Double amD = (Double) atomicMassD.elementAt(i);
				double am = amD.doubleValue();
				Double gcD = (Double) gammaConstantD.elementAt(i);
				double gc = gcD.doubleValue();
				Double hlD = (Double) halfLifeD.elementAt(i);
				double hl = hlD.doubleValue();
				String hlu = (String) halfLifeUnitsS.elementAt(i);

				label = new Label(0, i + 4, nuc);
				sheet.addCell(label);
				jxl.write.Number number = new jxl.write.Number(1, i + 4, am);
				sheet.addCell(number);
				number = new jxl.write.Number(2, i + 4, gc);
				sheet.addCell(number);
				label = new Label(3, i + 4, dmm);
				sheet.addCell(label);
				number = new jxl.write.Number(4, i + 4, hl);
				sheet.addCell(number);
				label = new Label(5, i + 4, hlu);
				sheet.addCell(label);
				number = new jxl.write.Number(6, i + 4, yi);
				sheet.addCell(number);
			}
			// --------------------------------------------------
			// chains===================
			int offset = nuclideS.size() + 5;
			label = new Label(0, offset, resources.getString("xls.chains.br"));// "Chains and nuclide branching ratios (BR)");
			sheet.addCell(label);
			offset++;
			String sss = "";
			int offset_old = offset;

			for (int j = 0; j <= nchain; j++)// nchain
			{
				sss = resources.getString("xls.chains.nr")// "Chain #"
						+ Convertor.intToString(j + 1);
				offset = offset_old;
				label = new Label(j * 2, offset, sss);
				sheet.addCell(label);
				label = new Label(j * 2 + 1, offset,
						resources.getString("xls.br"));// "BR");
				sheet.addCell(label);

				offset++;
				for (int i = 0; i < ndaughterChainMax; i++) {
					if (nuc_chains[i][j].equals("-1")) {
						break;
					}
					label = new Label(j * 2, offset, nuc_chains[i][j]);
					sheet.addCell(label);
					jxl.write.Number number1 = new jxl.write.Number(j * 2 + 1,
							offset, br_chains[i][j]);
					sheet.addCell(number1);
					offset++;
				}
			}
			// =========================
			workbook.write();
			workbook.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	/**
	 * Retrieve radiation intensities.
	 */
	private void getRadiationYields() {
		int nnuc = nuclideS.size();
		int ndata = 0;
		nucInChain = new String[nnuc];
		for (int i = 0; i < nnuc; i++) {
			if (STOPCOMPUTATION)
				return;
			nucInChain[i] = (String) nuclideS.elementAt(i);
			Integer rn = (Integer) radLocN.elementAt(i);
			int rln = rn.intValue();
			ndata = Math.max(ndata, rln);
		}

		rcode = new String[ndata][nnuc];
		rtype = new String[ndata][nnuc];
		ryield = new double[ndata][nnuc];
		renergy = new double[ndata][nnuc];

		for (int i = 0; i < nnuc; i++) {
			if (STOPCOMPUTATION)
				return;
			Integer rl = (Integer) radLocI.elementAt(i);
			int rli = rl.intValue();
			Integer rn = (Integer) radLocN.elementAt(i);
			int rln = rn.intValue();
			Double yD = (Double) yieldD.elementAt(i);
			double yi = yD.doubleValue();

			readRad(rli, rln, i, yi, ndata);
			if (STOPCOMPUTATION)
				return;
		}
	}

	/**
	 * Recursively collect nuclide data for both parent and daughters <br>
	 * from radioactive chain decay.
	 * @param nuclide the nuclide
	 */
	private void collectDecayData(String nuclide) {
		if (STOPCOMPUTATION)
			return;
		String input = nuclide;

		Statement s = null;
		ResultSet rs = null;

		try {

			s = conn.createStatement();
			statements.add(s);

			String stmnt = "select ID, Nuclide, HalfLife, Dau1, Dau2,"
					+ " Yield1, Yield2, AtomicMass, GammaConstant, HalfLifeUnits,"
					+ " RadLoc, RadNum, DecayMode1, DecayMode2" + " from "
					+ icrpTable + " where Nuclide = " + "'" + input + "'";
			s.execute(stmnt);

			rs = s.getResultSet();
			resultsets.add(rs);

			if (rs != null)
				while (rs.next()) {
					String NuclideS = rs.getString(2);
					double hl = Convertor.stringToDouble(rs.getString(3));
					double amass = Convertor.stringToDouble(rs.getString(8));
					String hl_units = rs.getString(10);

					double gammaconstant = Convertor.stringToDouble(rs
							.getString(9));

					int dau1 = Convertor.stringToInt(rs.getString(4));
					int dau2 = Convertor.stringToInt(rs.getString(5));

					double yield1 = Convertor.stringToDouble(rs.getString(6));
					double yield2 = Convertor.stringToDouble(rs.getString(7));

					int radloc = Convertor.stringToInt(rs.getString(11));
					int radnum = Convertor.stringToInt(rs.getString(12));

					String dm1s = rs.getString(13);
					// String dm2s=rs.getString(14);
					// ----------------------------------------------------
					if ((dau1 != 0) && (dau2 != 0))
					// we have two channels of decay for current nuclide
					{
						y1_old.addElement(new Double(yield_current));
						// old yield store
						y2_next_chain.addElement(new Double(yield2));// same
																		// size
						if (colect_first_time) {
							parent_dual = true;
						}
					}

					// current yield...always via 1st (high prob decay)
					if (yield1 == 0)
					// full decay to stable isotope..see ICRP table
					{
						yield1 = 1;
						yield2 = 0;
					}

					yield_current = yield_current * yield1;

					if ((dau1 != 0) && (dau2 != 0))
					// store the fix 2channel nuclides
					{
						y1_fix.addElement(new Double(yield_current));
						index.addElement(new Integer(nuclideS.size()));
						// ==========================
						nsave.addElement(ndaughter);
						yield2_readD.addElement(new Double(yield2));

					}

					// HERE WE COLLECT DATA WE NEED/////
					nuclideS.addElement(NuclideS);
					if (colect_first_time)
					// parent..always 1
					{
						yieldD.addElement(new Double(1.0));
						colect_first_time = false;
					} else {
						yieldD.addElement(new Double(yield_current));
					}
					decayModeMainS.addElement(dm1s);
					atomicMassD.addElement(new Double(amass));
					gammaConstantD.addElement(new Double(gammaconstant));
					radLocI.addElement(new Integer(radloc));
					radLocN.addElement(new Integer(radnum));
					halfLifeD.addElement(new Double(hl));
					halfLifeUnitsS.addElement(hl_units);
					// =======================================
					if ((nchain < nmax_chains) && (ndaughter < nmax_daughters)) {
						nuc_chains[ndaughter][nchain] = NuclideS;
						hlu_chains[ndaughter][nchain] = hl_units;
						hl_chains[ndaughter][nchain] = hl;
						am_chains[ndaughter][nchain] = amass;
						br_chains[ndaughter][nchain] = yield1;
						// start with yield 1
					}
					ndaughter++;
					ndaughterChainMax = Math.max(ndaughterChainMax, ndaughter);

					// END DATA COLLECTION/////

					if ((dau1 == 0) && (y2_next_chain.size() != 0))
					// we have a comeback: another chain!
					{
						Double y1old = (Double) y1_old
								.elementAt(y1_old.size() - 1);
						Double y2next = (Double) y2_next_chain
								.elementAt(y2_next_chain.size() - 1);
						yield_current = y1old.doubleValue();
						yield_current = yield_current * y2next.doubleValue();
						// ------------
						Double y1fix = (Double) y1_fix
								.elementAt(y1_fix.size() - 1);
						double y1fixd = y1fix.doubleValue();
						double yfix = y1fixd + yield_current;
						Integer indx = (Integer) index
								.elementAt(index.size() - 1);
						int indxi = indx.intValue();
						if ((y2_next_chain.size() == 1) && (parent_dual)) {
							// do nothin..it's parent comeback!!!
						} else {
							yieldD.setElementAt(new Double(yfix), indxi);
						}
						// ------------------------------
						// now we remove the current comeback in order to take
						// another!
						y1_old.removeElementAt(y1_old.size() - 1);
						y2_next_chain.removeElementAt(y2_next_chain.size() - 1);
						// ------
						y1_fix.removeElementAt(y1_fix.size() - 1);
						index.removeElementAt(index.size() - 1);
						// ============================
						Integer insaveI = (Integer) nsave.elementAt(nsave
								.size() - 1);
						int insave = insaveI.intValue();
						Double y2readD = (Double) yield2_readD
								.elementAt(yield2_readD.size() - 1);
						double y2read = y2readD.doubleValue();
						// System.out.println("!!!"+nucVecs+"   "+insave+"  "+ndaughter);
						yield2_readD.removeElementAt(yield2_readD.size() - 1);
						nsave.removeElementAt(nsave.size() - 1);
						nchain++;
						for (int i = 0; i <= insave; i++) {
							if ((i < nmax_daughters) && (nchain < nmax_chains)) {
								nuc_chains[i][nchain] = nuc_chains[i][nchain - 1];
								hlu_chains[i][nchain] = hlu_chains[i][nchain - 1];
								hl_chains[i][nchain] = hl_chains[i][nchain - 1];
								am_chains[i][nchain] = am_chains[i][nchain - 1];
								br_chains[i][nchain] = br_chains[i][nchain - 1];
							}
						}
						if ((insave < nmax_daughters) && (nchain < nmax_chains)) {
							br_chains[insave][nchain] = y2read;// fix last BR
						}
						ndaughter = insave + 1;
					}

					if (dau1 != 0) {

						String ids = Convertor.intToString(dau1 - 1);
						String ss = "select ID,Nuclide from " + icrpTable
								+ " where ID = " + ids;
						Statement s1 = conn.createStatement();
						s1.execute(ss);
						ResultSet rs1 = s1.getResultSet();
						if (rs1 != null)
							while (rs1.next()) {
								String output = rs1.getString(2);
								collectDecayData(output);
							}
					}

					// next in series but with LESS probability
					if (dau2 != 0) {

						String ids2 = Convertor.intToString(dau2 - 1);
						String ss2 = "select ID,Nuclide from " + icrpTable
								+ " where ID = " + ids2;
						Statement s2 = conn.createStatement();
						s2.execute(ss2);
						ResultSet rs2 = s2.getResultSet();
						if (rs2 != null)
							while (rs2.next())
							// can be rs.next()..NO LOOP!
							{
								String output2 = rs2.getString(2);
								collectDecayData(output2);
							}
					}

				}

		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	/**
	 * Some finalization. Format decay by removing duplicates.
	 */
	private void formatDecay() {
		if (nuclideS.size() < 2) {
			return;// do nothing
		}

		int i = 0;
		int j = 0;
		while (i < nuclideS.size() - 1) {
			if (STOPCOMPUTATION)
				return;
			j = i + 1;
			while (j < nuclideS.size()) {
				if (STOPCOMPUTATION)
					return;
				String nsi = (String) nuclideS.elementAt(i);
				String nsj = (String) nuclideS.elementAt(j);

				if (nsi.equals(nsj)) {
					Double yi = (Double) yieldD.elementAt(i);
					double yid = yi.doubleValue();
					Double yj = (Double) yieldD.elementAt(j);
					double yjd = yj.doubleValue();
					double yinew = yid + yjd;
					yieldD.setElementAt(new Double(yinew), i);

					yieldD.removeElementAt(j);
					nuclideS.removeElementAt(j);
					decayModeMainS.removeElementAt(j);
					atomicMassD.removeElementAt(j);
					gammaConstantD.removeElementAt(j);
					radLocI.removeElementAt(j);
					radLocN.removeElementAt(j);
					halfLifeD.removeElementAt(j);
					halfLifeUnitsS.removeElementAt(j);
				}

				j++;
			}
			i++;
		}

		// creating chains
		for (j = 0; j <= nchain; j++) {
			if (STOPCOMPUTATION)
				return;
			// nchain
			for (i = 0; i < ndaughterChainMax; i++) {
				if (STOPCOMPUTATION)
					return;
				if (j < nmax_chains)
					if (nuc_chains[i][j] == null) {
						if ((i < nmax_daughters) && (j < nmax_chains)) {
							nuc_chains[i][j] = "-1";
							hlu_chains[i][j] = "-1";
							am_chains[i][j] = -1;
							hl_chains[i][j] = -1;
							br_chains[i][j] = -1;
							a_chains[i][j] = -1;
							a_chains_equilibrum[i][j] = -1;
							at_chains[i][j] = -1;
						}
					}
			}
		}
		// ----------------------------------------

	}

	/**
	 * Formatting half life. Return half life in seconds.
	 * 
	 * @param hl hl
	 * @param hlu hlu
	 * @return the result
	 */
	// from master library ICRP38 we must convert HalfLife in SI units [seconds]
	public static double formatHalfLife(double hl, String hlu) {
		double result = hl;// return this value if hlu=seconds!!
		if (hlu.equals("y")) {
			result = hl * 365.25 * 24.0 * 3600.0;
		} else if (hlu.equals("d")) {
			result = hl * 24.0 * 3600.0;
		} else if (hlu.equals("h")) {
			result = hl * 3600.0;
		} else if (hlu.equals("m")) {
			result = hl * 60.0;
		} else if (hlu.equals("ms")) {
			result = hl / 1000.0;
		} else if (hlu.equals("us")) {
			result = hl / 1000000.0;
		}

		return result;
	}

	/**
	 * Retrieve the radiation type.
	 * 
	 * @param ircode ircode
	 * @return the result
	 */
	private String getRadiationType(int ircode) {
		String result = "";

		if (ircode == 1) {
			result = resources.getString("radiation.gamma");
		} else if (ircode == 2) {
			result = resources.getString("radiation.x");
		} else if (ircode == 3) {
			result = resources.getString("radiation.annihilationQuanta");
		} else if (ircode == 4) {
			result = resources.getString("radiation.betap");
		}// "Beta+ particle"; }
		else if (ircode == 5) {
			result = resources.getString("radiation.betan");
		}// "Beta- particle"; }
		else if (ircode == 6) {
			result = resources.getString("radiation.ice");
		}// "Internal conversion electron"; }
		else if (ircode == 7) {
			result = resources.getString("radiation.auger");
		}// "Auger electron"; }
		else if (ircode == 8) {
			result = resources.getString("radiation.alpha");
		}// "Alpha particle"; }

		return result;
	}

	/**
	 * Read radiation data from master library.
	 * 
	 * @param iradloc iradloc
	 * @param nrad nrad
	 * @param nucindex nucindex
	 * @param BR BR
	 * @param ndata ndata
	 */
	private void readRad(int iradloc, int nrad, int nucindex, double BR,
			int ndata) {
		Statement s = null;
		ResultSet rs = null;

		try {

			s = conn.createStatement();
			statements.add(s);

			String sttmnt = "select ID, NI, HY, LE" + " from " + icrpRadTable
					+ " where ID > " + Convertor.intToString(iradloc)
					+ " AND ID <= " + Convertor.intToString(iradloc + nrad);

			s.execute(sttmnt);

			rs = s.getResultSet();
			resultsets.add(rs);

			int index = 0;
			if (rs != null)
				while (rs.next()) {
					if (STOPCOMPUTATION)
						return;
					// String IDs=rs.getString(1);
					String IRs = rs.getString(2);
					String Ys = rs.getString(3);
					String Es = rs.getString(4);
					// System.out.println("IDs: "+IDs+" ;IR: "+IRs+" ;Ys: "+Ys+" ;Es: "+Es+
					// "  inx "+nucindex);

					rcode[index][nucindex] = IRs;
					rtype[index][nucindex] = getRadiationType(Convertor
							.stringToInt(IRs));
					double yd = Convertor.stringToDouble(Ys);
					ryield[index][nucindex] = yd * BR;
					renergy[index][nucindex] = Convertor.stringToDouble(Es);

					index++;
				}
			// ---fill with -1 the remaining rows!
			if (index < ndata) {
				for (int i = index; i < ndata; i++) {
					if (STOPCOMPUTATION)
						return;
					rcode[i][nucindex] = "-1";
					rtype[i][nucindex] = "-1";
					ryield[i][nucindex] = -1;
					renergy[i][nucindex] = -1;
				}
			}

		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	/**
	 * Display the results for internal exposure.
	 * @param tableUsed tableUsed
	 */
	private void displayInternalResults(String tableUsed) {
		String s = "";
		for (int i = 0; i < dose_internal.length; i++) {
			//s = "--------------------------------" + " \n";
			if (!title1B){
				s = "-------------------------------" + " \n";
				textArea.append(s);
				s = resources.getString("Dosimetry") + " \n\n";
				title1B=true;
			} else {
				s = "-------------------------------" + " \n";
			}
			textArea.append(s);
			for (int j = 0; j < 27; j++) {
				s = resources.getString("text.nuclide")// "Nuclide: "
						+ nuclides_all[i]
						+ resources.getString("text.dose")// "; Dose [Gy or Sv]: "
						+ organs_internal[j]
						+ "  "
						+ Convertor.formatNumberScientific(dose_internal[i][j])
						+ " \n";
				textArea.append(s);
			}
		}
		// System.out.println("EffDoseTot= "+formatNumberScientific(effectiveDoseTotal)+
		// " ; cases of fatal cancers/million pop ="+ risk);
		s = "--------------------------------" + " \n";
		textArea.append(s);
		//s = resources.getString("text.dose.total")// "Effective dose (Total) [Sv]= "
		//		+ Convertor.formatNumberScientific(totalDose)+"\n";
		//textArea.append(s);
		s = resources.getString("text.dose.thyroid")// "Effective dose (Total) [Sv]= "
				+ Convertor.formatNumberScientific(thyroidDoseTotal)+"\n";
		textArea.append(s);
		//=================================================================
		s = resources.getString("text.dose.eff")// "Effective dose (Total) [Sv]= "
				+ Convertor.formatNumberScientific(effectiveDoseTotal)
				+ "; "
				+ resources.getString("text.fatal.cases")// "; Cases of fatal cancers/1 million population = "
				+ Math.rint(risk) + " \n";
		textArea.append(s);

		if (excelB) {
			try {
				WritableWorkbook workbook = Workbook.createWorkbook(new File(
						parentNuclide + "_InternalExposure.xls"));
				WritableSheet sheet = workbook.createSheet("First Sheet", 0);
				
				Label label0 = new Label(0, 0, resources.getString("Dosimetry"));
				sheet.addCell(label0);
				
				// ------------------------------------------------
				Label label = new Label(0, 1, resources.getString("xls.organ"));// "Organ");//
																				// col,row
				sheet.addCell(label);
				for (int i = 0; i < dose_internal.length; i++) {
					label = new Label(i + 1, 1, nuclides_all[i]);// col,row
					sheet.addCell(label);
				}
				// -----------------------------------------------
				int nrows = 2;// current rows to write
				for (int j = 0; j < 27; j++) {
					// create Excel File
					label = new Label(0, nrows, organs_internal[j]);
					sheet.addCell(label);
					for (int i = 0; i < dose_internal.length; i++) {
						jxl.write.Number number = new jxl.write.Number(i + 1,
								nrows, dose_internal[i][j]);
						sheet.addCell(number);
					}
					// jxl.write.Number number = new jxl.write.Number(2, nrows,
					// ryield[i][j]);sheet.addCell(number);
					// number = new jxl.write.Number(3, nrows,
					// renergy[i][j]);sheet.addCell(number);
					nrows++;
				}
				
				nrows++;
				label = new Label(0, nrows,
						resources.getString("text.dose.thyroid"));// "Effective dose (total) [Sv]: ");
				sheet.addCell(label);
				// jxl.write.Number number =
				// new jxl.write.Number(5, nrows, effectiveDoseTotal);
				// sheet.addCell(number);
				label = new Label(5, nrows,
						Convertor.formatNumberScientific(thyroidDoseTotal));
				sheet.addCell(label);
				
				nrows++;
				label = new Label(0, nrows,
						resources.getString("text.dose.eff"));// "Effective dose (total) [Sv]: ");
				sheet.addCell(label);
				// jxl.write.Number number =
				// new jxl.write.Number(5, nrows, effectiveDoseTotal);
				// sheet.addCell(number);
				label = new Label(5, nrows,
						Convertor.formatNumberScientific(effectiveDoseTotal));
				sheet.addCell(label);
				
				//============
				/*nrows++;
				label = new Label(0, nrows,
						resources.getString("text.dose.total"));// "Effective dose (total) [Sv]: ");
				sheet.addCell(label);
				// jxl.write.Number number =
				// new jxl.write.Number(5, nrows, effectiveDoseTotal);
				// sheet.addCell(number);
				label = new Label(5, nrows,
						Convertor.formatNumberScientific(totalDose));
				sheet.addCell(label);*/
				//=============

				nrows++;
				label = new Label(0, nrows,
						resources.getString("text.fatal.cases"));// "Fatal cancer risk [cases/1 million population]: ");
				sheet.addCell(label);
				jxl.write.Number number = new jxl.write.Number(5, nrows,
						Math.rint(risk));
				sheet.addCell(number);
				nrows++;
				nrows++;
				label = new Label(0, nrows,
						resources.getString("xls.estimation.case")// "Estimation performed in case of: "
								+ tableUsed);
				sheet.addCell(label);

				// -----------------------------------------------
				workbook.write();
				workbook.close();
			} catch (Exception err) {
				err.printStackTrace();
			}

		}
	}

	/**
	 * Perform internal ingestion computations.
	 * @param tablename tablename
	 */
	private void computeInternalIngestion(String tablename) {
		dose_internal = new double[nuclides_all.length][27];
		organs_internal = new String[27];
		double[] act = new double[nuclides_all.length];
		for (int i = 0; i < nuclides_all.length; i++) {
			if (IA_use == IA_sec) {
				act[i] = activity_secular[i];
			} else if (IA_use == IA_elapsed) {
				act[i] = activity_chain[i];
			} else if (IA_use == IA_norm) {
				act[i] = activity_chain_scaled[i];
			} else if (IA_use == IA_eq) {
				act[i] = activity_chain_equilibrum[i];
			}

			if (!parentCh.isSelected()) {
				if (i > 0) {
					act[i] = 0.0;
				}
			}

		}

		try {
			// Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			// String datas="Data";//resources.getString("data.load");//"Data";
			// String currentDir=System.getProperty("user.dir");
			// String file_sep=System.getProperty("file.separator");
			// String opens=currentDir+file_sep+datas;
			// String fileS="icrp72.mdb";
			// String filename = opens+file_sep+fileS;
			// String database =
			// "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			// database+= filename.trim() + ";DriverID=22;READONLY=true}"; //
			// add on to the end
			// now we can get the connection from the DriverManager
			// Connection con1 = DriverManager.getConnection( database ,"","");
			String datas = resources.getString("data.load");// Data
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;
			String dbName = internalDB;// the name of the database
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			// ----------------
			effectiveDoseTotal = 0.0;thyroidDoseTotal=0.0;//double tdose=0.0;
			for (int i = 0; i < nuclides_all.length; i++) {
				Statement s = con1.createStatement();
				String input = nuclides_all[i];
				String statement = "select Nuclide, Adrenals, UrinaryBladder, "
						+ "BoneSurface, Brain, Breast, Esophagus, Stomach, "
						+ "SmallIntestine, UpperLargeIntestine, LowerLargeIntestine, Colon, "
						+ "Kidneys, Liver, Muscle, Ovaries, Pancreas, RedMarrow, ExtratrachialAirways, "
						+ "Lungs, Skin, Spleen, Testes, Thymus, Thyroid, Uterus, Remainder, E"
						+ " from " + tablename + " where Nuclide = " + "'"
						+ input + "'";

				s.execute(statement);

				ResultSet rs = s.getResultSet();
				if (rs != null) // if rs == null, then there is no ResultSet to
								// view
					while (rs.next()) // this will step through our data
										// row-by-row.
					{
						String adrenalsS = rs.getString(2);
						organs_internal[0] = resources.getString("Adrenals");
						String urinaryBladderS = rs.getString(3);
						organs_internal[1] = resources
								.getString("UrinaryBladder");
						String boneSurfaceS = rs.getString(4);
						organs_internal[2] = resources.getString("BoneSurface");
						String brainS = rs.getString(5);
						organs_internal[3] = resources.getString("Brain");
						String breastS = rs.getString(6);
						organs_internal[4] = resources.getString("Breast");
						String esophagusS = rs.getString(7);
						organs_internal[5] = resources.getString("Esophagus");
						String stomachS = rs.getString(8);
						organs_internal[6] = resources.getString("Stomach");
						String smallIntestineS = rs.getString(9);
						organs_internal[7] = resources
								.getString("SmallIntestine");
						String upperLargeIntestineS = rs.getString(10);
						organs_internal[8] = resources
								.getString("UpperLargeIntestine");
						String lowerLargeIntestineS = rs.getString(11);
						organs_internal[9] = resources
								.getString("LowerLargeIntestine");
						String colonS = rs.getString(12);
						organs_internal[10] = resources.getString("Colon");
						String kidneysS = rs.getString(13);
						organs_internal[11] = resources.getString("Kidneys");
						String liverS = rs.getString(14);
						organs_internal[12] = resources.getString("Liver");
						String muscleS = rs.getString(15);
						organs_internal[13] = resources.getString("Muscle");
						String ovariesS = rs.getString(16);
						organs_internal[14] = resources.getString("Ovaries");
						String pancreasS = rs.getString(17);
						organs_internal[15] = resources.getString("Pancreas");
						String redMarrowS = rs.getString(18);
						organs_internal[16] = resources.getString("RedMarrow");
						String eAirWaysS = rs.getString(19);
						organs_internal[17] = resources
								.getString("ExtratrachialAirways");
						String lungsS = rs.getString(20);
						organs_internal[18] = resources.getString("Lungs");
						String skinS = rs.getString(21);
						organs_internal[19] = resources.getString("Skin");
						String spleenS = rs.getString(22);
						organs_internal[20] = resources.getString("Spleen");
						String testesS = rs.getString(23);
						organs_internal[21] = resources.getString("Testes");
						String thymusS = rs.getString(24);
						organs_internal[22] = resources.getString("Thymus");
						String thyroidS = rs.getString(25);
						organs_internal[23] = resources.getString("Thyroid");
						String uterusS = rs.getString(26);
						organs_internal[24] = resources.getString("Uterus");
						String remainderS = rs.getString(27);
						organs_internal[25] = resources.getString("Remainder");
						String ES = rs.getString(28);
						organs_internal[26] = resources
								.getString("Effectivedose");

						dose_internal[i][0] = activity * act[i]
								* Convertor.stringToDouble(adrenalsS);
						dose_internal[i][1] = activity * act[i]
								* Convertor.stringToDouble(urinaryBladderS);
						dose_internal[i][2] = activity * act[i]
								* Convertor.stringToDouble(boneSurfaceS);
						dose_internal[i][3] = activity * act[i]
								* Convertor.stringToDouble(brainS);
						dose_internal[i][4] = activity * act[i]
								* Convertor.stringToDouble(breastS);
						dose_internal[i][5] = activity * act[i]
								* Convertor.stringToDouble(esophagusS);
						dose_internal[i][6] = activity * act[i]
								* Convertor.stringToDouble(stomachS);
						dose_internal[i][7] = activity * act[i]
								* Convertor.stringToDouble(smallIntestineS);
						dose_internal[i][8] = activity
								* act[i]
								* Convertor
										.stringToDouble(upperLargeIntestineS);
						dose_internal[i][9] = activity
								* act[i]
								* Convertor
										.stringToDouble(lowerLargeIntestineS);
						dose_internal[i][10] = activity * act[i]
								* Convertor.stringToDouble(colonS);
						dose_internal[i][11] = activity * act[i]
								* Convertor.stringToDouble(kidneysS);
						dose_internal[i][12] = activity * act[i]
								* Convertor.stringToDouble(liverS);
						dose_internal[i][13] = activity * act[i]
								* Convertor.stringToDouble(muscleS);
						dose_internal[i][14] = activity * act[i]
								* Convertor.stringToDouble(ovariesS);
						dose_internal[i][15] = activity * act[i]
								* Convertor.stringToDouble(pancreasS);
						dose_internal[i][16] = activity * act[i]
								* Convertor.stringToDouble(redMarrowS);
						dose_internal[i][17] = activity * act[i]
								* Convertor.stringToDouble(eAirWaysS);
						dose_internal[i][18] = activity * act[i]
								* Convertor.stringToDouble(lungsS);
						dose_internal[i][19] = activity * act[i]
								* Convertor.stringToDouble(skinS);
						dose_internal[i][20] = activity * act[i]
								* Convertor.stringToDouble(spleenS);
						dose_internal[i][21] = activity * act[i]
								* Convertor.stringToDouble(testesS);
						dose_internal[i][22] = activity * act[i]
								* Convertor.stringToDouble(thymusS);
						dose_internal[i][23] = activity * act[i]
								* Convertor.stringToDouble(thyroidS);
						dose_internal[i][24] = activity * act[i]
								* Convertor.stringToDouble(uterusS);
						dose_internal[i][25] = activity * act[i]
								* Convertor.stringToDouble(remainderS);
						dose_internal[i][26] = activity * act[i]
								* Convertor.stringToDouble(ES);
						
						/*tdose = 0.0;
						for (int kk = 0; kk <= 25; kk++){
							tdose = tdose + dose_internal[i][kk];
						}*/
					}
				effectiveDoseTotal = effectiveDoseTotal + dose_internal[i][26];
				//totalDose = totalDose + tdose;
				thyroidDoseTotal = thyroidDoseTotal + dose_internal[i][23];
			}
			// -------------------
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	/**
	 * Perform internal inhalation computations.
	 * @param tablename tablename
	 */
	private void computeInternalInhalation(String tablename) {
		dose_internal = new double[nuclides_all.length][27];
		organs_internal = new String[27];
		double[] act = new double[nuclides_all.length];
		for (int i = 0; i < nuclides_all.length; i++) {
			if (IA_use == IA_sec) {
				act[i] = activity_secular[i];
			} else if (IA_use == IA_elapsed) {
				act[i] = activity_chain[i];
			} else if (IA_use == IA_norm) {
				act[i] = activity_chain_scaled[i];
			} else if (IA_use == IA_eq) {
				act[i] = activity_chain_equilibrum[i];
			}

			if (!parentCh.isSelected()) {
				if (i > 0) {
					act[i] = 0.0;
				}
			}

		}

		try {
			// Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			// String datas="Data";//resources.getString("data.load");//"Data";
			// String currentDir=System.getProperty("user.dir");
			// String file_sep=System.getProperty("file.separator");
			// String opens=currentDir+file_sep+datas;
			// String fileS="icrp72.mdb";
			// String filename = opens+file_sep+fileS;
			// String database =
			// "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			// database+= filename.trim() + ";DriverID=22;READONLY=true}"; //
			// add on to the end
			// now we can get the connection from the DriverManager
			// Connection con1 = DriverManager.getConnection( database ,"","");
			String datas = resources.getString("data.load");// Data
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;
			String dbName = internalDB;// the name of the database
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			// ----------------
			effectiveDoseTotal = 0.0; thyroidDoseTotal =0.0; //double tdose = 0.0;
			for (int i = 0; i < nuclides_all.length; i++) {
				Statement s = con1.createStatement();
				String input = nuclides_all[i];
				String statement = "select Nuclide, Adrenals, UrinaryBladder, "
						+ "BoneSurface, Brain, Breast, Esophagus, Stomach, "
						+ "SmallIntestine, UpperLargeIntestine, LowerLargeIntestine, Colon, "
						+ "Kidneys, Liver, Muscle, Ovaries, Pancreas, RedMarrow, ExtratrachialAirways, "
						+ "Lungs, Skin, Spleen, Testes, Thymus, Thyroid, Uterus, Remainder, E"
						+ " from " + tablename + " where Nuclide = " + "'"
						+ input + "'" + " AND Type = 'M'";// Medium

				s.execute(statement);

				ResultSet rs = s.getResultSet(); // get any ResultSet that came
													// from our query
				// int index=0;
				if (rs != null) // if rs == null, then there is no ResultSet to
								// view
					while (rs.next()) // this will step through our data
										// row-by-row.
					{
						String adrenalsS = rs.getString(2);
						organs_internal[0] = resources.getString("Adrenals");
						String urinaryBladderS = rs.getString(3);
						organs_internal[1] = resources
								.getString("UrinaryBladder");
						String boneSurfaceS = rs.getString(4);
						organs_internal[2] = resources.getString("BoneSurface");
						String brainS = rs.getString(5);
						organs_internal[3] = resources.getString("Brain");
						String breastS = rs.getString(6);
						organs_internal[4] = resources.getString("Breast");
						String esophagusS = rs.getString(7);
						organs_internal[5] = resources.getString("Esophagus");
						String stomachS = rs.getString(8);
						organs_internal[6] = resources.getString("Stomach");
						String smallIntestineS = rs.getString(9);
						organs_internal[7] = resources
								.getString("SmallIntestine");
						String upperLargeIntestineS = rs.getString(10);
						organs_internal[8] = resources
								.getString("UpperLargeIntestine");
						String lowerLargeIntestineS = rs.getString(11);
						organs_internal[9] = resources
								.getString("LowerLargeIntestine");
						String colonS = rs.getString(12);
						organs_internal[10] = resources.getString("Colon");
						String kidneysS = rs.getString(13);
						organs_internal[11] = resources.getString("Kidneys");
						String liverS = rs.getString(14);
						organs_internal[12] = resources.getString("Liver");
						String muscleS = rs.getString(15);
						organs_internal[13] = resources.getString("Muscle");
						String ovariesS = rs.getString(16);
						organs_internal[14] = resources.getString("Ovaries");
						String pancreasS = rs.getString(17);
						organs_internal[15] = resources.getString("Pancreas");
						String redMarrowS = rs.getString(18);
						organs_internal[16] = resources.getString("RedMarrow");
						String eAirWaysS = rs.getString(19);
						organs_internal[17] = resources
								.getString("ExtratrachialAirways");
						String lungsS = rs.getString(20);
						organs_internal[18] = resources.getString("Lungs");
						String skinS = rs.getString(21);
						organs_internal[19] = resources.getString("Skin");
						String spleenS = rs.getString(22);
						organs_internal[20] = resources.getString("Spleen");
						String testesS = rs.getString(23);
						organs_internal[21] = resources.getString("Testes");
						String thymusS = rs.getString(24);
						organs_internal[22] = resources.getString("Thymus");
						String thyroidS = rs.getString(25);
						organs_internal[23] = resources.getString("Thyroid");
						String uterusS = rs.getString(26);
						organs_internal[24] = resources.getString("Uterus");
						String remainderS = rs.getString(27);
						organs_internal[25] = resources.getString("Remainder");
						String ES = rs.getString(28);
						organs_internal[26] = resources
								.getString("Effectivedose");

						dose_internal[i][0] = activity * act[i]
								* Convertor.stringToDouble(adrenalsS);
						dose_internal[i][1] = activity * act[i]
								* Convertor.stringToDouble(urinaryBladderS);
						dose_internal[i][2] = activity * act[i]
								* Convertor.stringToDouble(boneSurfaceS);
						dose_internal[i][3] = activity * act[i]
								* Convertor.stringToDouble(brainS);
						dose_internal[i][4] = activity * act[i]
								* Convertor.stringToDouble(breastS);
						dose_internal[i][5] = activity * act[i]
								* Convertor.stringToDouble(esophagusS);
						dose_internal[i][6] = activity * act[i]
								* Convertor.stringToDouble(stomachS);
						dose_internal[i][7] = activity * act[i]
								* Convertor.stringToDouble(smallIntestineS);
						dose_internal[i][8] = activity
								* act[i]
								* Convertor
										.stringToDouble(upperLargeIntestineS);
						dose_internal[i][9] = activity
								* act[i]
								* Convertor
										.stringToDouble(lowerLargeIntestineS);
						dose_internal[i][10] = activity * act[i]
								* Convertor.stringToDouble(colonS);
						dose_internal[i][11] = activity * act[i]
								* Convertor.stringToDouble(kidneysS);
						dose_internal[i][12] = activity * act[i]
								* Convertor.stringToDouble(liverS);
						dose_internal[i][13] = activity * act[i]
								* Convertor.stringToDouble(muscleS);
						dose_internal[i][14] = activity * act[i]
								* Convertor.stringToDouble(ovariesS);
						dose_internal[i][15] = activity * act[i]
								* Convertor.stringToDouble(pancreasS);
						dose_internal[i][16] = activity * act[i]
								* Convertor.stringToDouble(redMarrowS);
						dose_internal[i][17] = activity * act[i]
								* Convertor.stringToDouble(eAirWaysS);
						dose_internal[i][18] = activity * act[i]
								* Convertor.stringToDouble(lungsS);
						dose_internal[i][19] = activity * act[i]
								* Convertor.stringToDouble(skinS);
						dose_internal[i][20] = activity * act[i]
								* Convertor.stringToDouble(spleenS);
						dose_internal[i][21] = activity * act[i]
								* Convertor.stringToDouble(testesS);
						dose_internal[i][22] = activity * act[i]
								* Convertor.stringToDouble(thymusS);
						dose_internal[i][23] = activity * act[i]
								* Convertor.stringToDouble(thyroidS);
						dose_internal[i][24] = activity * act[i]
								* Convertor.stringToDouble(uterusS);
						dose_internal[i][25] = activity * act[i]
								* Convertor.stringToDouble(remainderS);
						dose_internal[i][26] = activity * act[i]
								* Convertor.stringToDouble(ES);
						
						/*tdose = 0.0;
						for (int kk = 0; kk <= 25; kk++){
							tdose = tdose + dose_internal[i][kk];
						}*/
					}
				effectiveDoseTotal = effectiveDoseTotal + dose_internal[i][26];
				//totalDose = totalDose + tdose;
				thyroidDoseTotal = thyroidDoseTotal + dose_internal[i][23];
			}
			// -------------------
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	/**
	 * Display the results for external exposure.
	 */
	private void displayExternalResults() {
		String s = "";
		for (int i = 0; i < dose_external.length; i++) {
			//s = "-------------------------------" + " \n";
			if (!title1B){
				s = "-------------------------------" + " \n";
				textArea.append(s);
				s = resources.getString("Dosimetry") + " \n\n";
				title1B=true;
			} else {
				s = "-------------------------------" + " \n";
			}
			textArea.append(s);
			for (int j = 0; j < 24; j++) {
				// System.out.println
				// ("Nuc: "+nuclides_all[i]+" index "+j+"  "+dose_external[i][j]);
				s = resources.getString("text.nuclide")// "Nuclide: "
						+ nuclides_all[i]
						+ resources.getString("text.dose")// "; Dose [Gy or Sv]: "
						+ organs_external[j]
						+ "  "
						+ Convertor.formatNumberScientific(dose_external[i][j])
						+ " \n";
				textArea.append(s);
			}
		}
		s = "--------------------------------" + " \n";
		textArea.append(s);

		// System.out.println("EffDoseTot= "+formatNumberScientific(effectiveDoseTotal)+
		// " ; cases of fatal cancers/million pop ="+ risk);
		//s = resources.getString("text.dose.total")// "Effective dose (Total) [Sv]= "
		//		+ Convertor.formatNumberScientific(totalDose)+"\n";
		//textArea.append(s);
		
		s = resources.getString("text.dose.thyroid")// "Effective dose (Total) [Sv]= "
				+ Convertor.formatNumberScientific(thyroidDoseTotal)+"\n";
		textArea.append(s);
		
		s = resources.getString("text.dose.eff")// "Effective dose (Total) [Sv]= "
				+ Convertor.formatNumberScientific(effectiveDoseTotal)
				+ "; "
				+ resources.getString("text.fatal.cases")// "; Cases of fatal cancers/1 million population = "
				+ Math.rint(risk) + " \n";
		textArea.append(s);

		if (excelB) {
			try {
				WritableWorkbook workbook = Workbook.createWorkbook(new File(
						parentNuclide + "_ExternalExposure.xls"));
				WritableSheet sheet = workbook.createSheet("First Sheet", 0);
				//================
				Label label0 = new Label(0, 0, resources.getString("Dosimetry"));
				sheet.addCell(label0);
				//================
				// ------------------------------------------------
				Label label = new Label(0, 1, resources.getString("xls.organ"));// "Organ");//
																				// col,row
				sheet.addCell(label);
				for (int i = 0; i < dose_external.length; i++) {
					label = new Label(i + 1, 1, nuclides_all[i]);// col,row
					sheet.addCell(label);
				}
				// -----------------------------------------------
				int nrows = 2;// current rows to write
				for (int j = 0; j < 24; j++) {
					// create Excel File
					label = new Label(0, nrows, organs_external[j]);
					sheet.addCell(label);
					for (int i = 0; i < dose_external.length; i++) {
						jxl.write.Number number = new jxl.write.Number(i + 1,
								nrows, dose_external[i][j]);
						sheet.addCell(number);
					}
					// jxl.write.Number number = new jxl.write.Number(2, nrows,
					// ryield[i][j]);sheet.addCell(number);
					// number = new jxl.write.Number(3, nrows,
					// renergy[i][j]);sheet.addCell(number);
					nrows++;
				}
				
				nrows++;
				label = new Label(0, nrows,
						resources.getString("text.dose.thyroid"));// "Effective dose (total) [Sv]: ");
				sheet.addCell(label);
				// jxl.write.Number number =
				// new jxl.write.Number(5, nrows, effectiveDoseTotal);
				// sheet.addCell(number);
				label = new Label(5, nrows,
						Convertor.formatNumberScientific(thyroidDoseTotal));
				sheet.addCell(label);
				
				nrows++;
				label = new Label(0, nrows,
						resources.getString("text.dose.eff"));// "Effective dose (total) [Sv]: ");
				sheet.addCell(label);
				// jxl.write.Number number =
				// new jxl.write.Number(5, nrows, effectiveDoseTotal);
				// sheet.addCell(number);
				label = new Label(5, nrows,
						Convertor.formatNumberScientific(effectiveDoseTotal));
				sheet.addCell(label);

				//============
				/*nrows++;
				label = new Label(0, nrows,
						resources.getString("text.dose.total"));// "Effective dose (total) [Sv]: ");
				sheet.addCell(label);
				// jxl.write.Number number =
				// new jxl.write.Number(5, nrows, effectiveDoseTotal);
				// sheet.addCell(number);
				label = new Label(5, nrows,
						Convertor.formatNumberScientific(totalDose));
				sheet.addCell(label);*/
				//=============
				
				nrows++;
				label = new Label(0, nrows,
						resources.getString("text.fatal.cases"));// "Fatal cancer risk [cases/1 million population]: ");
				sheet.addCell(label);
				jxl.write.Number number = new jxl.write.Number(5, nrows,
						Math.rint(risk));
				sheet.addCell(number);
				nrows++;
				nrows++;
				label = new Label(0, nrows,
						resources.getString("xls.estimation.case")// "Estimation performed in case of: "
								+ externalTable);
				sheet.addCell(label);

				// -----------------------------------------------
				workbook.write();
				workbook.close();
			} catch (Exception err) {
				err.printStackTrace();
			}

		}
	}

	/**
	 * Perform external exposure computations.
	 * @param tablename tablename
	 */
	private void computeExternal(String tablename) {
		dose_external = new double[nuclides_all.length][24];
		organs_external = new String[24];
		double[] act = new double[nuclides_all.length];
		for (int i = 0; i < nuclides_all.length; i++) {
			if (IA_use == IA_sec) {
				act[i] = activity_secular[i];
			} else if (IA_use == IA_elapsed) {
				act[i] = activity_chain[i];
			} else if (IA_use == IA_norm) {
				act[i] = activity_chain_scaled[i];
			} else if (IA_use == IA_eq) {
				act[i] = activity_chain_equilibrum[i];
			}

			if (!parentCh.isSelected()) {
				if (i > 0) {
					act[i] = 0.0;
				}
			}

		}

		try {
			double exposureTimeTemp = exposureTime;
			// Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			// String datas="Data";//resources.getString("data.load");//"Data";
			// String currentDir=System.getProperty("user.dir");
			// String file_sep=System.getProperty("file.separator");
			// String opens=currentDir+file_sep+datas;
			// String fileS="FGR12.mdb";
			// String filename = opens+file_sep+fileS;
			// String database =
			// "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			// database+= filename.trim() + ";DriverID=22;READONLY=true}"; //
			// add on to the end
			// now we can get the connection from the DriverManager
			// Connection con1 = DriverManager.getConnection( database ,"","");

			String datas = resources.getString("data.load");// Data
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;
			String dbName = externalDB;// the name of the database
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			// ----------------
			effectiveDoseTotal = 0.0; thyroidDoseTotal=0.0; //double tdose=0.0;
			for (int i = 0; i < nuclides_all.length; i++) {
				Statement s = con1.createStatement();
				String input = nuclides_all[i];
				String statement = "select Nuclide, Adrenals, BladderWall, "
						+ "BoneSurface, Brain, Breast, Esophagus, StomachWall, "
						+ "SmallIntestine, UpperLargeIntestine, LowerLargeIntestine, "
						+ "Kidneys, Liver, Lungs, Muscle, Ovaries, Pancreas, RedMarrow, "
						+ "Skin, Spleen, Testes, Thymus, Thyroid, Uterus, E"
						+ " from " + tablename + " where Nuclide = " + "'"
						+ input + "'";

				s.execute(statement);

				ResultSet rs = s.getResultSet(); // get any ResultSet that came
													// from our query
				// int index=0;
				if (rs != null) // if rs == null, then there is no ResultSet to
								// view
					while (rs.next()) // this will step through our data
										// row-by-row.
					{
						String adrenalsS = rs.getString(2);
						organs_external[0] = resources.getString("Adrenals");
						String bladderWallS = rs.getString(3);
						organs_external[1] = resources.getString("BladderWall");
						String boneSurfaceS = rs.getString(4);
						organs_external[2] = resources.getString("BoneSurface");
						String brainS = rs.getString(5);
						organs_external[3] = resources.getString("Brain");
						String breastS = rs.getString(6);
						organs_external[4] = resources.getString("Breast");
						String esophagusS = rs.getString(7);
						organs_external[5] = resources.getString("Esophagus");
						String stomachWallS = rs.getString(8);
						organs_external[6] = resources.getString("StomachWall");
						String smallIntestineWallS = rs.getString(9);
						organs_external[7] = resources
								.getString("SmallIntestineWall");
						String upperLargeIntestineWallS = rs.getString(10);
						organs_external[8] = resources
								.getString("UpperLargeIntestineWall");
						String lowerLargeIntestineWallS = rs.getString(11);
						organs_external[9] = resources
								.getString("LowerLargeIntestineWall");
						String kidneysS = rs.getString(12);
						organs_external[10] = resources.getString("Kidneys");
						String liverS = rs.getString(13);
						organs_external[11] = resources.getString("Liver");
						String lungsS = rs.getString(14);
						organs_external[12] = resources.getString("Lungs");
						String muscleS = rs.getString(15);
						organs_external[13] = resources.getString("Muscle");
						String ovariesS = rs.getString(16);
						organs_external[14] = resources.getString("Ovaries");
						String pancreasS = rs.getString(17);
						organs_external[15] = resources.getString("Pancreas");
						String redMarrowS = rs.getString(18);
						organs_external[16] = resources.getString("RedMarrow");
						String skinS = rs.getString(19);
						organs_external[17] = resources.getString("Skin");
						String spleenS = rs.getString(20);
						organs_external[18] = resources.getString("Spleen");
						String testesS = rs.getString(21);
						organs_external[19] = resources.getString("Testes");
						String thymusS = rs.getString(22);
						organs_external[20] = resources.getString("Thymus");
						String thyroidS = rs.getString(23);
						organs_external[21] = resources.getString("Thyroid");
						String uterusS = rs.getString(24);
						organs_external[22] = resources.getString("Uterus");
						String ES = rs.getString(25);
						organs_external[23] = resources
								.getString("Effectivedose");
						// System.out.println(input+"; Ars: "+adrenalsS+" ;ES: "+ES);//+"  "+dose_external[3][10]);

						if (IA_use == IA_norm) {
							exposureTime = 1.0;// we use time integrated
						}
						dose_external[i][0] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(adrenalsS);
						dose_external[i][1] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(bladderWallS);
						dose_external[i][2] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(boneSurfaceS);
						dose_external[i][3] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(brainS);
						dose_external[i][4] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(breastS);
						dose_external[i][5] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(esophagusS);
						dose_external[i][6] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(stomachWallS);
						dose_external[i][7] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(smallIntestineWallS);
						dose_external[i][8] = activity
								* act[i]
								* exposureTime
								* Convertor
										.stringToDouble(upperLargeIntestineWallS);
						dose_external[i][9] = activity
								* act[i]
								* exposureTime
								* Convertor
										.stringToDouble(lowerLargeIntestineWallS);
						dose_external[i][10] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(kidneysS);
						dose_external[i][11] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(liverS);
						dose_external[i][12] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(lungsS);
						dose_external[i][13] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(muscleS);
						dose_external[i][14] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(ovariesS);
						dose_external[i][15] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(pancreasS);
						dose_external[i][16] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(redMarrowS);
						dose_external[i][17] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(skinS);
						dose_external[i][18] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(spleenS);
						dose_external[i][19] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(testesS);
						dose_external[i][20] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(thymusS);
						dose_external[i][21] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(thyroidS);
						dose_external[i][22] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(uterusS);
						dose_external[i][23] = activity * act[i] * exposureTime
								* Convertor.stringToDouble(ES);

						/*tdose = 0.0;
						for (int kk = 0; kk <= 22; kk++){
							tdose = tdose + dose_external[i][kk];
						}
						
						totalDose = totalDose + tdose;*/
						
						effectiveDoseTotal = effectiveDoseTotal
								+ dose_external[i][23];
						thyroidDoseTotal = thyroidDoseTotal
								+ dose_external[i][21];
					}
			}
			// -------------------
			exposureTime = exposureTimeTemp;// reset!!
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	/**
	 * Start computation for individual exposure.
	 */
	private void calculate() {
		resetAll();
		textArea.selectAll();
		textArea.replaceSelection("");

		double sampleTime = 0.0;

		String s = "";
		try {
			sampleTime = Convertor.stringToDouble(elapsedTimeTf.getText());
			timesHalfLifeForEq = Convertor.stringToDouble(xparentHalfLifeTf
					.getText());
			activity = Convertor.stringToDouble(activityTf.getText());
		} catch (Exception e) {
			String title = resources.getString("number.error.title");
			String message = resources.getString("number.error");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);

			stopThread();// kill all threads
			statusL.setText(resources.getString("status.done"));
			e.printStackTrace();
			return;
		}

		if (sampleTime <= 0 || timesHalfLifeForEq <= 0.0 || activity <= 0.0) {
			String title = resources.getString("number.error.title");
			String message = resources.getString("number.error");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);

			stopThread();// kill all threads
			statusL.setText(resources.getString("status.done"));

			return;
		}

		s = (String) sampleTimeUnitsCb.getSelectedItem();
		elapsedTime = formatHalfLife(sampleTime, s);
		exposureTime = elapsedTime;

		yield_current = 1.0;
		parentNuclide = (String) nucCb.getSelectedItem();

		colect_first_time = true;
		parent_dual = false;

		setMasterTables();

		ndaughter = 0;// init
		nuc_chains = new String[nmax_daughters][nmax_chains];
		hlu_chains = new String[nmax_daughters][nmax_chains];
		am_chains = new double[nmax_daughters][nmax_chains];
		hl_chains = new double[nmax_daughters][nmax_chains];
		br_chains = new double[nmax_daughters][nmax_chains];
		a_chains = new double[nmax_daughters][nmax_chains];
		at_chains = new double[nmax_daughters][nmax_chains];
		a_chains_equilibrum = new double[nmax_daughters][nmax_chains];
		nchain = 0;// init
		ndaughter = 0;// init
		ndaughterChainMax = 0;// init
		timeSteps = 1;

		conn = null;
		String datas = resources.getString("data.load");
		String currentDir = System.getProperty("user.dir");
		String file_sep = System.getProperty("file.separator");
		String opens = currentDir + file_sep + datas;
		String dbName = icrpDB;
		opens = opens + file_sep + dbName;

		statements = new ArrayList<Statement>();
		resultsets = new ArrayList<ResultSet>();

		try {

			conn = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");

			collectDecayData(parentNuclide);
			if (STOPCOMPUTATION)
				return;
			// put all together removing duplicates and creating chains!!
			formatDecay();
			if (STOPCOMPUTATION)
				return;
			saveActivitiesSecularAndPrepareChainData();// here
			// main arrays are computed...let them finished!!

			excelB = excelCh.isSelected();
			createDecayInfoExcelFile();

			// -------------
			if (excelB) {
				try {
					workbookCA = Workbook.createWorkbook(new File(parentNuclide
							+ "_decayActivity.xls"));
					sheetCA = workbookCA.createSheet("First Sheet", 0);
				} catch (Exception err) {
					err.printStackTrace();
				}
			}
			// ---------------------
			title1B = false;
			for (int j = 0; j <= nchain; j++) {
				computeChainActivity(j);
			}
			saveActivitiesChain();
			// ------------------------------
			if (excelB) {
				try {
					workbookCA.write();
					workbookCA.close();
				} catch (Exception err) {
					err.printStackTrace();
				}
			}

			// getRadiationYields();
			// displayRadiation_PHOTON();

			if (a_secRb.isSelected()) {
				IA_use = IA_sec;
			}
			// if(a_afterRb.isSelected()){IA_use=IA_elapsed;}
			if (a_normRb.isSelected()) {
				IA_use = IA_norm;
			}
			if (a_eqRb.isSelected()) {
				IA_use = IA_eq;
			}

			title1B = false;
			
			boolean externalB = false;
			if (e_airRb.isSelected()) {
				externalB = true;
				externalTable = airTable;
			} else if (e_waterRb.isSelected()) {
				externalB = true;
				externalTable = waterTable;
			} else if (e_gsRb.isSelected()) {
				externalB = true;
				externalTable = groundSurfaceTable;
			} else if (e_isRb.isSelected()) {
				externalB = true;
				externalTable = infiniteSoilTable;
			} else if (i_ingRb.isSelected()) {
				if (IA_use == IA_norm) {
					// no case for time integrated activities! Bq x sec!!!
					// coeff are dose/Bq!!!!
					return;
				}
				publicIngestionTable = ingestionAdultTable;
				if (y1Rb.isSelected())
					publicIngestionTable = ingestion1Table;
				else if (y10Rb.isSelected())
					publicIngestionTable = ingestion10Table;
				computeInternalIngestion(publicIngestionTable);

				risk = 10 * lifetime_cancer_mortality * effectiveDoseTotal//totalDose//effectiveDoseTotal//
						/ 0.1;// per MILION POP;
				// excelB=excelCh.isSelected();//true;
				displayInternalResults(publicIngestionTable);
			} else if (i_inhRb.isSelected()) {
				if (IA_use == IA_norm) {
					// no case for time integrated activities! Bq x sec!!!
					// coeff are dose/Bq!!!!
					return;
				}
				publicInhalationTable = inhalationAdultTable;
				if (y1Rb.isSelected())
					publicInhalationTable = inhalation1Table;
				else if (y10Rb.isSelected())
					publicInhalationTable = inhalation10Table;
				computeInternalInhalation(publicInhalationTable);

				risk = 10 * lifetime_cancer_mortality *effectiveDoseTotal// totalDose//effectiveDoseTotal
						/ 0.1;// per MILION POP;
				// excelB=excelCh.isSelected();//true;
				displayInternalResults(publicInhalationTable);
			}

			// IA_use=IA_eq;
			if (externalB) {
				computeExternal(externalTable);

				risk = 10 * lifetime_cancer_mortality * effectiveDoseTotal//totalDose//effectiveDoseTotal
						/ 0.1;// per MILION POP;

				displayExternalResults();
			}

		} catch (Exception ex) {
			stopThread();// kill all threads
			statusL.setText(resources.getString("status.done"));
			ex.printStackTrace();
		} finally {
			// release all open resources to avoid unnecessary memory usage
			// ResultSet
			int j = 0;
			while (!resultsets.isEmpty()) {
				ResultSet rs = (ResultSet) resultsets.remove(j);
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
				} catch (Exception sqle) {
					sqle.printStackTrace();
					stopThread();// kill all threads
					statusL.setText(resources.getString("status.done"));
				}
			}
			// Statements and PreparedStatements
			int i = 0;
			while (!statements.isEmpty()) {
				// PreparedStatement extend Statement
				Statement st = (Statement) statements.remove(i);
				try {
					if (st != null) {
						st.close();
						st = null;
					}
				} catch (Exception sqle) {
					sqle.printStackTrace();
					stopThread();// kill all threads
					statusL.setText(resources.getString("status.done"));
				}
			}
			// Connection
			/*
			 * try { if (conn != null) { conn.close(); conn = null; } } catch
			 * (Exception sqle) { sqle.printStackTrace(); stopThread();// kill
			 * all threads statusL.setText(resources.getString("status.done"));
			 * }
			 */
		}
		// setup necessary variable and
		// remove unused objects from memory!
		// performCleaningUp();
		// ---------------------------
		stopThread();// kill all threads
		statusL.setText(resources.getString("status.done"));
		tabs.setSelectedIndex(1);
	}

	/**
	 * Display radiation information
	 */
	private void performRadiationDisplay() {
		textArea.selectAll();
		textArea.replaceSelection("");
		// String s = "";

		if (nuclides_all.length < 1)// no data
		{
			String title = resources.getString("number.error.title");
			String message = resources.getString("calculation.required");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);

			stopThread();// kill all threads
			return;
		}

		if (thresh_allRb.isSelected()) {
			DTHRESH = DTHRESH0;
		} else if (thresh_1Rb.isSelected()) {
			DTHRESH = DTHRESH1;
		} else if (thresh_2Rb.isSelected()) {
			DTHRESH = DTHRESH2;
		} else if (thresh_5Rb.isSelected()) {
			DTHRESH = DTHRESH5;
		} else if (thresh_10Rb.isSelected()) {
			DTHRESH = DTHRESH10;
		} else if (thresh_20Rb.isSelected()) {
			DTHRESH = DTHRESH20;
		}

		getRadiationYields();
		if (STOPCOMPUTATION)
			return;
		excelB = excelCh.isSelected();// true;

		if (rad_allRb.isSelected()) {
			displayRadiation_ALL();
		} else if (rad_gammaRb.isSelected()) {
			IRCODE = IGAMMA;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_xrayRb.isSelected()) {
			IRCODE = IXRAY;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_annihRb.isSelected()) {
			IRCODE = IANNIH;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_betapRb.isSelected()) {
			IRCODE = IBETAP;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_betanRb.isSelected()) {
			IRCODE = IBETAN;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_iceRb.isSelected()) {
			IRCODE = IICE;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_augerRb.isSelected()) {
			IRCODE = IAUGER;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_alphaRb.isSelected()) {
			IRCODE = IALPHA;
			displayRadiation_TYPE(IRCODE);
		} else if (rad_photonsRb.isSelected()) {
			displayRadiation_PHOTON();
			if (STOPCOMPUTATION)
				return;
		} else if (rad_electronsRb.isSelected()) {
			displayRadiation_ELECTRON();
		}

		tabs.setSelectedIndex(1);
		stopThread();// kill all threads
	}

	/**
	 * Display all radiations
	 */
	private void displayRadiation_ALL() {
		String s = "";
		if (excelB) {
			try {
				WritableWorkbook workbook = Workbook.createWorkbook(new File(
						parentNuclide + "_RadInfo.xls"));
				WritableSheet sheet = workbook.createSheet("First Sheet", 0);
				// ------------------------------------------------
				Label label = new Label(0, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				label = new Label(0, 1,
						resources.getString("text.radiation.from"));// "Radiation from");//
																	// col,row
				sheet.addCell(label);
				label = new Label(1, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				sheet.addCell(label);
				label = new Label(2, 1,
						resources.getString("text.radiation.yield"));// "Intensity");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 1,
						resources.getString("text.radiation.energy"));// "Energy");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 2,
						resources.getString("text.radiation.energy2"));// "MeV");//
																		// col,row
				sheet.addCell(label);
				// -----------------------------------------------
				int nrows = 3;// current rows to write
				for (int j = 0; j < nuclideS.size(); j++) {

					// System.out.println("Data for: "+nucInChain[j]);
					s = "--------------------------------" + " \n";
					textArea.append(s);
					s = resources.getString("text.dataFor")// "Data for: "
							+ nucInChain[j] + " \n";
					textArea.append(s);

					for (int i = 0; i < rcode.length; i++) {
						if (rcode[i][j].equals("-1")) {
							break;
						}

						if (ryield[i][j] > DTHRESH) {
							// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
							// " ; intensity: "+ryield[i][j]+
							// " ;energy: "+renergy[i][j]);
							s = rcode[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.type")// Rad.Type:
																				// ""
																				// +
									+ rtype[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.yield")// "; Intensity: "
									+ Convertor.formatNumber(ryield[i][j], 5)
									+ "; "
									+ resources
											.getString("text.radiation.energy1")// "; Energy [MeV]: "
									+ Convertor.formatNumber(renergy[i][j], 5)
									+ " \n";
							textArea.append(s);
							// create Excel File
							label = new Label(0, nrows, nucInChain[j]);
							sheet.addCell(label);
							label = new Label(1, nrows, rtype[i][j]);
							sheet.addCell(label);
							jxl.write.Number number = new jxl.write.Number(2,
									nrows, ryield[i][j]);
							sheet.addCell(number);
							number = new jxl.write.Number(3, nrows,
									renergy[i][j]);
							sheet.addCell(number);
							nrows++;
						}
					}
				}
				// -----------------------------------------------
				workbook.write();
				workbook.close();
			} catch (Exception err) {
				err.printStackTrace();
			}

		} else {
			for (int j = 0; j < nuclideS.size(); j++) {
				s = "--------------------------------" + " \n";
				textArea.append(s);
				s = resources.getString("text.dataFor")// "Data for: "
						+ nucInChain[j] + " \n";
				textArea.append(s);

				// System.out.println("Data for: "+nucInChain[j]);
				for (int i = 0; i < rcode.length; i++) {
					if (rcode[i][j].equals("-1")) {
						break;
					}

					if (ryield[i][j] > DTHRESH) {
						// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
						// " ; intensity: "+ryield[i][j]+
						// " ;energy: "+renergy[i][j]);
						s = rcode[i][j]
								+ "; "
								+ resources.getString("text.radiation.type")// Rad.Type:
																			// ""
																			// +
								+ rtype[i][j]
								+ "; "
								+ resources.getString("text.radiation.yield")// "; Intensity: "
								+ Convertor.formatNumber(ryield[i][j], 5)
								+ "; "
								+ resources.getString("text.radiation.energy1")// "; Energy [MeV]: "
								+ Convertor.formatNumber(renergy[i][j], 5)
								+ " \n";
						textArea.append(s);

					}
				}
			}
		}
	}

	/**
	 * Display radiation of give type (ircod).
	 * @param ircod ircod
	 */
	private void displayRadiation_TYPE(int ircod) {
		String s = "";
		if (excelB) {
			try {
				WritableWorkbook workbook = Workbook.createWorkbook(new File(
						parentNuclide + "_RadInfo.xls"));
				WritableSheet sheet = workbook.createSheet("First Sheet", 0);
				// ------------------------------------------------
				Label label = new Label(0, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				label = new Label(0, 1,
						resources.getString("text.radiation.from"));// "Radiation from");//
																	// col,row
				sheet.addCell(label);
				label = new Label(1, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				sheet.addCell(label);
				label = new Label(2, 1,
						resources.getString("text.radiation.yield"));// "Intensity");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 1,
						resources.getString("text.radiation.energy"));// "Energy");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 2,
						resources.getString("text.radiation.energy2"));// "MeV");//
																		// col,row
				sheet.addCell(label);
				// -----------------------------------------------
				int nrows = 3;// current rows to write

				boolean firstB = false;
				for (int j = 0; j < nuclideS.size(); j++) {
					firstB = false;
					for (int i = 0; i < rcode.length; i++) {
						if (rcode[i][j].equals("-1")) {
							break;
						}

						if ((rcode[i][j].equals(Convertor.intToString(ircod)))
								&& ((ryield[i][j] > DTHRESH))) {
							if (!firstB) {
								// System.out.println("Radiation from: "+nucInChain[j]);
								s = "--------------------------------" + " \n";
								textArea.append(s);
								s = resources.getString("text.radiation.from")// "Radiation from: "
										+ nucInChain[j] + " \n";
								textArea.append(s);
								firstB = true;
							}
							// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
							// " ; intensity: "+ryield[i][j]+
							// " ;energy: "+renergy[i][j]);
							// s = rcode[i][j] + "; Rad.Type: " + rtype[i][j]
							// + "; Intensity: "
							// + formatNumber(ryield[i][j])
							// + "; Energy [MeV]: "
							// + formatNumber(renergy[i][j]) + " \n";
							//textArea.append(s+"what a hell");
							s = rcode[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.type")// Rad.Type:
									// ""
									// +
									+ rtype[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.yield")// "; Intensity: "
									+ Convertor.formatNumber(ryield[i][j], 5)
									+ "; "
									+ resources
											.getString("text.radiation.energy1")// "; Energy [MeV]: "
									+ Convertor.formatNumber(renergy[i][j], 5)
									+ " \n";
							textArea.append(s);
							// create Excel File
							label = new Label(0, nrows, nucInChain[j]);
							sheet.addCell(label);
							label = new Label(1, nrows, rtype[i][j]);
							sheet.addCell(label);
							jxl.write.Number number = new jxl.write.Number(2,
									nrows, ryield[i][j]);
							sheet.addCell(number);
							number = new jxl.write.Number(3, nrows,
									renergy[i][j]);
							sheet.addCell(number);
							nrows++;

						}
					}

				}
				// -----------------------------------------------
				workbook.write();
				workbook.close();
			} catch (Exception err) {
				System.out.println("ERROR: " + err);
			}

		} else {
			boolean firstB = false;
			for (int j = 0; j < nuclideS.size(); j++) {
				firstB = false;
				for (int i = 0; i < rcode.length; i++) {
					if (rcode[i][j].equals("-1")) {
						break;
					}

					if ((rcode[i][j].equals(Convertor.intToString(ircod)))
							&& ((ryield[i][j] > DTHRESH))) {
						if (!firstB) {
							// System.out.println("Radiation from: "+nucInChain[j]);
							s = "--------------------------------" + " \n";
							textArea.append(s);
							s = resources.getString("text.radiation.from")// "Radiation from: "
									+ nucInChain[j] + " \n";
							textArea.append(s);

							firstB = true;
						}
						// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
						// " ; intensity: "+ryield[i][j]+
						// " ;energy: "+renergy[i][j]);
						// s = rcode[i][j] + "; Rad.Type: " + rtype[i][j]
						// + "; Intensity: " + formatNumber(ryield[i][j])
						// + "; Energy [MeV]: "
						// + formatNumber(renergy[i][j]) + " \n";
						// textArea.append(s);
						s = rcode[i][j]
								+ "; "
								+ resources.getString("text.radiation.type")// Rad.Type:
																			// ""
																			// +
								+ rtype[i][j]
								+ "; "
								+ resources.getString("text.radiation.yield")// "; Intensity: "
								+ Convertor.formatNumber(ryield[i][j], 5)
								+ "; "
								+ resources.getString("text.radiation.energy1")// "; Energy [MeV]: "
								+ Convertor.formatNumber(renergy[i][j], 5)
								+ " \n";
						textArea.append(s);
					}
				}

			}
		}
	}

	/**
	 * Display beta radiation
	 */
	private void displayRadiation_ELECTRON() {
		String s = "";
		if (excelB) {
			try {
				WritableWorkbook workbook = Workbook.createWorkbook(new File(
						parentNuclide + "_RadInfo.xls"));
				WritableSheet sheet = workbook.createSheet("First Sheet", 0);
				// ------------------------------------------------
				Label label = new Label(0, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				label = new Label(0, 1,
						resources.getString("text.radiation.from"));// "Radiation from");//
																	// col,row
				sheet.addCell(label);
				label = new Label(1, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				sheet.addCell(label);
				label = new Label(2, 1,
						resources.getString("text.radiation.yield"));// "Intensity");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 1,
						resources.getString("text.radiation.energy"));// "Energy");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 2,
						resources.getString("text.radiation.energy2"));// "MeV");//
																		// col,row
				sheet.addCell(label);
				// -----------------------------------------------
				int nrows = 3;// current rows to write
				boolean firstB = false;
				for (int j = 0; j < nuclideS.size(); j++) {
					firstB = false;
					for (int i = 0; i < rcode.length; i++) {
						if (rcode[i][j].equals("-1")) {
							break;
						}

						if (((rcode[i][j].equals(Convertor.intToString(IBETAP)))
								|| (rcode[i][j].equals(Convertor
										.intToString(IBETAN)))
								|| (rcode[i][j].equals(Convertor
										.intToString(IICE))) || (rcode[i][j]
								.equals(Convertor.intToString(IAUGER))))
								&& ((ryield[i][j] > DTHRESH))) {
							if (!firstB) {
								// System.out.println("Radiation from: "+nucInChain[j]);
								s = "--------------------------------" + " \n";
								textArea.append(s);
								s = resources.getString("text.radiation.from")// "Radiation from: "
										+ nucInChain[j] + " \n";
								textArea.append(s);

								firstB = true;
							}
							// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
							// " ; intensity: "+ryield[i][j]+
							// " ;energy: "+renergy[i][j]);
							// s = rcode[i][j] + "; Rad.Type: " + rtype[i][j]
							// + "; Intensity: "
							// + formatNumber(ryield[i][j])
							// + "; Energy [MeV]: "
							// + formatNumber(renergy[i][j]) + " \n";
							// textArea.append(s);
							s = rcode[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.type")// Rad.Type:
																				// ""
																				// +
									+ rtype[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.yield")// "; Intensity: "
									+ Convertor.formatNumber(ryield[i][j], 5)
									+ "; "
									+ resources
											.getString("text.radiation.energy1")// "; Energy [MeV]: "
									+ Convertor.formatNumber(renergy[i][j], 5)
									+ " \n";
							textArea.append(s);
							// create Excel File
							label = new Label(0, nrows, nucInChain[j]);
							sheet.addCell(label);
							label = new Label(1, nrows, rtype[i][j]);
							sheet.addCell(label);
							jxl.write.Number number = new jxl.write.Number(2,
									nrows, ryield[i][j]);
							sheet.addCell(number);
							number = new jxl.write.Number(3, nrows,
									renergy[i][j]);
							sheet.addCell(number);
							nrows++;

						}
					}

				}
				// -----------------------------------------------
				workbook.write();
				workbook.close();
			} catch (Exception err) {
				System.out.println("ERROR: " + err);
			}

		} else {
			boolean firstB = false;
			for (int j = 0; j < nuclideS.size(); j++) {
				firstB = false;
				for (int i = 0; i < rcode.length; i++) {
					if (rcode[i][j].equals("-1")) {
						break;
					}

					if (((rcode[i][j].equals(Convertor.intToString(IBETAP)))
							|| (rcode[i][j].equals(Convertor
									.intToString(IBETAN)))
							|| (rcode[i][j].equals(Convertor.intToString(IICE))) || (rcode[i][j]
							.equals(Convertor.intToString(IAUGER))))
							&& ((ryield[i][j] > DTHRESH))) {
						if (!firstB) {
							// System.out.println("Radiation from: "+nucInChain[j]);
							s = "--------------------------------" + " \n";
							textArea.append(s);
							s = resources.getString("text.radiation.from")// "Radiation from: "
									+ nucInChain[j] + " \n";
							textArea.append(s);

							firstB = true;
						}
						// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
						// " ; intensity: "+ryield[i][j]+
						// " ;energy: "+renergy[i][j]);
						// s = rcode[i][j] + "; Rad.Type: " + rtype[i][j]
						// + "; Intensity: " + formatNumber(ryield[i][j])
						// + "; Energy [MeV]: "
						// + formatNumber(renergy[i][j]) + " \n";
						// textArea.append(s);
						s = rcode[i][j]
								+ "; "
								+ resources.getString("text.radiation.type")// Rad.Type:
																			// ""
																			// +
								+ rtype[i][j]
								+ "; "
								+ resources.getString("text.radiation.yield")// "; Intensity: "
								+ Convertor.formatNumber(ryield[i][j], 5)
								+ "; "
								+ resources.getString("text.radiation.energy1")// "; Energy [MeV]: "
								+ Convertor.formatNumber(renergy[i][j], 5)
								+ " \n";
						textArea.append(s);
					}
				}

			}
		}
	}

	/**
	 * Display all photon radiations.
	 */
	private void displayRadiation_PHOTON() {
		// shielding calculation
		double[] act = new double[nucInChain.length];

		double doseratej = 0.0;
		double doseratenuc = 0.0;
		double doseratejshield = 0.0;

		ajV = new Vector<Double>();
		biV = new Vector<Double>();
		eiV = new Vector<Double>();
		yiV = new Vector<Double>();
		rmfpShieldV = new Vector<Double>();
		uiTissV = new Vector<Double>();

		try {
			elapsedTime = Convertor.stringToDouble(elapsedTimeTf.getText());
			// exposureTime=stringToDouble(exposureTimeTf.getText());
			exposureTime = elapsedTime;
			activity = Convertor.stringToDouble(activityTf.getText());

			distance = Convertor.stringToDouble(distanceTf.getText());
			shieldThickness = Convertor.stringToDouble(thicknessTf.getText());
		} catch (Exception e) {
			String title = resources.getString("number.error.title");
			String message = resources.getString("number.error");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);
			stopThread();// kill all threads
			return;
		}

		for (int i = 0; i < nucInChain.length; i++) {
			if (IA_use == IA_sec) {
				act[i] = activity * activity_secular[i];
			} else if (IA_use == IA_elapsed) {
				act[i] = activity * activity_chain[i];
			} else if (IA_use == IA_norm) {
				act[i] = activity * activity_chain_scaled[i];// Bqs dose not
																// dose rate!!!
			} else if (IA_use == IA_eq) {
				act[i] = activity * activity_chain_equilibrum[i];
			}

			if (!parentCh.isSelected()) {
				if (i > 0) {
					act[i] = 0.0;
				}
			}
		}
		// ====================================
		String doseS = "";
		String doseSTotal = "";
		if (IA_use == IA_norm) {
			doseS = resources.getString("text.tissueDose");// " Tissue dose [uSv]: ";
			doseSTotal = resources.getString("text.tissueDose.total");// " Total tissue dose [uSv]: ";
		} else {
			doseS = resources.getString("text.tissueDoseRate");// " Tissue dose rate [uSv/hr]: ";
			doseSTotal = resources.getString("text.tissueDoseRate.total");// " Total tissue dose rate [uSv/hr]: ";
		}
		String s = "";

		if (!excelB) {
			boolean firstB = false;
			doseratetotal = 0.0;
			boolean controlB = false;
			doseratetotalshield = 0.0;
			// if (thresh_allRb.isSelected()) {
			// DTHRESH = DTHRESH0;
			// } else if (thresh_1Rb.isSelected()) {
			// DTHRESH = DTHRESH1;
			// } else if (thresh_2Rb.isSelected()) {
			// DTHRESH = DTHRESH2;
			// } else if (thresh_5Rb.isSelected()) {
			// DTHRESH = DTHRESH5;
			// }

			double energyThreshold = 0.0;

			// String s = "";
			for (int j = 0; j < nuclideS.size(); j++) {
				firstB = false;
				controlB = false;
				for (int i = 0; i < rcode.length; i++) {
					if (rcode[i][j].equals("-1")) {
						break;
					}

					if (((rcode[i][j].equals(Convertor.intToString(IXRAY)))
							|| (rcode[i][j].equals(Convertor
									.intToString(IGAMMA))) || (rcode[i][j]
							.equals(Convertor.intToString(IANNIH))))
							&& (ryield[i][j] > DTHRESH)
							&& (renergy[i][j] > energyThreshold / 1000.0)) {
						// renergy is in MeV!!!!
						if (!firstB) {
							s = "--------------------------------" + " \n";
							textArea.append(s);
							s = resources.getString("text.radiation.from")
									+ nucInChain[j] + " \n";
							textArea.append(s);

							firstB = true;
							controlB = true;
							doseratenuc = 0.0;
						}

						// compute dose rate
						// =========================
						double uabsmasic = getTissueMassAbsorbtionCoefficient(renergy[i][j]);
						doseratej = act[j] * renergy[i][j] * ryield[i][j]
								* uabsmasic
								/ (4.0 * Math.PI * distance * distance);//
						// 1 MeV is equal to 1.6021773E-13 joule.
						double mevtojoule = 1.60218E-13;// 1MeV=1,60218 �
														// 10-13
														// J
						double factor = mevtojoule / 0.001;// u->cm2/g
						doseratej = doseratej * factor;// @@@@@@@@@@@@@@@@@@@@@Sv/sec
						doseratej = doseratej * 1.0E6 * 3600.0;// uSv/h
						doseratetotal = doseratetotal + doseratej;
						doseratenuc = doseratenuc + doseratej;

						double[] uattmasicrho = getShieldMassAtenuationCoefficient(renergy[i][j]);
						double mrf = uattmasicrho[0] * uattmasicrho[1]
								* shieldThickness;
						double bi = getShieldBuildUpFactor(renergy[i][j], mrf);// 10.4);//10);//mrf);
						doseratejshield = act[j] * renergy[i][j] * ryield[i][j]
								* bi * Math.exp(-mrf) * uabsmasic
								/ (4.0 * Math.PI * distance * distance);//
						doseratejshield = doseratejshield * factor;// @@@@@@@@@@@@@@@@@@@@@Sv/sec
						doseratejshield = doseratejshield * 1.0E6 * 3600.0;// uSv/h
						doseratetotalshield = doseratetotalshield
								+ doseratejshield;

						uiTissV.addElement(new Double(uabsmasic));
						ajV.addElement(new Double(act[j]
								/ (4.0 * Math.PI * distance * distance)));
						eiV.addElement(new Double(renergy[i][j]));
						yiV.addElement(new Double(ryield[i][j]));
						// ==============================

						s = rcode[i][j] + "; "
								+ resources.getString("text.radiation.type")
								+ rtype[i][j] + "; "
								+ resources.getString("text.radiation.yield")
								+ Convertor.formatNumber(ryield[i][j], 5)
								+ "; "
								+ resources.getString("text.radiation.energy1")
								+ Convertor.formatNumber(renergy[i][j], 5)
								// Convertor.formatNumber(1000.0 *
								// renergy[i][j], 2)
								+ " \n";
						textArea.append(s);

					}// if
				}// for i
				if (controlB) {
					s = nucInChain[j] + " - " + doseS
							+ Convertor.formatNumberScientific(doseratenuc)
							+ " \n";
					textArea.append(s);
				}
			}// for j
			hvlBoolean = hvlCh.isSelected();// true;
			if (hvlBoolean) {
				computeHVL();
				if (STOPCOMPUTATION)
					return;
				computeTVL();
				if (STOPCOMPUTATION)
					return;
			}
			s = "--------------------------------" + " \n";
			textArea.append(s);
			s = doseSTotal + Convertor.formatNumberScientific(doseratetotal)
					+ resources.getString("text.for")// " for "
					+ nucInChain[0] + resources.getString("text.of")// " of "
					+ activity + resources.getString("text.BqAtDistance")// " Bq, at distance "
					+ distance + resources.getString("text.cm")// " cm "
					+ " \n";
			textArea.append(s);

			s = doseSTotal
					+ Convertor.formatNumberScientific(doseratetotalshield)
					+ resources.getString("text.for")// " for "
					+ nucInChain[0]
					+ resources.getString("text.of")// " of "
					+ activity
					+ resources.getString("text.BqAtDistance")// " Bq, at distance "
					+ distance
					+ resources.getString("text.cm")// " cm "
					+ resources.getString("text.shieldedWith")// " shielded with "
					+ (String) shieldCb.getSelectedItem()
					+ resources.getString("text.ofThickness")// " of thickness "
					+ shieldThickness + resources.getString("text.cm")// " cm "
					+ " \n";
			textArea.append(s);

			if (hvlBoolean) {
				s = resources.getString("text.hvlEstimation")// "HVL dose estimation: "
						+ Convertor.formatNumberScientific(dosehvl)
						+ resources.getString("text.hvlEstimation1")// " uSv/hr; HVL: "
						+ hvl + resources.getString("text.cm")// " cm "
						+ " \n";
				textArea.append(s);

				s = resources.getString("text.tvlEstimation")// "TVL dose estimation: "
						+ Convertor.formatNumberScientific(dosetvl)
						+ resources.getString("text.tvlEstimation1")// " uSv/hr; TVL: "
						+ tvl + resources.getString("text.cm")// " cm "
						+ " \n";
				textArea.append(s);
			}

			// ===========
		} else {// if (excelB) {
			try {
				WritableWorkbook workbook = Workbook.createWorkbook(new File(
						parentNuclide + "_RadInfo.xls"));
				WritableSheet sheet = workbook.createSheet("First Sheet", 0);
				// ------------------------------------------------
				Label label = new Label(0, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				label = new Label(0, 1,
						resources.getString("text.radiation.from"));// "Radiation from");//
																	// col,row
				sheet.addCell(label);
				label = new Label(1, 1,
						resources.getString("text.radiation.type"));// "Radiation type");//
																	// col,row
				sheet.addCell(label);
				label = new Label(2, 1,
						resources.getString("text.radiation.yield"));// "Intensity");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 1,
						resources.getString("text.radiation.energy"));// "Energy");//
																		// col,row
				sheet.addCell(label);
				label = new Label(3, 2,
						resources.getString("text.radiation.energy2"));// "MeV");//
																		// col,row
				sheet.addCell(label);

				label = new Label(4, 1, doseS);// col,row
				sheet.addCell(label);
				// -----------------------------------------------
				int nrows = 3;// current rows to write

				boolean firstB = false;
				doseratetotal = 0.0;
				boolean controlB = false;
				doseratetotalshield = 0.0;
				for (int j = 0; j < nuclideS.size(); j++) {
					firstB = false;
					controlB = false;
					for (int i = 0; i < rcode.length; i++) {
						if (rcode[i][j].equals("-1")) {
							break;
						}

						if (((rcode[i][j].equals(Convertor.intToString(IXRAY)))
								|| (rcode[i][j].equals(Convertor
										.intToString(IGAMMA))) || (rcode[i][j]
								.equals(Convertor.intToString(IANNIH))))
								&& ((ryield[i][j] > DTHRESH))) {
							if (!firstB) {
								// System.out.println("Radiation from: "+nucInChain[j]);
								s = "--------------------------------" + " \n";
								textArea.append(s);
								s = resources.getString("text.radiation.from")// "Radiation from: "
										+ nucInChain[j] + " \n";
								textArea.append(s);

								firstB = true;
								controlB = true;
								doseratenuc = 0.0;
							}
							// compute dose rate
							// =========================
							double uabsmasic = getTissueMassAbsorbtionCoefficient(renergy[i][j]);
							doseratej = act[j] * renergy[i][j] * ryield[i][j]
									* uabsmasic
									/ (4.0 * Math.PI * distance * distance);//
							// 1 MeV is equal to 1.6021773E-13 joule.
							double mevtojoule = 1.60218E-13;// 1MeV=1,60218 �
															// 10-13 J
							double factor = mevtojoule / 0.001;// u->cm2/g
							doseratej = doseratej * factor;// @@@@@@@@@@@@@@@@@@@@@Sv/sec
							doseratej = doseratej * 1.0E6 * 3600.0;// uSv/h
							doseratetotal = doseratetotal + doseratej;
							doseratenuc = doseratenuc + doseratej;

							double[] uattmasicrho = getShieldMassAtenuationCoefficient(renergy[i][j]);
							double mrf = uattmasicrho[0] * uattmasicrho[1]
									* shieldThickness;
							double bi = getShieldBuildUpFactor(renergy[i][j],
									mrf);// 10.4);//10);//mrf);
							doseratejshield = act[j] * renergy[i][j]
									* ryield[i][j] * bi * Math.exp(-mrf)
									* uabsmasic
									/ (4.0 * Math.PI * distance * distance);//
							doseratejshield = doseratejshield * factor;// @@@@@@@@@@@@@@@@@@@@@Sv/sec
							doseratejshield = doseratejshield * 1.0E6 * 3600.0;// uSv/h
							doseratetotalshield = doseratetotalshield
									+ doseratejshield;

							uiTissV.addElement(new Double(uabsmasic));
							ajV.addElement(new Double(act[j]
									/ (4.0 * Math.PI * distance * distance)));
							eiV.addElement(new Double(renergy[i][j]));
							yiV.addElement(new Double(ryield[i][j]));
							// ==============================
							// System.out.println(rcode[i][j]+" rtype: "+rtype[i][j]+
							// " ; intensity: "+ryield[i][j]+
							// " ;energy: "+renergy[i][j]);
							// s = rcode[i][j] + "; Rad.Type: " + rtype[i][j]
							// + "; Intensity: "
							// + formatNumber(ryield[i][j])
							// + "; Energy [MeV]: "
							// + formatNumber(renergy[i][j]) + ";" + doseS
							// + formatNumberScientific(doseratej) + " \n";
							// textArea.append(s);
							s = rcode[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.type")
									+ rtype[i][j]
									+ "; "
									+ resources
											.getString("text.radiation.yield")
									+ Convertor.formatNumber(ryield[i][j], 5)
									+ "; "
									+ resources
											.getString("text.radiation.energy1")
									+ Convertor.formatNumber(renergy[i][j], 5)
									// Convertor.formatNumber(1000.0 *
									// renergy[i][j], 2)
									+ " \n";
							textArea.append(s);
							// create Excel File
							label = new Label(0, nrows, nucInChain[j]);
							sheet.addCell(label);
							label = new Label(1, nrows, rtype[i][j]);
							sheet.addCell(label);
							jxl.write.Number number = new jxl.write.Number(2,
									nrows, ryield[i][j]);
							sheet.addCell(number);
							number = new jxl.write.Number(3, nrows,
									renergy[i][j]);
							sheet.addCell(number);

							number = new jxl.write.Number(4, nrows, doseratej);
							sheet.addCell(number);

							nrows++;
						}// if
					}// for i
					if (controlB) {
						// System.out.println("nuc usv/h "+nucInChain[j]+" : "+doseratenuc);
						s = nucInChain[j] + " - " + doseS
								+ Convertor.formatNumberScientific(doseratenuc)
								+ " \n";
						String ssss = nucInChain[j] + " - " + doseS
								+ Convertor.formatNumberScientific(doseratenuc);
						textArea.append(s);
						label = new Label(0, nrows, ssss);
						sheet.addCell(label);
						nrows++;
					}

				}// for j ..nuclides
				hvlBoolean = hvlCh.isSelected();// true;
				if (hvlBoolean) {
					computeHVL();
					if (STOPCOMPUTATION)
						return;
					computeTVL();
					if (STOPCOMPUTATION)
						return;
				}

				s = "--------------------------------" + " \n";
				textArea.append(s);
				label = new Label(0, nrows, s);
				sheet.addCell(label);
				nrows++;

				s = doseSTotal
						+ Convertor.formatNumberScientific(doseratetotal)
						+ resources.getString("text.for")// " for "
						+ nucInChain[0] + resources.getString("text.of")// " of "
						+ activity + resources.getString("text.BqAtDistance")// " Bq, at distance "
						+ distance + resources.getString("text.cm")// " cm "
						+ " \n";
				textArea.append(s);
				label = new Label(0, nrows, s);
				sheet.addCell(label);
				nrows++;

				s = doseSTotal
						+ Convertor.formatNumberScientific(doseratetotalshield)
						+ resources.getString("text.for")// " for "
						+ nucInChain[0]
						+ resources.getString("text.of")// " of "
						+ activity
						+ resources.getString("text.BqAtDistance")// " Bq, at distance "
						+ distance
						+ resources.getString("text.cm")// " cm "
						+ resources.getString("text.shieldedWith")// " shielded with "
						+ (String) shieldCb.getSelectedItem()
						+ resources.getString("text.ofThickness")// " of thickness "
						+ shieldThickness + resources.getString("text.cm")// " cm "
						+ " \n";
				textArea.append(s);
				label = new Label(0, nrows, s);
				sheet.addCell(label);
				nrows++;
				if (hvlBoolean) {
					s = resources.getString("text.hvlEstimation")// "HVL dose estimation: "
							+ Convertor.formatNumberScientific(dosehvl)
							+ resources.getString("text.hvlEstimation1")// " uSv/hr; HVL: "
							+ hvl + resources.getString("text.cm")// " cm "
							+ " \n";
					textArea.append(s);

					label = new Label(0, nrows, s);
					sheet.addCell(label);
					nrows++;

					s = resources.getString("text.tvlEstimation")// "TVL dose estimation: "
							+ Convertor.formatNumberScientific(dosetvl)
							+ resources.getString("text.tvlEstimation1")// " uSv/hr; TVL: "
							+ tvl + resources.getString("text.cm")// " cm "
							+ " \n";
					textArea.append(s);

					label = new Label(0, nrows, s);
					sheet.addCell(label);
					nrows++;
				}
				workbook.write();
				workbook.close();
			} catch (Exception err) {
				System.out.println("ERROR: " + err);
			}

		}
	}

	/**
	 * Start displaying computation
	 */
	private void display() {
		performRadiationDisplay();
		// ---------------------------
		stopThread();// kill all threads
		statusL.setText(resources.getString("status.done"));

	}

	/**
	 * Evaluate absorber thickness for SHIELDING
	 */
	private void evaluateThickness() {
		textArea.selectAll();
		textArea.replaceSelection("");

		try {
			distance = Convertor.stringToDouble(distanceTf.getText());
			mainEnergy = Convertor.stringToDouble(mainEnergyTf.getText());
			measuredDoseRate = Convertor.stringToDouble(measuredDoseRateTf
					.getText());
			desiredDoseRate = Convertor.stringToDouble(desiredDoseRateTf
					.getText());
		} catch (Exception e) {
			// String s="Insert real, positive number!"+" \n";
			// textArea.append(s);
			String title = resources.getString("number.error.title");
			String message = resources.getString("number.error");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);

			stopThread();// kill all threads
			return;
		}
		// ========now evaluate required thickness.
		double dose = measuredDoseRate;// 0.0;
		double thi = 0.0;// MAXIMUM SEAK!!!
		double tlo = 0.0;
		double tt = 0.0;
		int kx = 0;
		double t1 = 0.0;
		double g = 0.0;

		double b = 1.0;
		double rmfp = 0.0;

		while (true) {
			if (STOPCOMPUTATION)
				return;
			kx = kx + 1;// contor iter

			t1 = measuredDoseRate * b * Math.exp(-rmfp);// uSv/hr

			if (kx == 1) {
				dose = t1;
				thi = distance;// initialise with maximum
								// available...shield=distance//THI_HVL2;
			} else {
				if (t1 > desiredDoseRate) {
					tlo = tt;
				} else {
					thi = tt;
				}// HVL
				if (t1 <= (desiredDoseRate) * 1.00005
						&& t1 >= (desiredDoseRate) * 0.99995)// >=,<= for
																// 0!!!!!!!!
				{
					// System.out.println("HVL succes!");
					break;
				}
			}
			g = (tlo + thi) / 2.0;// 1st guess
			tt = g;

			double[] uattmasicrho = getShieldMassAtenuationCoefficient(mainEnergy);// ei);
			rmfp = uattmasicrho[0] * uattmasicrho[1] * tt;// @@@@@@@@@
			b = getShieldBuildUpFactor(mainEnergy, rmfp);// 10.4);//10);//mrf);

			if (kx > MAXITER) {
				break;
			}

		}
		if (dose == 0)
			g = 0;// NaN
		// verification:
		double[] uattmasicrho11 = getShieldMassAtenuationCoefficient(mainEnergy);
		double rmf = uattmasicrho11[0] * uattmasicrho11[1] * g;
		double bb = getShieldBuildUpFactor(mainEnergy, rmf);
		double dos = measuredDoseRate * bb * Math.exp(-rmf);// uSv/hr

		// String s="";//Statistical accuracy must be a positive number!"+" \n";

		String s = resources.getString("text.desiredDoseRate")// "Desired dose rate verification [arbitrary units]: "
				+ dos + resources.getString("text.desiredDoseRate2")// " at distance "
				+ distance + resources.getString("text.desiredDoseRate3")// " cm"
				+ " \n";
		textArea.append(s);
		s = resources.getString("shieldCb")// "Shield: "
				+ (String) shieldCb.getSelectedItem()
				+ resources.getString("text.requiredShield")// "; Required shield thickness [cm]: "
				+ g + " \n";
		textArea.append(s);

		tabs.setSelectedIndex(1);
		stopThread();// kill all threads
	}

	/**
	 * Computes radiation half value layer 
	 */
	private void computeHVL() {
		double dose = 0.0;
		double thi = 0.0;// MAXIMUM SEAK!!!
		double tlo = 0.0;
		double tt = 0.0;
		int kx = 0;
		double t1 = 0.0;

		int NEFF = ajV.size();// same size all vectors!

		// initialise with 1.0 and 0.0 vectors for NO Shielding!!!
		biV = new Vector<Double>();
		rmfpShieldV = new Vector<Double>();
		for (int i = 0; i < NEFF; i++) {
			biV.addElement(new Double(1.0));
			rmfpShieldV.addElement(new Double(0.0));
		}

		while (true) {
			if (STOPCOMPUTATION)
				return;
			t1 = 0.0;
			kx = kx + 1;// contor iter
			for (int l = 1; l <= NEFF; l++) {
				Double ajD = (Double) ajV.elementAt(l - 1);
				double aj = ajD.doubleValue();
				Double yiD = (Double) yiV.elementAt(l - 1);
				double yi = yiD.doubleValue();
				Double eiD = (Double) eiV.elementAt(l - 1);
				double ei = eiD.doubleValue();
				Double uiTissD = (Double) uiTissV.elementAt(l - 1);
				double uiTiss = uiTissD.doubleValue();

				Double biD = (Double) biV.elementAt(l - 1);
				double bi = biD.doubleValue();
				Double rmfpShieldD = (Double) rmfpShieldV.elementAt(l - 1);
				double rmfpShield = rmfpShieldD.doubleValue();

				// compute doserate...first time equal No Shield!!
				t1 = t1 + aj * ei * yi * uiTiss * bi * Math.exp(-rmfpShield)
						* 1.60218E-13 * 1.0E6 * 3600.0 / 0.001;// uSv/hr
				// double mevtojoule=1.60218E-13;
				// double factor=mevtojoule/0.001;//u->cm2/g
				// t1=t1*factor;//@@@@@@@@@@@@@@@@@@@@@Sv/sec
				// t1=t1*1.0E6*3600.0;//uSv/h
			}
			if (kx == 1) {
				dose = t1;
				thi = distance;// initialise with maximum
								// available...shield=distance//THI_HVL2;
			} else {
				if (t1 > dose / 2.0) {
					tlo = tt;
				} else {
					thi = tt;
				}// HVL
				if (t1 <= (dose / 2.0) * 1.00005
						&& t1 >= (dose / 2.0) * 0.99995)// >=,<= for 0!!!!!!!!
				{
					// System.out.println("HVL succes!");
					break;
				}
			}
			hvl = (tlo + thi) / 2.0;// 1st guess
			tt = hvl;

			biV = new Vector<Double>();
			rmfpShieldV = new Vector<Double>();
			for (int i = 1; i <= NEFF; i++) {
				Double eiD = (Double) eiV.elementAt(i - 1);
				double ei = eiD.doubleValue();
				double[] uattmasicrho = getShieldMassAtenuationCoefficient(ei);
				double rmfp = uattmasicrho[0] * uattmasicrho[1] * tt;// @@@@@@@@@
				double bii = getShieldBuildUpFactor(ei, rmfp);// 10.4);//10);//mrf);

				biV.addElement(new Double(bii));
				rmfpShieldV.addElement(new Double(rmfp));
			}

			if (kx > MAXITER) {
				break;
			}

		}
		dosehvl = t1;// dose;
		if (dose == 0)
			hvl = 0;// NaN
		// System.out.println("dose= "+dose);
		// System.out.println("HVL in cm"+" = "+hvl);
	}

	/**
	 * Computes radiation tenth value layer 
	 */
	private void computeTVL() {
		double dose = 0.0;
		double thi = 0.0;// MAXIMUM SEAK!!!
		double tlo = 0.0;
		double tt = 0.0;
		int kx = 0;
		double t1 = 0.0;

		int NEFF = ajV.size();// same size all vectors!

		// initialise with 1.0 and 0.0 vectors for NO Shielding!!!
		biV = new Vector<Double>();
		rmfpShieldV = new Vector<Double>();
		for (int i = 0; i < NEFF; i++) {
			biV.addElement(new Double(1.0));
			rmfpShieldV.addElement(new Double(0.0));
		}

		while (true) {
			if (STOPCOMPUTATION)
				return;
			t1 = 0.0;
			kx = kx + 1;// contor iter
			for (int l = 1; l <= NEFF; l++) {
				Double ajD = (Double) ajV.elementAt(l - 1);
				double aj = ajD.doubleValue();
				Double yiD = (Double) yiV.elementAt(l - 1);
				double yi = yiD.doubleValue();
				Double eiD = (Double) eiV.elementAt(l - 1);
				double ei = eiD.doubleValue();
				Double uiTissD = (Double) uiTissV.elementAt(l - 1);
				double uiTiss = uiTissD.doubleValue();

				Double biD = (Double) biV.elementAt(l - 1);
				double bi = biD.doubleValue();
				Double rmfpShieldD = (Double) rmfpShieldV.elementAt(l - 1);
				double rmfpShield = rmfpShieldD.doubleValue();

				// compute doserate...first time equal No Shield!!
				t1 = t1 + aj * ei * yi * uiTiss * bi * Math.exp(-rmfpShield)
						* 1.60218E-13 * 1.0E6 * 3600.0 / 0.001;// uSv/hr
				// double mevtojoule=1.60218E-13;
				// double factor=mevtojoule/0.001;//u->cm2/g
				// t1=t1*factor;//@@@@@@@@@@@@@@@@@@@@@Sv/sec
				// t1=t1*1.0E6*3600.0;//uSv/h
			}
			if (kx == 1) {
				dose = t1;
				thi = distance;// initialise with maximum
								// available...shield=distance//THI_HVL2;
			} else {
				if (t1 > dose / 10.0) {
					tlo = tt;
				} else {
					thi = tt;
				}// TVL
				if (t1 <= (dose / 10.0) * 1.00005
						&& t1 >= (dose / 10.0) * 0.99995) {
					// System.out.println("TVL succes!");
					break;
				}
			}
			tvl = (tlo + thi) / 2.0;// 1st guess
			tt = tvl;

			biV = new Vector<Double>();
			rmfpShieldV = new Vector<Double>();
			for (int i = 1; i <= NEFF; i++) {
				Double eiD = (Double) eiV.elementAt(i - 1);
				double ei = eiD.doubleValue();
				double[] uattmasicrho = getShieldMassAtenuationCoefficient(ei);
				double rmfp = uattmasicrho[0] * uattmasicrho[1] * tt;// @@@@@@@@@
				double bii = getShieldBuildUpFactor(ei, rmfp);// 10.4);//10);//mrf);

				biV.addElement(new Double(bii));
				rmfpShieldV.addElement(new Double(rmfp));
			}

			if (kx > MAXITER) {
				break;
			}

		}
		dosetvl = t1;// dose;
		if (dose == 0)
			tvl = 0;// NaN
		// System.out.println("dose= "+dose);
		// System.out.println("TVL in cm"+" = "+tvl);
	}

	/**
	 * Retrieve the shield (absorber) buildup factor
	 * @param en en
	 * @param mrf mrf
	 * @return the result
	 */
	private double getShieldBuildUpFactor(double en, double mrf) {
		double result = 1.0;

		// String datas = "Data";// resources.getString("data.load");//"Data";
		// String currentDir = System.getProperty("user.dir");
		// String file_sep = System.getProperty("file.separator");
		// String opens = currentDir + file_sep + datas;
		// String fileS = "buildup.xls";
		// String filename = opens + file_sep + fileS;

		String datas = resources.getString("data.load");// "Data";
		String shieldings = resources.getString("data.shielding");// "Data";
		String currentDir = System.getProperty("user.dir");
		String file_sep = System.getProperty("file.separator");
		String opens = currentDir + file_sep + datas;
		String fileS = resources.getString("data.shielding.buildup");// "buildup.xls";
		String filename = opens + file_sep + shieldings + file_sep + fileS;

		int index = shieldCb.getSelectedIndex();

		boolean tab2 = false;// 2 tabels
		long col = 0;
		long row = 0;
		long col1 = 0;// table 1
		long row1 = 0;// table 1
		long col2 = 0;// table 2
		long row2 = 0;// table 2
		int offset = 0;// offset

		// double rho = 0.0;
		// double u = 0.0;
		// double uu = 0.0;

		double emin = 0.0;
		double emax = 0.0;
		double rmin = 0.0;
		// double rminspecial = -1.0;
		double rmax = 0.0;
		// double rmaxspecial = -1.0;
		int colmin = 0;
		int colmax = 0;// after E
		int rowmin = 0;
		int rowmax = 0;// after r
		int rowminspecial = -1;
		int rowmaxspecial = -1;

		double lastetab1 = 0.0;

		try {
			Workbook workbook11 = Workbook.getWorkbook(new File(filename));
			Sheet sheet11 = workbook11.getSheet(index); // always coeff.xls has
														// sheet 0 pointed to
														// TISSUE
														// coefficients!!!

			NumberCell nc = (NumberCell) sheet11.getCell(1, 0);// B1
			col = Math.round(nc.getValue());
			col1 = col;
			nc = (NumberCell) sheet11.getCell(3, 0);// D1
			row = Math.round(nc.getValue());
			row1 = row;
			nc = (NumberCell) sheet11.getCell(1, 18);// B19
			col2 = Math.round(nc.getValue());
			nc = (NumberCell) sheet11.getCell(3, 18);// D19
			row2 = Math.round(nc.getValue());

			offset = 2;// 20 respectevly

			Long lll = new Long(col1);
			int col11 = lll.intValue();
			nc = (NumberCell) sheet11.getCell(col11, 1);
			lastetab1 = nc.getValue();
			boolean specialCase = false;
			// System.out.println("Last "+lastetab1);

			double e = en;

			for (int k = 1; k < col - 1 + 1; k++)// 0=R(mfp)..and k obviously go
													// to col-1...look for
													// energy
			{
				NumberCell nca = (NumberCell) sheet11.getCell(k, offset - 1);
				double ea = nca.getValue();
				NumberCell ncb = (NumberCell) sheet11
						.getCell(k + 1, offset - 1);
				double eb = ncb.getValue();
				// System.out.println("ea "+ea+" eb "+eb);
				// NumberCell ncaa = (NumberCell) sheet11.getCell(2,k);
				// double ua = ncaa.getValue();
				// NumberCell ncbb = (NumberCell)
				// sheet11.getCell(2,k+1);//mass-absorbtion
				// double ub = ncbb.getValue();

				if ((e >= ea) && (k == 1) && (!tab2))// first
				{
					emin = eb;
					emax = ea;// u=linLogInt(ea,ua,eb,ub,e);
					colmin = k + 1;
					colmax = k;
					break;
				}
				if ((tab2) && (e <= lastetab1) && (e >= ea) && (k == 1))// first
																		// in
																		// tab
																		// 2..check
																		// to
																		// last
																		// in
																		// tab1
				{
					emin = ea;
					emax = lastetab1;
					colmin = k;
					colmax = col11;
					specialCase = true;
					// System.out.println("SPECIAL ");
					break;
				}
				if ((ea >= e) && (e >= eb)) {
					// System.out.println("Intra in pizda masii!");
					emin = eb;
					emax = ea;// u=linLogInt(ea,ua,eb,ub,e);
					colmin = k + 1;
					colmax = k;
					break;// find the bin
				}
				if ((e <= ea) && (k == col - 1))// 2))//last
				{
					if (tab2) {
						// u=linLogInt(ea,ua,eb,ub,e);
						emin = eb;
						emax = ea;
						colmin = k + 1;
						colmax = k;
						break;
					} else {
						// System.out.println("Intra futui pizda masii!");
						// e>eb also ..see above if
						tab2 = true;
						row = row2;
						col = col2;
						k = 0;
						offset = 20;
					}
				}
			}

			double mfp = mrf;
			// if (tab2)
			// {
			// row=row2;offset=20;//already done!
			// }

			for (int k = offset; k < offset + row - 1; k++)// look for mfp
			{
				NumberCell nca = (NumberCell) sheet11.getCell(0, k);// col 0!!
				double ra = nca.getValue();
				NumberCell ncb = (NumberCell) sheet11.getCell(0, k + 1);
				double rb = ncb.getValue();
				// System.out.println("ra "+ra+" rb "+rb);

				if ((mfp <= ra) && (k == offset))// first
				{
					rmin = ra;
					rmax = rb;// u=linLogInt(ea,ua,eb,ub,e);
					rowmin = k;
					rowmax = k + 1;
					break;
				}

				if ((ra <= mfp) && (mfp <= rb)) {
					rmin = ra;
					rmax = rb;// u=linLogInt(ea,ua,eb,ub,e);
					rowmin = k;
					rowmax = k + 1;
					break;// find the bin
				}
				if ((mfp >= ra) && (k == offset + row - 2))// 2))//last
				{
					rmin = ra;
					rmax = rb;
					rowmin = k;
					rowmax = k + 1;
					break;
				}
			}

			if (specialCase) {
				offset = 2;
				row = row1;
				for (int k = offset; k < offset + row - 1; k++)// look for mfp
				{
					NumberCell nca = (NumberCell) sheet11.getCell(0, k);// col
																		// 0!!
					double ra = nca.getValue();
					NumberCell ncb = (NumberCell) sheet11.getCell(0, k + 1);
					double rb = ncb.getValue();
					// System.out.println("ra special "+ra+" rb special "+rb);

					if ((mfp <= ra) && (k == offset))// first
					{
						// rminspecial = ra;
						// rmaxspecial = rb;// u=linLogInt(ea,ua,eb,ub,e);
						rowminspecial = k;
						rowmaxspecial = k + 1;
						break;
					}

					if ((ra <= mfp) && (mfp <= rb)) {
						// rminspecial = ra;
						// rmaxspecial = rb;// u=linLogInt(ea,ua,eb,ub,e);
						rowminspecial = k;
						rowmaxspecial = k + 1;
						break;// find the bin
					}
					if ((mfp >= ra) && (k == offset + row - 2))// 2))//last
					{
						// rminspecial = ra;
						// rmaxspecial = rb;
						rowminspecial = k;
						rowmaxspecial = k + 1;
						break;
					}
				}

			}
			// now perform calculation
			NumberCell nccc = null;

			if (specialCase) {
				nccc = (NumberCell) sheet11.getCell(colmax, rowminspecial);
			} else {
				nccc = (NumberCell) sheet11.getCell(colmax, rowmin);
			}
			double bRminEmax = nccc.getValue();

			// if (specialCase)
			// {
			// nccc = (NumberCell) sheet11.getCell(colmin,rowminspecial);
			// }
			// else
			// {
			nccc = (NumberCell) sheet11.getCell(colmin, rowmin);// next e=>tab2
																// always!!
			// }
			double bRminEmin = nccc.getValue();

			if (specialCase) {
				nccc = (NumberCell) sheet11.getCell(colmax, rowmaxspecial);
			} else {
				nccc = (NumberCell) sheet11.getCell(colmax, rowmax);
			}
			double bRmaxEmax = nccc.getValue();

			// if (specialCase)
			// {
			// nccc = (NumberCell) sheet11.getCell(colmin,rowmaxspecial);
			// }
			// else
			// {
			nccc = (NumberCell) sheet11.getCell(colmin, rowmax);
			// }
			double bRmaxEmin = nccc.getValue();
			// ================here not log int!!
			double ue1 = Interpolation.linInt(emin, bRminEmin, emax, bRminEmax,
					e);
			double ue2 = Interpolation.linInt(emin, bRmaxEmin, emax, bRmaxEmax,
					e);
			// System.out.println(" ue1 "+ue1+" ue2 "+ue2);
			double ue3 = Interpolation.linInt(rmin, ue1, rmax, ue2, mfp);// based
																			// on
																			// table2
			// !!!!!!!!!!!
			result = ue3;

			// System.out.println("e "+e+" emin "+emin+" emax "+emax+" rmin "+rmin+" rmax "+rmax+" rmispecial "+rminspecial+" rmaxspecial "+rmaxspecial);//WORKS!!
			// System.out.println("brminemax "+bRminEmax+
			// " bRminEmin "+bRminEmin+" bRmaxEmax "+bRmaxEmax+" bRmaxEmin "+bRmaxEmin);
			workbook11.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		// result=ue3;
		return result;
	}

	/**
	 * Retrieve absorber mass attenuation coefficient
	 * @param en en
	 * @return the result
	 */
	private double[] getShieldMassAtenuationCoefficient(double en) {
		double[] result = new double[2];// 1.0;

		// String datas="Data";//resources.getString("data.load");//"Data";
		// String currentDir=System.getProperty("user.dir");
		// String file_sep=System.getProperty("file.separator");
		// String opens=currentDir+file_sep+datas;
		// String fileS="coeff.xls";
		// String filename = opens+file_sep+fileS;
		String datas = resources.getString("data.load");// "Data";
		String shieldings = resources.getString("data.shielding");// "Data";
		String currentDir = System.getProperty("user.dir");
		String file_sep = System.getProperty("file.separator");
		String opens = currentDir + file_sep + datas;
		String fileS = resources.getString("data.shielding.coeff");// "coeff.xls";
		String filename = opens + file_sep + shieldings + file_sep + fileS;

		double rho = 0.0;
		double u = 0.0;
		// double uu=0.0;

		int index = shieldCb.getSelectedIndex();
		index = index + 1;// 0=tissue

		try {
			Workbook workbook11 = Workbook.getWorkbook(new File(filename));
			Sheet sheet11 = workbook11.getSheet(index);

			// int ncol=sheet11.getColumns();
			int nrow = sheet11.getRows();
			int offset = 2;// 0,1 cells=>antet

			double e = en;// renergy[i][j];//10000.0009;//38;//009;
			// double u=0.0;//linLogInt(e1,u1,e2,u2,e);
			// double uu=0.0;//linInt(e1,u1,e2,u2,e);

			NumberCell nca0 = (NumberCell) sheet11.getCell(3, 2);
			rho = nca0.getValue();

			for (int k = offset; k < nrow - 1; k++)// loop through data
			{
				NumberCell nca = (NumberCell) sheet11.getCell(0, k);
				double ea = nca.getValue();
				NumberCell ncb = (NumberCell) sheet11.getCell(0, k + 1);
				double eb = ncb.getValue();

				NumberCell ncaa = (NumberCell) sheet11.getCell(1, k);
				double ua = ncaa.getValue();
				NumberCell ncbb = (NumberCell) sheet11.getCell(1, k + 1);// mass-attenuation
				double ub = ncbb.getValue();

				if ((e <= ea) && (k == offset))// first
				{
					u = Interpolation.linLogInt(ea, ua, eb, ub, e);
					// uu=Interpolation.linInt(ea,ua,eb,ub,e);
					break;
				}
				if ((ea <= e) && (e <= eb)) {
					u = Interpolation.linLogInt(ea, ua, eb, ub, e);
					// uu=Interpolation.linInt(ea,ua,eb,ub,e);
					break;// find the bin
				}
				if ((e >= ea) && (k == nrow - 2))// last
				{
					u = Interpolation.linLogInt(ea, ua, eb, ub, e);
					// uu=Interpolation.linInt(ea,ua,eb,ub,e);
					break;
				}
			}
			// System.out.println("e "+e+" AUTO "+u+" AUTO lin "+uu);//WORKS!!
			workbook11.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		result[0] = u;
		result[1] = rho;
		return result;
	}

	/**
	 * Retrieve tissue mass attenuation coefficient
	 * @param en en
	 * @return the result
	 */
	private double getTissueMassAbsorbtionCoefficient(double en) {
		double result = 1.0;

		String datas = resources.getString("data.load");// "Data";
		String shieldings = resources.getString("data.shielding");// "Data";
		String currentDir = System.getProperty("user.dir");
		String file_sep = System.getProperty("file.separator");
		String opens = currentDir + file_sep + datas;

		// String datas="Data";//resources.getString("data.load");//"Data";
		// String currentDir=System.getProperty("user.dir");
		// String file_sep=System.getProperty("file.separator");
		// String opens=currentDir+file_sep+datas;
		String fileS = resources.getString("data.shielding.coeff");// "coeff.xls";
		// String filename = opens+file_sep+fileS;
		String filename = opens + file_sep + shieldings + file_sep + fileS;
		// double rho=0.0;
		double u = 0.0;
		// double uu=0.0;

		try {
			Workbook workbook11 = Workbook.getWorkbook(new File(filename));
			Sheet sheet11 = workbook11.getSheet(0); // always coeff.xls has
													// sheet 0 pointed to TISSUE
													// coefficients!!!

			// int ncol=sheet11.getColumns();
			int nrow = sheet11.getRows();
			int offset = 2;// 0,1 cells=>antet

			double e = en;// renergy[i][j];//10000.0009;//38;//009;
			// double u=0.0;//linLogInt(e1,u1,e2,u2,e);
			// double uu=0.0;//linInt(e1,u1,e2,u2,e);

			// NumberCell nca0 = (NumberCell) sheet11.getCell(3,2);
			// rho = nca0.getValue();

			for (int k = offset; k < nrow - 1; k++)// loop through data
			{
				NumberCell nca = (NumberCell) sheet11.getCell(0, k);
				double ea = nca.getValue();
				NumberCell ncb = (NumberCell) sheet11.getCell(0, k + 1);
				double eb = ncb.getValue();

				NumberCell ncaa = (NumberCell) sheet11.getCell(2, k);
				double ua = ncaa.getValue();
				NumberCell ncbb = (NumberCell) sheet11.getCell(2, k + 1);// mass-absorbtion
				double ub = ncbb.getValue();

				if ((e <= ea) && (k == offset))// first
				{
					u = Interpolation.linLogInt(ea, ua, eb, ub, e);
					// uu=Interpolation.linInt(ea,ua,eb,ub,e);
					break;
				}
				if ((ea <= e) && (e <= eb)) {
					u = Interpolation.linLogInt(ea, ua, eb, ub, e);
					// uu=Interpolation.linInt(ea,ua,eb,ub,e);
					break;// find the bin
				}
				if ((e >= ea) && (k == nrow - 2))// last
				{
					u = Interpolation.linLogInt(ea, ua, eb, ub, e);
					// uu=Interpolation.linInt(ea,ua,eb,ub,e);
					break;
				}
			}
			// System.out.println("e "+e+" AUTO "+u+" AUTO lin "+uu);//WORKS!!
			workbook11.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		result = u;
		return result;
	}

	/**
	 * Start evaluating absorber (shield) thickness
	 */
	private void evaluate() {
		evaluateThickness();
		// ---------------------------
		stopThread();// kill all threads
		statusL.setText(resources.getString("status.done"));

	}

	/**
	 * Start decay computations for graph
	 */
	private void graph() {
		plotSteps = 50;// default!
		// protected double[][] plotActiv=new double[0][0];
		// String s="";
		if (nuclides_all.length < 1)// no data
		{
			String title = resources.getString("number.error.title");
			String message = resources.getString("calculation.required");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);

			return;
		}
		if (IA_use != IA_norm) {
			return;
		}
		int nnuc = nuclideS.size();

		String[] nucs = new String[nnuc];
		double[] branchingRatio = new double[nnuc];
		double[] halfLife = new double[nnuc];
		double[] atomicMass = new double[nnuc];
		double[] initialMass = new double[nnuc];
		double[] initialNumberAtomsFromcSource = new double[nnuc];

		plotNucNumber = nnuc;
		plotNucSymbol = new String[nnuc];

		for (int i = 0; i < nnuc; i++) {
			String nuc = (String) nuclideS.elementAt(i);
			nucs[i] = nuc;
			plotNucSymbol[i] = nuc;// nuclides_all[i] = nuc;

			Double yD = (Double) yieldD.elementAt(i);
			double yi = yD.doubleValue();
			branchingRatio[i] = yi;

			Double amD = (Double) atomicMassD.elementAt(i);
			double am = amD.doubleValue();
			atomicMass[i] = am;

			Double hlD = (Double) halfLifeD.elementAt(i);
			double hl = hlD.doubleValue();
			halfLife[i] = hl;
			String hlu = (String) halfLifeUnitsS.elementAt(i);
			halfLife[i] = formatHalfLife(halfLife[i], hlu);

			double dc = Math.log(2.0) / halfLife[i];
			double navogadro = 6.02214199E26;// atoms/kmol!!
			if (i == 0)
				initialMass[i] = activity * atomicMass[i] / (navogadro * dc);// only
																				// parent
			else
				initialMass[i] = 0.0;
			initialNumberAtomsFromcSource[i] = 0.0;// no external sources
		}
		double[] dc = PhysUtilities.computeDecayConstant(halfLife);
		double[] pdc = PhysUtilities.computePartialDecayConstant(halfLife,
				branchingRatio);
		double[] initialNuclideNumbers = PhysUtilities
				.computeInitialNuclideNumbers(initialMass, atomicMass);

		PhysUtilities.steps = plotSteps;
		PhysUtilities.t = elapsedTime;
		plotTime = PhysUtilities.t;
		// compute===================================================================
		PhysUtilities.bateman(nnuc, dc, pdc, initialNuclideNumbers,
				initialNumberAtomsFromcSource);

		// double t = PhysUtilities.t;
		// int steps = PhysUtilities.steps;
		int j = PhysUtilities.steps;
		plotActiv = new double[PhysUtilities.at.length][j + 1];
		for (int i = 0; i < PhysUtilities.at.length; i++) {
			for (int k = 0; k <= PhysUtilities.steps; k++) {
				plotActiv[i][k] = PhysUtilities.at[i][k];
			}
		}
		// ==========================================================

		textArea.selectAll();
		textArea.replaceSelection("");

		// String prints = "";
		String ones = "";
		// boolean printB = false;
		if (PhysUtilities.t12m != 0.0) {
			ones = resources.getString("decay.idealEq.")// "Theoretical time elapsed for ideal equilibrum[sec]= "
					+ PhysUtilities.t12m + "; " + PhysUtilities.t12m
					/ 3600.
					+ resources.getString("decay.h")
					+ PhysUtilities.t12m
					/ (3600. * 24.) + resources.getString("decay.days");
			// prints = prints + ones + "\n";

			textArea.append(ones + "\n");
		}
		ones = resources.getString("decay.actualEq.");// "Time elapsed for secular equilibrum between first and second nuclides is computed when differences between activities falls below 1%.";
		// prints = prints + ones + "\n";
		textArea.append(ones + "\n");

		for (int jj = 0; jj <= PhysUtilities.steps; jj++) {
			textArea.append("--------------------" + "\n");
			for (int i = 1; i <= plotNucNumber; i++) {
				double ac = PhysUtilities.at[i - 1][jj];
				if (ac < 0.0)
					ac = 0.0;// round off errors!
				// if (jj == PhysUtilities.steps) {//at final!!
				ones = resources.getString("text.nuclide")// "nuc: "
						+ plotNucSymbol[i - 1]
						+ "; "
						+ resources.getString("decay.activity")// " activity[Bq]= "
						+ ac// PhysUtilities.at[i - 1][jj]
						+ resources.getString("decay.after")// " after "
						+ jj
						* PhysUtilities.t
						/ PhysUtilities.steps
						+ resources.getString("decay.sec")// " sec.; "
						+ jj
						* PhysUtilities.t
						/ (3600. * PhysUtilities.steps)
						+ resources.getString("decay.h")// " h; "
						+ jj
						* PhysUtilities.t
						/ (24. * 3600. * PhysUtilities.steps)
						+ resources.getString("decay.days")// " days "
				;

				// prints = prints + ones + "\n";
				textArea.append(ones + "\n");
				// }
			}

			if (plotNucNumber > 1)
				if (Math.abs(PhysUtilities.at[0][jj] - PhysUtilities.at[1][jj])
						/ PhysUtilities.at[0][jj] < 0.01) {
					// break;
					// if (!printB) {
					ones = resources.getString("decay.timeElapsed")// "Time elapsed ="
							+ jj
							* PhysUtilities.t
							/ PhysUtilities.steps
							+ resources.getString("decay.sec")// " sec.; "
							+ jj
							* PhysUtilities.t
							/ (3600. * PhysUtilities.steps)
							+ resources.getString("decay.h")// " h.; "
							+ jj
							* PhysUtilities.t
							/ (24 * 3600. * PhysUtilities.steps)
							+ resources.getString("decay.days")// " days "
					;

					// prints = prints + ones + "\n";
					textArea.append(ones + "\n");

					ones = resources.getString("decay.parent.activity")// "Parent activity [Bq]= "
							+ PhysUtilities.at[0][jj]
							+ resources.getString("decay.1stdaughter.activity")// " ; 1st daughter activity [Bq]= "
							+ PhysUtilities.at[1][jj];
					// prints = prints + ones + "\n";
					textArea.append(ones + "\n");

					// printB = true;
					// }
				}
		}
		tabs.setSelectedIndex(1);
		new DecayGraph(this);
	}

	/**
	 * Call the actual graph class to plot the decay
	 */
	private void plot() {
		graph();
		// -----------
		stopThread();// kill all threads
		statusL.setText(resources.getString("status.done"));
	}

	/**
	 * Changing the program look and feel can be done done here. Also displays some gadgets.
	 */
	private void lookAndFeel() {
		setVisible(false);// setEnabled(false);
		new ScanDiskLFGui(this);
	}

	/**
	 * Program close!
	 */
	private void attemptExit() {
		performCleaningUp();
		stopThread();

		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (Exception sqle) {
			sqle.printStackTrace();
			// stopThread();// kill all threads
			// statusL.setText(resources.getString("status.done"));
		}

		if (standalone) {
			DatabaseAgent.shutdownDerby();//DBConnection.shutdownDerby();
			dispose();
			System.exit(0);
		} else {
			parent.setVisible(true);
			dispose();
		}
	}

	/**
	 * Shows the about window!
	 */
	private void about() {
		new AboutFrame(this);
	}

	/**
	 * Initiate stop computations.
	 */
	private void kill() {
		// STOPCOMPUTATION=true;
		// System.out.println("kill");
		stopAppend = true;
		stopThread();
	}

	/**
	 * Start the computation thread.
	 */
	private void startThread() {
		stopAnim = false;

		if (computationTh == null) {
			STOPCOMPUTATION = false;
			computationTh = new Thread(this);
			computationTh.start();// Allow one simulation at time!
			// setEnabled(false);
		}

		if (statusTh == null) {
			statusTh = new Thread(this);
			statusTh.start();
		}
	}

	/**
	 * Stop the computation thread. 
	 */
	private void stopThread() {
		statusTh = null;
		frameNumber = 0;
		stopAnim = true;

		if (computationTh == null) {
			stopAppend = false;// press kill button but simulation never
								// started!
			return;
		}
		computationTh = null;
		if (stopAppend) {// kill button was pressed!
			STOPCOMPUTATION = true;
			// Alpha_MC.STOPSIMULATION = true;// tell to stop simulation loop
			// immediatly!
			textArea.append(resources.getString("text.simulation.stop") + "\n");
			tabs.setSelectedIndex(1);
			stopAppend = false;
			String label = resources.getString("status.done");
			statusL.setText(label);
		}
		// setEnabled(true);
	}

	private boolean stopAnim = true;

	/**
	 * Thread specific run method
	 */
	public void run() {
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);// both thread
																	// same
																	// priority
		Thread.yield();
		long startTime = System.currentTimeMillis();
		Thread currentThread = Thread.currentThread();
		while (!stopAnim && statusTh == currentThread) {// if thread is status
														// display
			// Thread!!

			frameNumber++;
			if (frameNumber % 2 == 0)
				statusL.setText(statusRunS + ".....");
			else
				statusL.setText(statusRunS);

			// Delay
			try {
				startTime += delay;
				Thread.sleep(Math.max(0, startTime - System.currentTimeMillis()));
			} catch (InterruptedException e) {
				break;
			}
		}

		if (currentThread == computationTh) {// if thread is the main
												// computation Thread!!
			if (command.equals(CALCULATE_COMMAND)) {
				calculate();
			} else if (command.equals(DISPLAY_COMMAND)) {
				display();
			} else if (command.equals(EVALUATE_COMMAND)) {
				evaluate();
			} else if (command.equals(GRAPH_COMMAND)) {
				plot();
			}
		}
	}

	@SuppressWarnings("unused")
	private void createDerbyDatabase(String dbName) throws Exception {
		String datas = resources.getString("data.load");// Data
		String currentDir = System.getProperty("user.dir");
		String file_sep = System.getProperty("file.separator");
		String opens = currentDir + file_sep + datas;

		opens = opens + file_sep + dbName;

		String protocol = "jdbc:derby:";

		DriverManager.getConnection(protocol + opens + ";create=true", "", "");
	}

	/**
	 * Copy a MSAcces database table directly to derby database.
	 */
	@SuppressWarnings("unused")
	private void copyMsAccesIngestionTable() {
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "icrp72.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			Connection conec1 = DriverManager.getConnection(database, "", "");

			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Ingestion Adult]";
			// String statement = "select *" + " from JAERI03index";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = internalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table IngestionAdult");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table IngestionAdult (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + "HalfLife VARCHAR(100), "
					+ "f1 DOUBLE PRECISION," + " Adrenals DOUBLE PRECISION,"
					+ " UrinaryBladder DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "Stomach DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ " Colon DOUBLE PRECISION, " + "Kidneys DOUBLE PRECISION,"
					+ "Liver DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION,"
					+ "ExtratrachialAirways DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION,"
					+ " Remainder DOUBLE PRECISION," + " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM IngestionAdult");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into IngestionAdult values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM IngestionAdult");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table IngestionAdult");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}
	
	/**
	 * For Java 8 you cannot use the JDBC-ODBC Bridge because it has been removed.
	 * You will need to use something like UCanAccess instead.
	 * For more information, see BLA BLA BLA
	 * 
	 * Now "U Can Access" data in .accdb and .mdb files using code like this

// assumes...
//     import java.sql.*;
Connection conn=DriverManager.getConnection(
        "jdbc:ucanaccess://C:/__tmp/test/zzz.accdb");
Statement s = conn.createStatement();
ResultSet rs = s.executeQuery("SELECT [LastName] FROM [Clients]");
while (rs.next()) {
    System.out.println(rs.getString(1));
}

	 * DE AIA NU-MI PLAC MIE UPDATE-URILE...BAGA-MI-AS **** SA-MI BAG!!!!
	 */
	@SuppressWarnings("unused")
	private void copyMsAccesIngestionTable1() {//1 year
		try {
			//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");//java 8 error!!!
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "icrp72.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			//String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			//database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			//Connection conec1 = DriverManager.getConnection(database, "", "");

			//---------------
			String database = "jdbc:ucanaccess://"+filename;
			Connection conec1 = DriverManager.getConnection(database);//, "", "");
			//-------------
			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Ingestion 1 yr-old]";
			// String statement = "select *" + " from JAERI03index";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<Object> randVec;//Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<Object>();//new Vector<String>();
					randVec.addElement(id);//(Convertor.intToString(id));// @@@@@
					//String row = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						//String rsStr = rs.getString(i);
						Object rsStr = rs.getObject(i);
						randVec.addElement(rsStr);
						//row = row+rsStr+"; ";
					}
					//System.out.println(row);//ok!
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = internalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			s.execute("drop table Ingestion1");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table Ingestion1 (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + "HalfLife VARCHAR(100), "
					+ "f1 VARCHAR(100)," + " Adrenals DOUBLE PRECISION,"//f1 is bad in MsAccess=>must be String
					+ " UrinaryBladder DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "Stomach DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ " Colon DOUBLE PRECISION, " + "Kidneys DOUBLE PRECISION,"
					+ "Liver DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION,"
					+ "ExtratrachialAirways DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION,"
					+ " Remainder DOUBLE PRECISION," + " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM Ingestion1");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into Ingestion1 values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				//String[] dataRow = new String[v.size()];
				Object[] dataRow = new Object[v.size()];
				for (int j = 0; j < v.size(); j++) {
					//dataRow[j] = (String) v.elementAt(j);
					dataRow[j] = (Object) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					//psInsert.setString(j + 1, dataRow[j]);
					psInsert.setObject(j+1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM Ingestion1");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table IngestionAdult");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}
	
	@SuppressWarnings("unused")
	private void copyMsAccesIngestionTable10() {//1 year
		try {
			//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");//java 8 error!!!
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "icrp72.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			//String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			//database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			//Connection conec1 = DriverManager.getConnection(database, "", "");

			//---------------
			String database = "jdbc:ucanaccess://"+filename;
			Connection conec1 = DriverManager.getConnection(database);//, "", "");
			//-------------
			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Ingestion 10 yr-old]";
			// String statement = "select *" + " from JAERI03index";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<Object> randVec;//Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<Object>();//new Vector<String>();
					randVec.addElement(id);//(Convertor.intToString(id));// @@@@@
					//String row = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						//String rsStr = rs.getString(i);
						Object rsStr = rs.getObject(i);
						randVec.addElement(rsStr);
						//row = row+rsStr+"; ";
					}
					//System.out.println(row);//ok!
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = internalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			//s.execute("drop table Ingestion10");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table Ingestion10 (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + "HalfLife VARCHAR(100), "
					+ "f1 VARCHAR(100)," + " Adrenals DOUBLE PRECISION,"//f1 is bad in MsAccess=>must be String
					+ " UrinaryBladder DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "Stomach DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ " Colon DOUBLE PRECISION, " + "Kidneys DOUBLE PRECISION,"
					+ "Liver DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION,"
					+ "ExtratrachialAirways DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION,"
					+ " Remainder DOUBLE PRECISION," + " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM Ingestion10");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into Ingestion10 values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				//String[] dataRow = new String[v.size()];
				Object[] dataRow = new Object[v.size()];
				for (int j = 0; j < v.size(); j++) {
					//dataRow[j] = (String) v.elementAt(j);
					dataRow[j] = (Object) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					//psInsert.setString(j + 1, dataRow[j]);
					psInsert.setObject(j+1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM Ingestion10");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table IngestionAdult");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private void copyMsAccesInhalationTable() {
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "icrp72.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			Connection conec1 = DriverManager.getConnection(database, "", "");

			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Inhalation Adult]";
			// String statement = "select *" + " from JAERI03index";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = internalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table IngestionAdult");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table InhalationAdult (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + "HalfLife VARCHAR(100), "
					+ "Type VARCHAR(100), " + "f1 VARCHAR(100),"
					+ " Adrenals DOUBLE PRECISION,"
					+ " UrinaryBladder DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "Stomach DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ " Colon DOUBLE PRECISION, " + "Kidneys DOUBLE PRECISION,"
					+ "Liver DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION,"
					+ "ExtratrachialAirways DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION,"
					+ " Remainder DOUBLE PRECISION," + " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM InhalationAdult");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into InhalationAdult values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM InhalationAdult");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table IngestionAdult");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}
	
	@SuppressWarnings("unused")
	private void copyMsAccesInhalationTable1() {
		try {
			//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "icrp72.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			//String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			//database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			//Connection conec1 = DriverManager.getConnection(database, "", "");

			String database = "jdbc:ucanaccess://"+filename;
			Connection conec1 = DriverManager.getConnection(database);//, "", "");
			
			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Inhalation 1 yr-old]";
			// String statement = "select *" + " from JAERI03index";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = internalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table Inhalation1");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table Inhalation1 (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + "HalfLife VARCHAR(100), "
					+ "Type VARCHAR(100), " + "f1 VARCHAR(100),"
					+ " Adrenals DOUBLE PRECISION,"
					+ " UrinaryBladder DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "Stomach DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ " Colon DOUBLE PRECISION, " + "Kidneys DOUBLE PRECISION,"
					+ "Liver DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION,"
					+ "ExtratrachialAirways DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION,"
					+ " Remainder DOUBLE PRECISION," + " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM Inhalation1");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into Inhalation1 values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM Inhalation1");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table IngestionAdult");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private void copyMsAccesInhalationTable10() {
		try {
			//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "icrp72.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			//String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			//database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			//Connection conec1 = DriverManager.getConnection(database, "", "");

			String database = "jdbc:ucanaccess://"+filename;
			Connection conec1 = DriverManager.getConnection(database);//, "", "");
			
			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Inhalation 10 yr-old]";
			// String statement = "select *" + " from JAERI03index";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = internalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table Inhalation1");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table Inhalation10 (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + "HalfLife VARCHAR(100), "
					+ "Type VARCHAR(100), " + "f1 VARCHAR(100),"
					+ " Adrenals DOUBLE PRECISION,"
					+ " UrinaryBladder DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "Stomach DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ " Colon DOUBLE PRECISION, " + "Kidneys DOUBLE PRECISION,"
					+ "Liver DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION,"
					+ "ExtratrachialAirways DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION,"
					+ " Remainder DOUBLE PRECISION," + " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM Inhalation10");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into Inhalation10 values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM Inhalation10");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table IngestionAdult");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}
	
	@SuppressWarnings("unused")
	private void copyMsAccesAirTable() {
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "FGR12.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			Connection conec1 = DriverManager.getConnection(database, "", "");

			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Air Submersion]";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = externalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table AirSubmersion");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table AirSubmersion (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + " Adrenals DOUBLE PRECISION,"
					+ " BladderWall DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "StomachWall DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ "Kidneys DOUBLE PRECISION," + "Liver DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION," + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION," + " hE DOUBLE PRECISION,"
					+ " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM AirSubmersion");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into AirSubmersion values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM AirSubmersion");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table AirSubmersion");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private void copyMsAccesWaterTable() {
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "FGR12.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			Connection conec1 = DriverManager.getConnection(database, "", "");

			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Water Submersion]";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = externalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table AirSubmersion");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table WaterSubmersion (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + " Adrenals DOUBLE PRECISION,"
					+ " BladderWall DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "StomachWall DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ "Kidneys DOUBLE PRECISION," + "Liver DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION," + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION," + " hE DOUBLE PRECISION,"
					+ " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM WaterSubmersion");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into WaterSubmersion values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM WaterSubmersion");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table AirSubmersion");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private void copyMsAccesGroundTable() {
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "FGR12.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			Connection conec1 = DriverManager.getConnection(database, "", "");

			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Ground Surface]";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = externalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table AirSubmersion");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table GroundSurface (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + " Adrenals DOUBLE PRECISION,"
					+ " BladderWall DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "StomachWall DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ "Kidneys DOUBLE PRECISION," + "Liver DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION," + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION," + " hE DOUBLE PRECISION,"
					+ " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM GroundSurface");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1
					.prepareStatement("insert into GroundSurface values "
							+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM GroundSurface");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table AirSubmersion");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	@SuppressWarnings("unused")
	private void copyMsAccesSoilTable() {
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			String datas = resources.getString("data.load");
			String currentDir = System.getProperty("user.dir");
			String file_sep = System.getProperty("file.separator");
			String opens = currentDir + file_sep + datas;

			String fileS = "FGR12.mdb";
			String filename = opens + file_sep + fileS;
			// System.out.println(filename);
			String database = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
			database += filename.trim() + ";DriverID=22;READONLY=true}";
			// now we can get the connection from the DriverManager
			Connection conec1 = DriverManager.getConnection(database, "", "");

			Statement s = conec1.createStatement();
			String statement = "select *" + " from [Infinite Soil]";
			s.execute(statement);

			ResultSet rs = s.getResultSet();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			Vector<Object> dataVec = new Vector<Object>();
			Vector<String> randVec;

			int id = 1;
			if (rs != null)
				while (rs.next())
				// this will step through our data row-by-row.
				{
					randVec = new Vector<String>();
					randVec.addElement(Convertor.intToString(id));// @@@@@
					for (int i = 1; i <= numberOfColumns; i++) {
						// We always operate with strings
						// even if column type is double (e.g.).
						// problem is null data..not empty String but NULL
						// object
						String rsStr = rs.getString(i);
						randVec.addElement(rsStr);
					}
					dataVec.addElement(randVec);
					id++;
				}

			// -------------------
			conec1.close();
			// .............
			opens = currentDir + file_sep + datas;
			String dbName = externalDB;
			opens = opens + file_sep + dbName;
			Connection con1 = DatabaseAgent.getConnection(opens, "", "");//DBConnection.getDerbyConnection(opens, "", "");
			s = con1.createStatement();

			// delete the table
			// s.execute("drop table AirSubmersion");

			// We create a table...
			String str = // "create table ICRP38Index (" + " ID integer,"
			"create table InfiniteSoil (" + " ID integer,"
					+ " Nuclide VARCHAR(100)," + " Adrenals DOUBLE PRECISION,"
					+ " BladderWall DOUBLE PRECISION, "
					+ "BoneSurface DOUBLE PRECISION,"
					+ " Brain DOUBLE PRECISION," + "Breast DOUBLE PRECISION,"
					+ " Esophagus DOUBLE PRECISION, "
					+ "StomachWall DOUBLE PRECISION, "
					+ "SmallIntestine DOUBLE PRECISION,"
					+ " UpperLargeIntestine DOUBLE PRECISION,"
					+ " LowerLargeIntestine DOUBLE PRECISION,"
					+ "Kidneys DOUBLE PRECISION," + "Liver DOUBLE PRECISION, "
					+ "Lungs DOUBLE PRECISION, " + "Muscle DOUBLE PRECISION,"
					+ " Ovaries DOUBLE PRECISION,"
					+ " Pancreas DOUBLE PRECISION, "
					+ "RedMarrow DOUBLE PRECISION," + "Skin DOUBLE PRECISION,"
					+ " Spleen DOUBLE PRECISION," + " Testes DOUBLE PRECISION,"
					+ " Thymus DOUBLE PRECISION,"
					+ " Thyroid DOUBLE PRECISION, "
					+ "Uterus DOUBLE PRECISION," + " hE DOUBLE PRECISION,"
					+ " E DOUBLE PRECISION )";
			s.execute(str);
			// Now table is created...copy!
			Vector<Object> colNumeVec = new Vector<Object>();
			rs = s.executeQuery("SELECT * FROM InfiniteSoil");
			rsmd = rs.getMetaData();
			numberOfColumns = rsmd.getColumnCount();
			String[] columns = new String[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++) {
				columns[i - 1] = rsmd.getColumnName(i);
				colNumeVec.addElement(columns[i - 1]);
			}

			PreparedStatement psInsert = null;
			// psInsert =
			// con1.prepareStatement("insert into ICRP38Index values "
			psInsert = con1.prepareStatement("insert into InfiniteSoil values "
					+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
					+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
					+ " ?, ?, ?, ?, ?, ?, ?)");

			for (int i = 0; i < dataVec.size(); i++) {
				@SuppressWarnings("unchecked")
				Vector<Object> v = (Vector<Object>) dataVec.elementAt(i);
				String[] dataRow = new String[v.size()];
				for (int j = 0; j < v.size(); j++) {
					dataRow[j] = (String) v.elementAt(j);
					// Only works with mysql:
					// DBOperation.insert(icrpTable, columns, dataRow, con1);

					// That is ALWAYS WORKING:
					psInsert.setString(j + 1, dataRow[j]);
				}
				psInsert.executeUpdate();
			}
			// /////////
			// Let's see what we got!
			rs = s.executeQuery("SELECT * FROM InfiniteSoil");
			if (rs != null)
				while (rs.next()) {
					String str1 = "";
					for (int i = 1; i <= numberOfColumns; i++) {
						str1 = str1 + ", " + rs.getString(i);
					}
					System.out.println("@ " + str1);
				}

			// delete the table
			// s.execute("drop table AirSubmersion");
			con1.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

	}
}
