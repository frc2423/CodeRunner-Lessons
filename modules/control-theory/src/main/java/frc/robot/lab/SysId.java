package frc.robot.lab;

/**
 * Finds a velocity mechanism's kS, kV and kA from a trial's data by ordinary least squares, the way
 * WPILib's SysId tool does (Controls Engineering in FRC, section 14.2.1).
 *
 * <p>Between two loops, the model {@code volts = kS sign(v) + kV v + kA a} becomes
 *
 * <pre>
 *   v[k+1] = α v[k] + β u[k] + γ sign(v[k])
 * </pre>
 *
 * <p>Fitting α, β and γ to the data gives kV = (1 - α) / β, kS = -γ / β and kA = (α - 1) T / (β ln
 * α), where T is the loop period.
 */
final class SysId {
  record Fit(boolean ok, String problem, double kS, double kV, double kA, int samples) {
    static Fit failed(String problem, int samples) {
      return new Fit(false, problem, Double.NaN, Double.NaN, Double.NaN, samples);
    }
  }

  /** Samples slower than this (RPM) are left out: static friction makes them misleading. */
  private static final double MIN_SPEED = 50.0;

  private SysId() {}

  static Fit fit(Trace trace) {
    int n = trace.size();
    // Normal equations for [α β γ], built up one sample pair at a time.
    double[][] ata = new double[3][3];
    double[] atb = new double[3];
    int count = 0;
    double minSpeed = Double.POSITIVE_INFINITY;
    double maxSpeed = Double.NEGATIVE_INFINITY;
    // Running sums for the correlation between the columns.
    double[] sum = new double[3];
    double[][] cross = new double[3][3];

    for (int k = 0; k + 1 < n; k++) {
      double v = trace.measured.get(k);
      double next = trace.measured.get(k + 1);
      if (Math.abs(v) < MIN_SPEED || Math.abs(next) < MIN_SPEED) {
        continue;
      }
      double[] row = {v, trace.volts.get(k), Math.signum(v)};
      for (int i = 0; i < 3; i++) {
        atb[i] += row[i] * next;
        sum[i] += row[i];
        for (int j = 0; j < 3; j++) {
          ata[i][j] += row[i] * row[j];
          cross[i][j] += row[i] * row[j];
        }
      }
      count++;
      minSpeed = Math.min(minSpeed, v);
      maxSpeed = Math.max(maxSpeed, v);
    }

    if (count < 50) {
      return Fit.failed(
          "not enough data: the flywheel was moving for only " + count + " loops", count);
    }
    if (maxSpeed - minSpeed < 1000) {
      return Fit.failed("the flywheel's speed hardly changed: test a wider range of speeds", count);
    }
    double voltsVariance = cross[1][1] / count - (sum[1] / count) * (sum[1] / count);
    if (voltsVariance < 0.25) {
      return Fit.failed(
          "the voltage hardly changed during your test: try a ramp and a sudden step", count);
    }
    if (collinearity(sum, cross, count) < 0.002) {
      return Fit.failed(
          "the data can't tell kV and kA apart. Speed always followed voltage the same way, so"
              + " include a sudden change in voltage (a dynamic test), not only a slow ramp",
          count);
    }

    double[] x = solve(ata, atb);
    if (x == null) {
      return Fit.failed("the data can't be fit: try a different test", count);
    }
    double alpha = x[0];
    double beta = x[1];
    double gamma = x[2];
    if (!(alpha > 0 && alpha < 1 && beta > 0)) {
      return Fit.failed("the fit doesn't make physical sense: try a longer or richer test", count);
    }
    double kV = (1 - alpha) / beta;
    double kS = -gamma / beta;
    double kA = (alpha - 1) * Mechanism.DT / (beta * Math.log(alpha));
    return new Fit(true, "", kS, kV, kA, count);
  }

  /**
   * The determinant of the correlation matrix of the columns that vary: 1 when they are unrelated,
   * 0 when one is a mix of the others (then least squares can't separate them).
   */
  private static double collinearity(double[] sum, double[][] cross, int n) {
    int[] varying = new int[3];
    int m = 0;
    for (int i = 0; i < 3; i++) {
      double variance = cross[i][i] / n - (sum[i] / n) * (sum[i] / n);
      if (variance > 1e-9) {
        varying[m++] = i;
      }
    }
    double[][] r = new double[m][m];
    for (int a = 0; a < m; a++) {
      for (int b = 0; b < m; b++) {
        int i = varying[a];
        int j = varying[b];
        double cov = cross[i][j] / n - (sum[i] / n) * (sum[j] / n);
        double vi = cross[i][i] / n - (sum[i] / n) * (sum[i] / n);
        double vj = cross[j][j] / n - (sum[j] / n) * (sum[j] / n);
        r[a][b] = cov / Math.sqrt(vi * vj);
      }
    }
    return determinant(r);
  }

  private static double determinant(double[][] m) {
    return switch (m.length) {
      case 0 -> 1.0;
      case 1 -> m[0][0];
      case 2 -> m[0][0] * m[1][1] - m[0][1] * m[1][0];
      default -> m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
          - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
          + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    };
  }

  /** Solves a 3×3 system by Gaussian elimination with partial pivoting; null if singular. */
  private static double[] solve(double[][] a, double[] b) {
    int n = b.length;
    double[][] m = new double[n][n + 1];
    for (int i = 0; i < n; i++) {
      System.arraycopy(a[i], 0, m[i], 0, n);
      m[i][n] = b[i];
    }
    for (int col = 0; col < n; col++) {
      int pivot = col;
      for (int row = col + 1; row < n; row++) {
        if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
          pivot = row;
        }
      }
      if (Math.abs(m[pivot][col]) < 1e-12) {
        return null;
      }
      double[] swap = m[col];
      m[col] = m[pivot];
      m[pivot] = swap;
      for (int row = 0; row < n; row++) {
        if (row != col) {
          double factor = m[row][col] / m[col][col];
          for (int k = col; k <= n; k++) {
            m[row][k] -= factor * m[col][k];
          }
        }
      }
    }
    double[] x = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = m[i][n] / m[i][i];
    }
    return x;
  }
}
