package abstractEntity;

import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * @author yashasvi
 */
public abstract class Regression {
    /**
     * The regression.
     */
    private final SimpleRegression regression = new SimpleRegression();
    /**
     * The slope of the regression.
     */
    private double slope = Double.NaN;
    /**
     * The intercept of the regression.
     */
    private double intercept = Double.NaN;

    /**
     * Adds the observation <code>(f(x), y)</code>
     * to the regression data set.
     *
     * @param x The independent variable value.
     * @param y The dependent variable value.
     */
    public void addData(double x, double y) {
        regression.addData(f(x), y);
        reset();
    }

    /**
     * Returns the "predicted" y value associated
     * with the supplied x value, based on the data that
     * has been added to the model so far.
     * In particular, <code>y = a * f(x) + b</code>,
     * where <code>a</code> and <code>b</code>
     * are estimated by the regression.
     *
     * <p>
     * Returns 0 if the number of added observations is less than 2.
     * </p>
     *
     * @param x The input <code>x</code> value.
     * @return The predicted <code>y</code> value.
     */
    public double predict(double x) {
        if (getN() < 2) {
            return 0;
        }

        if (Double.isNaN(slope) || Double.isNaN(intercept)) {
            slope = regression.getSlope();
            intercept = regression.getIntercept();
        }
        return slope * f(x) + intercept;
    }

    /**
     * Returns the coefficient of determination
     * usually denoted as r^2.
     *
     * <p>
     * Returns 0 if the number of added observations is less than 2.
     * </p>
     *
     * @return Pearson's r.
     */
    public double getRSquare() {
        if (getN() < 2) {
            return 0;
        }

        return regression.getRSquare();
    }

    /**
     * Returns the number of observations that have been added to the model.
     *
     * @return The number of observations that have been added.
     */
    public long getN() {
        return regression.getN();
    }

    /**
     * Returns <code>f(x)</code>. Must be implemented by subclasses.
     *
     * @param x The <code>x</code> value.
     * @return The <code>f(x)</code> value.
     */
    protected abstract double f(double x);

    /**
     * Resets the slope and intercept of the regression.
     */
    private void reset() {
        slope = Double.NaN;
        intercept = Double.NaN;
    }
}