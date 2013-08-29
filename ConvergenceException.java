/**
 * Error thrown when a numerical computation can not be performed because the
 * numerical result failed to converge to a finite value.
 *
 * @since 2.2
 * @version $Id: ConvergenceException.java 1364378 2012-07-22 17:42:38Z tn $
 */
public class ConvergenceException extends MathIllegalStateException {
    /** Serializable version Id. */
    private static final long serialVersionUID = 4330003017885151975L;

    /**
     * Construct the exception.
     */
    public ConvergenceException() {
        this(LocalizedFormats.CONVERGENCE_FAILED);
    }

    /**
     * Construct the exception with a specific context and arguments.
     *
     * @param pattern Message pattern providing the specific context of
     * the error.
     * @param args Arguments.
     */
    public ConvergenceException(Localizable pattern,
                                Object ... args) {
        getContext().addMessage(pattern, args);
    }
}
