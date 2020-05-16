package the_fireplace.clans.forge.compat;

import the_fireplace.chatcensor.util.CensorHelper;
import the_fireplace.clans.abstraction.IChatCensorCompat;

public class ChatCensorCompat implements IChatCensorCompat {
    @Override
    public String getCensoredString(String input) {
        return CensorHelper.censor(input);
    }
}
