package x.mvmn.photometastats.gui;

import java.awt.FontMetrics;
import java.util.Map;
import java.util.TreeSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class ChartHelper {

	public static ChartPanel createChartPanel(String label, Map<String, ? extends Number> values, final FontMetrics fontMetrics) {
		DefaultCategoryDataset dataset = createDataset(values);
		JFreeChart chart = createBarChart(label, dataset);
		ChartPanel chartPanel = new ChartPanel(chart);
		final CategoryPlot plot = ((CategoryPlot) chart.getPlot());
		int rowCount = dataset.getRowCount();
		if (dataset.getColumnCount() > 30) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				if (!plot.getRenderer().isSeriesVisible(i)) {
					rowCount--;
				}
			}
			((BarRenderer) plot.getRenderer()).setItemMargin(0.5d / rowCount);
			int oneBarWidth = (int) (fontMetrics.getHeight() * 1.2);
			// if (oneBarWidth * mainDataset.getColumnCount() * rowCount > container.getPreferredSize().width) {
			chartPanel.setMinimumDrawWidth(oneBarWidth * dataset.getColumnCount() * rowCount);
			chartPanel.setMaximumDrawWidth(oneBarWidth * dataset.getColumnCount() * rowCount);
			chartPanel.setPreferredSize(new java.awt.Dimension(oneBarWidth * dataset.getColumnCount() * rowCount, (int) chartPanel.getPreferredSize().height));
		}
		return chartPanel;

	}

	public static JFreeChart createBarChart(final String label, DefaultCategoryDataset dataset) {
		JFreeChart chart = ChartFactory.createBarChart(label, // chart title
				label, // domain axis label
				"Count", // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				false, // include legend
				true, // tooltips?
				false // URLs?
				);

		final CategoryPlot plot = ((CategoryPlot) chart.getPlot());
		for (int i = 0; i < dataset.getRowCount(); i++) {
			plot.getRenderer().setSeriesItemLabelGenerator(i, new StandardCategoryItemLabelGenerator());
			plot.getRenderer().setSeriesItemLabelsVisible(i, true);
		}
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);

		return chart;
	}

	public static DefaultCategoryDataset createDataset(Map<String, ? extends Number> values) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// Sort alphabetically
		for (final String key : new TreeSet<String>(values.keySet())) {
			dataset.addValue(values.get(key), "Count", key);
		}

		return dataset;
	}
}
