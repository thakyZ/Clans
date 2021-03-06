package the_fireplace.clans.model;

public class ClanDimInfo {
    private final String clanName;
    private final String clanDescription;

    private final int clanColor;
    private final String clanUniqueID;
    private final int clanDimension;

    public ClanDimInfo(String clanID, int dim, String clanName, String description, int rgbColor) {
        this.clanUniqueID = clanID;
        this.clanDimension = dim;
        this.clanName = clanName;
        this.clanDescription = description;
        this.clanColor = rgbColor;
    }

    public ClanDimInfo(Clan clan, int dim) {
        this(clan.getId().toString(), dim, clan.getName(), clan.getDescription(), clan.getColor());
    }

    public String getClanIdString() {
        return clanUniqueID;
    }

    public int getDim() {
        return clanDimension;
    }

    public int getClanColor() {
        return clanColor;
    }

    public String getClanDescription() {
        return clanDescription;
    }

    public String getClanName() {
        return clanName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (obj == this)
            return true;
        else
            return obj instanceof ClanDimInfo && this.equals((ClanDimInfo) obj);
    }

    private boolean equals(ClanDimInfo other) {
        if (other == null)
            return false;
        else
            return clanUniqueID.equals(other.clanUniqueID) && clanDimension == other.clanDimension;
    }

    @Override
    public int hashCode() {
        return clanUniqueID.hashCode() + clanDimension;
    }
}

