package me.botsko.mcmeprism.commands;

import me.botsko.mcmeprism.MCMEPrism;
import me.botsko.mcmeprism.commandlibs.CallInfo;
import me.botsko.mcmeprism.commandlibs.SubHandler;
import me.botsko.mcmeprism.settings.Settings;
import me.botsko.mcmeprism.wands.Wand;
import org.bukkit.ChatColor;

import java.util.List;

public class ResetmyCommand implements SubHandler {

    /**
	 * 
	 */
    private final MCMEPrism plugin;

    /**
     * 
     * @param plugin
     * @return
     */
    public ResetmyCommand(MCMEPrism plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command
     */
    @Override
    public void handle(CallInfo call) {

        String setType = null;
        if( call.getArgs().length >= 2 ) {
            setType = call.getArg( 1 );
        }

        /**
         * Inspector wand
         */
        if( setType != null && !setType.equalsIgnoreCase( "wand" ) ) {
            call.getPlayer().sendMessage(MCMEPrism.messenger.playerError( "Invalid arguments. Use /prism ? for help." ) );
            return;
        }

        if( !plugin.getConfig().getBoolean( "prism.wands.allow-user-override" ) ) {
            call.getPlayer().sendMessage(MCMEPrism.messenger.playerError( "Sorry, but personalizing the wand is currently not allowed." ) );
        }

        // Check for any wand permissions. @todo There should be some central
        // way to handle this - some way to centralize it at least
        if( !call.getPlayer().hasPermission( "prism.rollback" ) && !call.getPlayer().hasPermission( "prism.restore" )
                && !call.getPlayer().hasPermission( "prism.wand.*" )
                && !call.getPlayer().hasPermission( "prism.wand.inspect" )
                && !call.getPlayer().hasPermission( "prism.wand.profile" )
                && !call.getPlayer().hasPermission( "prism.wand.rollback" )
                && !call.getPlayer().hasPermission( "prism.wand.restore" ) ) {
            call.getPlayer().sendMessage(MCMEPrism.messenger.playerError( "You do not have permission for this." ) );
            return;
        }

        // Disable any current wand
        if( MCMEPrism.playersWithActiveTools.containsKey( call.getPlayer().getName() ) ) {
            final Wand oldwand = MCMEPrism.playersWithActiveTools.get( call.getPlayer().getName() );
            oldwand.disable( call.getPlayer() );
            MCMEPrism.playersWithActiveTools.remove( call.getPlayer().getName() );
            call.getPlayer().sendMessage(MCMEPrism.messenger.playerHeaderMsg( "Current wand " + ChatColor.RED + "disabled" + ChatColor.WHITE
                            + "." ) );
        }

        Settings.deleteSetting( "wand.item", call.getPlayer() );
        Settings.deleteSetting( "wand.mode", call.getPlayer() );
        call.getPlayer().sendMessage(MCMEPrism.messenger.playerHeaderMsg( "Your personal wand settings have been reset to server defaults." ) );
    }

    @Override
    public List<String> handleComplete(CallInfo call) {
        return null;
    }
}