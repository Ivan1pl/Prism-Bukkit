package me.botsko.mcmeprism.purge;

import java.util.concurrent.CopyOnWriteArrayList;

import me.botsko.mcmeprism.MCMEPrism;
import me.botsko.mcmeprism.actionlibs.ActionsQuery;
import me.botsko.mcmeprism.actionlibs.QueryParameters;

public class PurgeTask implements Runnable {

    /**
	 * 
	 */
    private final MCMEPrism plugin;

    /**
	 * 
	 */
    private final CopyOnWriteArrayList<QueryParameters> paramList;

    /**
	 * 
	 */
    private int cycle_rows_affected = 0;

    /**
	 * 
	 */
    private final int purge_tick_delay;

    /**
	 * 
	 */
    private int minId = 0;

    /**
	 * 
	 */
    private int maxId = 0;

    /**
	 * 
	 */
    private final PurgeCallback callback;

    /**
     * 
     * @param plugin
     */
    public PurgeTask(MCMEPrism plugin, CopyOnWriteArrayList<QueryParameters> paramList, int purge_tick_delay, int minId,
            int maxId, PurgeCallback callback) {
        this.plugin = plugin;
        this.paramList = paramList;
        this.purge_tick_delay = purge_tick_delay;
        this.minId = minId;
        this.maxId = maxId;
        this.callback = callback;
    }

    /**
	 * 
	 */
    @Override
    public void run() {

        if( paramList.isEmpty() )
            return;

        final ActionsQuery aq = new ActionsQuery( plugin );

        // Pull the next-in-line purge param
        final QueryParameters param = paramList.get( 0 );

        boolean cycle_complete = false;

        // We're chunking by IDs instead of using LIMIT because
        // that should be a lot better as far as required record lock counts
        // http://mysql.rjweb.org/doc.php/deletebig
        int spread = plugin.getConfig().getInt( "prism.purge.records-per-batch" );
        if( spread <= 1 )
            spread = 10000;
        int newMinId = minId + spread;
        param.setMinPrimaryKey( minId );
        param.setMaxPrimaryKey( newMinId );

        cycle_rows_affected = aq.delete( param );
        plugin.total_records_affected += cycle_rows_affected;

        // If nothing (or less than the limit) has been deleted this cycle, we
        // need to move on
        if( newMinId > maxId ) {
            // Remove rule, reset affected count, mark complete
            paramList.remove( param );
            cycle_complete = true;
        }

        MCMEPrism.debug( "------------------- " + param.getOriginalCommand() );
        MCMEPrism.debug( "minId: " + minId );
        MCMEPrism.debug( "maxId: " + maxId );
        MCMEPrism.debug( "newMinId: " + newMinId );
        MCMEPrism.debug( "cycle_rows_affected: " + cycle_rows_affected );
        MCMEPrism.debug( "cycle_complete: " + cycle_complete );
        MCMEPrism.debug( "plugin.total_records_affected: " + plugin.total_records_affected );
        MCMEPrism.debug( "-------------------" );

        // Send cycle to callback
        callback.cycle( param, cycle_rows_affected, plugin.total_records_affected, cycle_complete );

        if( !plugin.isEnabled() ) {
            MCMEPrism.log( "Can't schedule new purge tasks as plugin is now disabled. If you're shutting down the server, ignore me." );
            return;
        }

        // If cycle is incomplete, reschedule it, or reset counts
        if( !cycle_complete ) {
            plugin.getPurgeManager().deleteTask = plugin
                    .getServer()
                    .getScheduler()
                    .runTaskLaterAsynchronously( plugin,
                            new PurgeTask( plugin, paramList, purge_tick_delay, newMinId, maxId, callback ),
                            purge_tick_delay );
        } else {

            // reset counts
            plugin.total_records_affected = 0;

            if( paramList.isEmpty() )
                return;

            MCMEPrism.log( "Moving on to next purge rule..." );

            // Identify the minimum for chunking
            newMinId = PurgeChunkingUtil.getMinimumPrimaryKey();
            if( newMinId == 0 ) {
                MCMEPrism.log( "No minimum primary key could be found for purge chunking." );
                return;
            }

            // schedule a new task with next param
            plugin.getPurgeManager().deleteTask = plugin
                    .getServer()
                    .getScheduler()
                    .runTaskLaterAsynchronously( plugin,
                            new PurgeTask( plugin, paramList, purge_tick_delay, newMinId, maxId, callback ),
                            purge_tick_delay );

        }
    }
}