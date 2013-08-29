public class MathFunction {

	/** Factorial Swing simple algorithm implementation
	 * See http://www.luschny.de/math/factorial/index.html
	 */
	public static double fact(int n){
		if (n < 0){
			throw new ArithmeticException("Factorial: n has to be >= 0, but was : " + n);
		}
		if(n > 173){
			throw new ArithmeticException("Factorial: n has to be <= 173 because of overflow");
		}
		if (n == 1 || n == 0)
			return 1;
		else{
			if (n <= 23)
				return(n * fact(n-1));
			else{
				return Math.pow(fact(n/2),2) * swing(n);
			}
		}		
	}

	public static double swing(int n){
		int z;

		switch( n % 4){
		case 1:
			z = n / 2 + 1;
			break;
		case 2:
			z = 2;
			break;
		case 3:
			z = 2 * (n/2 + 2);
			break;
		default:
			z = 1;
			break;
		}

		double b = z;
		z = 2 * (n - ((n+1) & 1));

		for(int i = 1; i <= n/4; i ++, z -=4 ){
			b = b * z / i;
		}
		return b;
	}

	/**Return the cdf of a normal distribution with mean mu, standard deviation sigma and evaluated 
	 * at value x.
	 * 
	 */
	public static double normCDF(double x,double mu,double sigma){
		return 0.5*(1 + erf( (x - mu )/  ( sigma * Math.sqrt(2) ) ) ) ;
	}

	public static double erf(double x){
		//Use an approximation given by Abramowitz and Stegun
		//ERF(x) = 1 - (a1t + a2t^2 + a3t^3..a5t^5)*e^-x^2 , t = 1 /(1+px)
		double a1 =  0.254829592;
		double a2 = -0.284496736;
		double a3 = 1.421413741;
		double a4 = -1.453152027;
		double a5 = 1.061405429;
		double p = 0.3275911;

		double t = 1 / (1+ p*x);
		return 1 - ((a1 * t + a2*Math.pow(t,2) + a3*Math.pow(t,3) + a4*Math.pow(t,4) + a5*Math.pow(t,5)) * Math.exp(-Math.pow(x,2)));
	}
	/**Return the cdf of a binomial distribution with probability p, number of trial n and with x success
	 * cdfValue = Pr(X <= x) 
	 * @param x -> number of success
	 * @param n -> number of trial
	 * @param p -> probability of success
	 * @return cdf
	 */
	public static double binoCDF(double x, int numberOfTrials, double p){
		double ret;
		if (x < 0) {
			ret = 0.0;
		} else if (x >= numberOfTrials) {
			ret = 1.0;
		} else {
			ret = 1.0 - Beta.regularizedBeta(p,
					x + 1.0, numberOfTrials - x);
		}
		return ret;
	}


}
