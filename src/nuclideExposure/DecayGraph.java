package nuclideExposure;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import danfulea.math.Convertor;
import danfulea.utils.FrameUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
/**
 * Class designed to plot the decay graph.
 * @author Dan Fulea, 20 Jun. 2011
 *
 */
@SuppressWarnings("serial")
public class DecayGraph extends JFrame implements ActionListener {

	private static final Dimension PREFERRED_SIZE = new Dimension(980, 600);
	private final Dimension chartDimension = new Dimension(700, 450);
	private static final String BASE_RESOURCE_CLASS = "nuclideExposure.resources.NuclideExposureResources";
	private ResourceBundle resources;
	private static final String EVALUATE_COMMAND = "EVALUATE";
	private String command = null;

	private NuclideExposure mf;
	private ChartPanel chartPanel;

	public Color[] colors = { Color.red, Color.blue, Color.ORANGE, Color.GREEN,
			Color.CYAN, Color.BLACK, Color.YELLOW, Color.darkGray, Color.GRAY,
			Color.PINK, Color.lightGray, Color.MAGENTA };

	private JTextField stepTf = new JTextField(5);
	private JTextField resultTf = new JTextField(50);

	// private JButton computeB=new JButton("Compute time");

	/**
	 * Constructor
	 * @param mf the NuclideExposure object
	 */
	public DecayGraph(NuclideExposure mf) {
		this.mf = mf;
		resources = ResourceBundle.getBundle(BASE_RESOURCE_CLASS);
		this.setTitle(resources.getString("ViewGraph.NAME"));
		createGUI();

		setDefaultLookAndFeelDecorated(true);
		FrameUtilities.createImageIcon(
				this.resources.getString("form.icon.url"), this);

		FrameUtilities.centerFrameOnScreen(this);

		setVisible(true);
		mf.setEnabled(false);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);// not necessary,
																// exit normal!
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				attemptExit();
			}
		});

	}

	/**
	 * Setting up the frame size.
	 */
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	/**
	 * Exit method
	 */
	private void attemptExit() {
		mf.setEnabled(true);
		dispose();
	}

	/**
	 * Compute time steps.
	 */
	private void compute() {
		double d = 0;
		boolean b = true;
		String s = stepTf.getText();

		try {
			d = Convertor.stringToDouble(s);
			if (d < 0) {
				b = false;
				String title = resources.getString("number.error.title");
				String message = resources.getString("number.error");
				JOptionPane.showMessageDialog(null, message, title,
						JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception ex) {
			b = false;
			String title = resources.getString("number.error.title");
			String message = resources.getString("number.error");
			JOptionPane.showMessageDialog(null, message, title,
					JOptionPane.ERROR_MESSAGE);
		}

		if (!b)
			return;

		// String reS=d*PhysUtilities.t/PhysUtilities.steps+" sec; ";
		String reS = d * mf.plotTime / mf.plotSteps + " sec; ";
		// reS=reS+d*PhysUtilities.t/PhysUtilities.steps/3600.+" h; ";
		reS = reS + d * mf.plotTime / mf.plotSteps / 3600. + " h; ";
		// reS=reS+d*PhysUtilities.t/PhysUtilities.steps/(3600.*24.)+" days; ";
		reS = reS + d * mf.plotTime / mf.plotSteps / (3600. * 24.) + " days; ";
		// reS=reS+d*PhysUtilities.t/PhysUtilities.steps/(3600.*24.*365.25)+" years; ";
		reS = reS + d * mf.plotTime / mf.plotSteps / (3600. * 24. * 365.25)
				+ " years; ";

		resultTf.setText(reS);

	}

	/**
	 * Create GUI
	 */
	private void createGUI() {
		Character mnemonic = null;
		JButton button = null;
		String buttonName = "";
		String buttonToolTip = "";
		String buttonIconName = "";

		JPanel content = new JPanel();//new BorderLayout());
		BoxLayout blc = new BoxLayout(content, BoxLayout.Y_AXIS);
		content.setLayout(blc);
		//content.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		//JPanel graphP = createMainGraphPanel();
		createMainGraphPanel();
		//content.add(graphP, BorderLayout.CENTER);
		//graphP.setBackground(NuclideExposure.bkgColor);
		content.add(chartPanel);

		buttonName = resources.getString("computeTime.button");
		buttonToolTip = resources.getString("computeTime.button.toolTip");
		buttonIconName = resources.getString("img.set");
		button = FrameUtilities.makeButton(buttonIconName, EVALUATE_COMMAND,
				buttonToolTip, buttonName, this, this);
		mnemonic = (Character) resources
				.getObject("computeTime.button.mnemonic");
		button.setMnemonic(mnemonic.charValue());

		JPanel resP = new JPanel();
		resP.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		JLabel lab = new JLabel(resources.getString("time.steps.label"));//"Time Steps:");
		lab.setForeground(NuclideExposure.foreColor);
		resP.add(lab);
		resP.add(stepTf);
		resP.add(button);// computeB);
		resP.setBackground(NuclideExposure.bkgColor);
		// resP.add(resultTf);

		JPanel rP = new JPanel();
		rP.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
		lab = new JLabel(resources.getString("results.label"));//"Result:");
		lab.setForeground(NuclideExposure.foreColor);
		rP.add(lab);
		rP.add(resultTf);
		rP.setBackground(NuclideExposure.bkgColor);

		JPanel southP = new JPanel();
		BoxLayout bl = new BoxLayout(southP, BoxLayout.Y_AXIS);
		southP.setLayout(bl);
		southP.add(resP);
		southP.add(rP);
		southP.setBackground(NuclideExposure.bkgColor);

		Component rigidArea = Box.createRigidArea(new Dimension(0, 20));
		rigidArea.setBackground(NuclideExposure.bkgColor);// redundant
		content.add(rigidArea);// some space
		
		content.add(southP);//, BorderLayout.SOUTH);
		content.setBackground(NuclideExposure.bkgColor);
		
		//JPanel main2P = new JPanel(new BorderLayout());
		//main2P.add(content, BorderLayout.CENTER);
		//main2P.setBackground(NuclideExposure.bkgColor);

		//setContentPane(main2P);// (content);
		//setContentPane(new JScrollPane(main2P));
		//main2P.setOpaque(true); // content panes must be opaque
		setContentPane(new JScrollPane(content));
		content.setOpaque(true);
		
		pack();
	}

	/**
	 * Create the decay chart.
	 * @return the result
	 */
	private JFreeChart getChartPanel() {

		XYSeries[] series = new XYSeries[mf.plotNucNumber];
		for (int i = 0; i < mf.plotNucNumber; i++) {
			series[i] = new XYSeries(mf.plotNucSymbol[i]);
			// for (int j=0;j<=PhysUtilities.steps;j++)
			for (int j = 0; j <= mf.plotSteps; j++) {
				// series[i].add(j,PhysUtilities.at[i][j]);
				series[i].add(j, mf.plotActiv[i][j]);
			}
		}

		XYSeriesCollection data = new XYSeriesCollection(series[0]);
		for (int i = 1; i < mf.plotNucNumber; i++)
			data.addSeries(series[i]);

		NumberAxis xAxis = new NumberAxis(
				resources.getString("chart.x")//"TimeSteps [t/delta_t]; delta_t[sec]= " 
				+ mf.plotTime/ mf.plotSteps);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(
				resources.getString("chart.y")//"Activity [Bq]"
				);
		XYItemRenderer renderer = new StandardXYItemRenderer(
				StandardXYItemRenderer.LINES);
		// ------------------------
		for (int i = 0; i < mf.plotNucNumber; i++) {
			if (i < colors.length)
				renderer.setSeriesPaint(i, colors[i]);
			else
				renderer.setSeriesPaint(i, Color.BLACK);
		}
		renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

		// XYPlot plot = new XYPlot(data, xAxis, yAxis, renderer);
		// plot.setOrientation(PlotOrientation.VERTICAL);
		XYPlot plot = new XYPlot();
		plot.setOrientation(PlotOrientation.VERTICAL);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		// allow chart movement by pressing CTRL and drag with mouse!
		plot.setDomainPannable(true);
		plot.setRangePannable(true);
		// 1st axis
		plot.setDomainAxis(0, xAxis);// the axis index;axis
		plot.setRangeAxis(0, yAxis);

		int idataset = 0;
		plot.setDataset(idataset, data);// idataset=0!
		plot.setRenderer(idataset, renderer);// idataset=0!

		if (mf.plotNucNumber > 1) {
			XYSeries dotseries = new XYSeries(
					resources.getString("dot.series")//"Lead Points"
					);
			for (int i = 0; i < mf.plotSteps; i++) {
				// dotseries.add(i,PhysUtilities.at[1][i]);//allways 2nd
				// nuclides!!
				dotseries.add(i, mf.plotActiv[1][i]);// allways 2nd nuclides!!
			}

			XYSeriesCollection dotdata = new XYSeriesCollection(dotseries);
			XYItemRenderer renderer2 = new StandardXYItemRenderer(
					StandardXYItemRenderer.SHAPES);
			renderer2.setSeriesPaint(0, Color.BLACK);

			idataset = 1;
			plot.setDataset(idataset, dotdata);// idataset=0!
			plot.setRenderer(idataset, renderer2);// idataset=0!
			// plot.setSecondaryDataset(0, dotdata);
			// plot.setSecondaryRenderer(0, renderer2);
		}

		JFreeChart chart = new JFreeChart(
				resources.getString("chart.name"),//" Chain decay",
				JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		chart.setBackgroundPaint(NuclideExposure.bkgColor);
		// chart.setBackgroundPaint(new GradientPaint(0, 0, Color.white, 1000,
		// 0, Color.green));
		return chart;
	}

	//private JPanel createMainGraphPanel() {
	/**
	 * Create chart panel.
	 */
	private void createMainGraphPanel() {
		JFreeChart gammaChart = getChartPanel();
		chartPanel = new ChartPanel(gammaChart, false, true, true, false, true);//new ChartPanel(getChartPanel());
		chartPanel.setMouseWheelEnabled(true);// mouse wheel zooming!
		chartPanel.setPreferredSize(chartDimension);
		//chartPanel.setPreferredSize(new java.awt.Dimension(300, 300));
		//chartPanel.setMinimumSize(new java.awt.Dimension(300, 300));

		/*JPanel pd = new JPanel();
		BoxLayout bl = new BoxLayout(pd, BoxLayout.X_AXIS);
		pd.setLayout(bl);
		pd.add(chartPanel);

		GridBagLayout gbl1 = new GridBagLayout();
		GridBagConstraints cts1 = new GridBagConstraints();
		cts1.anchor = GridBagConstraints.WEST;// default
		cts1.fill = GridBagConstraints.BOTH;
		Insets ins1 = new Insets(5, 5, 5, 5);
		cts1.insets = ins1;

		JPanel mainP = new JPanel(gbl1);
		cts1.weightx = 1.0; // ponderi egale in maximizare toate pe orizontala
		cts1.gridheight = 1;
		cts1.weighty = 1.0;// maximizare pe verticala--atrage dupa sine tot
							// restul!!
		cts1.gridwidth = GridBagConstraints.REMAINDER;
		gbl1.setConstraints(pd, cts1);
		mainP.add(pd);
		mainP.setBackground(new Color(122, 181, 140));

		return mainP;*/
		//return chartPanel;
	}

	/**
	 * All actions are set here
	 */
	public void actionPerformed(ActionEvent evt) {
		command = evt.getActionCommand();
		if (command.equals(EVALUATE_COMMAND)) {
			compute();
		}
	}
}
