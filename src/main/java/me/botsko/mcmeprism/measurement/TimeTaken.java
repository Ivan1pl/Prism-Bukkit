package me.botsko.mcmeprism.measurement;

import java.util.Calendar;
import java.util.TreeMap;
import java.util.Map.Entry;

import me.botsko.mcmeprism.MCMEPrism;

public class TimeTaken {

    protected final MCMEPrism plugin;

    /**
     * 
     * @param plugin
     */
    public TimeTaken(MCMEPrism plugin) {
        this.plugin = plugin;
    }

    /**
	 * 
	 */
    protected final TreeMap<Long, String> eventsTimed = new TreeMap<Long, String>();

    /**
     * 
     * @return
     */
    protected long getTimestamp() {
        final Calendar lCDateTime = Calendar.getInstance();
        return lCDateTime.getTimeInMillis();
    }

    /**
     * 
     * @param eventname
     */
    public void recordTimedEvent(String eventname) {
        if( !plugin.getConfig().getBoolean( "prism.debug" ) )
            return;
        eventsTimed.put( getTimestamp(), eventname );
    }

    /**
	 * 
	 */
    protected void resetEventList() {
        eventsTimed.clear();
    }

    /**
     * 
     * @return
     */
    protected TreeMap<Long, String> getEventsTimedList() {
        return eventsTimed;
    }

    /**
     * 
     * @param plugin
     */
    public void printTimeRecord() {

        // record timed events to log
        if( plugin.getConfig().getBoolean( "prism.debug" ) ) {
            final TreeMap<Long, String> timers = plugin.eventTimer.getEventsTimedList();
            if( timers.size() > 0 ) {
                long lastTime = 0;
                long total = 0;
                MCMEPrism.debug( "-- Timer information for last action: --" );
                for ( final Entry<Long, String> entry : timers.entrySet() ) {
                    long diff = 0;
                    if( lastTime > 0 ) {
                        diff = entry.getKey() - lastTime;
                        total += diff;
                    }
                    MCMEPrism.debug( entry.getValue() + " " + diff + "ms" );
                    lastTime = entry.getKey();
                }
                MCMEPrism.debug( "Total time: " + total + "ms" );
            }
        }
        plugin.eventTimer.resetEventList();
    }
}