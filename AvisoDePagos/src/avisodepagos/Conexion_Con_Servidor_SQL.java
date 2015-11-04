package avisodepagos;

/**
 *
 * @autor Francisco Javier Lamas Green
 * Clase de conexión SQL
 */

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.codehaus.jackson.map.ObjectMapper;

public class Conexion_Con_Servidor_SQL {
	
    
    Configuracion config;
    String ip;
    String BaseDatos;
    String Usuario;
    String Password;
    
    /*Conexión a DB*/
    @SuppressWarnings("finally")
    public static Connection createConnection() throws Exception {
    	        Connection conexion=null;
                ObjectMapper mapperLectura = new ObjectMapper(); // can reuse, share globally
                ConfigLectura configLectura = mapperLectura.readValue(new File("C:\\Program Files\\AvisoPagos\\ConfigLectura.json"), ConfigLectura.class);
    	        String host = configLectura.getHost();
                ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
                Configuracion config = mapper.readValue(new File("C:\\Users\\"+host+"\\Documents\\ConfiguracionCXP\\Configuracion.json"), Configuracion.class);

               try{
    	            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    	            String url = "jdbc:sqlserver:// "+ config.getIp()+" ;databaseName= "+config.getBasedatos()+" ; "
                            + "user= "+config.getUsuario()+" ;password = " +config.getPassword()+ ";";
    	            conexion= DriverManager.getConnection(url);
    	            //System.out.println("Conexión con SQL Server Exitosa");
    	        }
    	        catch(ClassNotFoundException ex){
    	            JOptionPane.showMessageDialog(new JFrame(),"Error en la Conexión con la BD: "+ex.getMessage());
    	            conexion=null;
    	        }
    	        catch(SQLException ex){
    	            JOptionPane.showMessageDialog(new JFrame(),"Error en la Conexión con la BD: "+ex.getMessage());
    	            conexion=null;
    	        }
    	        catch(Exception ex){
    	            JOptionPane.showMessageDialog(new JFrame(),"Error en la Conexión con la BD: "+ex.getMessage());
    	            conexion=null;
    	        }
    	        finally{   
    	            return conexion;
    	        }
    	 }
    
}