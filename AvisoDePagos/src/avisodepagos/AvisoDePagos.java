package avisodepagos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author Ing. Francisco Javier Lamas Green
 */

public class AvisoDePagos {
    //Objeto de conexión con la base de datos
    Connection Objeto_ConexionSQL;
    //Objetos JSON
    ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
    Configuracion config;
    ObjectMapper mapperLectura = new ObjectMapper(); // can reuse, share globally
    ConfigLectura configLectura;
    //Errores de envío
    String erroresDeEnvio = "";
    String host = "";
    //Constructor
    AvisoDePagos(){

        try {
            this.configLectura = mapperLectura.readValue(new File("C:\\Program Files\\AvisoPagos\\ConfigLectura.json"), ConfigLectura.class);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error, archivo de configuración de lectura no encontrado o tiene campos incorrectos");
        }
        host = configLectura.getHost();
        try {
            this.config = mapper.readValue(new File("C:\\Users\\"+host+"\\Documents\\ConfiguracionCXP\\Configuracion.json"), Configuracion.class);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error, archivo de configuración no encontrado o tiene campos incorrectos");
            erroresDeEnvio = erroresDeEnvio + "Error, archivo de configuración no encontrado o tiene campos incorrectos";
        }

        try {
            Objeto_ConexionSQL = Conexion_Con_Servidor_SQL.createConnection();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error, Conexión de base de datos en la clase principal");
             erroresDeEnvio = erroresDeEnvio +  "Error, Conexión de base de datos en la clase principal";
        }        
        ObtenerNuevosRegistros();
    }

    //Proceso a ejecutar inicialmente
    private void ObtenerNuevosRegistros() {
            int filasNuevas = ObtenerTotalFilas();
            int totalFilasOriginales = Integer.parseInt(config.getFilas());
            System.out.println("Total Filas Nuevas (ObtenerNuevosRegistros que llama a ObtenerTotalFilas): " + filasNuevas);
            int cantidadNuevosRegistros = filasNuevas - totalFilasOriginales;
            System.out.println("Filas Nuevas (Obtener Nuevos Registros): " + String.valueOf(cantidadNuevosRegistros));
            boolean existenNuevosRegistros = (cantidadNuevosRegistros > 0);
        
            if(existenNuevosRegistros){
                //Llamar al método de existen nuevos registros
                ExistenNuevosRegistros(cantidadNuevosRegistros);
                //Finalizar actualizando el número de filas.
                ActualizarNumeroDeFilas(filasNuevas);
            } 
    }
    
        //Obtener las filas una vez que ya se ejecutó el programa. 
    private int ObtenerTotalFilas(){
        int totalFilas= 0;
        
        String comandoBusquedaTotalDeRegistros = "SELECT COUNT(*) FROM BoCxpMovimientos";

        try {
            PreparedStatement objetoBusqueda = Objeto_ConexionSQL.prepareStatement(comandoBusquedaTotalDeRegistros);
            ResultSet rsResultadoNumeroFilas = objetoBusqueda.executeQuery();
            if(rsResultadoNumeroFilas.next()){
                totalFilas = rsResultadoNumeroFilas.getInt(1);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error al obtener el número de filas");
            erroresDeEnvio = erroresDeEnvio + "Error al obtener el número de filas";
        }
        return totalFilas;
    }
    
    //Es llamado cuando existen nuevos registros
    private void ExistenNuevosRegistros(int cantidadDeRegistrosNuevos){
        System.out.println("Nuevos Registros");
         String comandoBusquedaNuevosRegistros = "SELECT * FROM BoCxpMovimientos " +
            "EXCEPT (SELECT TOP ((SELECT COUNT(*) FROM BoCxpMovimientos) - ?) * FROM BoCxpMovimientos)";
        //String comandoBusquedaNuevosRegistros = "SELECT * FROM BoCxpMovimientos WHERE Descripcion = '1-00 PRUEBA'";
        //ArrayList<String> listaCargoAbono = new ArrayList<>();
        ArrayList<String> listaFolio = new ArrayList<>();
        
        try {
            PreparedStatement psBusquedaImportes = Objeto_ConexionSQL.prepareStatement(comandoBusquedaNuevosRegistros);
            psBusquedaImportes.setInt(1, cantidadDeRegistrosNuevos);
            ResultSet rsResultadoBusquedaImportes = psBusquedaImportes.executeQuery();
            
            while(rsResultadoBusquedaImportes.next()){
                //listaCargoAbono.add(rsResultadoBusquedaImportes.getString("CargoAbono"));
                if("TRANSFESE".equals(rsResultadoBusquedaImportes.getString("Banco")) && "C".equals(rsResultadoBusquedaImportes.getString("CargoAbono"))){
                    if(!listaFolio.contains(rsResultadoBusquedaImportes.getString("Folio"))){
                    listaFolio.add(rsResultadoBusquedaImportes.getString("Folio"));
                    System.out.println("Folio en Ciclo (ExistenNuevosRegistros): " + rsResultadoBusquedaImportes.getString("Folio"));
                    }
                }   
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error al seleccionar los nuevos registros");
            erroresDeEnvio = erroresDeEnvio + "Error al seleccionar los nuevos registros";
        }
        
        listaFolio.stream().forEach((listaFolio1) -> { //Puesto como funcional
            ObtenerImportePorFactura(listaFolio1);
        });
    }

    private void ObtenerImportePorFactura(String folio){
        String obtenerCargoYFactura = "SELECT Importe, AplicaFolio FROM BoCxpMovimientos WHERE Folio = ? AND CargoAbono = 'C' AND Banco = 'TRANSFESE' ";
        
        ArrayList<Float> importes;
        importes = new ArrayList<>();
        ArrayList<String> facturas;
        facturas = new ArrayList<>();
        
        try {
            PreparedStatement psBuscarFacturas = Objeto_ConexionSQL.prepareStatement(obtenerCargoYFactura);
            psBuscarFacturas.setString(1, folio);
            ResultSet rsBuscarFacturas = psBuscarFacturas.executeQuery();

            while(rsBuscarFacturas.next()){
                importes.add(rsBuscarFacturas.getFloat("Importe"));
                facturas.add(rsBuscarFacturas.getString("AplicaFolio"));
            }
            
            //Obtener totales
            ObtenerImporteYAbono(folio, importes,  facturas);            
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error al obtener el importe o factura");
            erroresDeEnvio = erroresDeEnvio + "Error al obtener el importe o factura";
        }
    }
    
    //Obtener el total del importe y del cargo.
    private void ObtenerImporteYAbono(String Folio, ArrayList<Float> importes,  ArrayList<String> facturas){
        float importeFinal = 0;
           
        for (Float importe : importes) {
            importeFinal = importeFinal + importe;
        }
        
        System.out.println("Total Importe (ObtenerImporteYAbono): " + String.valueOf(importeFinal));
        
        ObtenerProveedor(importeFinal, Folio, importes, facturas);
    }
    
    //Obtener el nombre del Proveedor
    private void ObtenerProveedor(float importeFinal,  String folio, ArrayList<Float> importes, ArrayList<String> facturas){
        String buscarClaveProveedor = "SELECT Proveedor FROM BoCxpMovimientos WHERE Folio = ?";
        
        try {
            PreparedStatement psBuscarClaveProveedor = Objeto_ConexionSQL.prepareStatement(buscarClaveProveedor);
            psBuscarClaveProveedor.setString(1, folio);
            ResultSet rsBuscarClaveProveedor = psBuscarClaveProveedor.executeQuery();
            
            if(rsBuscarClaveProveedor.next()){
                ObtenerNombreCorreoProveedor(importeFinal, rsBuscarClaveProveedor.getString("Proveedor"), importes,  facturas);
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error al buscar proveedor");
            erroresDeEnvio = erroresDeEnvio +  "Error al buscar proveedor";
        }
    }
    
    //La clave obtenida en ExistenNuevosRegistros se utilizan para obtener el nombre y el correo
    private void ObtenerNombreCorreoProveedor(float importeFinal, String claveProveedor, ArrayList<Float> importes, ArrayList<String> facturas){
        String buscarNombreCorreoEmpleado = "SELECT NombreComercial, Email1 FROM BoCXPProveedores WHERE ClaveProveedor = ?";
        System.out.println("Clave obtener nombre correo:" + claveProveedor);
        try {
            PreparedStatement psBuscarNombreCorreo = Objeto_ConexionSQL.prepareStatement(buscarNombreCorreoEmpleado);
            psBuscarNombreCorreo.setString(1, claveProveedor);
            ResultSet rsBuscarNombreCorreo = psBuscarNombreCorreo.executeQuery();
            if(rsBuscarNombreCorreo.next()){
                EnviarCorreo(rsBuscarNombreCorreo.getString("NombreComercial"), rsBuscarNombreCorreo.getString("Email1"),  String.valueOf(importeFinal), importes, facturas,1);
            }
            else{
                JOptionPane.showMessageDialog(new JFrame(), "No se ha encontrado el correo del proveedor");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(new JFrame(), "Error al obtener correo de proveedor");
            erroresDeEnvio = erroresDeEnvio + "Error al obtener correo de proveedor";
        }
    }
    
    //Envia el correo de informe de pago
    private void EnviarCorreo(String Nombre, String Correo, String importeFinal, ArrayList<Float> importes, ArrayList<String> facturas, int tipoDeCorreo){
      //Destinatario del correo
      String destinatario = Correo;
      System.out.println("Destinatario: "+Correo);
      // Información de envío
      String remitente = "sistemadealertavidanta@sjd1.grandmayan.com.mx";
      final String usuario = "sistemadealertavidanta@sjd1.grandmayan.com.mx";
      final String password = "Soporte2524";

      // Host
      String hostEmail = "smtp.gmail.com";
      String cuerpoCorreoFacturas = "";
      
      
      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.host", hostEmail);
      props.put("mail.smtp.port", "587");
      
      //Información de la sesión 
      Session session = Session.getInstance(props,
      new javax.mail.Authenticator() {
         @Override
         protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(usuario, password);
         }
      });
      
        try {
        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);
        MimeMultipart multipart = new MimeMultipart("related");
          
        // Set From: header field of the header.
        message.setFrom(new InternetAddress(remitente));
        
        // Set To: header field of the header.
        message.setRecipients(Message.RecipientType.TO,
        InternetAddress.parse(destinatario));
        BodyPart messageBodyPart = new MimeBodyPart();
        
        //Cuerpo final del correo
        String cuerpoFinal = "";
        //Asunto
        
        System.out.println("NOMBRE"+Nombre);
        switch(tipoDeCorreo){
            case 1:
                message.setSubject("Aviso de Pago de Grand Mayan Los Cabos para " +Nombre);
            break;
            
            case 2:
                message.setSubject("Aviso de errores de pagos a proveedores para Cuentas Por Pagar");
            break;
        }
        

        String textoCuerpoIntroduccion = "Por este medio se informa que el pago de la(s) siguiente(s) factura(s) ha sido programado: <br> <br>";
        
        switch(tipoDeCorreo){
            case 1:
                for ( int i = 0; i < importes.size(); i++){
                    cuerpoCorreoFacturas = cuerpoCorreoFacturas + ("Factura: "  + facturas.get(i) + ", Importe: $" + importes.get(i) + "<br>");
                }
            break;
        }


        String textoCuerpoTotal = " <br> Obteniendo un total de: $" + importeFinal + "<br>";
        
        String textoCuerpoFinal = "<br> Le Agradecemos por su Paciencia y sus Servicios. <br> ";
        
        String disculpaCorreo = "Si ya recibió a este correo, ignorarlo y no responder, gracias. <br>";
         // Now set the actual message
        String firmaDeCorreo = "<p>&nbsp;</p>\n" +
                "<p><span style=\"font-family: arial, helvetica, sans-serif; color: #333333; font-size: medium;\"><strong>Informador Vidanta</strong></span></p>\n" +
                "<p><span style=\"font-size: x-small; font-family: arial, helvetica, sans-serif; color: #808080;\">Informador Vidanta / Informer Vidanta</span></p>\n" +
                "<hr />\n" +
                "<p><span style=\"color: #333399; font-size: small;\"><strong><span style=\"font-family: arial, helvetica, sans-serif;\"><span style=\"font-family: arial, helvetica, sans-serif;\">Vidanta Los Cabos</span></span></strong></span></p>\n" +
                "<p><span style=\"font-size: small; font-family: arial, helvetica, sans-serif; color: #808080;\">+52 01 (624) 163 4000 Ext. 4021</span></p>\n" +
                "<p><span style=\"font-size: small; font-family: arial, helvetica, sans-serif; color: #808080;\">Boulevard San Jos&eacute; s/n Lote 12 Campo de Golf C.P. 23400 San Jos&eacute; del Cabo,&nbsp;</span><br /><span style=\"font-size: small; font-family: arial, helvetica, sans-serif; color: #808080;\">Baja California Sur, M&eacute;xico.</span></p>\n" +
                "<table style=\"width: 441px;\" border=\"0\">\n" +
                "    <tbody>\n" +
                "        <tr>\n" +
                "            <td width=\"37\"><a href=\"http://www.twitter.com/grupovidanta\" target=\"_blank\"><img src=\"cid:twitterIcon\" alt=\"\" width=\"29\" height=\"35\" /></a></td>\n" +
                "            <td width=\"30\"><a href=\"http://facebook.com/grupovidanta\" target=\"_blank\"><img src=\"cid:facebookIcon\" alt=\"\" width=\"27\" height=\"35\" /></a></td>\n" +
                "            <td width=\"35\"><a href=\"http://instagram.com/grupovidanta\" target=\"_blank\"><img src=\"cid:instagramIcon\" alt=\"\" width=\"30\" height=\"35\" /></a></td>\n" +
                "            <td width=\"12\"><img src=\"cid:separador\" alt=\"\" width=\"5\" height=\"35\" /></td>\n" +
                "            <td width=\"305\"><span style=\"font-size: x-small; font-family: arial, helvetica, sans-serif; color: #808080;\"><a href=\"http://www.grupovidanta.com/\" target=\"_blank\"><span style=\"color: #808080;\">grupovidanta.com</span></a></span></td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table>";

        switch(tipoDeCorreo){
            case 1:
                cuerpoFinal = textoCuerpoIntroduccion + cuerpoCorreoFacturas + textoCuerpoTotal + textoCuerpoFinal + disculpaCorreo + firmaDeCorreo;
            break;
            
            case 2:
                cuerpoFinal = "Ha ocurrido un error con los siguiente proveedores: <br><br>" + erroresDeEnvio + "<br>" + firmaDeCorreo;
            break;
        }
        
        messageBodyPart.setContent(cuerpoFinal, "text/html");
        multipart.addBodyPart(messageBodyPart);
           
        messageBodyPart = new MimeBodyPart(); 
        DataSource twitterIcon = new FileDataSource("C:\\Program Files\\AvisoPagos\\iconos\\twitterIcon.jpg");
        messageBodyPart.setDataHandler(new DataHandler(twitterIcon));
        messageBodyPart.setHeader("Content-ID","<twitterIcon>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart(); 
        DataSource facebookIcon = new FileDataSource("C:\\Program Files\\AvisoPagos\\iconos\\facebookIcon.jpg");
        messageBodyPart.setDataHandler(new DataHandler(facebookIcon));
        messageBodyPart.setHeader("Content-ID","<facebookIcon>");
        multipart.addBodyPart(messageBodyPart);

        messageBodyPart = new MimeBodyPart(); 
        DataSource instagramIcon = new FileDataSource("C:\\Program Files\\AvisoPagos\\iconos\\instagramIcon.jpg");
        messageBodyPart.setDataHandler(new DataHandler(instagramIcon));
        messageBodyPart.setHeader("Content-ID","<instagramIcon>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart(); 
        DataSource separador = new FileDataSource("C:\\Program Files\\AvisoPagos\\iconos\\separador.jpg");
        messageBodyPart.setDataHandler(new DataHandler(separador));
        messageBodyPart.setHeader("Content-ID","<separador>");
        multipart.addBodyPart(messageBodyPart);
        
        // put everything together
        message.setContent(multipart);
         // Send message
        
        Transport.send(message);
        
        System.out.println("Correo Enviado");

          if(!"".equals(erroresDeEnvio)){
          try {
              PrintWriter writer = new PrintWriter("C:\\Users\\"+host+"\\Documents\\ConfiguracionCXP\\Errores.txt", "UTF-8");
              writer.println(erroresDeEnvio);
              writer.close();
          } catch (FileNotFoundException ex) {     
          } catch (UnsupportedEncodingException ex) {
          }
        }
        } catch (MessagingException e) {
            erroresDeEnvio = erroresDeEnvio + "Proveedor: " + Nombre + "\n";
        }

    }

    //Actualiza el número de filas original con el número de filas nuevas
    private void ActualizarNumeroDeFilas(int filasNuevas){
    String ip;
    String basedatos;
    String usuario;
    String password;
    String correoerrores;
    
    ip = config.getIp();
    basedatos = config.getBasedatos();
    usuario = config.getUsuario();
    password = config.getPassword();
    correoerrores = config.getCorreoerrores();
    
    JsonFactory factory = new JsonFactory();
    String host = configLectura.getHost();

    try {
        try (JsonGenerator generator = factory.createJsonGenerator(new File("C:\\Users\\"+host+"\\Documents\\ConfiguracionCXP\\Configuracion.json"), JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField("ip", ip);
            generator.writeStringField("basedatos", basedatos);
            generator.writeStringField("usuario", usuario);
            generator.writeStringField("password", password);
            generator.writeStringField("filas", String.valueOf(filasNuevas));
            generator.writeStringField("correoerrores", correoerrores);
            generator.writeEndObject();
        }
    } catch (IOException ex) {
        erroresDeEnvio = erroresDeEnvio + "Error al actualizar el número de filas";
    }
    }
    
    //main
    public static void main(String[] args) {
        AvisoDePagos mainAviso = new AvisoDePagos();
    } 
}
