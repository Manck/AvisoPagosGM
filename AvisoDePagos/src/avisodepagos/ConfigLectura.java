package avisodepagos;

/**
 *
 * @author Francisco
 */
public class ConfigLectura{
    private String host;

    public String getHost ()
    {
        return host;
    }

    public void setHost (String host)
    {
        this.host = host;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [host = "+host+"]";
    }
}