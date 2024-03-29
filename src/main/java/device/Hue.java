package device;

import ai.api.GsonFactory;
import com.google.common.util.concurrent.AtomicDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import support.Rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Classe che permette di controllare le luci Philips Hue
 */
public class Hue {

    /**
     * Log che serve per debug
     */
    private static final Logger LOG = LoggerFactory.getLogger("Hue");

    /**
     * La luminosita' massima a cui si puo' arrivare
     */
    private static final int MAX_BRIGHTNESS = 254;

    /**
     * Una mappa che ad ogni colore (in italiano) assegna il proprio valore in hue
     */
    private static final Map<String, Double[]> COLORS = new HashMap<>();

	/**
	 * L'url in cui si possono trovare le luci
	 */
    private final String lightsURL;

    /**
     * Tutte le luci che sono state registrate dall'url
     */
    private final Map<String, Map<String, Object>> allLights;

    /**
     * L'ultima luminosita' impostata
     */
    private final AtomicDouble brightness = new AtomicDouble(0);

    // Riempimento della mappa con i colori
    static {
        COLORS.put("giall[oae]", new Double[]{0.45, 0.45});
        COLORS.put("ross[oae]", new Double[]{0.7, 0.25});
        COLORS.put("verd[ei]", new Double[]{0.1, 0.55});
        COLORS.put("blu", new Double[]{0.12, 0.125});
        COLORS.put("rosa", new Double[]{0.45, 0.275});
        COLORS.put("viola", new Double[]{0.25, 0.1});
        COLORS.put("azzurr[oae]", new Double[]{0.15, 0.25});
        COLORS.put("arancio(ne|ni)?", new Double[]{0.55, 0.4});
        //COLORS.put("nero", new Double[]{1.0, 1.0});
        COLORS.put("bianc(o|a|he)", new Double[]{0.275, 0.3});
    }

    /**
     * Cerca le luci Philips Hue all'indirizzo <a href="http://172.30.1.138/api/C0vPwqjJZo5Jt9Oe5HgO6sBFFMxgoR532IxFoGmx/lights/">http://172.30.1.138/api/C0vPwqjJZo5Jt9Oe5HgO6sBFFMxgoR532IxFoGmx/lights/</a>
     * @throws NullPointerException se non trova nessun bridge
     */
    public Hue () throws NullPointerException { this("172.30.1.138", "C0vPwqjJZo5Jt9Oe5HgO6sBFFMxgoR532IxFoGmx"); }

    /**
     * Cerca le luci Philips Hue nell'indirizzo specificato e con l'utente specificato.<br>
     * Una volta trovate le luci le setta tutte alla stessa luminosita' e allo stesso colore<br>
     * (luminosità massima e colore bianco)
     * @param ip l'indirizzo IP (seguito dalla porta se e' diversa dalla solita 8000)
     * @param user l'utente
     * @throws NullPointerException se non trova nessun bridge
     */
    public Hue(String ip, String user) throws NullPointerException {
        lightsURL = "http://" + ip + "/api/" + user + "/lights/";
        allLights = (Map<String, Map<String, Object>>)Rest.get(lightsURL);

        if(allLights.isEmpty())
            throw new NullPointerException("Non e' stato possibile connettersi alle luci");

        setBrightness(100);
        changeColor("bianco");
    }

    /**
     * Ritorna un insieme contenente tutti i nomi delle luci che si sono trovate
     *
     * @return l'insieme dei nomi delle luci
     */
    public Set<String> getNameLights() { return allLights.keySet(); }

    /**
     * Rimuove dal controllo tutte le luci che hanno il nome uguale ad uno contenuto nell'insieme passato
     * 
     * @param toRemove le luci da rimuovere
     */
    public void removeLights(Set<String> toRemove) {
        for(String string : toRemove)
            allLights.remove(string);
    }

    /**
     * Accende o spegne le luci controllate
     * @param on vero se si vuole le luci accese, false per spegnerle
     */
    public void on(boolean on) { setState("on", on, false); }

    /**
     * Accende tutte le luci controllate
     */
    public void turnOn() { on(true); }

    /**
     * Spegne tutte le luci controllate
     */
    public void turnOff() { on(false); }
    
    /**
     * Ritorna la luminosita' attuale delle luci controllate
     * @return il valore e' compreso tra 0 e 100
     */
    public double getCurrentBrightness() { return brightness.doubleValue(); }
    
    /**
     * Modifica la luminosita' delle luci a seconda del valore inserito
     * @param num la luminosita' che si vuole da (0 a 100)
     */
    public void setBrightness(double num) {
    	if (num<0)
    		num=0;
    	else if (num>100)
    	    num=100;

    	setState("bri", (int) (num*MAX_BRIGHTNESS)/100, true);
        brightness.set(num);
    }

    /**
     * Aggiunge il valore delta alla luminosita' corrente delle luci.
     * @param delta un qualsiasi numero che va da -100 a 100
     */
    public void addBrightness(double delta) {
        setBrightness(brightness.doubleValue() + delta);
    }

    /**
     * Aumenta la luminosita' delle luci controllate della percentuale che viene passata
     * @param percentage la percentuale di aumento della luminosita'
     */
    public void increaseBrightness(double percentage) {
        if (percentage<0)
            percentage = 0;
        else if (percentage>100)
            percentage = 100;
        setBrightness(brightness.doubleValue() + percentage);
    }

    /**
     * Aumenta la luminosita' delle luci controllate del 25%
     */
    public void increaseBrightness() { increaseBrightness(25); }

    /**
     * Diminuisce la luminosita' delle luci controllate della percentuale che viene passata
     * @param percentage la percentuale di diminuzione della luminosita'
     */
    public void decreaseBrightness(int percentage) {
        if (percentage<0)
            percentage = 0;
        else if (percentage>100)
            percentage = 100;
        setBrightness(brightness.doubleValue() - percentage);
    }

    /**
     * Diminuisce la luminosita' delle luci controllate del 25%
     */
    public void decreaseBrightness() { decreaseBrightness(25); }

    public void changeColor(String colorName) {
        for (String regex: COLORS.keySet())
            if(colorName.matches("(?i)" + regex))
                setState("xy", COLORS.get(regex), true);
    }

    /**
     * Modifica il colore delle luci in modo da fare un bel effetto arcobaleno continuo
     */
    public void colorLoop() { setState("effect", "colorloop", false); }

    /**
     * Invia una richiesta a tutte le luci hue con l'attributo selezionato ed il suo valore<br>
     * Con esso invia anche un valore di transizione in modo che sia piu fluido il cambiamento se si mette true al terzo parametro
     * @param attribute l'attributo da modificare
     * @param value il valore da inserire
     * @param transition se includere la transizione o meno
     */
    private void setState(String attribute, Object value, boolean transition){
        Map<String, Object> map = new HashMap<>();
        map.put(attribute, value);
        if(transition)
            map.put("transition", 200);
        setState(map);
    }

    /**
     * Invia una richiesta a tutte le luci hue con gli attributi selezionati ed il loro valore
     * @param attributes una mappa di attributi -> valori
     */
    private synchronized void setState(Map<String, Object> attributes) {
        String body = GsonFactory.getDefaultFactory().getGson().toJson(attributes);
        LOG.info("Setting: " + body);
        for (String light : allLights.keySet()) {
            Rest.put(lightsURL + light + "/state", body,"application/json");

            Map<String, Object> state = (Map<String, Object>)allLights.get(light).get("state");
            for (String attr : attributes.keySet())
                state.put(attr, attributes.get(attr));
        }
    }
}
