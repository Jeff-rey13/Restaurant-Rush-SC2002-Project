package rush.gui;

import java.awt.Color;

/** How a character is drawn: colours, hair and the item that identifies their role. */
final class Look {

    enum Hair { SHORT, LONG, BUN, CURLY, BALD, SPIKY }

    enum Accessory { NONE, CHEF_HAT, WAITER_VEST, CASHIER_CAP }

    final Color skin;
    final Color hair;
    final Hair hairStyle;
    final Color outfit;
    final Color pants;
    final Accessory accessory;
    final Color accent;

    Look(Color skin, Color hair, Hair hairStyle, Color outfit, Color pants, Accessory accessory, Color accent) {
        this.skin = skin;
        this.hair = hair;
        this.hairStyle = hairStyle;
        this.outfit = outfit;
        this.pants = pants;
        this.accessory = accessory;
        this.accent = accent;
    }
}
