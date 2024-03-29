package device.fitbitdata;

/**
 * Classe che serve ad avere una data collegata al dato preso dal fitbit
 */
public abstract class FitbitData {
    private long millisec = 0;

    /**
     * Setta la data collegata al dato preso dal fitbit
     * @param millisec la data in millisecondi
     */
    public void setDate(long millisec) { this.millisec = millisec; }

    /**
     * Ricevi la data collegata al dato richiesto dal fitbit
     * @return la data in millisecondi
     */
    public long getDate() { return this.millisec; }
}
