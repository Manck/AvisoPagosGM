package avisodepagos;
                                            
public class Configuracion
{
    private String basedatos;

    private String usuario;

    private String password;

    private String ip;
    
    private String filas;
    
    private String correoerrores;
   

    public String getBasedatos (){
        return basedatos;
    }

    public void setBasedatos (String basedatos){
        this.basedatos = basedatos;
    }

    public String getUsuario (){
        return usuario;
    }

    public void setUsuario (String usuario){
        this.usuario = usuario;
    }

    public String getPassword (){
        return password;
    }

    public void setPassword (String password){
        this.password = password;
    }

    public String getIp (){
        return ip;
    }

    public void setIp (String ip){
        this.ip = ip;
    }
    
    public String getFilas (){
        return filas;
    }

    public void setFilas (String filas){
        this.filas = filas;
    }
    
    public String getCorreoerrores (){
        return correoerrores;
    }

    public void setCorreoerrores (String correoerrores){
        this.correoerrores = correoerrores;
    }

    @Override
    public String toString(){
        return "ClassPojo [basedatos = "+basedatos+", usuario = "+usuario+", password = "+password+", ip = "+ip+", filas = "+filas+", correoerrores = "+correoerrores+"]";
    }
}